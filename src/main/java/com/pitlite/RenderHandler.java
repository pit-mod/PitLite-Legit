package com.pitlite;

import com.pitlite.module.Module;
import com.pitlite.module.impl.render.DarkList;
import com.pitlite.module.impl.render.KOSList;
import com.pitlite.module.impl.render.NickedList;
import com.pitlite.module.impl.render.RageList;
import com.pitlite.utils.VideoRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderHandler {

    private static ScaledResolution cachedResolution;
    private static int cachedResW = -1;
    private static int cachedResH = -1;
    private static List<Module> cachedListModules = new ArrayList<>();
    private static int lastSortHash;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        Minecraft mc = Minecraft.getMinecraft();
        int sw = mc.displayWidth;
        int sh = mc.displayHeight;
        if (cachedResolution == null || sw != cachedResW || sh != cachedResH) {
            cachedResolution = new ScaledResolution(mc);
            cachedResW = sw;
            cachedResH = sh;
        }

        int sortHash = 0;
        cachedListModules.clear();
        for (Module m : PitLite.moduleManager.getModules()) {
            if (!m.isToggled()) continue;
            if (m instanceof KOSList || m instanceof DarkList || m instanceof RageList
                    || m instanceof NickedList) {
                cachedListModules.add(m);
                if (m instanceof KOSList) sortHash = sortHash * 31 + ((KOSList) m).getConfigY();
                else if (m instanceof DarkList) sortHash = sortHash * 31 + ((DarkList) m).getConfigY();
                else if (m instanceof RageList) sortHash = sortHash * 31 + ((RageList) m).getConfigY();
                else if (m instanceof NickedList) sortHash = sortHash * 31 + ((NickedList) m).getConfigY();
            }
        }

        if (sortHash != lastSortHash) {
            cachedListModules.sort(Comparator
                    .comparingInt((Module m) -> {
                        if (m instanceof KOSList) return ((KOSList) m).getConfigY();
                        if (m instanceof DarkList) return ((DarkList) m).getConfigY();
                        if (m instanceof RageList) return ((RageList) m).getConfigY();
                        if (m instanceof NickedList) return ((NickedList) m).getConfigY();
                        return 0;
                    })
                    .thenComparing(Module::getName));
            lastSortHash = sortHash;
        }

        for (Module m : cachedListModules) {
            if (m instanceof KOSList) {
                ((KOSList) m).renderStacked();
            } else if (m instanceof DarkList) {
                ((DarkList) m).renderStacked();
            } else if (m instanceof RageList) {
                ((RageList) m).renderStacked();
            } else if (m instanceof NickedList) {
                ((NickedList) m).renderStacked();
            }
        }

        VideoRenderer.render(cachedResolution);
    }
}
