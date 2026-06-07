package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabJoinNotifier {

    private final Set<String> lastTabUuids = new HashSet<>();
    private int tickCounter;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getNetHandler() == null) {
            lastTabUuids.clear();
            return;
        }

        if (++tickCounter % 20 != 0) {
            return;
        }

        Set<String> currentUuids = new HashSet<>();
        List<PlayerListTabSync.TabPlayer> tabPlayers = PlayerListTabSync.collectTabPlayers(mc);

        for (PlayerListTabSync.TabPlayer tp : tabPlayers) {
            currentUuids.add(tp.uuid);

            if (lastTabUuids.contains(tp.uuid)) {
                continue;
            }
            if (mc.thePlayer != null && tp.name.equalsIgnoreCase(mc.thePlayer.getName())) {
                continue;
            }

            PlayerListStore.ListType type = PlayerListTabSync.getListType(tp.uuid, tp.name);
            if (type == null) {
                continue;
            }
            if (type == PlayerListStore.ListType.KOS && !KOSManager.isEnabled()) {
                continue;
            }

            String renamedFrom = PlayerListTabSync.syncRename(type, tp.uuid, tp.name);
            NotificationManager.show(PlayerListTabSync.formatTabJoinMessage(type, tp.name, renamedFrom), 5000);
        }

        lastTabUuids.clear();
        lastTabUuids.addAll(currentUuids);
    }
}
