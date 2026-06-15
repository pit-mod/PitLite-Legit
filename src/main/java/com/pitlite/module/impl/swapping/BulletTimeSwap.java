package com.pitlite.module.impl.swapping;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.FriendManager;
import com.pitlite.utils.InventoryUtils;
import com.pitlite.utils.PitMapManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BulletTimeSwap extends Module {
    private final BooleanSetting autoMode = new BooleanSetting("Auto (Arrow Detection)", false);
    private final NumberSetting hitThreshold = new NumberSetting("Hit Radius (blocks)", 1.5, 0.5, 4.0, 1);
    private final BooleanSetting bowPredict = new BooleanSetting("Bow Predict", false);
    private final NumberSetting predictRange = new NumberSetting("Predict Range", 15.0, 0.0, 30.0, 1);
    private final NumberSetting bowChargePercent = new NumberSetting("Charge% Swap", 90.0, 50.0, 100.0, 0);
    private final BooleanSetting mlbHoldSwap = new BooleanSetting("MLB Hold Swap", false);
    private final NumberSetting mlbHoldRange = new NumberSetting("MLB Hold Range", 15.0, 0.0, 30.0, 1);
    private final NumberSetting mlbMaxHoldTime = new NumberSetting("MLB Max Hold (s)", 2.0, 0.5, 10.0, 1);
    private final NumberSetting bowMaxHoldTime = new NumberSetting("Bow Max Draw (s)", 2.0, 0.5, 10.0, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting dontSwapWhenCharging = new BooleanSetting("Dont swap when charging", false);

    private int previousSlot = -1;
    private final Map<UUID, Long> mlbHoldStart = new HashMap<>();

    private boolean autoBlocking = false;
    private int autoPreSlot = -1;

    public BulletTimeSwap() {
        super("BulletTimeSwap", "Swaps to Bullet Time sword to block incoming arrows.", Category.SWAPPING);
        markDangerous();
        addSettings(autoMode, hitThreshold, bowPredict, predictRange, bowChargePercent,
                mlbHoldSwap, mlbHoldRange, mlbMaxHoldTime, bowMaxHoldTime,
                ignoreFriends, dontSwapWhenCharging);
    }

    public boolean isAutoModeEnabled() {
        return autoMode.enabled;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseManual();
        releaseAuto();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        if (autoMode.enabled) {
            tickAuto();
        } else if (autoBlocking) {
            releaseAuto();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (autoMode.enabled) {
            tickAuto();
        }
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (!isToggled()) return;
        if (autoMode.enabled || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (useKey < 0 && event.button == useKey + 100) {
            handleManualSwap(event.buttonstate);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!isToggled()) return;
        if (autoMode.enabled || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (useKey >= 0 && Keyboard.getEventKey() == useKey) {
            handleManualSwap(Keyboard.getEventKeyState());
        }
    }

    private void handleManualSwap(boolean isPressed) {
        if (isPressed) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemSword)) return;

            int currentSlot = mc.thePlayer.inventory.currentItem;
            int btSlot = findBulletTimeSlot();
            if (btSlot != -1 && currentSlot != btSlot) {
                previousSlot = currentSlot;
                mc.thePlayer.inventory.currentItem = btSlot;
            }
        } else {
            int btSlot = findBulletTimeSlot();
            int targetSlot = findMainSwordSlot(btSlot);
            if (targetSlot != -1) {
                mc.thePlayer.inventory.currentItem = targetSlot;
            } else if (previousSlot != -1) {
                mc.thePlayer.inventory.currentItem = previousSlot;
            }
            previousSlot = -1;
        }
    }

    private void tickAuto() {
        if (PitMapManager.isInSpawn(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) {
            if (autoBlocking) releaseAuto();
            return;
        }

        boolean threatDetected = (bowPredict.enabled && checkBowPredict()) || findIncomingArrow() != null;

        if (dontSwapWhenCharging.enabled && mc.thePlayer.isUsingItem()
                && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            if (autoBlocking) releaseAuto();
            return;
        }

        if (threatDetected) {
            if (!autoBlocking) {
                int btSlot = findBulletTimeSlot();
                if (btSlot != -1) {
                    autoPreSlot = mc.thePlayer.inventory.currentItem;
                    ItemStack heldItem = mc.thePlayer.getHeldItem();
                    if (heldItem == null || !(heldItem.getItem() instanceof ItemSword)) {
                        int mainSwordSlot = findMainSwordSlot(btSlot);
                        if (mainSwordSlot != -1) {
                            mc.thePlayer.inventory.currentItem = mainSwordSlot;
                            startBlocking();
                        }
                    }

                    mc.thePlayer.inventory.currentItem = btSlot;
                    startBlocking();
                    autoBlocking = true;
                }
            } else {
                startBlocking();
            }
        } else if (autoBlocking) {
            releaseAuto();
        }
    }

    private EntityArrow findIncomingArrow() {
        Vec3 playerPos = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.eyeHeight * 0.5,
                mc.thePlayer.posZ
        );
        double threshold = hitThreshold.value;

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityArrow)) continue;
            EntityArrow arrow = (EntityArrow) obj;

            if (arrow.ticksExisted > 20) continue;

            Entity shooter = arrow.shootingEntity;
            if (shooter != null) {
                if (shooter.getEntityId() == mc.thePlayer.getEntityId()) continue;
                if (PitMapManager.isInSpawn(shooter.posX, shooter.posY, shooter.posZ)) continue;
                if (ignoreFriends.enabled && shooter instanceof EntityPlayer && FriendManager.isFriend((EntityPlayer) shooter))
                    continue;
            }

            double speed = Math.sqrt(
                    arrow.motionX * arrow.motionX +
                            arrow.motionY * arrow.motionY +
                            arrow.motionZ * arrow.motionZ
            );
            if (speed < 0.05) continue;

            Vec3 arrowPos = new Vec3(arrow.posX, arrow.posY, arrow.posZ);
            Vec3 vel = new Vec3(arrow.motionX, arrow.motionY, arrow.motionZ);
            Vec3 toPlayer = playerPos.subtract(arrowPos);

            double velDot = vel.dotProduct(vel);
            if (velDot == 0) continue;
            double t = toPlayer.dotProduct(vel) / velDot;

            if (t < 0 || t > 60) continue;

            Vec3 closest = arrowPos.addVector(vel.xCoord * t, vel.yCoord * t, vel.zCoord * t);
            if (closest.distanceTo(playerPos) <= threshold) return arrow;
        }
        return null;
    }

    private boolean checkBowPredict() {
        double chargeMaxDist = predictRange.value;
        double holdMaxDist = mlbHoldRange.value;
        Vec3 playerPos = mc.thePlayer.getPositionEyes(1.0f);
        Set<UUID> holdingMLB = new HashSet<>();
        boolean foundThreat = false;

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer || entity.isDead) continue;
            if (PitMapManager.isInSpawn(entity.posX, entity.posY, entity.posZ)) continue;
            if (ignoreFriends.enabled && FriendManager.isFriend(entity)) continue;

            ItemStack heldItem = entity.getHeldItem();
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBow)) continue;
            double dist = mc.thePlayer.getDistanceToEntity(entity);
            boolean isMegaLongbow = InventoryUtils.hasLore(heldItem, "Mega Longbow");
            boolean meetsChargeCondition = false;

            if (isMegaLongbow && mlbHoldSwap.enabled) {
                holdingMLB.add(entity.getUniqueID());
                if (!entity.isUsingItem()) {
                    long currentTime = System.currentTimeMillis();
                    long holdStart = mlbHoldStart.containsKey(entity.getUniqueID())
                            ? mlbHoldStart.get(entity.getUniqueID())
                            : currentTime;
                    mlbHoldStart.put(entity.getUniqueID(), holdStart);
                    if ((currentTime - holdStart) <= mlbMaxHoldTime.value * 1000.0) {
                        if (dist <= holdMaxDist) meetsChargeCondition = true;
                    }
                }
            }

            if (entity.isUsingItem()) {
                if (dist <= chargeMaxDist) {
                    int requiredTicks = (int) (20 * (bowChargePercent.value / 100.0));
                    int chargeThreshold = isMegaLongbow ? 0 : requiredTicks;
                    int useDuration = entity.getItemInUseDuration();
                    int maxTicks = (int) (bowMaxHoldTime.value * 20.0);
                    if (useDuration >= chargeThreshold && useDuration <= maxTicks) {
                        meetsChargeCondition = true;
                    }
                }
            }

            if (meetsChargeCondition) {
                Vec3 enemyLook = entity.getLookVec();
                Vec3 enemyPos = entity.getPositionEyes(1.0f);
                Vec3 toUs = playerPos.subtract(enemyPos).normalize();
                if (enemyLook.dotProduct(toUs) > 0.8) {
                    foundThreat = true;
                }
            }
        }

        mlbHoldStart.keySet().retainAll(holdingMLB);
        return foundThreat;
    }

    private void releaseManual() {
        if (previousSlot != -1 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = previousSlot;
        }
        previousSlot = -1;
    }

    private void releaseAuto() {
        if (mc.thePlayer != null) {
            int btSlot = findBulletTimeSlot();
            int targetSlot = findMainSwordSlot(btSlot);
            if (targetSlot != -1) {
                mc.thePlayer.inventory.currentItem = targetSlot;
            } else if (autoPreSlot != -1) {
                mc.thePlayer.inventory.currentItem = autoPreSlot;
            }
        }
        stopBlocking();
        autoPreSlot = -1;
        autoBlocking = false;
    }

    private int findBulletTimeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack == null || !(stack.getItem() instanceof ItemSword)) continue;
            if (InventoryUtils.hasLore(stack, "Bullet Time")) return i;
        }
        return -1;
    }

    private int findMainSwordSlot(int excludeSlot) {
        for (int i = 0; i < 9; i++) {
            if (i == excludeSlot) continue;
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemSword) return i;
        }
        return -1;
    }

    private void startBlocking() {
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
    }

    private void stopBlocking() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
    }
}
