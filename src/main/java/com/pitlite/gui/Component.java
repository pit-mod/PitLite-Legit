package com.pitlite.gui;

import com.pitlite.settings.Setting;

public abstract class Component {
    public Setting setting;
    public int x, y, width, height;
    
    public Component(Setting setting) {
        this.setting = setting;
    }
    
    public void updatePosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public abstract void render(int mouseX, int mouseY);
    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);
    public abstract void mouseReleased(int mouseX, int mouseY, int state);

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }
    public abstract void keyTyped(char typedChar, int keyCode);
    
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    protected float rowAlpha = 1f;
    protected float rowSlideOffset = 0f;

    public void setRowAnim(float alpha, float slideOffset) {
        this.rowAlpha = alpha;
        this.rowSlideOffset = slideOffset;
    }

    protected void drawSmartText(String text, int drawX, int drawY, int color, int reserveRightPx) {
        int available = Math.max(1, width - reserveRightPx - (drawX - x));
        GuiText.drawFit(text, drawX, drawY, available, alphaColor(color));
    }

    protected int alphaColor(int color) {
        if (rowAlpha >= 0.999f) {
            return color;
        }
        int a = (color >> 24) & 0xFF;
        a = (int) (a * rowAlpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    protected String animPrefix = "";

    public void setAnimPrefix(String prefix) {
        this.animPrefix = prefix == null ? "" : prefix;
    }
}
