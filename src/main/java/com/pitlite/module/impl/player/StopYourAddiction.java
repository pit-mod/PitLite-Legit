package com.pitlite.module.impl.player;

import com.pitlite.PitLite;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.utils.FullVolumeStreamingSound;
import com.pitlite.utils.VideoRenderer;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StopYourAddiction extends Module {

    private static final String ACCEPT_PHRASE = "i accept my sentence.";
    private static final long FREDDY_SOUND_MS = 21300L;
    private static final int JUMPSCARE_FRAMES = 26;
    private static final int JUMPSCARE_FPS = 30;

    private static final String ACCEPT_CLICK_COMMAND = "/pitlite_accept";

    private static final String[] INTERVENTION = {
            "\u00A7c\u00A7l\u26A0\uFE0F PIT ADDICTION INTERVENTION \u2014 FINAL ASCENSION \u26A0\uFE0F",
            "",
            "\u00A77Traveler of the Pit,",
            "",
            "\u00A77After countless prestiges, impossible streaks, questionable mystics, and approximately 17,483 unnecessary \"Play Again\" clicks, you have arrived at the final upgrade.",
            "",
            "\u00A77Not Prestige 36.",
            "\u00A77Not Prestige 50.",
            "\u00A77Not Prestige 100.",
            "",
            "\u00A7aFreedom.",
            "",
            "\u00A77Before you stands a button forbidden by the Pit Elders themselves.",
            "",
            "\u00A77Beyond it lies a realm where:",
            "",
            "\u00A77* Hopper armies cannot find you.",
            "\u00A77* Bounty hunters cannot target you.",
            "\u00A77* Fresh pants have no value.",
            "\u00A77* The Gold Nano Factory produces nothing.",
            "\u00A77* The Mystic Well falls silent.",
            "\u00A77* Nobody asks, \"bro what's your streak?\"",
            "",
            "\u00A77For the first time in your journey, there will be no next prestige.",
            "\u00A77No next event.",
            "\u00A77No next bounty.",
            "\u00A77No next \"just one more hour.\"",
            "",
            "\u00A7eAsk yourself:",
            "",
            "\u00A77Have you truly collected enough fresh?",
            "\u00A77Have you been venom'd enough times?",
            "\u00A77Have you suffered enough random hunts?",
            "\u00A77Have you spent enough hours chasing numbers that will be forgotten by next wipe?",
            "",
            "\u00A77The Pit Recovery Foundation has reviewed your case and reached a unanimous conclusion:",
            "",
            "\u00A7cThis patient may be beyond saving.",
            "",
            "\u00A77Proceeding will permanently separate you from this accursed cycle.",
            "",
            "\u00A77You may experience the following side effects:",
            "\u00A7a\u2022 Touching grass",
            "\u00A7a\u2022 Seeing sunlight",
            "\u00A7a\u2022 Discovering other video games exist",
            "\u00A7a\u2022 Improved mental health",
            "\u00A7a\u2022 Family members recognizing you again",
            "",
            "\u00A77Once this seal is broken, there shall be no /respawn.",
            "\u00A77No second chance.",
            "\u00A77No divine mystic.",
            "\u00A77No rollback.",
            "",
            "\u00A74Only the Void.",
            "",
            "\u00A7eTo proceed, type here or click the message below:",
            "",
            "\u00A77And let your final streak be remembered not for how high it climbed...",
            "\u00A77...but for having the strength to end it.",
            "",
            "\u00A7c\u00A7l\u26A0\uFE0F THIS ACTION IS PERMANENT \u26A0\uFE0F",
            "\u00A7c\u00A7l\u26A0\uFE0F EVEN FRESH PANTS CANNOT SAVE YOU NOW \u26A0\uFE0F"
    };

    private enum Phase {
        AWAITING,
        PLAYING_SOUND,
        PLAYING_VIDEO,
        BAN_SPAM,
        FINISHED
    }

    private Phase phase = Phase.AWAITING;
    private long soundEndTime;
    private long soundStartedAt;
    private long videoStartedAt;
    private boolean videoStarted;
    private boolean disabledAutoReconnect;
    private int banPacketCount;
    private int lastCountdownSecond = -1;
    private ISound countdownSound;
    private double banX;
    private double banY;
    private double banZ;

    public StopYourAddiction() {
        super("Stop Your Addiction", "Final intervention. Type or click the acceptance phrase to proceed.", Category.MISC);
        markDangerous();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetSequence();
        sendIntervention();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopCountdownSound();
        resetSequence();
    }

    private void resetSequence() {
        phase = Phase.AWAITING;
        soundEndTime = 0L;
        soundStartedAt = 0L;
        videoStartedAt = 0L;
        videoStarted = false;
        banPacketCount = 0;
        lastCountdownSecond = -1;
        banX = 0;
        banY = 0;
        banZ = 0;
        disabledAutoReconnect = false;
        countdownSound = null;
    }

    private void stopCountdownSound() {
        if (countdownSound != null && mc.getSoundHandler() != null) {
            mc.getSoundHandler().stopSound(countdownSound);
        }
        countdownSound = null;
    }

    private void sendIntervention() {
        if (mc.thePlayer == null) return;
        for (String line : INTERVENTION) {
            if (line.isEmpty()) {
                mc.thePlayer.addChatMessage(new ChatComponentText(" "));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText(line));
            }
        }
        sendAcceptPrompt();
    }

    private void sendAcceptPrompt() {
        if (mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(createClickableAcceptText());
    }

    private IChatComponent createClickableAcceptText() {
        ChatComponentText accept = new ChatComponentText("i accept my sentence.");
        ChatStyle style = new ChatStyle()
                .setColor(net.minecraft.util.EnumChatFormatting.GREEN)
                .setBold(true)
                .setUnderlined(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ACCEPT_CLICK_COMMAND))
                .setChatHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("\u00A7aClick to begin your final ascension")));
        accept.setChatStyle(style);
        return accept;
    }

    public static void tryAcceptFromClick() {
        if (PitLite.moduleManager == null) return;
        for (Module module : PitLite.moduleManager.getModules()) {
            if (module instanceof StopYourAddiction && module.isToggled()) {
                ((StopYourAddiction) module).handleOutgoingChat(ACCEPT_PHRASE);
                return;
            }
        }
    }

    public static boolean tryHandleOutgoingChat(String message) {
        if (PitLite.moduleManager == null) return false;
        for (Module module : PitLite.moduleManager.getModules()) {
            if (module instanceof StopYourAddiction && module.isToggled()) {
                if (((StopYourAddiction) module).handleOutgoingChat(message)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleOutgoingChat(String message) {
        if (phase != Phase.AWAITING) return false;
        if (matchesAcceptance(message)) {
            beginAscension();
            return true;
        }
        return false;
    }

    private boolean matchesAcceptance(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase();
        return normalized.equals(ACCEPT_PHRASE) || normalized.equals("i accept my sentence");
    }

    private void beginAscension() {
        if (mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText("\u00A7c\u00A7l[PitLite] Sentence accepted. Ascension begins..."));
        phase = Phase.PLAYING_SOUND;
        long now = System.currentTimeMillis();
        soundStartedAt = now;
        soundEndTime = now + FREDDY_SOUND_MS;
        lastCountdownSecond = -1;
        updateCountdownChat(true);

        try {
            stopCountdownSound();
            countdownSound = new FullVolumeStreamingSound(new ResourceLocation("pitlite", "freddy_countdown_3"));
            mc.getSoundHandler().playSound(countdownSound);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isToggled() || event.phase != TickEvent.Phase.END) return;

        switch (phase) {
            case PLAYING_SOUND:
                updateCountdownChat(false);
                long soundElapsed = System.currentTimeMillis() - soundStartedAt;
                boolean soundFinished = System.currentTimeMillis() >= soundEndTime;
                boolean soundStoppedEarly = countdownSound != null
                        && soundElapsed > 1500L
                        && !mc.getSoundHandler().isSoundPlaying(countdownSound);
                if (soundFinished || soundStoppedEarly) {
                    stopCountdownSound();
                    startJumpscare();
                }
                break;
            case PLAYING_VIDEO:
                if (videoStarted && !VideoRenderer.isPlaying()
                        && System.currentTimeMillis() - videoStartedAt > 500L) {
                    startBanSpam();
                }
                break;
            case BAN_SPAM:
                if (mc.thePlayer == null || mc.getNetHandler() == null || mc.theWorld == null) {
                    phase = Phase.FINISHED;
                    setToggled(false);
                    return;
                }
                sendWatchdogPackets();
                break;
            default:
                break;
        }
    }

    private void updateCountdownChat(boolean force) {
        if (mc.thePlayer == null || phase != Phase.PLAYING_SOUND) return;

        int secondsLeft = (int) Math.ceil(Math.max(0L, soundEndTime - System.currentTimeMillis()) / 1000.0);
        if (!force && secondsLeft == lastCountdownSecond) return;

        lastCountdownSecond = secondsLeft;
        String color = secondsLeft <= 5 ? "\u00A74" : (secondsLeft <= 10 ? "\u00A76" : "\u00A7c");
        mc.thePlayer.addChatMessage(new ChatComponentText(
                color + "\u00A7l[PitLite] Final ascension in \u00A7f" + secondsLeft + color + "\u00A7l..."));
    }

    private void startJumpscare() {
        stopCountdownSound();
        phase = Phase.PLAYING_VIDEO;
        videoStartedAt = System.currentTimeMillis();
        videoStarted = true;
        VideoRenderer.play("jumpscare", JUMPSCARE_FRAMES, JUMPSCARE_FPS);
    }

    private void startBanSpam() {
        phase = Phase.BAN_SPAM;
        disableAutoReconnect();
        if (mc.thePlayer != null) {
            banX = mc.thePlayer.posX;
            banY = mc.thePlayer.posY;
            banZ = mc.thePlayer.posZ;
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00A7c\u00A7l[PitLite] The Void calls. Goodbye, Pit."));
        }
    }

    private void disableAutoReconnect() {
        if (disabledAutoReconnect || PitLite.moduleManager == null) return;
        for (Module module : PitLite.moduleManager.getModules()) {
            if ("AutoReconnect".equals(module.getName()) && module.isToggled()) {
                module.setToggled(false);
                disabledAutoReconnect = true;
                break;
            }
        }
    }

    private void sendWatchdogPackets() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        banPacketCount++;

        int swordSlot = findSwordSlot();
        if (mc.thePlayer.inventory.currentItem != swordSlot) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(swordSlot));
            mc.thePlayer.inventory.currentItem = swordSlot;
        }

        ItemStack sword = mc.thePlayer.getHeldItem();

        mc.getNetHandler().addToSendQueue(new C0CPacketInput(1.0F, 0.0F, true, false));
        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(
                mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));

        double yawRad = Math.toRadians(mc.thePlayer.rotationYaw);
        banX += -Math.sin(yawRad) * 1.2;
        banZ += Math.cos(yawRad) * 1.2;
        if (banPacketCount % 2 == 0) {
            banY += 0.42;
        }

        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                banX, banY, banZ, banPacketCount % 3 == 0));

        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());

        Entity attackTarget = findAttackTarget();
        if (attackTarget != null) {
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(
                    attackTarget, C02PacketUseEntity.Action.ATTACK));
        }

        if (sword != null) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(sword));
        }
    }

    private int findSwordSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) {
                return i;
            }
        }
        return mc.thePlayer.inventory.currentItem;
    }

    private Entity findAttackTarget() {
        Entity closest = null;
        double closestDist = 6.0;

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) obj;
            if (entity == mc.thePlayer || entity.isDead) continue;

            double dist = mc.thePlayer.getDistanceToEntity(entity);
            if (dist <= closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        return closest;
    }
}
