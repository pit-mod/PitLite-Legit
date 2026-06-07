package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.GuiMath;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.settings.KeybindSetting;
import com.pitlite.utils.RenderUtils;
import org.lwjgl.input.Keyboard;

public class KeybindComponent extends Component {
    private KeybindSetting keySet;
    private boolean binding = false;

    public KeybindComponent(KeybindSetting setting) {
        super(setting);
        this.keySet = setting;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        RenderUtils.drawRect(x, y, x + width, y + height, alphaColor(hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.ROW_BG));
        RenderUtils.drawHLine(x, x + width, y + height - 1, alphaColor(GuiTheme.SEPARATOR));

        float pulse = binding ? 1f : 0f;
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null) {
            pulse = anim.animateClamped01(animPrefix + ":pulse", binding ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
        }

        if (pulse > 0.01f) {
            int glow = alphaColor(GuiMath.lerpColor(0, GuiTheme.ACCENT, pulse * 0.35f));
            RenderUtils.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, glow);
        }

        String text = binding
                ? keySet.name + ": ..."
                : keySet.name + ": " + Keyboard.getKeyName(keySet.code);
        int baseColor = binding ? GuiTheme.ACCENT : GuiTheme.SETTINGS_TEXT;
        int color = pulse > 0.01f
                ? GuiMath.lerpColor(baseColor, GuiTheme.ACCENT, pulse * 0.5f)
                : baseColor;
        drawSmartText(text, x + GuiTheme.PADDING_X + (int) rowSlideOffset, y + GuiTheme.PADDING_Y, color, 4);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            binding = !binding;
            if (binding) {
                InputComponent.clearFocus();
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            InputComponent.clearFocus();
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_ESCAPE) {
                keySet.code = Keyboard.KEY_NONE;
            } else {
                keySet.code = keyCode;
            }
            binding = false;
        }
    }
}
