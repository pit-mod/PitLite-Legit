package com.pitlite.gui;

import com.pitlite.settings.ColorSetting;
import com.pitlite.utils.RenderUtils;

import java.awt.Color;

public class ColorComponent extends Component {
    private final ColorSetting colorSet;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.colorSet = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG);
        RenderUtils.drawHLine(x, x + width, y + height - 1, GuiTheme.SEPARATOR);

        Color c = colorSet.getColor();
        String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        drawSmartText(colorSet.name + ": " + hex, x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, GuiTheme.SETTINGS_TEXT, 16);

        int bx2 = x + width - 4;
        int bx1 = bx2 - 12;
        int by1 = y + 2;
        int by2 = y + height - 2;
        RenderUtils.drawRect(bx1, by1, bx2, by2, c.getRGB());
        RenderUtils.drawOutline(bx1, by1, bx2, by2, GuiTheme.BORDER);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isHovered(mouseX, mouseY)) return;

        if (mouseButton == 2) {
            colorSet.setColor(colorSet.getDefaultColor());
            return;
        }

        int channelAreaStart = x + width - 54; // last ~50 px split into RGB zones
        int rel = Math.max(0, mouseX - channelAreaStart);
        int zone = Math.min(2, rel / 18); // 0=R,1=G,2=B
        int step = mouseButton == 0 ? 8 : (mouseButton == 1 ? -8 : 0);
        if (step == 0) return;

        Color c = colorSet.getColor();
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        if (zone == 0) r = clamp255(r + step);
        else if (zone == 1) g = clamp255(g + step);
        else b = clamp255(b + step);

        colorSet.setColor(new Color(r, g, b));
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
