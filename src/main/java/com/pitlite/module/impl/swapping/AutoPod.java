package com.pitlite.module.impl.swapping;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.InventoryUtils;
import com.pitlite.utils.NotificationManager;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class AutoPod extends Module {

    private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", 4.0, 0.0, 28.0, 1);
    private final NumberSetting delay = new NumberSetting("Delay (ms)", 100.0, 5.0, 1000.0, 0);
    private final BooleanSetting autoSwapBack = new BooleanSetting("Auto Swap Back", true);
    private final NumberSetting swapBackHealth = new NumberSetting("Swap Back Health", 14.0, 1.0, 28.0, 1);

    private SwapState currentState = SwapState.IDLE;
    private long lastActionTime = 0;
    
    private int podTransitSlot = -1;
    private int podOriginalInvSlot = -1;

    private enum SwapState {
        IDLE, OPENING_GUI, PREPARING_TRANSIT, SWAPPING_PANTS, CLOSING_GUI,
        WAITING_FOR_RECOVERY, RESTORING_OPEN_GUI, RESTORING_PANTS, RESTORING_INVENTORY, RESTORING_CLOSING
    }

    public AutoPod() {
        super("AutoPod", "Auto swaps Escape Pod pants.", Category.SWAPPING);
        markDangerous();
        addSetting(healthThreshold);
        addSetting(delay);
        addSetting(autoSwapBack);
        addSetting(swapBackHealth);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.thePlayer != null && mc.thePlayer.getHealth() > healthThreshold.value) {
            int slot = InventoryUtils.findItem(InventoryUtils::isEscapePod);
            if (slot != -1 && !isWearingPod()) {
                podOriginalInvSlot = slot;
                currentState = SwapState.OPENING_GUI;
                lastActionTime = 0;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentState = SwapState.IDLE;
        lastActionTime = 0;
        podTransitSlot = -1;
        podOriginalInvSlot = -1;
    }

    private boolean isWearingPod() {
        if (mc.thePlayer == null) return false;
        ItemStack legs = mc.thePlayer.getEquipmentInSlot(2);
        return InventoryUtils.isEscapePod(legs);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || !isToggled()) return;
        if (System.currentTimeMillis() - lastActionTime < delay.value) return;

        float health = mc.thePlayer.getHealth();

        switch (currentState) {
            case IDLE:
                if (mc.currentScreen != null) return;
                if (health <= healthThreshold.value && !isWearingPod()) {
                    int slot = InventoryUtils.findItem(InventoryUtils::isEscapePod);
                    if (slot != -1) {
                        podOriginalInvSlot = slot;
                        currentState = SwapState.OPENING_GUI;
                        lastActionTime = System.currentTimeMillis();
                    }
                }
                break;
            case OPENING_GUI:
            case RESTORING_OPEN_GUI:
                if (mc.currentScreen != null) return;
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                currentState = (currentState == SwapState.OPENING_GUI) ? SwapState.PREPARING_TRANSIT : SwapState.RESTORING_PANTS;
                lastActionTime = System.currentTimeMillis();
                break;
            case PREPARING_TRANSIT:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                if (podOriginalInvSlot >= 36) {
                    podTransitSlot = podOriginalInvSlot - 36;
                } else {
                    List<Integer> best = InventoryUtils.getBestTransitSlots(1, mc.thePlayer.inventory.currentItem);
                    podTransitSlot = best.get(0);
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, podOriginalInvSlot, podTransitSlot, 2, mc.thePlayer);
                }
                currentState = SwapState.SWAPPING_PANTS;
                lastActionTime = System.currentTimeMillis();
                break;
            case SWAPPING_PANTS:
            case RESTORING_PANTS:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                if (podTransitSlot != -1) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, podTransitSlot, 2, mc.thePlayer);
                    NotificationManager.show(currentState == SwapState.SWAPPING_PANTS ? "§aEscape Pod Equipped!" : "§aOriginal Pants Restored!", 2000);
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
                if (autoSwapBack.enabled && health >= swapBackHealth.value) {
                    if (isWearingPod()) {
                        currentState = SwapState.RESTORING_OPEN_GUI;
                    } else {
                        currentState = SwapState.IDLE;
                    }
                    lastActionTime = System.currentTimeMillis();
                }
                break;
            case RESTORING_INVENTORY:
                if (podOriginalInvSlot != -1 && podOriginalInvSlot < 36) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, podOriginalInvSlot, podTransitSlot, 2, mc.thePlayer);
                }
                currentState = SwapState.RESTORING_CLOSING;
                lastActionTime = System.currentTimeMillis();
                break;
            case RESTORING_CLOSING:
                mc.thePlayer.closeScreen();
                currentState = SwapState.IDLE;
                lastActionTime = System.currentTimeMillis();
                break;
            default:
                break;
        }
    }
}


