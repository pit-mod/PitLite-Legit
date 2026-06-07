package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.RenderUtils;

public class SliderComponent extends Component {
    private NumberSetting numSet;
    private boolean dragging = false;

    public SliderComponent(NumberSetting setting) {
        super(setting);
        this.numSet = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, alphaColor(hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG));
        RenderUtils.drawHLine(x, x + width, y + height - 1, alphaColor(GuiTheme.SEPARATOR));

        if (dragging) {
            double diff = Math.min(width, Math.max(0, mouseX - x));
            double newValue = numSet.min + (diff / width) * (numSet.max - numSet.min);
            numSet.setValue(newValue);
        }

        int barY1 = y + height - 3;
        int barY2 = y + height - 1;
        RenderUtils.drawRect(x + 1, barY1, x + width - 1, barY2, alphaColor(0x80222222));

        double pct = (numSet.value - numSet.min) / (numSet.max - numSet.min);
        float fillT = (float) Math.max(0.0, Math.min(1.0, pct));
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null) {
            fillT = anim.animate(animPrefix + ":fill", fillT, PhysicsSpring.Preset.IOS_FLUID);
        }

        int fillW = (int) ((width - 2) * fillT);
        RenderUtils.drawRect(x + 1, barY1, x + 1 + fillW, barY2, alphaColor(GuiTheme.ACCENT));

        drawSmartText(numSet.name + ": " + numSet.value, x + GuiTheme.PADDING_X + (int) rowSlideOffset, y + GuiTheme.PADDING_Y, GuiTheme.SETTINGS_TEXT, 4);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
