package com.pitlite.module.impl.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.HudStackManager;

public class ArmorHUD extends Module implements DraggableHud {
    private static final double DEFAULT_X = 200.0;
    private static final double DEFAULT_Y = 200.0;

    private final BooleanSetting horizontal = new BooleanSetting("Horizontal", true);

    public ArmorHUD() {
        super("Armor HUD", "Displays your armor on the screen.", Category.RENDER);
        addSettings(horizontal);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || event.type != RenderGameOverlayEvent.ElementType.ALL || mc.thePlayer == null) return;

        int x = getRenderX();
        int y = getRenderY();

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
            if (stack == null) {
                if (!horizontal.enabled) y += 18;
                else x += 18;
                continue;
            }
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, y);
            if (horizontal.enabled) x += 18;
            else y += 18;
        }

        RenderHelper.disableStandardItemLighting();
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
        return horizontal.enabled ? 72 : 18;
    }

    @Override
    public int getHudHeight() {
        return horizontal.enabled ? 18 : 72;
    }
}
