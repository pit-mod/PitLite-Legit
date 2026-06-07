package com.pitlite.gui;

import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.module.Category;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    public Category category;
    public int x, y, width, height;
    public int dragX, dragY;
    public boolean dragging, open;
    public List<ModuleButton> buttons = new ArrayList<>();
    private final String frameKey;

    public Frame(Category category, int frameIndex, int x, int y, int width, int height) {
        this.category = category;
        this.frameKey = category.name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.open = true;
    }

    public List<ModuleButton> getVisibleButtons(String query) {
        List<ModuleButton> out = new ArrayList<>();
        for (ModuleButton b : buttons) {
            if (b.matchesSearch(query)) {
                out.add(b);
            }
        }
        return out;
    }

    private ModuleButton findTopHoveredButton(int mouseX, int mouseY, String query) {
        if (!open || mouseX < x || mouseX > x + width) {
            return null;
        }
        List<ModuleButton> visible = getVisibleButtons(query);
        for (int i = visible.size() - 1; i >= 0; i--) {
            ModuleButton button = visible.get(i);
            if (button.isHovered(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    public ModuleButton getHoveredModuleButton(int mouseX, int mouseY, String query) {
        return findTopHoveredButton(mouseX, mouseY, query);
    }

    public int getContentHeight(String query) {
        if (!open) {
            return 0;
        }
        int total = 0;
        for (ModuleButton button : getVisibleButtons(query)) {
            total += button.getHeight();
        }
        return total;
    }

    public void renderFrame(int mouseX, int mouseY, String query) {
        GuiAnimationController anim = GuiAnimationController.get();

        float categoryOpenT = open ? 1f : 0f;
        if (anim != null) {
            categoryOpenT = anim.animate01Stable("frame:" + frameKey + ":open", open);
        }

        List<ModuleButton> visible = getVisibleButtons(query);
        int fullContentH = 0;
        if (open && !visible.isEmpty()) {
            for (ModuleButton button : visible) {
                fullContentH += button.getHeight();
            }
        }
        int contentH = (int) (fullContentH * categoryOpenT);
        int panelBottom = y + GuiTheme.HEADER_H + contentH;

        RenderUtils.drawRect(x, y, x + width, panelBottom + 1, GuiTheme.PANEL_BG);
        RenderUtils.drawRect(x, y, x + width, y + GuiTheme.HEADER_H, GuiTheme.HEADER_BG);
        RenderUtils.drawOutline(x, y, x + width, panelBottom + 1, GuiTheme.BORDER);
        RenderUtils.drawHLine(x + 1, x + width - 1, y + 1, GuiTheme.ACCENT);
        RenderUtils.drawHLine(x + 1, x + width - 1, y + GuiTheme.HEADER_H - 1, GuiTheme.SEPARATOR);

        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                category.name, x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, GuiTheme.TEXT);

        ExpandChevronRenderer.draw(
                Minecraft.getMinecraft().fontRendererObj,
                x + width - 12,
                y + GuiTheme.PADDING_Y,
                categoryOpenT,
                GuiTheme.ACCENT);

        if (categoryOpenT > 0.001f) {
            ModuleButton rowHoverTarget = findRowHoverTarget(mouseX, mouseY, visible);
            int offset = GuiTheme.HEADER_H;
            for (ModuleButton button : visible) {
                int rowY = y + offset;
                button.updatePosition(x, rowY, width, GuiTheme.ROW_H);
                button.render(mouseX, mouseY, query, button == rowHoverTarget);
                offset += button.getHeight();
            }
        }
    }

    private ModuleButton findRowHoverTarget(int mouseX, int mouseY, List<ModuleButton> visible) {
        for (int i = visible.size() - 1; i >= 0; i--) {
            ModuleButton button = visible.get(i);
            if (button.isRowHovered(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    public void keyTyped(char typedChar, int keyCode, String query) {
        if (open) {
            for (ModuleButton button : getVisibleButtons(query)) {
                if (button.expanded) {
                    button.keyTyped(typedChar, keyCode, query);
                }
            }
        }
    }

    public void updatePosition(int mouseX, int mouseY) {
        if (dragging) {
            this.x = mouseX - dragX;
            this.y = mouseY - dragY;
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, String query) {
        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        if (isHeaderHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                open = !open;
                GuiAnimationController anim = GuiAnimationController.get();
                if (anim != null) {
                    anim.retargetClamped01("frame:" + frameKey + ":open", open ? 1f : 0f);
                }
            }
            return true;
        }

        ModuleButton hit = findTopHoveredButton(mouseX, mouseY, query);
        if (hit != null) {
            hit.mouseClicked(mouseX, mouseY, mouseButton, query);
            return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int state, String query) {
        dragging = false;
        if (!open) {
            return;
        }
        ModuleButton hit = findTopHoveredButton(mouseX, mouseY, query);
        if (hit != null) {
            hit.mouseReleased(mouseX, mouseY, state, query);
        }
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick, String query) {
        if (!open) {
            return;
        }
        ModuleButton hit = findTopHoveredButton(mouseX, mouseY, query);
        if (hit != null) {
            hit.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, query);
        }
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + GuiTheme.HEADER_H;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return isHeaderHovered(mouseX, mouseY);
    }
}
