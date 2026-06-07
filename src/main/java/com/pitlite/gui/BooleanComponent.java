package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.RenderUtils;

public class BooleanComponent extends Component {
    private BooleanSetting boolSet;

    public BooleanComponent(BooleanSetting setting) {
        super(setting);
        this.boolSet = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        int bg = alphaColor(hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG);
        RenderUtils.drawRect(x, y, x + width, y + height, bg);
        RenderUtils.drawHLine(x, x + width, y + height - 1, alphaColor(GuiTheme.SEPARATOR));

        float knob = boolSet.enabled ? 1f : 0f;
        float press = 1f;
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null) {
            knob = anim.animateClamped01(animPrefix + ":knob", boolSet.enabled ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
        }

        IoSToggleRenderer.draw(
                net.minecraft.client.Minecraft.getMinecraft().fontRendererObj,
                boolSet.name,
                boolSet.enabled,
                knob,
                press,
                x + (int) rowSlideOffset,
                y,
                width,
                height,
                alphaColor(GuiTheme.SETTINGS_TEXT));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            boolSet.toggle();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
