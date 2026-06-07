package com.pitlite.utils;

import com.pitlite.module.impl.player.ClickToView;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatClickViewHelper {

    private static final Pattern SOCIAL_HOVER =
            Pattern.compile("(?i)show social options for\\s+(.+)");
    private static final Pattern MINECRAFT_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    private ChatClickViewHelper() {
    }

    public static void rewriteSocialClicks(IChatComponent component) {
        if (component == null || !ClickToView.isActive()) {
            return;
        }
        applyViewClick(component);
        for (IChatComponent sibling : component.getSiblings()) {
            rewriteSocialClicks(sibling);
        }
    }

    public static boolean tryHandleClick(IChatComponent component) {
        if (component == null || !ClickToView.isActive()) {
            return false;
        }
        if (!isSocialPlayerComponent(component)) {
            return false;
        }
        String username = resolveUsername(component);
        if (username == null) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return false;
        }
        mc.thePlayer.sendChatMessage("/view " + username);
        return true;
    }

    private static void applyViewClick(IChatComponent component) {
        String username = resolveUsername(component);
        if (username == null || !isSocialPlayerComponent(component)) {
            return;
        }
        ChatStyle style = component.getChatStyle().createDeepCopy();
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/view " + username));
        style.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText("\u00a7eClick to view the profile of \u00a7b" + username)));
        component.setChatStyle(style);
    }

    public static boolean isSocialPlayerComponent(IChatComponent component) {
        if (component == null) {
            return false;
        }
        HoverEvent hover = component.getChatStyle().getChatHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            String hoverText = getHoverPlainText(hover);
            if (hoverText.toLowerCase().contains("social options")) {
                return true;
            }
        }
        ClickEvent click = component.getChatStyle().getChatClickEvent();
        if (click != null && click.getValue() != null) {
            String value = click.getValue().toLowerCase();
            if (value.contains("socialoptions") || value.contains("social_options")
                    || value.contains("social options")) {
                return true;
            }
        }
        return false;
    }

    private static String resolveUsername(IChatComponent component) {
        String fromHover = extractFromHover(component.getChatStyle().getChatHoverEvent());
        if (fromHover != null) {
            return fromHover;
        }
        String fromClick = extractFromClick(component.getChatStyle().getChatClickEvent());
        if (fromClick != null) {
            return fromClick;
        }
        String plain = StringUtils.stripControlCodes(component.getUnformattedText()).trim();
        if (MINECRAFT_NAME.matcher(plain).matches()) {
            return plain;
        }
        return null;
    }

    private static String extractFromHover(HoverEvent hover) {
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return null;
        }
        String hoverText = getHoverPlainText(hover);
        Matcher matcher = SOCIAL_HOVER.matcher(hoverText);
        if (matcher.find()) {
            return cleanUsername(matcher.group(1));
        }
        return null;
    }

    private static String extractFromClick(ClickEvent click) {
        if (click == null || click.getValue() == null) {
            return null;
        }
        String value = StringUtils.stripControlCodes(click.getValue()).trim();
        if (value.toLowerCase().startsWith("/socialoptions ")) {
            return cleanUsername(value.substring("/socialoptions ".length()));
        }
        if (value.toLowerCase().startsWith("/socialoptions")) {
            return cleanUsername(value.substring("/socialoptions".length()).trim());
        }
        int lastSpace = value.lastIndexOf(' ');
        if (lastSpace >= 0 && value.toLowerCase().contains("social")) {
            return cleanUsername(value.substring(lastSpace + 1));
        }
        return null;
    }

    private static String getHoverPlainText(HoverEvent hover) {
        Object value = hover.getValue();
        if (value instanceof IChatComponent) {
            return StringUtils.stripControlCodes(((IChatComponent) value).getUnformattedText()).trim();
        }
        if (value != null) {
            return StringUtils.stripControlCodes(value.toString()).trim();
        }
        return "";
    }

    private static String cleanUsername(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = StringUtils.stripControlCodes(raw).trim();
        if (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (MINECRAFT_NAME.matcher(cleaned).matches()) {
            return cleaned;
        }
        return null;
    }
}
