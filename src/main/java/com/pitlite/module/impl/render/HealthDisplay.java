package com.pitlite.module.impl.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.HudStackManager;

public class HealthDisplay extends Module implements DraggableHud {
    private static final double DEFAULT_X = 430.0;
    private static final double DEFAULT_Y = 285.0;

    private final NumberSetting scale = new NumberSetting("Scale", 2.0, 0.5, 5.0, 1);
    private final BooleanSetting showHearts = new BooleanSetting("Show Hearts", false);

    public HealthDisplay() {
        super("HealthDisplay", "Displays your HP near the crosshair.", Category.RENDER);
        addSettings(scale, showHearts);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.CHAT) return;

        float health = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
        float displayHealth = showHearts.enabled ? health / 2.0f : health;
        String hpString = String.format("%.1f", displayHealth);
        if (hpString.endsWith(".0")) hpString = hpString.substring(0, hpString.length() - 2);
        hpString += " \u2764";

        float posX = getRenderX();
        float posY = getRenderY();
        float scaleFactor = (float) scale.value;

        float ratio = health / mc.thePlayer.getMaxHealth();
        int color = ratio > 0.6f ? 0x55FF55 : (ratio > 0.3f ? 0xFFFF55 : 0xFF5555);

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 0);
        GlStateManager.scale(scaleFactor, scaleFactor, 1.0f);
        mc.fontRendererObj.drawStringWithShadow(hpString, 0, 0, color);
        GlStateManager.popMatrix();
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        return mc.thePlayer != null;
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
        return (int) (mc.fontRendererObj.getStringWidth("24.0 \u2764") * scale.value);
    }

    @Override
    public int getHudHeight() {
        return (int) (mc.fontRendererObj.FONT_HEIGHT * scale.value);
    }
}
