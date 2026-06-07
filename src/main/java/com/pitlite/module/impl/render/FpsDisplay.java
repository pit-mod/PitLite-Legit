package com.pitlite.module.impl.render;

import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.utils.HudPositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FpsDisplay extends Module implements DraggableHud {

    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 5.0;

    public FpsDisplay() {
        super("Fps", "Displays the current FPS.", Category.RENDER);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.CHAT) {
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        int posX = getRenderX();
        int posY = getRenderY();
        int fps = Minecraft.getDebugFPS();

        String label = "FPS: ";
        String value = String.valueOf(fps);

        GlStateManager.pushMatrix();
        fr.drawStringWithShadow(label, posX, posY, 0xFFFFFF);
        fr.drawStringWithShadow(value, posX + fr.getStringWidth(label), posY, 0x00FF00);
        GlStateManager.popMatrix();
    }

    @Override
    public String getHudKey() {
        return "fps";
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
        return mc.fontRendererObj.getStringWidth("FPS: 999");
    }

    @Override
    public int getHudHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}
