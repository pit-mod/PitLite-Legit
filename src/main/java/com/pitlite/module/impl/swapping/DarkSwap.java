package com.pitlite.module.impl.swapping;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.InventoryUtils;
import com.pitlite.utils.NotificationManager;

import java.util.List;

public class DarkSwap extends Module {

    private final NumberSetting healthThreshold = new NumberSetting("HP Threshold", 10.0, 1.0, 40.0, 1);
    private final NumberSetting swapDelay = new NumberSetting("Swap Delay (ms)", 100, 0, 500, 0);

    private EntityPlayer target = null;
    private long lastHitTime = 0L;
    private static final long TARGET_EXPIRATION = 3000L;

    private boolean hasSwapped = false;
    private SwapState currentState = SwapState.IDLE;
    private int tickDelayCount = 0;

    // Original state tracking
    private int originalItemSlot = -1;
    private int pantsTransitSlot = -1;
    private int pantsOriginalInvSlot = -1;
    private int spadeTransitSlot = -1;
    private int spadeOriginalInvSlot = -1;

    private enum SwapState {
        IDLE,
        PREPARING_OPEN_GUI,
        PREPARING_MOVE_ITEMS,
        SWAPPING_PANTS,
        CLOSING_GUI_PREP,
        SELECTING_SPADE,
        WAITING_FOR_DEATH,
        RESTORING_OPEN_GUI,
        RESTORING_PANTS,
        RESTORING_INVENTORY,
        CLOSING_GUI_FINAL
    }

    public DarkSwap() {
        super("DarkSwap", "Advanced swapping with inventory support and intelligent slot selection.", Category.SWAPPING);
        markDangerous();
        addSettings(healthThreshold, swapDelay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        target = null;
        hasSwapped = false;
        currentState = SwapState.IDLE;
        tickDelayCount = 0;
        originalItemSlot = -1;
        pantsTransitSlot = -1;
        pantsOriginalInvSlot = -1;
        spadeTransitSlot = -1;
        spadeOriginalInvSlot = -1;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer == mc.thePlayer && event.target instanceof EntityPlayer) {
            EntityPlayer attacked = (EntityPlayer) event.target;
            if (attacked != target && currentState == SwapState.IDLE) {
                hasSwapped = false; 
            }
            target = attacked;
            lastHitTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        updateTarget();

        if (hasSwapped && (target == null || target.isDead) && currentState == SwapState.IDLE) {
            if (pantsOriginalInvSlot != -1 || spadeOriginalInvSlot != -1) {
                currentState = SwapState.RESTORING_OPEN_GUI;
                tickDelayCount = 0;
            } else {
                resetState();
            }
            return;
        }

        float hp = target != null ? target.getHealth() + target.getAbsorptionAmount() : 100f;

        if (!hasSwapped && currentState == SwapState.IDLE && target != null && hp <= healthThreshold.value) {
            startSwapSequence();
        }

        if (currentState != SwapState.IDLE) {
            handleSequence();
        }
    }

    private void updateTarget() {
        if (target != null) {
            if (target.isDead || target.getHealth() <= 0 || mc.thePlayer.getDistanceToEntity(target) > 16.0) {
                target = null;
            } else if (System.currentTimeMillis() - lastHitTime > TARGET_EXPIRATION) {
                target = null;
            }
        }
    }

    private void startSwapSequence() {
        int pantsSlot = InventoryUtils.findItem(InventoryUtils::isDarkPants);
        if (pantsSlot == -1) {
            NotificationManager.show("§cNo Dark Pants found!", 2000);
            hasSwapped = true;
            return;
        }

        int spadeSlot = InventoryUtils.findItem(InventoryUtils::isCombatSpade);
        if (spadeSlot == -1) {
            NotificationManager.show("§cNo Combat Spade found!", 2000);
            hasSwapped = true;
            return;
        }

        pantsOriginalInvSlot = pantsSlot;
        spadeOriginalInvSlot = spadeSlot;
        originalItemSlot = mc.thePlayer.inventory.currentItem;

        currentState = SwapState.PREPARING_OPEN_GUI;
        tickDelayCount = 0;
    }

    private void handleSequence() {
        int ticksNeeded = (int) (swapDelay.value / 50.0);
        if (tickDelayCount < ticksNeeded) {
            tickDelayCount++;
            return;
        }

        switch (currentState) {
            case PREPARING_OPEN_GUI:
                if (mc.currentScreen != null) return;
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                currentState = SwapState.PREPARING_MOVE_ITEMS;
                tickDelayCount = 0;
                break;

            case PREPARING_MOVE_ITEMS:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                prepareTransit();
                currentState = SwapState.SWAPPING_PANTS;
                tickDelayCount = 0;
                break;

            case SWAPPING_PANTS:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                if (pantsTransitSlot != -1) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, pantsTransitSlot, 2, mc.thePlayer);
                    NotificationManager.show("§6Swapped to Dark Pants", 2000);
                }
                currentState = SwapState.CLOSING_GUI_PREP;
                tickDelayCount = 0;
                break;

            case CLOSING_GUI_PREP:
                mc.thePlayer.closeScreen();
                currentState = SwapState.SELECTING_SPADE;
                tickDelayCount = 0;
                break;

            case SELECTING_SPADE:
                if (spadeTransitSlot != -1) {
                    mc.thePlayer.inventory.currentItem = spadeTransitSlot;
                    NotificationManager.show("§6Selected Combat Spade", 2000);
                }
                hasSwapped = true;
                currentState = SwapState.IDLE;
                break;

            case RESTORING_OPEN_GUI:
                if (mc.currentScreen != null) return;
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                currentState = SwapState.RESTORING_PANTS;
                tickDelayCount = 0;
                break;

            case RESTORING_PANTS:
                if (!(mc.currentScreen instanceof GuiInventory)) return;
                if (pantsTransitSlot != -1) {
                    mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, 7, pantsTransitSlot, 2, mc.thePlayer);
                    NotificationManager.show("§aRestored Original Pants", 2000);
                }
                currentState = SwapState.RESTORING_INVENTORY;
                tickDelayCount = 0;
                break;

