package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.GuiScissor;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.module.Module;
import com.pitlite.settings.*;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModuleButton {
    private static final int SETTINGS_INDENT = 6;
    private static final int EXPAND_ARROW_RESERVE = 14;
    private static final long TOGGLE_DEBOUNCE_MS = 200L;

    public Module module;
    public int x, y, width, height;
    public boolean expanded = false;
    public List<Component> components = new ArrayList<>();
    private String animKey;
    private long lastToggleClickMs = 0L;

    public ModuleButton(Module module, String frameKey, int x, int y, int width, int height) {
        this.module = module;
        this.animKey = frameKey + "/" + module.getName();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for (Setting s : module.settings) {
            if ("X Pos".equals(s.name) || "Y Pos".equals(s.name)) {
                continue;
            }
            Component comp = null;
            if (s instanceof BooleanSetting) comp = new BooleanComponent((BooleanSetting) s);
            else if (s instanceof NumberSetting) comp = new SliderComponent((NumberSetting) s);
            else if (s instanceof ModeSetting) comp = new ModeComponent((ModeSetting) s);
            else if (s instanceof ColorSetting) comp = new ColorComponent((ColorSetting) s);
            else if (s instanceof KeybindSetting) comp = new KeybindComponent((KeybindSetting) s);
            else if (s instanceof InputSetting) comp = new InputComponent((InputSetting) s);
            else if (s instanceof ButtonSetting) comp = new ButtonComponent((ButtonSetting) s);
            if (comp != null) {
                comp.setAnimPrefix(animKey + "/" + s.name);
                components.add(comp);
            }
        }
    }

    public void updatePosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        return module.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    public String getAnimKey() {
        return animKey;
    }

    private float getExpandT(GuiAnimationController anim) {
        if (components.isEmpty()) {
            return 0f;
        }
        if (anim == null) {
            return expanded ? 1f : 0f;
        }
        return anim.animateClamped01(animKey + ":expand", expanded ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
    }

    public void render(int mouseX, int mouseY, String query, boolean rowHovered) {
        if (!matchesSearch(query)) {
            return;
        }

        GuiAnimationController anim = GuiAnimationController.get();

        boolean enabled = module.isToggled();
        String label = getDisplayLabel(module);
        int arrowReserve = components.isEmpty() ? GuiTheme.PADDING_X : EXPAND_ARROW_RESERVE;
        float expandT = getExpandT(anim);

        float hoverT = rowHovered ? 1f : 0f;
        if (anim != null) {
            hoverT = anim.animateClamped01(animKey + ":hover", rowHovered ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
        }

        int textInset = ModuleRowRenderer.getTextInsetX(enabled);
        int available = width - textInset - arrowReserve;

        ModuleRowRenderer.drawModuleRow(
                Minecraft.getMinecraft().fontRendererObj,
                label,
                enabled,
                module.isDangerous(),
                hoverT,
                1f,
                x, y, width, height,
                available);

        if (!components.isEmpty()) {
            int arrowColor = enabled ? ModuleRowRenderer.ACCENT_IOS_BLUE : GuiTheme.TEXT_MUTED;
            ExpandChevronRenderer.draw(
                    Minecraft.getMinecraft().fontRendererObj,
                    x + width - 12,
                    y + GuiTheme.PADDING_Y,
                    expandT,
                    arrowColor);
        }

        if (expandT <= 0.001f) {
            return;
        }

        int settingsHeight = (int) (components.size() * GuiTheme.ROW_H * expandT);
        int settingsX = x + 1;
        int settingsY = y + height;
        int settingsW = width - 2;

        RenderUtils.drawRect(settingsX, settingsY, settingsX + settingsW, settingsY + settingsHeight,
                GuiTheme.SETTINGS_CONTAINER_BG);
        RenderUtils.drawOutline(settingsX, settingsY, settingsX + settingsW, settingsY + settingsHeight,
                GuiTheme.SETTINGS_CONTAINER_BORDER);
        RenderUtils.drawHLine(x + 1, x + width - 1, y + height, GuiTheme.ACCENT_MUTED);

        if (expanded) {
            GuiScissor.enable(settingsX, settingsY, settingsW, settingsHeight);
            int offset = height;
            for (Component comp : components) {
                comp.setRowAnim(1f, 0f);
                comp.updatePosition(x + SETTINGS_INDENT, y + offset, width - SETTINGS_INDENT, GuiTheme.ROW_H);
                comp.render(mouseX, mouseY);
                offset += GuiTheme.ROW_H;
            }
            GuiScissor.disable();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, String query) {
        if (!matchesSearch(query)) {
            return;
        }
        if (isRowHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                long now = System.currentTimeMillis();
                if (now - lastToggleClickMs < TOGGLE_DEBOUNCE_MS) {
                    return;
                }
                lastToggleClickMs = now;
                GuiAnimationController anim = GuiAnimationController.get();
                if (anim != null) {
                    anim.removeSpring(animKey + ":enable");
                }
                module.toggle();
            } else if (mouseButton == 1 && !components.isEmpty()) {
                expanded = !expanded;
                GuiAnimationController anim = GuiAnimationController.get();
                if (anim != null) {
                    anim.retargetClamped01(animKey + ":expand", expanded ? 1f : 0f);
                }
            }
            return;
        }

        if (expanded) {
            for (Component comp : components) {
                comp.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state, String query) {
        if (!matchesSearch(query)) {
            return;
        }
        if (expanded) {
            for (Component comp : components) {
                comp.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick, String query) {
        if (!matchesSearch(query)) {
            return;
        }
        if (expanded) {
            for (Component comp : components) {
                comp.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode, String query) {
        if (!matchesSearch(query)) {
            return;
        }
        if (expanded) {
            for (Component comp : components) {
                comp.keyTyped(typedChar, keyCode);
            }
        }
    }

    public boolean isRowHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        if (isRowHovered(mouseX, mouseY)) {
            return true;
        }
        if (expanded) {
            int settingsTop = y + height;
            int settingsBottom = y + getHeight();
            return mouseX >= x && mouseX <= x + width && mouseY >= settingsTop && mouseY < settingsBottom;
        }
        return false;
    }

    public int getHeight() {
        float expandT = expanded ? 1f : 0f;
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null && !components.isEmpty()) {
            expandT = anim.getValue(animKey + ":expand");
        }
        return height + (int) (components.size() * GuiTheme.ROW_H * expandT);
    }

    public static int measureRequiredRowWidth(Module module, int baseWidth) {
        return baseWidth;
    }

    public static String getDisplayLabel(Module module) {
        return module.getName();
    }

    public static int measureLabelWidth(Module module) {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(getDisplayLabel(module));
    }
}
