package com.pitlite.utils;

import com.pitlite.PitLite;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudStackManager {
    private static final int COLUMN_WIDTH = 80;
    private static final int GAP = 5;

    private static final Map<String, Integer> resolvedY = new HashMap<>();
    private static final Map<Integer, Integer> columnBottoms = new HashMap<>();
    private static boolean dirty = true;

    public static void markDirty() {
        dirty = true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null || PitLite.moduleManager == null) {
            resolvedY.clear();
            columnBottoms.clear();
            dirty = true;
            return;
        }
        if (dirty) {
            rebuild();
            dirty = false;
        }
    }

    private static void rebuild() {
        resolvedY.clear();
        columnBottoms.clear();

        List<DraggableHud> visible = new ArrayList<>();
        for (Module module : PitLite.moduleManager.getModules()) {
            if (!module.isToggled() || !(module instanceof DraggableHud)) {
                continue;
            }
            DraggableHud hud = (DraggableHud) module;
            if (hud.isHudVisible()) {
                visible.add(hud);
            }
        }

        Collections.sort(visible, Comparator
                .comparingInt(DraggableHud::getHudY)
                .thenComparingInt(DraggableHud::getHudX)
                .thenComparing(DraggableHud::getHudKey));

        for (DraggableHud hud : visible) {
            int width = Math.max(1, hud.getHudWidth());
            int height = Math.max(1, hud.getHudHeight());
            int x = HudBounds.clampX(hud.getHudX(), width, hud.isHudCenterAnchored());
            int configY = hud.getHudY();
            int column = x / COLUMN_WIDTH;
            int previousBottom = columnBottoms.getOrDefault(column, 0);
            int startY = HudBounds.clampY(Math.max(configY, previousBottom), height);
            columnBottoms.put(column, startY + height + GAP);
            resolvedY.put(hud.getHudKey(), startY);
        }
    }

    public static int getStackedY(String hudKey, int configY) {
        return resolvedY.getOrDefault(hudKey, configY);
    }

    public static int getStackedY(DraggableHud hud) {
        return getStackedY(hud.getHudKey(), hud.getHudY());
    }
}
