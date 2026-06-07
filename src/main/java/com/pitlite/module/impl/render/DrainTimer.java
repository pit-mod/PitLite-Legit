package com.pitlite.module.impl.render;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.pitlite.module.Category;
import com.pitlite.module.Module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DrainTimer extends Module {

    private static final Map<UUID, Long> drainPlayers = new HashMap<>();
    private long lastDrainFire = 0;
    private boolean debugSent = false;
    public static Map<UUID, Long> getDrainPlayers() {
        return drainPlayers;
    }

    public DrainTimer() {
        super("DrainTimer", "Displays a countdown timer above players affected by Sprint Drain.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        drainPlayers.clear();
        lastDrainFire = 0;
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        if (!isToggled()) return;
        if (event.entityPlayer != null && event.entityPlayer == mc.thePlayer) {
            int level = getDrainLevel(mc.thePlayer.getHeldItem());
            if (level >= 2) {
                lastDrainFire = System.currentTimeMillis();
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isToggled()) return;
        if (event.type == 2) {
            String text = event.message.getUnformattedText();
            if (text.contains("\u2764") && System.currentTimeMillis() - lastDrainFire < 5000) {
                EntityPlayer hitPlayer = null;
                for (EntityPlayer p : mc.theWorld.playerEntities) {
                    if (p != mc.thePlayer && text.contains(p.getName())) {
                        hitPlayer = p;
                        break;
                    }
                }

                if (hitPlayer != null) {
                    long duration = 3000;
                    drainPlayers.put(hitPlayer.getUniqueID(), System.currentTimeMillis() + duration);
                    if (!debugSent) {
                        mc.thePlayer.addChatMessage(new ChatComponentText("\u00A77[PitLite] \u00A7aDrain detected on " + hitPlayer.getName()));
                        debugSent = true;
                    }
                    lastDrainFire = 0;
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!isToggled()) return;
        if (event.source.getEntity() != null && event.source.getEntity().getEntityId() == mc.thePlayer.getEntityId()
            && event.entity instanceof EntityPlayer && event.source.isProjectile()) {
            EntityPlayer target = (EntityPlayer) event.entity;
            if (System.currentTimeMillis() - lastDrainFire < 5000) {
                long duration = 3000;
                drainPlayers.put(target.getUniqueID(), System.currentTimeMillis() + duration);
                lastDrainFire = 0;
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isToggled()) return;
        if (drainPlayers.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = drainPlayers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID uuid = entry.getKey();
            long expiry = entry.getValue();
            EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(uuid);

            if (currentTime > expiry || player == null || player.isDead) {
                iterator.remove();
                continue;
            }
        }
    }

    private int getDrainLevel(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        NBTTagCompound tag = stack.getTagCompound();
        String tagStr = tag.toString().toLowerCase();

        if (tagStr.contains("sprint_drain:3") || tagStr.contains("sprint drain iii")) return 3;
        if (tagStr.contains("sprint_drain:2") || tagStr.contains("sprint drain ii")) return 2;
        if (tagStr.contains("sprint_drain:1") || tagStr.contains("sprint drain i")) return 1;

        if (tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore", 9)) {
                NBTTagList lore = display.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String line = StringUtils.stripControlCodes(lore.getStringTagAt(i));
                    if (line.contains("Sprint Drain III")) return 3;
                    if (line.contains("Sprint Drain II")) return 2;
                    if (line.contains("Sprint Drain I")) return 1;
                }
            }
        }

        if (tagStr.contains("sprint drain") || tagStr.contains("sprint_drain")) return 1;

        return 0;
    }
}
