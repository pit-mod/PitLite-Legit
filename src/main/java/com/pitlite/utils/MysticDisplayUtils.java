package com.pitlite.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MysticDisplayUtils {
    private static final int MYSTIC_COLOR = 0xFF55FF;

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

    public static class Label {
        public final String text;
        public final int color;

        public Label(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    private MysticDisplayUtils() {
    }

    public static List<Label> collect(EntityPlayer player) {
        List<Label> labels = new ArrayList<>();
        if (player == null) {
            return labels;
        }
        addPantsMystic(player.getEquipmentInSlot(2), labels);
        addHelmetMystic(player.getEquipmentInSlot(4), labels);
        return labels;
    }

    private static void addHelmetMystic(ItemStack stack, List<Label> labels) {
        if (!hasMysticNonce(stack)) {
            return;
        }
        Item item = stack.getItem();
        if (item instanceof ItemArmor && ((ItemArmor) item).armorType == 0 && item == Items.golden_helmet) {
            labels.add(new Label("Helm" + getTierSuffix(stack), MYSTIC_COLOR));
        }
    }

    private static void addPantsMystic(ItemStack legs, List<Label> labels) {
        if (legs == null || !(legs.getItem() instanceof ItemArmor)) {
            return;
        }
        ItemArmor armor = (ItemArmor) legs.getItem();
        if (armor.armorType != 2 || armor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) {
            return;
        }
        if (!hasMysticNonce(legs) && !hasMysticLore(legs)) {
            return;
        }

        String pantsType = getPantsType(legs, armor);

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
                            || cleanLine.contains("Lives:")) {
                        continue;
                    }
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
            labels.add(new Label(abbrev, ENCHANT_COLORS.getOrDefault(abbrev, 0xFFFFFF)));
        }
    }

    private static String getPantsType(ItemStack legs, ItemArmor armor) {
        String pantsType = "leather";
        if (armor.hasColor(legs)) {
            int color = armor.getColor(legs);
            if (color == 0xFF5555 || color == 0xB02E26 || color == 11546150) {
                pantsType = "rage";
            }
            if (color == 0 || color == 0x1D1D21 || color == 0x000000 || color == 0x191919) {
                pantsType = "dark";
            }
        }
        String dispName = StringUtils.stripControlCodes(legs.getDisplayName()).toLowerCase();
        if (dispName.contains("rage")) {
            pantsType = "rage";
        }
        if (dispName.contains("dark")) {
            pantsType = "dark";
        }
        if (dispName.contains("aqua")) {
            pantsType = "aqua";
        }
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
        return pantsType;
    }

    private static boolean hasMysticNonce(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && DenickUtils.extractNonceFromNBT(stack.getTagCompound()) != null;
    }

    private static boolean hasMysticLore(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) {
            return false;
        }
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int i = 0; i < lore.tagCount(); i++) {
            String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
            if (line.contains("tier") || line.contains("lives:")) {
                return true;
            }
        }
        return false;
    }

    private static String getTierSuffix(ItemStack stack) {
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        if (name.contains("fresh")) {
            return "F";
        }
        if (stack.hasTagCompound()) {
            NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            if (display.hasKey("Lore", 9)) {
                NBTTagList lore = display.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
                    if (line.contains("tier iii") || line.contains("tier: iii") || line.contains("tier 3")) {
                        return "3";
                    }
                    if (line.contains("tier ii") || line.contains("tier: ii") || line.contains("tier 2")) {
                        return "2";
                    }
                    if (line.contains("tier i") || line.contains("tier: i") || line.contains("tier 1")) {
                        return "1";
                    }
                    if (line.contains("fresh") || line.contains("used in the mystic well")) {
                        return "F";
                    }
                }
            }
        }
        return "";
    }
}