            case RESTORING_INVENTORY:
                restoreFromTransit();
                if (originalItemSlot != -1) mc.thePlayer.inventory.currentItem = originalItemSlot;
                currentState = SwapState.CLOSING_GUI_FINAL;
                tickDelayCount = 0;
                break;

            case CLOSING_GUI_FINAL:
                mc.thePlayer.closeScreen();
                resetState();
                break;
                
            default:
                break;
        }
    }

    private void prepareTransit() {
        List<Integer> bestSlots = InventoryUtils.getBestTransitSlots(2, originalItemSlot);
        int transitIdx = 0;

        // Pants
        if (pantsOriginalInvSlot >= 36) { // Hotbar
            pantsTransitSlot = pantsOriginalInvSlot - 36;
        } else {
            pantsTransitSlot = bestSlots.get(transitIdx++);
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, pantsOriginalInvSlot, pantsTransitSlot, 2, mc.thePlayer);
        }

        // Spade
        if (spadeOriginalInvSlot >= 36) {
            spadeTransitSlot = spadeOriginalInvSlot - 36;
        } else {
            spadeTransitSlot = bestSlots.get(transitIdx++);
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, spadeOriginalInvSlot, spadeTransitSlot, 2, mc.thePlayer);
        }
    }

    private void restoreFromTransit() {
        if (pantsOriginalInvSlot != -1 && pantsOriginalInvSlot < 36) {
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, pantsOriginalInvSlot, pantsTransitSlot, 2, mc.thePlayer);
        }
        if (spadeOriginalInvSlot != -1 && spadeOriginalInvSlot < 36) {
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, spadeOriginalInvSlot, spadeTransitSlot, 2, mc.thePlayer);
        }
        NotificationManager.show("§aGear restored to inventory", 2000);
    }
}
