package com.pitlite.module.impl.render;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.ModeSetting;

public class FullBright extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Gamma", new String[]{"Gamma", "Effect"});
    private float previousGamma = Float.NaN;
    private boolean appliedNightVision = false;

    public FullBright() {
        super("FullBright", "Makes everything fully bright", Category.RENDER);
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.thePlayer == null) return;

        if (mode.getMode().equals("Gamma")) {
            previousGamma = mc.gameSettings.gammaSetting;
        } else if (mode.getMode().equals("Effect")) {
            appliedNightVision = true;
        }
    }

    @Override
    public void onDisable() {
        if (!Float.isNaN(previousGamma)) {
            mc.gameSettings.gammaSetting = previousGamma;
            previousGamma = Float.NaN;
        }
        if (appliedNightVision && mc.thePlayer != null) {
            mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
            appliedNightVision = false;
        }
        super.onDisable();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        if (mode.getMode().equals("Gamma")) {
            mc.gameSettings.gammaSetting = 1000.0F;
        } else if (mode.getMode().equals("Effect")) {
            mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 25940, 0));
        }
    }

}
