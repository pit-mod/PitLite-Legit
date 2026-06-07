package com.pitlite.module.impl.render;

import net.minecraft.client.gui.FontRenderer;

import net.minecraft.client.renderer.GlStateManager;
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
import com.pitlite.utils.FriendManager;
import com.pitlite.utils.KOSManager;
import com.pitlite.utils.PlayerListStore;
import com.pitlite.utils.PlayerListTabSync;
import com.pitlite.utils.TruceManager;
import com.pitlite.utils.PitMapManager;
import com.pitlite.utils.HudStackManager;
import com.pitlite.utils.LobbyPlayerIndex;

import java.util.*;

public class KOSList extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 100.0;
    {
    }
    private final BooleanSetting showPants = new BooleanSetting("Show Pants", true);
    private final BooleanSetting showRegion = new BooleanSetting("Show Region", true);
    private final BooleanSetting joinSound = new BooleanSetting("Join Sound", true);
    private final BooleanSetting joinNotify = new BooleanSetting("Join Notification", true);
    private final BooleanSetting jumpNotify = new BooleanSetting("Jump Notifications", true);
    public final BooleanSetting notificationSound = new BooleanSetting("Notification Sound", true);

    private final Set<String> kosUuidsInTab = new HashSet<>();
    private boolean firstRun = true;

    private enum EntryType {
        FRIENDS, TRUCE, ENEMIES
    }

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
        String previousZone;
        String arrow;
        List<EnchantEntry> pantsEnchants = new ArrayList<>();
        EntityPlayer entity;
        EntryType type;
    }

    private final List<PlayerEntry> entries = new ArrayList<>();
    private long lastListRebuild = 0;
    private int cachedTotalHeight = 0;
    private int cachedMaxWidth = 40;
    private final List<PlayerEntry> cachedFriends = new ArrayList<>();
    private final List<PlayerEntry> cachedTruces = new ArrayList<>();
    private final List<PlayerEntry> cachedEnemies = new ArrayList<>();

    private static final Map<String, Integer> ENCHANT_COLORS = new HashMap<>();
    static {
        ENCHANT_COLORS.put("reg", 0x780000);
        ENCHANT_COLORS.put("venom", 0x333333);
        ENCHANT_COLORS.put("sw", 0x55FFFF);
        ENCHANT_COLORS.put("blob", 0x55FF55);
        ENCHANT_COLORS.put("abs", 0xFFA500);
        ENCHANT_COLORS.put("pod", 0xFF5555);
        ENCHANT_COLORS.put("prick", 0xFF5555);
        ENCHANT_COLORS.put("fast", 0xFFFFAA);
        ENCHANT_COLORS.put("gh", 0xFFFF55);
        ENCHANT_COLORS.put("sco", 0xFFFF55);
        ENCHANT_COLORS.put("pero", 0xFF5555);
        ENCHANT_COLORS.put("boo", 0xFF5555);
        ENCHANT_COLORS.put("nego", 0xFFFF55);
        ENCHANT_COLORS.put("cf", 0xFFFF55);
        ENCHANT_COLORS.put("french", 0xFFFF55);
    }

    private static final String[][] LEATHER_ENCHANTS = {
            { "\"Not\" Gladiator", "notglad" },
            { "Sweaty", "sw" },
            { "Negotiator", "nego" },
            { "Self-checkout", "sco" },
            { "Pants Radar", "pr" },
            { "Boo-boo", "boo" },
            { "Counter-Offensive", "co" },
            { "Critically Funky", "cf" },
            { "Golden Heart", "gh" },
            { "Gotta Go Fast", "fast" },
            { "Mirror", "mirror" },
            { "Peroxide", "pero" },
            { "Prick", "prick" },
            { "Respawn: Absorption", "abs" },
            { "Escape Pod", "pod" },
            { "Phoenix", "phx" },
            { "Pit Blob", "blob" },
            { "Retro-Gravity Microcosm", "rgm" },
            { "Singularity", "sing" },
            { "Solitude", "soli" },
            { "Paparazzi", "papa" },
    };

    private static final String[][] RAGE_ENCHANTS = {
            { "Do it like the French", "french" },
            { "Regularity", "reg" },
            { "Heigh-Ho", "hh" },
            { "New Deal", "nd" },
            { "\"Not\" Gladiator", "notglad" },
            { "Sweaty", "sw" },
            { "Negotiator", "nego" },
            { "Self-checkout", "sco" },
            { "Pants Radar", "pr" },
            { "Boo-boo", "boo" },
            { "Counter-Offensive", "co" },
            { "Critically Funky", "cf" },
            { "Golden Heart", "gh" },
            { "Gotta Go Fast", "fast" },
            { "Mirror", "mirror" },
            { "Peroxide", "pero" },
            { "Prick", "prick" },
            { "Respawn: Absorption", "abs" },
            { "Escape Pod", "pod" },
            { "Phoenix", "phx" },
            { "Pit Blob", "blob" },
            { "Retro-Gravity Microcosm", "rgm" },
            { "Singularity", "sing" },
            { "Solitude", "soli" },
    };

    private static final String[][] DARK_ENCHANTS = {
            { "Combo: Venom", "venom" },
    };

    public KOSList() {
        super("Players List", "Shows KOS and Friend players in your lobby with distance and direction.",
                Category.RENDER);
        addSetting(showPants);
        addSetting(showRegion);
        addSetting(joinSound);
        addSetting(joinNotify);
        addSetting(jumpNotify);
        addSetting(notificationSound);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        kosUuidsInTab.clear();
        entries.clear();
    }

    public int getPlayerCount() {
        return entries.size();
    }

    public int getConfigY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        entries.clear();
        firstRun = true;
    }

    @SubscribeEvent
    public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        if (!isToggled()) return;
        kosUuidsInTab.clear();
        entries.clear();
        firstRun = true;
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
                String currentZone = PitMapManager.getZone(e.entity.posX, e.entity.posY, e.entity.posZ);
                if (jumpNotify.enabled && e.type == EntryType.ENEMIES && e.previousZone != null) {
                    if (e.previousZone.equals("Spawn") && !currentZone.equals("Spawn") && !currentZone.equals("Unknown")) {
                        com.pitlite.utils.NotificationManager.show("§c" + e.name + " is in " + currentZone.toUpperCase(), 3000);
                    }
                }
                e.previousZone = currentZone;
                e.zone = currentZone;
                e.arrow = getArrow(e.entity);

                if (showPants.enabled) {
                    e.pantsEnchants = extractPantsEnchants(e.entity);
                }

                if (FriendManager.isFriend(e.entity)) {
                    e.type = EntryType.FRIENDS;
                } else if (TruceManager.isTruce(e.entity)) {
                    e.type = EntryType.TRUCE;
                } else {
                    e.type = EntryType.ENEMIES;
                }
            } else {
                e.distance = -1;
                e.pantsEnchants.clear();
            }
        }

        entries.sort((a, b) -> {
            if (a.type != b.type)
                return Integer.compare(a.type.ordinal(), b.type.ordinal());

            if (a.inSpawn && !b.inSpawn)
                return 1;
            if (!a.inSpawn && b.inSpawn)
                return -1;
            if (a.distance < 0 && b.distance < 0)
                return 0;
            if (a.distance < 0)
                return 1;
            if (b.distance < 0)
                return -1;
            return Integer.compare(a.distance, b.distance);
        });

        cachedFriends.clear();
        cachedTruces.clear();
        cachedEnemies.clear();
        for (PlayerEntry e : entries) {
            if (e.type == EntryType.FRIENDS) cachedFriends.add(e);
            else if (e.type == EntryType.TRUCE) cachedTruces.add(e);
            else cachedEnemies.add(e);
        }

        int lh = mc.fontRendererObj.FONT_HEIGHT + 2;
        int totalHeight = 0;
        if (!cachedFriends.isEmpty()) totalHeight += lh * (1 + cachedFriends.size());
        if (!cachedTruces.isEmpty()) totalHeight += lh * (1 + cachedTruces.size());
        if (!cachedEnemies.isEmpty()) totalHeight += lh * (1 + cachedEnemies.size());
        cachedTotalHeight = totalHeight;
        cachedMaxWidth = measureMaxWidth(mc.fontRendererObj);
    }

    private int measureMaxWidth(FontRenderer fr) {
        int maxW = Math.max(fr.getStringWidth("Friends"), fr.getStringWidth("Enemies"));
        maxW = Math.max(maxW, fr.getStringWidth("Truce"));
        for (PlayerEntry e : cachedFriends) {
            maxW = Math.max(maxW, measureEntryWidth(fr, e));
        }
        for (PlayerEntry e : cachedTruces) {
            maxW = Math.max(maxW, measureEntryWidth(fr, e));
        }
        for (PlayerEntry e : cachedEnemies) {
            maxW = Math.max(maxW, measureEntryWidth(fr, e));
        }
        return Math.max(20, maxW);
    }

    private static int darkenColor(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private static int getTypeLetterColor(EntryType type) {
        switch (type) {
            case FRIENDS:
                return 0x55FF55;
            case TRUCE:
                return TruceManager.COLOR_RGB;
            case ENEMIES:
            default:
                return 0xFF5555;
        }
    }

    private static char getTypeLetter(EntryType type) {
        switch (type) {
            case FRIENDS:
                return 'F';
            case TRUCE:
                return 'T';
            case ENEMIES:
            default:
                return 'E';
        }
    }

    private int drawTypePrefix(FontRenderer fr, EntryType type, int x, int y) {
        int letterColor = getTypeLetterColor(type);
        int bracketColor = darkenColor(letterColor, 0.55f);
        char letter = getTypeLetter(type);

        fr.drawStringWithShadow("[", x, y, bracketColor);
        x += fr.getStringWidth("[");
        fr.drawStringWithShadow(String.valueOf(letter), x, y, letterColor);
        x += fr.getStringWidth(String.valueOf(letter));
        fr.drawStringWithShadow("]", x, y, bracketColor);
        x += fr.getStringWidth("]");
        return x;
    }

    private int getTypePrefixWidth(FontRenderer fr) {
        return fr.getStringWidth("[E]");
    }

    private static class ListSection {
        private final String title;
        private final int titleColor;
        private final List<PlayerEntry> players;

        private ListSection(String title, int titleColor, List<PlayerEntry> players) {
            this.title = title;
            this.titleColor = titleColor;
            this.players = players;
        }
    }

    private int measureEntryWidth(FontRenderer fr, PlayerEntry e) {
        int width = getTypePrefixWidth(fr);
        String nameToRender = (e.displayName != null ? e.displayName : e.name);
        nameToRender = nameToRender.replaceAll("(?i)\\s+(?=(§[0-9a-fk-or])*$)", "");
        width += fr.getStringWidth(nameToRender);

        boolean hasExtra = (showPants.enabled && !e.pantsEnchants.isEmpty()) || e.distance >= 0;
        if (hasExtra) {
            width += fr.getStringWidth(" - ");
        }

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
            String distText = (e.distance < 15 ? "§a" : (e.distance < 50 ? "§6" : "§c")) + e.distance;
            width += fr.getStringWidth(distText);
            if (e.arrow != null && !e.arrow.isEmpty()) {
                String arrowText = (e.distance < 15 ? "§a" : (e.distance < 50 ? "§6" : "§c")) + e.arrow;
                width += (int) (fr.getStringWidth(arrowText) * 1.25f) + 2;
            }
            width += fr.getStringWidth(" §7- ");
            if (showRegion.enabled) {
                if (e.zone != null && !e.zone.equals("Unknown")) {
                    width += fr.getStringWidth("§e§l" + e.zone.toUpperCase());
                } else if (e.inSpawn) {
                    width += fr.getStringWidth("§a§lSPAWN");
                }
            }
        }
        return width;
    }

    private void rebuildList() {
        List<PlayerListTabSync.TabPlayer> tabPlayers = PlayerListTabSync.collectTabPlayers(mc);
        Map<String, PlayerListTabSync.TabPlayer> tabByUuid = new HashMap<>();
        Map<String, String> tabInfo = new HashMap<>();
        for (PlayerListTabSync.TabPlayer tp : tabPlayers) {
            tabByUuid.put(tp.uuid, tp);
            tabByUuid.putIfAbsent(tp.uuid.replace("-", ""), tp);
            tabInfo.put(tp.name.toLowerCase(), tp.formattedName);
        }

        Map<String, EntryType> shouldShow = new HashMap<>();
        Set<String> currentKosUuids = new HashSet<>();

        if (KOSManager.isEnabled()) {
            for (Map.Entry<String, String> kos : KOSManager.getKosPlayers().entrySet()) {
                PlayerListTabSync.TabPlayer tp = findTabPlayer(tabByUuid, kos.getKey());
                if (tp == null) {
                    continue;
                }
                String renamedFrom = PlayerListTabSync.syncRename(PlayerListStore.ListType.KOS, tp.uuid, tp.name);
                shouldShow.put(tp.name.toLowerCase(), EntryType.ENEMIES);
                currentKosUuids.add(tp.uuid);

                if (!firstRun && joinNotify.enabled) {
                    if (!kosUuidsInTab.contains(tp.uuid)) {
                        com.pitlite.utils.NotificationManager.show(
                                PlayerListTabSync.formatJoinMessage(PlayerListStore.ListType.KOS, tp.name, renamedFrom),
                                5000);
                        if (joinSound.enabled) {
                            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(
                                    new net.minecraft.util.ResourceLocation("random.levelup"), 1.0F));
                        }
                    } else if (renamedFrom != null) {
                        com.pitlite.utils.NotificationManager.show(
                                PlayerListTabSync.formatRenameMessage(PlayerListStore.ListType.KOS, tp.name, renamedFrom),
                                5000);
                    }
                }
            }
        }

        for (Map.Entry<String, String> friend : FriendManager.getFriends().entrySet()) {
            PlayerListTabSync.TabPlayer tp = findTabPlayer(tabByUuid, friend.getKey());
            if (tp == null) {
                continue;
            }
            PlayerListTabSync.syncRename(PlayerListStore.ListType.FRIEND, tp.uuid, tp.name);
            shouldShow.putIfAbsent(tp.name.toLowerCase(), EntryType.FRIENDS);
        }

        for (Map.Entry<String, String> truce : TruceManager.getTrucePlayers().entrySet()) {
            PlayerListTabSync.TabPlayer tp = findTabPlayer(tabByUuid, truce.getKey());
            if (tp == null) {
                continue;
            }
            PlayerListTabSync.syncRename(PlayerListStore.ListType.TRUCE, tp.uuid, tp.name);
            shouldShow.putIfAbsent(tp.name.toLowerCase(), EntryType.TRUCE);
        }

        entries.removeIf(e -> !shouldShow.containsKey(e.name.toLowerCase()));

        Set<String> existing = new HashSet<>();
        for (PlayerEntry e : entries)
            existing.add(e.name.toLowerCase());

        for (Map.Entry<String, EntryType> entry : shouldShow.entrySet()) {
            if (existing.contains(entry.getKey())) {
                for (PlayerEntry e : entries) {
                    if (e.name.equalsIgnoreCase(entry.getKey())) {
                        e.type = entry.getValue();
                        String tabFormatted = tabInfo.get(entry.getKey());
                        if (tabFormatted != null)
                            e.displayName = tabFormatted;
                    }
                }
                continue;
            }

            String nameKey = entry.getKey();
            String displayName = nameKey;
            for (PlayerListTabSync.TabPlayer tp : tabPlayers) {
                if (tp.name.equalsIgnoreCase(nameKey)) {
                    displayName = tp.name;
                    break;
                }
            }

            PlayerEntry e = new PlayerEntry();
            e.name = displayName;
            e.displayName = tabInfo.get(nameKey);
            if (e.displayName == null)
                e.displayName = displayName;

            e.distance = -1;
            e.inSpawn = false;
            e.zone = "Unknown";
            e.arrow = "";
            e.pantsEnchants = new ArrayList<>();
            e.type = entry.getValue();
            e.entity = mc.theWorld != null ? mc.theWorld.getPlayerEntityByName(displayName) : null;
            entries.add(e);
        }

        firstRun = false;

        kosUuidsInTab.clear();
        kosUuidsInTab.addAll(currentKosUuids);
    }

    private static PlayerListTabSync.TabPlayer findTabPlayer(Map<String, PlayerListTabSync.TabPlayer> tabByUuid,
            String uuidKey) {
        if (uuidKey == null) {
            return null;
        }
        PlayerListTabSync.TabPlayer tp = tabByUuid.get(com.pitlite.utils.ProfileLookup.normalizeUuid(uuidKey));
        if (tp == null) {
            tp = tabByUuid.get(uuidKey.toLowerCase().replace("-", ""));
        }
        return tp;
    }

    private List<EnchantEntry> extractPantsEnchants(EntityPlayer player) {
        List<EnchantEntry> result = new ArrayList<>();
        ItemStack legs = player.getEquipmentInSlot(2);
        if (legs == null || !(legs.getItem() instanceof ItemArmor))
            return result;

        ItemArmor armor = (ItemArmor) legs.getItem();
        if (armor.armorType != 2 || armor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER)
            return result;

        String pantsType = "leather";
        if (armor.hasColor(legs)) {
            int color = armor.getColor(legs);
            if (color == 0xFF5555 || color == 0xB02E26 || color == 11546150)
                pantsType = "rage";
            if (color == 0 || color == 0x1D1D21 || color == 0x000000 || color == 0x191919)
                pantsType = "dark";
        }
        String dispName = StringUtils.stripControlCodes(legs.getDisplayName()).toLowerCase();
        if (dispName.contains("rage pant"))
            pantsType = "rage";
        if (legs.hasTagCompound() && legs.getTagCompound().hasKey("display")) {
            NBTTagCompound tagDisplay = legs.getTagCompound().getCompoundTag("display");
            if (tagDisplay.hasKey("Lore", 9)) {
                NBTTagList lore = tagDisplay.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
                    if (line.contains("regularity")) {
                        pantsType = "rage";
                        break;
                    }
                    if (line.contains("somber")) {
                        pantsType = "dark";
                        break;
                    }
                }
            }
        }

        String[][] enchantTable;
        switch (pantsType) {
            case "rage":
                enchantTable = RAGE_ENCHANTS;
                break;
            case "dark":
                enchantTable = DARK_ENCHANTS;
                break;
            default:
                enchantTable = LEATHER_ENCHANTS;
                break;
        }

        List<String> foundAbbrevs = new ArrayList<>();
        if (legs.hasTagCompound()) {
            NBTTagCompound tagDisplay = legs.getTagCompound().getCompoundTag("display");
            if (tagDisplay.hasKey("Lore", 9)) {
                NBTTagList lore = tagDisplay.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String cleanLine = StringUtils.stripControlCodes(lore.getStringTagAt(i));
                    if (cleanLine.trim().isEmpty() || cleanLine.contains("Tier") || cleanLine.contains("Kept on death")
                            || cleanLine.contains("Lives:"))
                        continue;
                    for (String[] pair : enchantTable) {
                        if (cleanLine.toLowerCase().contains(pair[0].toLowerCase())) {
                            if (!foundAbbrevs.contains(pair[1])) {
                                foundAbbrevs.add(pair[1]);
                            }
                            break;
                        }
                    }
                }
            }
        }

        for (String abbrev : foundAbbrevs) {
            int c = ENCHANT_COLORS.getOrDefault(abbrev, 0xFFFFFF);
            result.add(new EnchantEntry(abbrev, c));
        }
        return result;
    }

    private String getArrow(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = mc.thePlayer.rotationYaw % 360;
        double rel = ((angleToTarget - playerYaw) % 360 + 360) % 360;
        if (rel >= 337.5 || rel < 22.5)
            return "↑";
        else if (rel >= 22.5 && rel < 67.5)
            return "↗";
        else if (rel >= 67.5 && rel < 112.5)
            return "→";
        else if (rel >= 112.5 && rel < 157.5)
            return "↘";
        else if (rel >= 157.5 && rel < 202.5)
            return "↓";
        else if (rel >= 202.5 && rel < 247.5)
            return "↙";
        else if (rel >= 247.5 && rel < 292.5)
            return "←";
        else if (rel >= 292.5 && rel < 337.5)
            return "↖";
        else
            return "↖";
    }

    public void renderStacked() {
        if (mc.thePlayer == null || entries.isEmpty())
            return;

        FontRenderer fr = mc.fontRendererObj;
        int lh = fr.FONT_HEIGHT + 2;

        int posX = getRenderX();
        int posY = getRenderY();

        List<ListSection> sections = new ArrayList<>();
        if (!cachedFriends.isEmpty()) {
            sections.add(new ListSection("Friends", 0x55FF55, cachedFriends));
        }
        if (!cachedTruces.isEmpty()) {
            sections.add(new ListSection("Truce", TruceManager.COLOR_RGB, cachedTruces));
        }
        if (!cachedEnemies.isEmpty()) {
            sections.add(new ListSection("Enemies", 0xFF5555, cachedEnemies));
        }

        sections.sort((a, b) -> Integer.compare(b.players.size(), a.players.size()));

        float y = posY;
        for (ListSection section : sections) {
            fr.drawStringWithShadow(section.title, posX, (int) y, section.titleColor);
            y += lh;
            for (PlayerEntry e : section.players) {
                y = drawPlayerEntry(fr, e, posX, y, lh);
            }
        }
    }

    private float drawPlayerEntry(FontRenderer fr, PlayerEntry e, int posX, float y, int lh) {
        int currentX = drawTypePrefix(fr, e.type, posX, (int) y);

        String nameToRender = (e.displayName != null ? e.displayName : e.name);
        nameToRender = nameToRender.replaceAll("(?i)\\s+(?=(§[0-9a-fk-or])*$)", "");
        fr.drawStringWithShadow(nameToRender, currentX, (int) y, 0xFFFFFF);
        currentX += fr.getStringWidth(nameToRender);

        boolean hasExtra = (showPants.enabled && !e.pantsEnchants.isEmpty()) || e.distance >= 0;
        if (hasExtra) {
            fr.drawStringWithShadow(" - ", currentX, (int) y, 0xFFFFFF);
            currentX += fr.getStringWidth(" - ");
        }

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

            String distanceColor = "§c";
            if (e.distance < 15)
                distanceColor = "§a";
            else if (e.distance < 50)
                distanceColor = "§6";

            String distText = distanceColor + e.distance;
            fr.drawStringWithShadow(distText, currentX, (int) y, 0xFFFFFF);
            currentX += fr.getStringWidth(distText);

            if (e.arrow != null && !e.arrow.isEmpty()) {
                String arrowText = distanceColor + e.arrow;
                float arrowWidth = fr.getStringWidth(arrowText);

                GlStateManager.pushMatrix();
                GlStateManager.translate(currentX + 1, y - 1, 0);
                GlStateManager.scale(1.25f, 1.25f, 1.0f);
                fr.drawStringWithShadow(arrowText, 0, 0, 0xFFFFFF);
                GlStateManager.popMatrix();

                currentX += (arrowWidth * 1.25f) + 2;
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

        return y + lh;
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
