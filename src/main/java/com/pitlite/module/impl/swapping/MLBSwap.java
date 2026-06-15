package com.pitlite.module.impl.swapping;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.MouseEvent;
import org.lwjgl.input.Keyboard;
import com.pitlite.PitLite;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.NumberSetting;

public class MLBSwap extends Module {

    private final NumberSetting chargeTicks = new NumberSetting("Charge Ticks", 3.0, 1.0, 20.0, 0);
    private final NumberSetting swapDelay = new NumberSetting("Swap Back Delay", 1.0, 0.0, 10.0, 0);

    private boolean isActive = false;
    private boolean isWaitingToSwapBack = false;

    private int previousSlot = -1;
    private int mlbTargetSlot = -1;
    private int ticksSinceRelease = 0;
    private long lastSwapTime = 0L;

    public MLBSwap() {
        super("MLBSwap", "Swaps to Mega Longbow, shoots, and swaps back.", Category.SWAPPING);
        markDangerous();
        addSetting(chargeTicks);
        addSetting(swapDelay);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        endSwap();
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
            return;

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (useKey < 0 && event.button == useKey + 100) {
            if (event.buttonstate) { // pressed
                tryStartSwap();
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
            return;

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (useKey >= 0 && Keyboard.getEventKey() == useKey) {
            if (Keyboard.getEventKeyState()) {
                tryStartSwap();
            }
        }
    }

    private void tryStartSwap() {
        if (isActive || isWaitingToSwapBack) return;

        if (System.currentTimeMillis() - lastSwapTime < 470L) return;
        
        if (!mc.thePlayer.inventory.hasItem(net.minecraft.init.Items.arrow)) return;

        if (PitLite.moduleManager != null) {
            for (Module m : PitLite.moduleManager.getModules()) {
                if (m instanceof BulletTimeSwap && m.isToggled()) {
                    BulletTimeSwap bts = (BulletTimeSwap) m;
                    if (!bts.isAutoModeEnabled()) {
                        return; // Let BulletTimeSwap handle the swap
                    }
                }
            }
        }

        ItemStack held = mc.thePlayer.getHeldItem();
        // Only activate when holding a sword
        if (held == null || !(held.getItem() instanceof ItemSword)) {
            return;
        }

        mlbTargetSlot = findMLBSlot();
        if (mlbTargetSlot != -1 && mc.thePlayer.inventory.currentItem != mlbTargetSlot) {
            doMLBSwap();
        }
    }

    private void doMLBSwap() {
        previousSlot = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = mlbTargetSlot;
        isActive = true;
        lastSwapTime = System.currentTimeMillis();
        
        // Force start using the item (charging bow)
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Stage 2: Charging the MLB
        if (isActive) {
            ItemStack held = mc.thePlayer.getHeldItem();
            // Stop if the item in hand is no longer the Mega Longbow
            if (held == null || !hasLore(held, "Mega Longbow")) {
                endSwap();
                return;
            }

            int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
            // Force right click down to charge
            KeyBinding.setKeyBindState(keyCode, true);

            if (mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse() != null && mc.thePlayer.getItemInUse().getItem() instanceof ItemBow) {
                int duration = mc.thePlayer.getItemInUseDuration();
                if (duration >= (int) chargeTicks.value) {
                    // Stage 3: Release to shoot
                    KeyBinding.setKeyBindState(keyCode, false);
                    isActive = false;
                    isWaitingToSwapBack = true;
                    ticksSinceRelease = 0;
                }
            } else if (!mc.thePlayer.isUsingItem()) {
                // Keep calling onTick to start use if it hasn't
                KeyBinding.onTick(keyCode);
            }
        } 
        
        // Stage 4: Wait out the release delay, swap back to sword and restore pants
        else if (isWaitingToSwapBack) {
            ticksSinceRelease++;
            if (ticksSinceRelease >= (int) swapDelay.value) {
                // Swap back to sword
                if (previousSlot != -1) {
                    mc.thePlayer.inventory.currentItem = previousSlot;
                    previousSlot = -1;
                }
                isWaitingToSwapBack = false;
            }
        }
    }

    private void endSwap() {
        isActive = false;
        isWaitingToSwapBack = false;

        if (previousSlot != -1 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = previousSlot;
        }
        previousSlot = -1;
    }

    private int findMLBSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack == null || !(stack.getItem() instanceof ItemBow)) continue;
            if (hasLore(stack, "Mega Longbow")) return i;
        }
        return -1;
    }

    private boolean hasLore(ItemStack stack, String text) {
        if (!stack.hasTagCompound()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("display")) return false;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (!display.hasKey("Lore")) return false;
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int i = 0; i < lore.tagCount(); i++) {
            if (lore.getStringTagAt(i).replaceAll("\u00A7.", "").contains(text)) {
                return true;
            }
        }
        return false;
    }
}
