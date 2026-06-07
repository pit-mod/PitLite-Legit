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

public class RageList extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 50.0;
    {
    }
    private final BooleanSetting showPants = new BooleanSetting("Show Pants", true);
    private final BooleanSetting showRegion = new BooleanSetting("Show Region", true);
    private final BooleanSetting onlyRegularity = new BooleanSetting("Only Regularity", true);

    private static class EnchantEntry {
        String abbrev;
        int color;
        int priority;

        EnchantEntry(String abbrev, int color, int priority) {
            this.abbrev = abbrev;
            this.color = color;
            this.priority = priority;
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
    private static final Map<String, Integer> ENCHANT_PRIORITIES = new HashMap<>();
    static {
        addEnchant("reg", 0xFF5555, 1);
        addEnchant("soli", 0x55FFFF, 2);
        addEnchant("rgm", 0x55FFFF, 3);
        addEnchant("abs", 0xFFA500, 4);
        addEnchant("fast", 0xFFFFAA, 5);
        addEnchant("pero", 0xFF5555, 6);
        addEnchant("boo", 0xFF5555, 7);
        addEnchant("pod", 0xFF5555, 8);
        addEnchant("sw", 0x55FFFF, 10);
        addEnchant("blob", 0x55FF55, 11);
        addEnchant("prick", 0xFF5555, 12);
        addEnchant("gh", 0xFFFF55, 13);
        addEnchant("sco", 0xFFFF55, 14);
        addEnchant("nego", 0xFFFF55, 15);
        addEnchant("cf", 0xFFFF55, 16);
        addEnchant("french", 0xFFFF55, 17);
    }

    private static void addEnchant(String name, int color, int priority) {
        ENCHANT_COLORS.put(name, color);
        ENCHANT_PRIORITIES.put(name, priority);
    }

    private static final String[][] RAGE_ENCHANTS = {
            { "Regularity", "reg" },
            { "Solitude", "soli" },
            { "Retro-Gravity Microcosm", "rgm" },
            { "Respawn: Absorption", "abs" },
            { "Gotta Go Fast", "fast" },
            { "Peroxide", "pero" },
            { "Boo-boo", "boo" },
            { "Escape Pod", "pod" },
            { "Do it like the French", "french" },
            { "Heigh-Ho", "hh" },
            { "New Deal", "nd" },
            { "\"Not\" Gladiator", "notglad" },
            { "Sweaty", "sw" },
            { "Negotiator", "nego" },
            { "Self-checkout", "sco" },
            { "Critically Funky", "cf" },
            { "Golden Heart", "gh" },
            { "Mirror", "mirror" },
            { "Prick", "prick" },
            { "Escape Pod", "pod" },
            { "Phoenix", "phx" },
            { "Pit Blob", "blob" },
            { "Singularity", "sing" },
    };

    public RageList() {
        super("Rage List", "Shows Rage players in your lobby.", Category.RENDER);
        addSetting(showPants);
        addSetting(showRegion);
        addSetting(onlyRegularity);
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
            e.entity = LobbyPlayerIndex.findByName(e.name);
            if (e.entity != null) {
                e.distance = (int) mc.thePlayer.getDistanceToEntity(e.entity);
                e.inSpawn = PitMapManager.isInSpawn(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.zone = PitMapManager.getZone(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.arrow = getArrow(e.entity);
                if (showPants.enabled) {
                    e.pantsEnchants = extractPantsEnchants(e.entity);
                }
            } else {
                e.distance = -1;
            }
        }

        entries.sort((a, b) -> {
            if (a.inSpawn && !b.inSpawn)
                return 1;
            if (!a.inSpawn && b.inSpawn)
                return -1;
            if (a.distance < 0)
                return 1;
            if (b.distance < 0)
                return -1;
            return Integer.compare(a.distance, b.distance);
        });

        int lh = mc.fontRendererObj.FONT_HEIGHT + 2;
        cachedTotalHeight = lh * (1 + entries.size());
        FontRenderer fr = mc.fontRendererObj;
        int maxW = fr.getStringWidth(onlyRegularity.enabled ? "Regularity" : "Rage List");
        for (PlayerEntry e : entries) {
            maxW = Math.max(maxW, measureEntryWidth(fr, e));
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
                        net.minecraft.scoreboard.ScorePlayerTeam team = mc.theWorld.getScoreboard()
                                .getPlayersTeam(name);
                        if (team != null)
                            formatted = team.formatString(name);
                    }
                    tabInfo.put(name.toLowerCase(), formatted != null ? formatted : name);
                }
            }
        }

        List<EntityPlayer> targets = new ArrayList<>();
        if (mc.theWorld != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer)
                    continue;
                ItemStack legs = player.getEquipmentInSlot(2);
                if (legs != null && isRagePants(legs)) {
                    if (!onlyRegularity.enabled || getRegularityLevel(legs) >= 1) {
                        targets.add(player);
                    }
                }
            }
        }

        entries.removeIf(e -> targets.stream().noneMatch(p -> p.getName().equals(e.name)));

        for (EntityPlayer p : targets) {
            PlayerEntry existing = entries.stream().filter(e -> e.name.equals(p.getName())).findFirst().orElse(null);
            if (existing != null) {
                existing.entity = p;
                String dn = tabInfo.get(p.getName().toLowerCase());
                if (dn != null)
                    existing.displayName = dn;
                else
                    existing.displayName = p.getDisplayName().getFormattedText();
                continue;
            }
            PlayerEntry e = new PlayerEntry();
            e.name = p.getName();
            e.displayName = tabInfo.get(p.getName().toLowerCase());
            if (e.displayName == null)
                e.displayName = p.getDisplayName().getFormattedText();
            e.entity = p;
            entries.add(e);
        }
    }

    private boolean isRagePants(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemArmor))
            return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 2 || armor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER)
            return false;
        if (armor.hasColor(stack)) {
            int color = armor.getColor(stack);
            if (color == 0xFF5555 || color == 0xB02E26 || color == 11546150)
                return true;
        }
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        if (name.contains("rage pant"))
            return true;

        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("display")) {
            NBTTagCompound tagDisplay = stack.getTagCompound().getCompoundTag("display");
            if (tagDisplay.hasKey("Lore", 9)) {
                NBTTagList lore = tagDisplay.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    if (StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase().contains("regularity"))
                        return true;
                }
            }
        }
        return false;
    }

    private int getRegularityLevel(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound())
            return 0;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9))
            return 0;
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int i = 0; i < lore.tagCount(); i++) {
            String line = StringUtils.stripControlCodes(lore.getStringTagAt(i));
            if (line.contains("Regularity III"))
                return 3;
            if (line.contains("Regularity II"))
                return 2;
            if (line.contains("Regularity I"))
                return 1;
        }
        return 0;
    }

    private List<EnchantEntry> extractPantsEnchants(EntityPlayer player) {
        List<EnchantEntry> result = new ArrayList<>();
        ItemStack legs = player.getEquipmentInSlot(2);
        if (legs == null || !legs.hasTagCompound())
            return result;

        NBTTagCompound display = legs.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9))
            return result;
        NBTTagList lore = display.getTagList("Lore", 8);

        for (int i = 0; i < lore.tagCount(); i++) {
            String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
            if (line.trim().isEmpty() || line.contains("tier") || line.contains("kept on death")
                    || line.contains("lives:"))
                continue;
            for (String[] pair : RAGE_ENCHANTS) {
                if (line.contains(pair[0].toLowerCase())) {
                    String abbrev = pair[1];
                    int color = ENCHANT_COLORS.getOrDefault(abbrev, 0xFFFFFF);
                    int priority = ENCHANT_PRIORITIES.getOrDefault(abbrev, 99);
                    if (result.stream().noneMatch(en -> en.abbrev.equals(abbrev))) {
                        result.add(new EnchantEntry(abbrev, color, priority));
                    }
                }
            }
        }

        result.sort(Comparator.comparingInt(en -> en.priority));
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
        else
            return "↖";
    }

    public int getPlayerCount() {
        return entries.size();
    }

    public int getConfigY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    public void renderStacked() {
        if (mc.thePlayer == null || entries.isEmpty())
            return;

        FontRenderer fr = mc.fontRendererObj;
        int lh = fr.FONT_HEIGHT + 2;
        int posX = getRenderX();
        int posY = getRenderY();

        fr.drawStringWithShadow(onlyRegularity.enabled ? "Regularity" : "Rage List", posX, posY, 0xFF5555);
        float y = posY + lh;

        for (PlayerEntry e : entries) {
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
