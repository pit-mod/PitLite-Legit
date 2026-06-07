package com.pitlite.gui;

import com.pitlite.settings.ButtonSetting;
import com.pitlite.utils.RenderUtils;

public class ButtonComponent extends Component {
    private final ButtonSetting setting;

    public ButtonComponent(ButtonSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG);
        RenderUtils.drawHLine(x, x + width, y + height - 1, GuiTheme.SEPARATOR);
        drawSmartText(setting.name, x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, hovered ? GuiTheme.ACCENT : GuiTheme.SETTINGS_TEXT, 4);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            setting.run();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}
