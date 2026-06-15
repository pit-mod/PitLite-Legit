package com.pitlite.module.impl.swapping;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.settings.KeybindSetting;
import com.pitlite.utils.NotificationManager;

public class PantSwapper extends Module {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean isSwapping = false;
    private final NumberSetting delay = new NumberSetting("Delay", 100, 0, 500, 0);
    private final BooleanSetting includeBoots = new BooleanSetting("Include Boots", false);
    private final KeybindSetting rightPantBind = new KeybindSetting("Bind for right pant", Keyboard.KEY_NONE);

    private int targetPantIndex = 0;

    public PantSwapper() {
        super("PantSwapper", "Automatically swaps pants and optionally boots in inventory.", Category.SWAPPING);
        markDangerous();
        addSetting(delay);
        addSetting(includeBoots);
        addSetting(rightPantBind);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        isSwapping = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isSwapping = false;
    }

    @Override
    public void onKey(int key) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || isSwapping) return;
        if (key == keybind.code && key != Keyboard.KEY_NONE) {
            targetPantIndex = 0;
            if (isToggled()) {
                setToggled(false);
            }
        } else if (key == rightPantBind.code && key != Keyboard.KEY_NONE) {
            targetPantIndex = 1;
            if (!isToggled()) {
                toggle();
            } else {
                isSwapping = false;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || isSwapping || !isToggled()) {
            return;
        }
        isSwapping = true;
        if (delay.value > 0) {
            scheduler.schedule(this::openInventory, 100L, TimeUnit.MILLISECONDS);
        } else {
            openInventory();
        }
    }

    private void openInventory() {
        mc.addScheduledTask(() -> {
            if (!(mc.currentScreen instanceof GuiInventory)) {
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            }
            if (delay.value > 0) {
                scheduler.schedule(this::swapPants, (long) delay.value, TimeUnit.MILLISECONDS);
            } else {
                swapPants();
            }
        });
    }

    private void swapPants() {
        mc.addScheduledTask(() -> {
            if (mc.currentScreen instanceof GuiInventory) {
                int invIdx = this.findPantsSlot(targetPantIndex);
                if (invIdx != -1) {
                    mc.thePlayer.playSound("random.click", 1.0f, 1.2f);
                    NotificationManager.show("§aSuccessfully Swapped Pants", 3000);
                    if (invIdx < 9) {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, invIdx, 2, mc.thePlayer);
                    } else {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, 0, 0, mc.thePlayer);
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, invIdx, 0, 0, mc.thePlayer);
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, 0, 0, mc.thePlayer);
                    }
                    if (includeBoots.enabled) {
                        if (delay.value > 0) scheduler.schedule(this::swapBoots, (long) delay.value, TimeUnit.MILLISECONDS);
                        else swapBoots();
                    } else {
                        closeInventory();
                        this.toggle();
                    }
                } else {
                    mc.thePlayer.playSound("mob.villager.no", 1.0f, 0.7f);
                    NotificationManager.show("§cNo Leggings Found", 3000);
                    closeInventory();
                    this.toggle();
                }
            } else {
                isSwapping = false;
            }
        });
    }

    private void swapBoots() {
        mc.addScheduledTask(() -> {
            if (mc.currentScreen instanceof GuiInventory) {
                int invIdx = this.findBootsSlot();
                if (invIdx != -1) {
                    mc.thePlayer.playSound("random.click", 1.0f, 1.2f);
                    NotificationManager.show("§aSuccessfully Swapped Boots", 3000);
                    if (invIdx < 9) {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 8, invIdx, 2, mc.thePlayer);
                    } else {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 8, 0, 0, mc.thePlayer);
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, invIdx, 0, 0, mc.thePlayer);
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 8, 0, 0, mc.thePlayer);
                    }
                } else {
                    mc.thePlayer.playSound("mob.villager.no", 1.0f, 0.7f);
                    NotificationManager.show("§cNo Boots Found", 3000);
                }
                closeInventory();
                this.toggle();
            } else {
                isSwapping = false;
            }
        });
    }

    private int findPantsSlot(int indexToFind) {
        int foundCount = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) itemStack.getItem();
                if (armor.armorType == 2) {
                    if (foundCount == indexToFind) return i;
                    foundCount++;
                }
            }
        }
        return -1;
    }

    private int findBootsSlot() {
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) itemStack.getItem();
                if (armor.armorType == 3) return i;
            }
        }
        return -1;
    }

    private void closeInventory() {
        mc.addScheduledTask(() -> {
            mc.thePlayer.closeScreen();
            isSwapping = false;
        });
    }
}


