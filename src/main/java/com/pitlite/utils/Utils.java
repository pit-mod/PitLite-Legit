package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import com.pitlite.settings.NumberSetting;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Random;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.input.Mouse;

public class Utils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    public static boolean nullCheck() {
        return mc.thePlayer == null || mc.theWorld == null;
    }

    public static boolean onHypixel() {
        if (mc.theWorld != null && mc.thePlayer != null) {
            if (mc.isSingleplayer()) {
                return false;
            } else {
                return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net");
            }
        } else {
            return false;
        }
    }

    public static boolean isInLobby() {
        if (mc.theWorld == null) return false;
        String title = getScoreboardTitle().toLowerCase();
        return title.contains("hypixel") || title.contains("lobby") || title.isEmpty();
    }

    public static String getScoreboardTitle() {
        if (mc.theWorld == null) return "";
        try {
            net.minecraft.scoreboard.Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) return "";
            net.minecraft.scoreboard.ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
            if (objective == null) return "";
            return net.minecraft.util.StringUtils.stripControlCodes(objective.getDisplayName());
        } catch (Exception e) {
            return "";
        }
    }

    public static java.util.List<String> getScoreboardLines() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (mc.theWorld == null) return lines;
        net.minecraft.scoreboard.Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return lines;
        net.minecraft.scoreboard.ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;

        java.util.Collection<net.minecraft.scoreboard.Score> scores = scoreboard.getSortedScores(objective);
        java.util.List<net.minecraft.scoreboard.Score> list = scores.stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .collect(java.util.stream.Collectors.toList());

        if (list.size() > 15) {
            scores = java.util.stream.Stream.concat(list.stream().skip(list.size() - 15), java.util.stream.Stream.empty()).collect(java.util.stream.Collectors.toList());
        } else {
            scores = list;
        }

        for (net.minecraft.scoreboard.Score score : scores) {
            net.minecraft.scoreboard.ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            lines.add(net.minecraft.util.StringUtils.stripControlCodes(net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(team, score.getPlayerName())));
        }

        java.util.Collections.reverse(lines);
        return lines;
    }

    public static int getBounty() {
        for (String line : getScoreboardLines()) {
            if (line.contains("Bounty:")) {
                String bountyStr = line.split("Bounty:")[1].replaceAll("[^0-9,]", "").replace(",", "");
                try {
                    return Integer.parseInt(bountyStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static int getGold() {
        for (String line : getScoreboardLines()) {
            if (line.contains("Gold:")) {
                String goldLine = line.split("Gold:")[1];
                if (goldLine.contains(".")) {
                    goldLine = goldLine.split("\\.")[0];
                }
                String goldStr = goldLine.replaceAll("[^0-9,]", "").replace(",", "");
                try {
                    return Integer.parseInt(goldStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static int getLevel() {
        for (String line : getScoreboardLines()) {
            if (line.contains("Level:")) {
                String levelLine = line.split("Level:")[1];
                if (levelLine.contains("[") && levelLine.contains("]")) {
                    String levelStr = levelLine.split("\\[")[1].split("\\]")[0].replaceAll("[^0-9]", "");
                    try {
                        return Integer.parseInt(levelStr);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    public static int getStreak() {
        for (String line : getScoreboardLines()) {
            if (line.contains("Streak:")) {
                String streakStr = line.split("Streak:")[1].replaceAll("[^0-9,]", "").replace(",", "");
                try {
                    return Integer.parseInt(streakStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static boolean isInSpawn() {
        if (mc.thePlayer == null) return false;
        return PitMapManager.isInSpawn(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    public static boolean holdingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public static boolean holdingWeapon() {
        return holdingSword();
    }

    public static void attackEntity(EntityLivingBase target, boolean swingArm, boolean silent) {
        if (nullCheck() || target == null) {
            return;
        }
        mc.thePlayer.swingItem();
        mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
        if (silent) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
    }

    public static double getRandomValue(NumberSetting min, NumberSetting max, Random rand) {
        return min.value + (max.value - min.value) * rand.nextDouble();
    }

    public static void correctValue(NumberSetting min, NumberSetting max) {
        if (min.value > max.value) {
            min.setValue(max.value);
        }
    }

    public static long getDifference(long start, long end) {
        return end - start;
    }

    public static String formatColor(String message) {
        return message.replaceAll("&", "\u00a7");
    }

    public static boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0;
    }

    public static double getHorizontalSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static void setSpeed(double speed) {
        if (nullCheck()) {
            return;
        }
        float yaw = mc.thePlayer.rotationYaw;
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        mc.thePlayer.motionX = speed * cos;
        mc.thePlayer.motionZ = speed * sin;
    }

    public static boolean jumpDown() {
        return mc.thePlayer.motionY < 0;
    }

    public static int getColorForHealth(float health) {
        return Color.HSBtoRGB((float) (health * 0.4), 0.75f, 0.85f);
    }

    public static void drawRoundedGradientRect(float x, float y, float x2, float y2, float radius, int color1, int color2, int color3, int color4) {
    }

    public static void drawRoundedGradientOutlinedRectangle(float x, float y, float x2, float y2, float radius, int outlineColor, int startColor, int endColor) {
    }

    public static void drawRoundedRectangle(float x, float y, float x2, float y2, float radius, int color) {
    }

    public static int getMouseX() {
        if (mc.thePlayer == null) return 0;
        return org.lwjgl.input.Mouse.getX() / new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
    }

    public static int getMouseY() {
        if (mc.thePlayer == null) return 0;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        return sr.getScaledHeight() - org.lwjgl.input.Mouse.getY() / sr.getScaleFactor();
    }

    public static void setMouseButtonState(int mouseButton, boolean held) {
        if (nullCheck()) return;
        MouseEvent m = new MouseEvent();

        ObfuscationReflectionHelper.setPrivateValue(MouseEvent.class, m, mouseButton, "button");
        ObfuscationReflectionHelper.setPrivateValue(MouseEvent.class, m, held, "buttonstate");
        MinecraftForge.EVENT_BUS.post(m);

        try {
            ByteBuffer buttons = (ByteBuffer) ReflectionUtils.getField(Mouse.class, null, "buttons");
            if (buttons != null) {
                buttons.put(mouseButton, (byte) (held ? 1 : 0));
                ReflectionUtils.setField(Mouse.class, null, "buttons", buttons);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
