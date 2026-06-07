package com.pitlite.module.impl.render;

import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.PitMapManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class PlayerCounter extends Module implements DraggableHud {

    private static final double DEFAULT_X = 2.0;
    private static final double DEFAULT_Y = 2.0;
    private static final int MAX_TAB = 81;

    private final BooleanSetting extraInfo = new BooleanSetting("Extra Info", false);
    private final BooleanSetting middleCounter = new BooleanSetting("Mid Player", false);
    private final BooleanSetting sewerCounter = new BooleanSetting("Sewer Player", false);

    public PlayerCounter() {
        super("Player", "Shows how many players are in the lobby.", Category.RENDER);
        addSettings(extraInfo, middleCounter, sewerCounter);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.CHAT) {
            return;
        }
        if (mc.getNetHandler() == null) {
            return;
        }

        int current = mc.getNetHandler().getPlayerInfoMap().size();
        String text = "Player: " + current + (extraInfo.enabled ? "/" + MAX_TAB : "");

        float ratio = (float) current / MAX_TAB;
        int color;
        if (ratio < 0.5f) {
            color = new Color(85, 255, 85).getRGB();
        } else if (ratio < 0.85f) {
            color = new Color(255, 255, 85).getRGB();
        } else {
            color = new Color(255, 85, 85).getRGB();
        }

        int posX = getRenderX();
        int posY = getRenderY();
        int line = 0;

        GlStateManager.pushMatrix();
        mc.fontRendererObj.drawStringWithShadow(text, posX, posY, color);
        line += 10;

        if (middleCounter.enabled) {
            int middleCount = countPlayersInZone("Pit");
            int middleColor = middleCount <= 10 ? 0xFF55FF55 : (middleCount <= 15 ? 0xFFFFAA00 : 0xFFFF5555);
            mc.fontRendererObj.drawStringWithShadow("Mid Player: " + middleCount, posX, posY + line, middleColor);
            line += 10;
        }

        if (sewerCounter.enabled && PitMapManager.getCurrentMap() == PitMapManager.PitMap.CASTLE) {
            int sewerCount = countPlayersInZone("Sewer");
            int sewerColor = sewerCount <= 5 ? 0xFF55FF55 : (sewerCount <= 10 ? 0xFFFFAA00 : 0xFFFF5555);
            mc.fontRendererObj.drawStringWithShadow("Sewer Player: " + sewerCount, posX, posY + line, sewerColor);
        }

        GlStateManager.popMatrix();
    }

    private int countPlayersInZone(String zone) {
        int count = 0;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) {
                continue;
            }
            if (PitMapManager.getZone(player.posX, player.posY, player.posZ).equals(zone)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getHudKey() {
        return "player_counter";
    }

    @Override
    public boolean isHudVisible() {
        return isToggled() && mc.theWorld != null;
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
        return Math.max(mc.fontRendererObj.getStringWidth("Player: 81/81"), 90);
    }

    @Override
    public int getHudHeight() {
        int lines = 1;
        if (middleCounter.enabled) {
            lines++;
        }
        if (sewerCounter.enabled) {
            lines++;
        }
        return lines * 10;
    }
}
