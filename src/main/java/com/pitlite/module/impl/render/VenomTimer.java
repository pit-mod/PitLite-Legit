package com.pitlite.module.impl.render;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.HudStackManager;

public class VenomTimer extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 60.0;

    private static long cooldownEndTime = 0;
    private static EntityPlayer lastHitPlayer = null;
    public static long getCooldownEndTime() { return cooldownEndTime; }
    public static EntityPlayer getLastHitPlayer() { return lastHitPlayer; }

    private boolean wasPoisonedLastTick = false;
    private int lastPoisonDuration = 0;
    private static boolean userIsVenomer = false;
    public static boolean isUserVenomer() { return userIsVenomer; }

    public VenomTimer() {
        super("VenomTimer", "Displays a timer when you get poisoned.", Category.RENDER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        boolean isPoisoned = mc.thePlayer.isPotionActive(Potion.poison);
        int currentDuration = isPoisoned ? mc.thePlayer.getActivePotionEffect(Potion.poison).getDuration() : 0;
        if (isPoisoned && (!wasPoisonedLastTick || currentDuration > lastPoisonDuration)) {
            cooldownEndTime = System.currentTimeMillis() + 12000;
            userIsVenomer = hasVenom(mc.thePlayer);
        }
        if (cooldownEndTime <= System.currentTimeMillis() && !isPoisoned) {
            userIsVenomer = false;
        }
        wasPoisonedLastTick = isPoisoned;
        lastPoisonDuration = currentDuration;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        if (!isToggled()) return;
        float posX = getRenderX();
        float posY = getRenderY();

        if (cooldownEndTime > System.currentTimeMillis() && !hasVenom(mc.thePlayer)) {
            float exactTime = Math.max(0.0f, (cooldownEndTime - System.currentTimeMillis()) / 1000.0f);
            String timeStr = String.format("%.1f", exactTime);
            mc.fontRendererObj.drawStringWithShadow("\u00A72Venom: ", posX, posY, 0xFFFFFF);
            mc.fontRendererObj.drawStringWithShadow(timeStr, posX + mc.fontRendererObj.getStringWidth("Venom: "), posY, 0x00AA00);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isToggled()) return;
        if (event.message.getUnformattedText().contains("DEATH!")) {
            cooldownEndTime = 0;
            wasPoisonedLastTick = false;
            userIsVenomer = false;
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!isToggled()) return;
        if (event.target instanceof EntityPlayer) {
            lastHitPlayer = (EntityPlayer) event.target;
        }
    }

    public static boolean hasVenom(EntityPlayer player) {
        ItemStack legs = player.getEquipmentInSlot(2);
        if (legs == null || !(legs.getItem() instanceof ItemArmor)) return false;
        if (!legs.hasTagCompound()) return false;
        NBTTagCompound tagDisplay = legs.getTagCompound().getCompoundTag("display");
        if (tagDisplay.hasKey("Lore", 9)) {
            NBTTagList lore = tagDisplay.getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) {
                String cleanLine = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
                if (cleanLine.contains("venom")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        return cooldownEndTime > System.currentTimeMillis() && mc.thePlayer != null && !hasVenom(mc.thePlayer);
    }

    @Override
    public int getHudX() {
        return (int) HudPositionManager.getX(getHudKey(), DEFAULT_X);
    }

    @Override
    public int getHudY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public int getHudWidth() {
        return mc.fontRendererObj.getStringWidth("Venom: 25.0");
    }

    @Override
    public int getHudHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}
