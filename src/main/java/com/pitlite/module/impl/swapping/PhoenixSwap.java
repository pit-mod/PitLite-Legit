package com.pitlite.module.impl.swapping;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.potion.Potion;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.InventoryUtils;
import com.pitlite.utils.NotificationManager;
import com.pitlite.utils.PitMapManager;

import java.util.List;

public class PhoenixSwap extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ms)", 100.0, 5.0, 1000.0, 0);
    private final BooleanSetting autoSwapBack = new BooleanSetting("Auto Swap Back", true);
    private final NumberSetting swapBackHealth = new NumberSetting("Swap Back Health", 14.0, 1.0, 28.0, 1);

    private SwapState currentState = SwapState.IDLE;
    private long lastActionTime = 0;
    
    // Tracking slots
    private int phoenixTransitSlot = -1;
    private int phoenixOriginalInvSlot = -1;
    private int originalItemSlot = -1;

    private enum SwapState {
        IDLE, OPENING_GUI, PREPARING_TRANSIT, SWAPPING_PANTS, CLOSING_GUI,
        WAITING_FOR_RECOVERY, RESTORING_OPEN_GUI, RESTORING_PANTS, RESTORING_INVENTORY, RESTORING_CLOSING
    }

    public PhoenixSwap() {
        super("PhoenixSwap", "Manual Phoenix swap with auto-restore when healthy.", Category.SWAPPING);
        markDangerous();
        addSetting(delay);
        addSetting(autoSwapBack);
        addSetting(swapBackHealth);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.thePlayer == null) return;
        if (isWearingPhoenix()) {
            NotificationManager.show("§eAlready wearing Phoenix!", 2000);
            setToggled(false);
            return;
        }
        int slot = InventoryUtils.findItem(InventoryUtils::isPhoenix);
        if (slot != -1) {
            phoenixOriginalInvSlot = slot;
            originalItemSlot = mc.thePlayer.inventory.currentItem;
            currentState = SwapState.OPENING_GUI;
            lastActionTime = 0;
        } else {
            NotificationManager.show("§cNo Phoenix pants found!", 2000);
            toggle();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        currentState = SwapState.IDLE;
        lastActionTime = 0;
        phoenixTransitSlot = -1;
        phoenixOriginalInvSlot = -1;
        originalItemSlot = -1;
    }

    private boolean isWearingPhoenix() {
        if (mc.thePlayer == null) return false;
        ItemStack legs = mc.thePlayer.getEquipmentInSlot(2);
        return InventoryUtils.isPhoenix(legs);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !isToggled()) return;
        if (System.currentTimeMillis() - lastActionTime < delay.value) return;

        if (PitMapManager.isInSpawn(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ) || mc.thePlayer.isDead) {
            if (currentState != SwapState.IDLE && currentState != SwapState.WAITING_FOR_RECOVERY) resetState();
            return;
        }

        if (mc.thePlayer.isPotionActive(Potion.poison) && currentState == SwapState.IDLE) return;
        handleSequence();
    }

    private void handleSequence() {
        float health = mc.thePlayer.getHealth();
        switch (currentState) {
            case IDLE: break;
            case OPENING_GUI:
            case RESTORING_OPEN_GUI:
                if (mc.currentScreen != null) return;
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                currentState = (currentState == SwapState.OPENING_GUI) ? SwapState.PREPARING_TRANSIT : SwapState.RESTORING_PANTS;
                lastActionTime = System.currentTimeMillis();
                break;
            case PREPARING_TRANSIT:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                prepareTransit();
                currentState = SwapState.SWAPPING_PANTS;
                lastActionTime = System.currentTimeMillis();
                break;
            case SWAPPING_PANTS:
            case RESTORING_PANTS:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                if (phoenixTransitSlot != -1) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, phoenixTransitSlot, 2, mc.thePlayer);
                    NotificationManager.show(currentState == SwapState.SWAPPING_PANTS ? "§6Phoenix Equipped!" : "§aOriginal Pants Restored!", 2000);
                }
                currentState = (currentState == SwapState.SWAPPING_PANTS) ? SwapState.CLOSING_GUI : SwapState.RESTORING_INVENTORY;
                lastActionTime = System.currentTimeMillis();
                break;
            case CLOSING_GUI:
                mc.thePlayer.closeScreen();
                currentState = SwapState.WAITING_FOR_RECOVERY;
                lastActionTime = System.currentTimeMillis();
                break;
            case WAITING_FOR_RECOVERY:
                if (autoSwapBack.enabled && health >= (float) swapBackHealth.value) {
                    if (isWearingPhoenix()) currentState = SwapState.RESTORING_OPEN_GUI;
                    else { currentState = SwapState.IDLE; toggle(); }
                    lastActionTime = System.currentTimeMillis();
                }
                break;
            case RESTORING_INVENTORY:
                if (phoenixOriginalInvSlot != -1 && phoenixOriginalInvSlot < 36) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, phoenixOriginalInvSlot, phoenixTransitSlot, 2, mc.thePlayer);
                }
                if (originalItemSlot != -1) mc.thePlayer.inventory.currentItem = originalItemSlot;
                currentState = SwapState.RESTORING_CLOSING;
                lastActionTime = System.currentTimeMillis();
                break;
            case RESTORING_CLOSING:
                mc.thePlayer.closeScreen();
                currentState = SwapState.IDLE;
                lastActionTime = System.currentTimeMillis();
                toggle();
                break;
        }
    }

    private void prepareTransit() {
        if (phoenixOriginalInvSlot >= 36) { 
            phoenixTransitSlot = phoenixOriginalInvSlot - 36;
        } else {
            List<Integer> best = InventoryUtils.getBestTransitSlots(1, originalItemSlot != -1 ? originalItemSlot : mc.thePlayer.inventory.currentItem);
            if (best.isEmpty()) return;
            phoenixTransitSlot = best.get(0);
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, phoenixOriginalInvSlot, phoenixTransitSlot, 2, mc.thePlayer);
        }
    }
}


