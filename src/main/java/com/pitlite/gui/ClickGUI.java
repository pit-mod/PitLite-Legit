package com.pitlite.gui;

import com.pitlite.PitLite;
import com.pitlite.gui.physics.GuiAnimationController;
import com.pitlite.gui.physics.GuiMath;
import com.pitlite.gui.physics.GuiScissor;
import com.pitlite.gui.physics.PhysicsSpring;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends GuiScreen {
    public List<Frame> frames = new ArrayList<>();
    private boolean initialized = false;

    private static final int MOVE_BTN_W = 110;
    private static final int MOVE_BTN_H = 18;
    private static final int MIN_FRAME_WIDTH = 120;
    private static final int FRAME_ARROW_RESERVE = 14;
    private static final float BACKDROP_ALPHA = 0.62f;

    private final GuiAnimationController anim = new GuiAnimationController();

    private String searchQuery = "";
    private boolean typingSearch = false;
    private final TextInputHelper searchEditor = new TextInputHelper();
    private Module hoveredModule = null;
    private long hoveredSinceMs = 0L;

    public ClickGUI() {
    }

    private void initFrames() {
        if (initialized) return;
        initialized = true;

        frames.clear();
        Category[] categories = Category.values();
        int gap = 4;
        int totalWidth = 0;
        for (Category category : categories) {
            List<Module> mods = PitLite.moduleManager.getModulesByCategory(category);
            if (mods.isEmpty()) continue;
            totalWidth += computeFrameWidth(category) + gap;
        }
        if (totalWidth > 0) {
            totalWidth -= gap;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int startX = Math.max(4, (sr.getScaledWidth() - totalWidth) / 2);

        int xOffset = startX;
        int frameIndex = 0;
        for (Category category : categories) {
            List<Module> mods = PitLite.moduleManager.getModulesByCategory(category);
            if (mods.isEmpty()) continue;

            int frameWidth = computeFrameWidth(category);
            Frame frame = new Frame(category, frameIndex++, xOffset, 20, frameWidth, 15);
            for (Module m : mods) {
                frame.buttons.add(new ModuleButton(m, category.name, 0, 0, frameWidth, 15));
            }
            frames.add(frame);
            xOffset += frameWidth + gap;
        }
    }

    private int computeFrameWidth(Category category) {
        int categoryText = fontRendererObj.getStringWidth(category.name);
        return Math.max(MIN_FRAME_WIDTH, categoryText + GuiTheme.PADDING_X + FRAME_ARROW_RESERVE + 4);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!initialized) {
            initFrames();
        }
        anim.onGuiOpened();
        GuiAnimationController.bind(anim);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        anim.beginFrame();

        drawBackdrop();

        int scrollOffset = (int) anim.tickScroll(getMaxScroll());
        int contentMouseY = mouseY + scrollOffset;

        int contentTop = searchY() + searchH() + 4;
        int contentBottom = moveButtonY() - 4;
        int contentH = Math.max(0, contentBottom - contentTop);

        GlStateManager.pushMatrix();
        if (contentH > 0) {
            GuiScissor.enable(0, contentTop, width, contentH);
        }
        GlStateManager.translate(0.0f, -scrollOffset, 0.0f);
        for (Frame frame : frames) {
            frame.updatePosition(mouseX, contentMouseY);
            frame.renderFrame(mouseX, contentMouseY, searchQuery);
        }
        GlStateManager.popMatrix();
        if (contentH > 0) {
            GuiScissor.disable();
        }

        updateHoveredModule(mouseX, contentMouseY);
        drawSearchBar(mouseX, mouseY);
        drawMoveModulesButton(mouseX, mouseY);
        drawModuleDescriptionTooltip(mouseX, mouseY);
    }

    private void drawBackdrop() {
        int a = (int) (255f * BACKDROP_ALPHA);
        RenderUtils.drawRect(0, 0, width, height, (a << 24));
    }

    private void updateHoveredModule(int mouseX, int contentMouseY) {
        Module current = null;
        for (Frame frame : frames) {
            ModuleButton hoveredButton = frame.getHoveredModuleButton(mouseX, contentMouseY, searchQuery);
            if (hoveredButton != null) {
                current = hoveredButton.module;
                break;
            }
        }

        if (current != hoveredModule) {
            hoveredModule = current;
            hoveredSinceMs = System.currentTimeMillis();
        }
    }

    private void drawModuleDescriptionTooltip(int mouseX, int mouseY) {
        boolean show = false;
        String desc = null;
        if (hoveredModule != null && System.currentTimeMillis() - hoveredSinceMs >= 500L) {
            desc = hoveredModule.getDescription();
            show = desc != null && !desc.trim().isEmpty();
        }

        float tooltipT = anim.animateClamped01("gui:tooltip", show ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);
        if (!show || tooltipT < 0.01f || desc == null) {
            return;
        }

        int padding = 4;
        int w = fontRendererObj.getStringWidth(desc) + padding * 2;
        int h = fontRendererObj.FONT_HEIGHT + padding * 2;
        int x = mouseX + 10;
        int y = mouseY + 12 + (int) ((1f - tooltipT) * 6f);

        if (x + w > width - 2) x = width - w - 2;
        if (y + h > height - 2) y = height - h - 2;
        if (x < 2) x = 2;
        if (y < 2) y = 2;

        int bg = GuiFade.tint((0xC8 << 24) | (GuiTheme.PANEL_BG & 0x00FFFFFF), tooltipT);
        RenderUtils.drawRect(x, y, x + w, y + h, bg);
        RenderUtils.drawOutline(x, y, x + w, y + h, GuiFade.tint(GuiTheme.BORDER, tooltipT));
        RenderUtils.drawHLine(x + 1, x + w - 1, y + 1, GuiFade.tint(GuiTheme.ACCENT, tooltipT));
        fontRendererObj.drawStringWithShadow(desc, x + padding, y + padding, GuiFade.tint(GuiTheme.TEXT, tooltipT));
    }

    private int searchW() { return 160; }
    private int searchH() { return 14; }
    private int searchX() { return (width - searchW()) / 2; }
    private int searchY() { return 4; }
    private boolean isSearchHovered(int mouseX, int mouseY) {
        int x = searchX(), y = searchY();
        return mouseX >= x && mouseX <= x + searchW() && mouseY >= y && mouseY <= y + searchH();
    }

    private void drawSearchBar(int mouseX, int mouseY) {
        int x = searchX();
        int y = searchY();
        boolean hovered = isSearchHovered(mouseX, mouseY);
        float focusT = anim.animateClamped01("gui:searchFocus", (typingSearch || hovered) ? 1f : 0f, PhysicsSpring.Preset.IOS_FLUID);

        int bg = GuiMath.lerpColor(GuiTheme.PANEL_BG, GuiTheme.ROW_BG_HOVER, focusT * 0.85f);
        RenderUtils.drawRect(x, y, x + searchW(), y + searchH(), bg);
        RenderUtils.drawOutline(x, y, x + searchW(), y + searchH(), GuiTheme.BORDER);
        RenderUtils.drawHLine(x + 1, x + searchW() - 1, y + 1,
                GuiMath.lerpColor(GuiTheme.ACCENT_MUTED, GuiTheme.ACCENT, focusT));

        String text = (searchQuery == null || searchQuery.isEmpty()) ? "Search modules..." : searchQuery;
        int color = (searchQuery == null || searchQuery.isEmpty()) ? GuiTheme.TEXT_MUTED : GuiTheme.TEXT;
        int textX = x + 6;
        int textY = y + 3;
        if (typingSearch) {
            searchEditor.draw(fontRendererObj, textX, textY, color, true);
        } else {
            fontRendererObj.drawStringWithShadow(text, textX, textY, color);
        }
    }

    private int moveButtonX() {
        return 4;
    }

    private int moveButtonY() {
        return height - MOVE_BTN_H - 4;
    }

    private boolean isMoveButtonHovered(int mouseX, int mouseY) {
        int x = moveButtonX();
        int y = moveButtonY();
        return mouseX >= x && mouseX <= x + MOVE_BTN_W && mouseY >= y && mouseY <= y + MOVE_BTN_H;
    }

    private void drawMoveModulesButton(int mouseX, int mouseY) {
        int x = moveButtonX();
        int y = moveButtonY();
        boolean hovered = isMoveButtonHovered(mouseX, mouseY);
        int bg = hovered ? GuiTheme.ROW_BG_HOVER : GuiTheme.PANEL_BG;
        RenderUtils.drawRect(x, y, x + MOVE_BTN_W, y + MOVE_BTN_H, bg);
        RenderUtils.drawOutline(x, y, x + MOVE_BTN_W, y + MOVE_BTN_H, GuiTheme.BORDER);
        RenderUtils.drawHLine(x + 1, x + MOVE_BTN_W - 1, y + 1, GuiTheme.ACCENT);
        fontRendererObj.drawStringWithShadow("Move Modules", x + 6, y + 5, GuiTheme.TEXT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isMoveButtonHovered(mouseX, mouseY)) {
            mc.displayGuiScreen(new CustomGuiChat(""));
            return;
        }

        if (mouseButton == 0 && isSearchHovered(mouseX, mouseY)) {
            InputComponent.clearFocus();
            if (!typingSearch) {
                searchEditor.setText(searchQuery == null ? "" : searchQuery);
            }
            typingSearch = true;
            searchEditor.beginMouseSelection(mouseX, searchX() + 6, fontRendererObj);
            syncSearchQuery();
            return;
        } else if (mouseButton == 1 && isSearchHovered(mouseX, mouseY)) {
            anim.setReduceAnimations(!anim.isReduceAnimations());
            com.pitlite.utils.NotificationManager.show(
                    anim.isReduceAnimations() ? "Reduced motion enabled" : "Reduced motion disabled", 2000);
            return;
        } else if (mouseButton == 0) {
            typingSearch = false;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        int scrollOffset = (int) anim.getScrollY();
        int contentMouseY = mouseY + scrollOffset;
        for (int i = frames.size() - 1; i >= 0; i--) {
            if (frames.get(i).mouseClicked(mouseX, contentMouseY, mouseButton, searchQuery)) {
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (typingSearch && clickedMouseButton == 0) {
            searchEditor.updateMouseSelection(mouseX, searchX() + 6, fontRendererObj);
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        int scrollOffset = (int) anim.getScrollY();
        int contentMouseY = mouseY + scrollOffset;
        for (int i = frames.size() - 1; i >= 0; i--) {
            frames.get(i).mouseClickMove(mouseX, contentMouseY, clickedMouseButton, timeSinceLastClick, searchQuery);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (typingSearch) {
            searchEditor.endMouseSelection();
        }

        super.mouseReleased(mouseX, mouseY, state);
        int scrollOffset = (int) anim.getScrollY();
        int contentMouseY = mouseY + scrollOffset;
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, contentMouseY, state, searchQuery);
        }
    }

    @Override
    public void updateScreen() {
        if (typingSearch) {
            searchEditor.tickRepeatKeys();
            syncSearchQuery();
        }
        InputComponent.tickRepeatKeys();
    }

    private void syncSearchQuery() {
        searchQuery = searchEditor.getText();
        anim.setScrollTarget(Math.min(anim.getScrollY(), (float) getMaxScroll()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (typingSearch) {
                typingSearch = false;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }

        if (typingSearch) {
            if (keyCode == Keyboard.KEY_RETURN) {
                typingSearch = false;
                return;
            }
            if (searchEditor.handleKeyTyped(typedChar, keyCode)) {
                syncSearchQuery();
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
        for (Frame frame : frames) {
            frame.keyTyped(typedChar, keyCode, searchQuery);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        anim.applyScrollImpulse(wheel);
    }

    private int getMaxScroll() {
        int contentTop = searchY() + searchH() + 4;
        int contentBottom = moveButtonY() - 4;
        int viewportH = Math.max(0, contentBottom - contentTop);

        int contentBottomY = 0;
        for (Frame frame : frames) {
            int bottom = frame.y + GuiTheme.HEADER_H + frame.getContentHeight(searchQuery);
            contentBottomY = Math.max(contentBottomY, bottom);
        }
        return Math.max(0, contentBottomY - (contentTop + viewportH));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        GuiAnimationController.unbind();
        super.onGuiClosed();
        com.pitlite.utils.ConfigManager.saveConfig();
        com.pitlite.utils.HudPositionManager.save();
    }
}
