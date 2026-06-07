package com.pitlite.module.impl.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.PitMapManager;
import com.pitlite.utils.HudStackManager;
import com.pitlite.utils.LobbyPlayerIndex;
import org.lwjgl.input.Mouse;

import java.util.*;

public class DarkList extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 10.0;
    private final BooleanSetting showPants = new BooleanSetting("Show Pants", true);
    private final BooleanSetting showRegion = new BooleanSetting("Show Region", true);

    private final BooleanSetting showVenom = new BooleanSetting("Venom", true);
    private final BooleanSetting showMisery = new BooleanSetting("Misery", true);
    private final BooleanSetting showSpite = new BooleanSetting("Spite", true);
    private final BooleanSetting showMindAssault = new BooleanSetting("Mind Assault", true);
    private final BooleanSetting showGoldenHandcuffs = new BooleanSetting("Golden Handcuffs", true);
    private final BooleanSetting showNostalgia = new BooleanSetting("Nostalgia", true);
    private final BooleanSetting showGrimReaper = new BooleanSetting("Grim Reaper", true);
    private final BooleanSetting showHedgeFund = new BooleanSetting("Hedge Fund", true);
    private final BooleanSetting showHeartripper = new BooleanSetting("Heartripper", true);
    private final BooleanSetting showLycanthropy = new BooleanSetting("Lycanthropy", true);

    private static class EnchantEntry {
        String abbrev;
        int color;
        EnchantEntry(String abbrev, int color) {
            this.abbrev = abbrev;
            this.color = color;
        }
    }

    private static class PlayerEntry {
        String name;
        String displayName;
        int distance;
        boolean inSpawn;
        String zone;
        String arrow;
        List<EnchantEntry> pantsEnchants = new ArrayList<>();
        EntityPlayer entity;
    }

    private final List<PlayerEntry> entries = new ArrayList<>();
    private long lastListRebuild = 0;
    private int cachedTotalHeight = 0;
    private int cachedMaxWidth = 40;

    private static final Map<String, Integer> ENCHANT_COLORS = new HashMap<>();
    private final Map<String, BooleanSetting> ENCHANT_SETTINGS = new HashMap<>();
    static {
        ENCHANT_COLORS.put("venom", 0x333333);
        ENCHANT_COLORS.put("misery", 0xAA00AA);
        ENCHANT_COLORS.put("spite", 0xAA00AA);
        ENCHANT_COLORS.put("mind", 0xAA00AA);
        ENCHANT_COLORS.put("cuffs", 0xFFA500);
        ENCHANT_COLORS.put("nost", 0x55FFFF);
        ENCHANT_COLORS.put("reaper", 0xAA00AA);
        ENCHANT_COLORS.put("hedge", 0x55FF55);
        ENCHANT_COLORS.put("heart", 0xFF5555);
        ENCHANT_COLORS.put("lycan", 0xAA00AA);
    }

    private static final String[][] DARK_ENCHANTS = {
        {"Combo: Venom", "venom"},
        {"Misery", "misery"},
        {"Spite", "spite"},
        {"Mind Assault", "mind"},
        {"Golden Handcuffs", "cuffs"},
        {"Nostalgia", "nost"},
        {"Grim Reaper", "reaper"},
        {"Hedge Fund", "hedge"},
        {"Heartripper", "heart"},
        {"Lycanthropy", "lycan"},
    };

    public DarkList() {
        super("Dark List", "Shows players in Dark Pants/Venom.", Category.RENDER);
        addSetting(showPants);
        addSetting(showRegion);
        addSetting(showVenom);
        addSetting(showMisery);
        addSetting(showSpite);
        addSetting(showMindAssault);
        addSetting(showGoldenHandcuffs);
        addSetting(showNostalgia);
        addSetting(showGrimReaper);
        addSetting(showHedgeFund);
        addSetting(showHeartripper);
        addSetting(showLycanthropy);
        ENCHANT_SETTINGS.put("venom", showVenom);
        ENCHANT_SETTINGS.put("misery", showMisery);
        ENCHANT_SETTINGS.put("spite", showSpite);
        ENCHANT_SETTINGS.put("mind", showMindAssault);
        ENCHANT_SETTINGS.put("cuffs", showGoldenHandcuffs);
        ENCHANT_SETTINGS.put("nost", showNostalgia);
        ENCHANT_SETTINGS.put("reaper", showGrimReaper);
        ENCHANT_SETTINGS.put("hedge", showHedgeFund);
        ENCHANT_SETTINGS.put("heart", showHeartripper);
        ENCHANT_SETTINGS.put("lycan", showLycanthropy);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        entries.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        entries.clear();
    }

    @SubscribeEvent
    public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        if (!isToggled()) return;
        entries.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null || mc.thePlayer == null) {
            if (!entries.isEmpty()) entries.clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastListRebuild >= 1000) {
            lastListRebuild = now;
            rebuildList();
        }

        for (PlayerEntry e : entries) {
            String targetName = StringUtils.stripControlCodes(e.name);
            EntityPlayer found = LobbyPlayerIndex.findByName(targetName);
            e.entity = found;

            if (e.entity != null) {
                e.distance = (int) mc.thePlayer.getDistanceToEntity(e.entity);
                e.inSpawn = PitMapManager.isInSpawn(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.zone = PitMapManager.getZone(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.arrow = getArrow(e.entity);
                if (showPants.enabled) {
                    e.pantsEnchants = extractPantsEnchants(e.entity);
                }
            }
        }

        entries.sort((a, b) -> {
            if (a.inSpawn && !b.inSpawn) return 1;
            if (!a.inSpawn && b.inSpawn) return -1;
            if (a.distance < 0) return 1;
            if (b.distance < 0) return -1;
            return Integer.compare(a.distance, b.distance);
        });

        int lh = mc.fontRendererObj.FONT_HEIGHT + 2;
        int visibleCount = 0;
        for (PlayerEntry e : entries) {
            if (!e.pantsEnchants.isEmpty()) visibleCount++;
        }
        cachedTotalHeight = entries.isEmpty() ? 0 : lh * (1 + visibleCount);
        FontRenderer fr = mc.fontRendererObj;
        int maxW = fr.getStringWidth("Dark List");
        for (PlayerEntry e : entries) {
            if (!e.pantsEnchants.isEmpty()) {
                maxW = Math.max(maxW, measureEntryWidth(fr, e));
            }
        }
        cachedMaxWidth = Math.max(20, maxW);
    }

    private int measureEntryWidth(FontRenderer fr, PlayerEntry e) {
        String nameToRender = e.displayName != null ? e.displayName : e.name;
        nameToRender = nameToRender.replaceAll("(?i)\\s+(?=(§[0-9a-fk-or])*$)", "");
        int width = fr.getStringWidth(nameToRender) + fr.getStringWidth(" - ");
        if (showPants.enabled && !e.pantsEnchants.isEmpty()) {
            for (int i = 0; i < e.pantsEnchants.size(); i++) {
                if (i > 0) {
                    width += fr.getStringWidth("/");
                }
                width += fr.getStringWidth(e.pantsEnchants.get(i).abbrev);
            }
        }
        if (e.distance >= 0) {
            width += fr.getStringWidth(" [");
            String distColor = e.distance < 15 ? "§a" : (e.distance < 50 ? "§6" : "§c");
            width += fr.getStringWidth(distColor + e.distance);
            if (e.arrow != null && !e.arrow.isEmpty()) {
                width += (int) (fr.getStringWidth(distColor + e.arrow) * 1.25f) + 2;
            }
            width += fr.getStringWidth(" §7- ");
            if (showRegion.enabled && e.zone != null && !e.zone.equals("Unknown")) {
                width += fr.getStringWidth("§e§l" + e.zone.toUpperCase());
            } else if (showRegion.enabled && e.inSpawn) {
                width += fr.getStringWidth("§a§lSPAWN");
            }
        }
        return width;
    }

    private void rebuildList() {
        Map<String, String> tabInfo = new HashMap<>();
        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                    String name = info.getGameProfile().getName();
                    String formatted = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : null;
                    if (formatted == null && mc.theWorld != null) {
                        net.minecraft.scoreboard.ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(name);
                        if (team != null) formatted = team.formatString(name);
                    }
                    tabInfo.put(name.toLowerCase(), formatted);
                }
            }
        }

        List<EntityPlayer> targets = new ArrayList<>();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            ItemStack legs = player.getEquipmentInSlot(2);
            if (legs != null && isDarkPants(legs)) {
                List<EnchantEntry> visible = extractPantsEnchants(player);
                if (!visible.isEmpty()) {
                    targets.add(player);
                }
            }
        }

        entries.removeIf(e -> targets.stream().noneMatch(p -> p.getName().equals(e.name)));

        for (EntityPlayer p : targets) {
            PlayerEntry existing = entries.stream().filter(e -> e.name.equals(p.getName())).findFirst().orElse(null);
            if (existing != null) {
                existing.entity = p;
                String dn = tabInfo.get(p.getName().toLowerCase());
                if (dn != null) existing.displayName = dn;
                else existing.displayName = p.getDisplayName().getFormattedText();
                continue;
            }
            PlayerEntry e = new PlayerEntry();
            e.name = p.getName();
            e.displayName = tabInfo.get(p.getName().toLowerCase());
            if (e.displayName == null) e.displayName = p.getDisplayName().getFormattedText();
            e.entity = p;
            entries.add(e);
        }
    }

    private boolean isDarkPants(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 2 || armor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) return false;
        if (armor.hasColor(stack)) {
            int color = armor.getColor(stack);
            if (color == 0 || color == 0x1D1D21 || color == 0x000000 || color == 0x191919) return true;
        }
        return StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase().contains("dark pant");
    }

    private List<EnchantEntry> extractPantsEnchants(EntityPlayer player) {
        List<EnchantEntry> result = new ArrayList<>();
        ItemStack legs = player.getEquipmentInSlot(2);
        if (legs == null || !legs.hasTagCompound()) return result;

        NBTTagCompound display = legs.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return result;
        NBTTagList lore = display.getTagList("Lore", 8);

        for (int i = 0; i < lore.tagCount(); i++) {
            String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
            if (line.contains("somber")) continue;
            if (line.trim().isEmpty() || line.contains("tier") || line.contains("kept on death") || line.contains("lives:")) continue;
            for (String[] pair : DARK_ENCHANTS) {
                if (line.contains(pair[0].toLowerCase())) {
                    String abbrev = pair[1];
                    BooleanSetting setting = ENCHANT_SETTINGS.get(abbrev);
                    if (setting != null && !setting.enabled) {
                        continue;
                    }

                    int color = ENCHANT_COLORS.getOrDefault(abbrev, 0xFFFFFF);
                    if (result.stream().noneMatch(en -> en.abbrev.equals(abbrev))) {
                        result.add(new EnchantEntry(abbrev, color));
                    }
                    break;
                }
            }
        }
        return result;
    }

    private String getArrow(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = mc.thePlayer.rotationYaw % 360;
        double rel = ((angleToTarget - playerYaw) % 360 + 360) % 360;
        if (rel >= 337.5 || rel < 22.5) return "↑";
        else if (rel >= 22.5 && rel < 67.5) return "↗";
        else if (rel >= 67.5 && rel < 112.5) return "→";
        else if (rel >= 112.5 && rel < 157.5) return "↘";
        else if (rel >= 157.5 && rel < 202.5) return "↓";
        else if (rel >= 202.5 && rel < 247.5) return "↙";
        else if (rel >= 247.5 && rel < 292.5) return "←";
        else return "↖";
    }

    public int getPlayerCount() {
        int count = 0;
        for (PlayerEntry e : entries) {
            if (!e.pantsEnchants.isEmpty()) count++;
        }
        return count;
    }

    public int getConfigY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    public void renderStacked() {
        if (mc.thePlayer == null || entries.isEmpty()) return;

        FontRenderer fr = mc.fontRendererObj;
        int lh = fr.FONT_HEIGHT + 2;
        int posX = getRenderX();
        int posY = getRenderY();

        fr.drawStringWithShadow("Dark List", posX, posY, 0xAA00AA);
        float y = posY + lh;

        for (PlayerEntry e : entries) {
            if (e.pantsEnchants.isEmpty()) continue;

            int currentX = posX;
            String nameToRender = e.displayName != null ? e.displayName : e.name;
            nameToRender = nameToRender.replaceAll("(?i)\\s+(?=(§[0-9a-fk-or])*$)", "");
            fr.drawStringWithShadow(nameToRender, currentX, (int) y, 0xFFFFFF);
            currentX += fr.getStringWidth(nameToRender);

            fr.drawStringWithShadow(" - ", currentX, (int) y, 0xFFFFFF);
            currentX += fr.getStringWidth(" - ");

            if (showPants.enabled && !e.pantsEnchants.isEmpty()) {
                for (int i = 0; i < e.pantsEnchants.size(); i++) {
                    if (i > 0) {
                        fr.drawStringWithShadow("/", currentX, (int) y, 0xFFFFFF);
                        currentX += fr.getStringWidth("/");
                    }
                    EnchantEntry enc = e.pantsEnchants.get(i);
                    fr.drawStringWithShadow(enc.abbrev, currentX, (int) y, enc.color);
                    currentX += fr.getStringWidth(enc.abbrev);
                }
            }

            if (e.distance >= 0) {
                fr.drawStringWithShadow(" [", currentX, (int) y, 0xAAAAAA);
                currentX += fr.getStringWidth(" [");
                String distColor = e.distance < 15 ? "§a" : (e.distance < 50 ? "§6" : "§c");
                String distText = distColor + e.distance;
                fr.drawStringWithShadow(distText, currentX, (int) y, 0xFFFFFF);
                currentX += fr.getStringWidth(distText);

                if (e.arrow != null && !e.arrow.isEmpty()) {
                    String arrowText = distColor + e.arrow;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(currentX + 1, y - 1, 0);
                    GlStateManager.scale(1.25f, 1.25f, 1.0f);
                    fr.drawStringWithShadow(arrowText, 0, 0, 0xFFFFFF);
                    GlStateManager.popMatrix();
                    currentX += (fr.getStringWidth(arrowText) * 1.25f) + 2;
                }
                fr.drawStringWithShadow(" §7- ", currentX, (int) y, 0xFFFFFF);
                currentX += fr.getStringWidth(" §7- ");

                if (showRegion.enabled) {
                    if (e.zone != null && !e.zone.equals("Unknown")) {
                        String zoneColor = "§e";
                        int hexColor = 0xFFFFFF;
                        String zLower = e.zone.toLowerCase();
                        if (zLower.equals("spawn") || zLower.equals("garden") || zLower.equals("sewer") || zLower.equals("seaweed")) zoneColor = "§a";
                        else if (zLower.equals("pit") || zLower.equals("demon") || zLower.equals("lava") || zLower.equals("port") || zLower.equals("geyser")) zoneColor = "§c";
                        else if (zLower.equals("palace") || zLower.equals("winter") || zLower.equals("city")) zoneColor = "§b";
                        else if (zLower.equals("badlands") || zLower.equals("autumn") || zLower.equals("mountains") || zLower.equals("farm") || zLower.equals("shipwreck")) zoneColor = "§6";
                        else if (zLower.equals("fortress")) zoneColor = "§4";
                        else if (zLower.equals("angel") || zLower.equals("summer") || zLower.equals("temple")) zoneColor = "§e";
                        else if (zLower.equals("spring")) {
                            zoneColor = "";
                            hexColor = 0xFFC0CB;
                        } 
                        else if (zLower.equals("overspawn")) zoneColor = "§5";
                        else if (zLower.equals("water")) zoneColor = "§9";
                        else if (zLower.equals("sky")) zoneColor = "§f";
                        else if (zLower.equals("forest")) zoneColor = "§2";

                        String zoneText = zoneColor + "§l" + e.zone.toUpperCase();
                        fr.drawStringWithShadow(zoneText, currentX, (int) y, hexColor);
                        currentX += fr.getStringWidth(zoneText);
                    } else if (e.inSpawn) {
                        fr.drawStringWithShadow("§a§lSPAWN", currentX, (int) y, 0xFFFFFF);
                        currentX += fr.getStringWidth("§a§lSPAWN");
                    }
                }
                fr.drawStringWithShadow(" §7]", currentX, (int) y, 0xFFFFFF);
            }
            y += lh;
        }
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        return !entries.isEmpty();
    }

    @Override
    public int getHudX() {
        return (int) HudPositionManager.getX(getHudKey(), DEFAULT_X);
    }

    @Override
    public int getHudY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public int getHudWidth() {
        return cachedMaxWidth;
    }

    @Override
    public int getHudHeight() {
        return Math.max(mc.fontRendererObj.FONT_HEIGHT + 2, cachedTotalHeight);
    }
}
