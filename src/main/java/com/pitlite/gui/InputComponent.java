package com.pitlite.gui;

import com.pitlite.settings.InputSetting;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

public class InputComponent extends Component {
    private static InputComponent active;

    private final InputSetting input;
    private final TextInputHelper editor = new TextInputHelper();
    private boolean typing;

    public InputComponent(InputSetting setting) {
        super(setting);
        this.input = setting;
        editor.setText(setting.getContent());
    }

    public static void clearFocus() {
        if (active != null) {
            active.syncToSetting();
            active.typing = false;
            active = null;
        }
    }

    public static boolean isTyping() {
        return active != null && active.typing;
    }

    public static void tickRepeatKeys() {
        if (active != null && active.typing) {
            active.editor.tickRepeatKeys();
            active.syncToSetting();
        }
    }

    private void syncToSetting() {
        input.setContent(editor.getText());
    }

    private void syncFromSetting() {
        editor.setText(input.getContent());
    }

    private String getPrefix() {
        return input.name + ": ";
    }

    private int getValueDrawX(FontRenderer fr) {
        return x + GuiTheme.PADDING_X + fr.getStringWidth(getPrefix());
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG);
        RenderUtils.drawHLine(x, x + width, y + height - 1, GuiTheme.SEPARATOR);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        boolean focused = typing && active == this;
        int prefixColor = focused ? GuiTheme.ACCENT : GuiTheme.SETTINGS_TEXT;
        int valueColor = focused ? GuiTheme.ACCENT : GuiTheme.SETTINGS_TEXT;

        fr.drawStringWithShadow(getPrefix(), x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, prefixColor);

        if (focused) {
            String settingValue = input.getContent();
            if (settingValue == null) {
                settingValue = "";
            }
            if (!settingValue.equals(editor.getText())) {
                editor.setText(settingValue);
            }
            editor.draw(fr, getValueDrawX(fr), y + GuiTheme.PADDING_Y, valueColor, true);
        } else {
            drawSmartText(getPrefix() + input.getContent(), x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, prefixColor, 4);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isHovered(mouseX, mouseY) || mouseButton != 0) {
            return;
        }

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int valueX = getValueDrawX(fr);

        if (active != this) {
            clearFocus();
            active = this;
            typing = true;
            syncFromSetting();
        }

        if (mouseX >= valueX) {
            editor.beginMouseSelection(mouseX, valueX, fr);
        } else {
            editor.selectAll();
            editor.endMouseSelection();
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!typing || active != this || clickedMouseButton != 0) {
            return;
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        editor.updateMouseSelection(mouseX, getValueDrawX(fr), fr);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (typing && active == this) {
            editor.endMouseSelection();
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!typing || active != this) {
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            clearFocus();
            return;
        }
        if (editor.handleKeyTyped(typedChar, keyCode)) {
            syncToSetting();
        }
    }
}
