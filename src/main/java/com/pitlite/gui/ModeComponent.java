package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.GuiMath;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.settings.ModeSetting;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.Minecraft;

public class ModeComponent extends Component {
    private ModeSetting modeSet;

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.modeSet = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, alphaColor(hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG));
        RenderUtils.drawHLine(x, x + width, y + height - 1, alphaColor(GuiTheme.SEPARATOR));

        float highlight = hovered ? 1f : 0f;
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null) {
            highlight = anim.animateClamped01(animPrefix + ":hl", hovered ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
        }

        if (highlight > 0.01f) {
            int hlW = (int) (4 * highlight);
            RenderUtils.drawRect(x + 1, y + 2, x + 1 + hlW, y + height - 2, alphaColor(GuiMath.lerpColor(0, GuiTheme.ACCENT, highlight)));
        }

        drawSmartText(modeSet.name + ": " + modeSet.getMode(), x + GuiTheme.PADDING_X + (int) rowSlideOffset, y + GuiTheme.PADDING_Y, GuiTheme.SETTINGS_TEXT, 14);

        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                ">", x + width - 10, y + GuiTheme.PADDING_Y, alphaColor(GuiTheme.TEXT_MUTED));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            modeSet.cycle();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
