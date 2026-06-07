package com.pitlite.module.impl.player;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.InventoryUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class LowLivesWarning extends Module {

    private static LowLivesWarning instance;

    private final BooleanSetting backgroundHighlight = new BooleanSetting("Background Highlight", true);
    private final BooleanSetting soundWarning = new BooleanSetting("Sound Warning", true);

    private long alertEndTime;
    private String alertText = "";
    private final Map<Integer, Integer> lastLives = new HashMap<>();

    public LowLivesWarning() {
        super("Low Lives Warning", "Highlights items with low lives and alerts when they enter your inventory.", Category.PLAYER);
        addSettings(backgroundHighlight, soundWarning);
        instance = this;
    }

    public static boolean isEnabled() {
        return instance != null && instance.isToggled();
    }

    public static boolean isBackgroundEnabled() {
        return isEnabled() && instance.backgroundHighlight.enabled;
    }

    public static boolean isSoundWarningEnabled() {
        return isEnabled() && instance.soundWarning.enabled;
    }

    @Override
    protected void onDisable() {
        lastLives.clear();
        alertText = "";
        alertEndTime = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || !isSoundWarningEnabled()) {
            return;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            int currentLives = InventoryUtils.getLives(stack);

            Integer last = lastLives.get(i);
            if (currentLives != -1 && currentLives < 3) {
                if (last == null || last == -1) {
                    triggerAlert(stack, currentLives);
                }
            }
            lastLives.put(i, currentLives);
        }
    }

    public void triggerAlert(ItemStack stack, int lives) {
        if (stack == null) {
            return;
        }
        String customName = MysticRename.getCustomName(stack);
        if (customName == null) {
            customName = stack.getDisplayName();
        }

        alertText = "your " + customName + " \u00a7ris on \u00a7c" + lives + " \u00a7rlives";
        alertEndTime = System.currentTimeMillis() + 2000;
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("random.orb"), 1.0F));
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        if (System.currentTimeMillis() >= alertEndTime || alertText.isEmpty()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 3;
        int width = mc.fontRendererObj.getStringWidth(alertText);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(2.0f, 2.0f, 1.0f);
        mc.fontRendererObj.drawStringWithShadow(alertText, -width / 2.0f, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();
    }
}
