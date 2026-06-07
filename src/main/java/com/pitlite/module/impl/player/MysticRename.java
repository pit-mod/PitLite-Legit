package com.pitlite.module.impl.player;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import com.pitlite.module.Category;
import com.pitlite.module.Module;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class MysticRename extends Module {

    private static final java.util.Map<ItemStack, String> NAME_CACHE = new java.util.WeakHashMap<>();
    private static MysticRename instance;

    private static final Map<String, String> SWORD_ENCHANTS = new HashMap<>();
    private static final Map<String, String> SWORD_COLORS = new HashMap<>();
    private static final Map<String, String> PANTS_ENCHANTS = new HashMap<>();
    private static final Map<String, String> PANTS_COLORS = new HashMap<>();
    private static final Map<String, String> BOW_ENCHANTS = new HashMap<>();
    private static final Map<String, String> BOW_COLORS = new HashMap<>();

    static {
        addSwordEnchant("gamble", "gamble", "§e");
        addSwordEnchant("healer", "healer", "§a");
        addSwordEnchant("punch", "punch", "§9");
        addSwordEnchant("executioner", "exe", "§c");
        addSwordEnchant("stun", "stun", "§e");
        addSwordEnchant("perun", "perun", "§e");
        addSwordEnchant("billionaire", "bill", "§e");
        addSwordEnchant("lifesteal", "ls", "§c");
        addSwordEnchant("duelist", "duelist", "§c");
        addSwordEnchant("combo: heal", "cheal", "§c");
        addSwordEnchant("bullet time", "bt", "§5");
        addSwordEnchant("bruiser", "bruiser", "§d");
        addSwordEnchant("berserker", "bers", "§c");
        addSwordEnchant("pants radar", "pr", "§a");
        addSwordEnchant("gold bump", "gbump", "§e");
        addSwordEnchant("gold boost", "gboost", "§e");
        addSwordEnchant("sweaty", "sw", "§9");
        addSwordEnchant("sharp", "sharp", "§c");
        addSwordEnchant("shark", "shark", "§c");
        addSwordEnchant("pain focus", "pf", "§c");
        addSwordEnchant("punisher", "punisher", "§c");
        addSwordEnchant("king buster", "kb", "§c");
        addSwordEnchant("gold and boosted", "gab", "§e");
        addSwordEnchant("diamond stomp", "ds", "§c");
        addSwordEnchant("combo: damage", "cd", "§c");

        addPantsEnchant("combo: venom", "venom", "§8");
        addPantsEnchant("misery", "misery", "§5");
        addPantsEnchant("spite", "spite", "§5");
        addPantsEnchant("mind assault", "mind", "§5");
        addPantsEnchant("golden handcuffs", "cuffs", "§6");
        addPantsEnchant("nostalgia", "nost", "§b");
        addPantsEnchant("grim reaper", "reaper", "§5");
        addPantsEnchant("hedge fund", "hedge", "§a");
        addPantsEnchant("heartripper", "heart", "§c");
        addPantsEnchant("lycanthropy", "lycan", "§5");
        addPantsEnchant("regularity", "reg", "§4");
        addPantsEnchant("solitude", "soli", "§b");
        addPantsEnchant("retro-gravity microcosm", "rgm", "§b");
        addPantsEnchant("respawn: absorption", "abs", "§6");
        addPantsEnchant("gotta go fast", "fast", "§e");
        addPantsEnchant("peroxide", "pero", "§c");
        addPantsEnchant("boo-boo", "boo", "§c");
        addPantsEnchant("escape pod", "pod", "§c");
        addPantsEnchant("do it like the french", "french", "§e");
        addPantsEnchant("pit blob", "blob", "§a");
        addPantsEnchant("sweaty", "sw", "§b");
        addPantsEnchant("\"not\" gladiator", "notglad", "§f");
        addPantsEnchant("negotiator", "nego", "§e");
        addPantsEnchant("self-checkout", "sco", "§e");
        addPantsEnchant("pants radar", "pr", "§a");
        addPantsEnchant("counter-offensive", "co", "§f");
        addPantsEnchant("critically funky", "cf", "§e");
        addPantsEnchant("golden heart", "gh", "§e");
        addPantsEnchant("mirror", "mirror", "§f");
        addPantsEnchant("prick", "prick", "§c");
        addPantsEnchant("phoenix", "phx", "§f");
        addPantsEnchant("singularity", "sing", "§f");
        addPantsEnchant("paparazzi", "papa", "§f");
        addPantsEnchant("heigh-ho", "hh", "§f");
        addPantsEnchant("new deal", "nd", "§f");

        addBowEnchant("chipping", "chip", "§c");
        addBowEnchant("faster than their shadow", "ftts", "§e");
        addBowEnchant("parasite", "para", "§c");
        addBowEnchant("pin down", "pin", "§9");
        addBowEnchant("push comes to shove", "pcts", "§5");
        addBowEnchant("sprint drain", "drain", "§e");
        addBowEnchant("wasp", "wasp", "§e");
        addBowEnchant("devil chicks", "dc", "§c");
        addBowEnchant("explosive", "explo", "§c");
        addBowEnchant("mega longbow", "mlb", "§a");
        addBowEnchant("pullbow", "pull", "§c");
        addBowEnchant("telebow", "tele", "§5");
        addBowEnchant("volley", "volley", "§f");
        addBowEnchant("robinhood", "robinhood", "§f");
    }

    private static void addSwordEnchant(String enchant, String abbrev, String color) {
        SWORD_ENCHANTS.put(enchant, abbrev);
        SWORD_COLORS.put(enchant, color);
    }

    private static void addPantsEnchant(String enchant, String abbrev, String color) {
        PANTS_ENCHANTS.put(enchant, abbrev);
        PANTS_COLORS.put(enchant, color);
    }

    private static void addBowEnchant(String enchant, String abbrev, String color) {
        BOW_ENCHANTS.put(enchant, abbrev);
        BOW_COLORS.put(enchant, color);
    }

    public MysticRename() {
        super("Mystic Rename", "Renames items client-side based on their enchants.", Category.PLAYER);
        instance = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        NAME_CACHE.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        NAME_CACHE.clear();
    }

    public static boolean isEnabled() {
        return instance != null && instance.isToggled();
    }

    public static String getCustomName(ItemStack stack) {
        if (!isEnabled())
            return null;
        if (stack == null)
            return null;
        if (NAME_CACHE.containsKey(stack))
            return NAME_CACHE.get(stack);

        String result = getEnchantName(stack);
        NAME_CACHE.put(stack, result);
        return result;
    }

    private static String getEnchantName(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound())
            return null;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9))
            return null;
        NBTTagList lore = display.getTagList("Lore", 8);

        boolean isSword = stack.getItem() instanceof net.minecraft.item.ItemSword;
        boolean isPants = stack.getItem() instanceof net.minecraft.item.ItemArmor
                && ((net.minecraft.item.ItemArmor) stack.getItem()).armorType == 2;
        boolean isBow = stack.getItem() instanceof net.minecraft.item.ItemBow;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lore.tagCount(); i++) {
            String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
            if (line.trim().isEmpty() || line.contains("tier") || line.contains("kept on death")
                    || line.contains("lives:"))
                continue;

            if (isSword) {
                for (Map.Entry<String, String> entry : SWORD_ENCHANTS.entrySet()) {
                    if (line.contains(entry.getKey())) {
                        String color = SWORD_COLORS.getOrDefault(entry.getKey(), "§d");
                        if (sb.length() > 0)
                            sb.append("§f/");
                        sb.append(color).append(entry.getValue());
                        break;
                    }
                }
            } else if (isPants) {
                for (Map.Entry<String, String> entry : PANTS_ENCHANTS.entrySet()) {
                    if (line.contains(entry.getKey())) {
                        String color = PANTS_COLORS.getOrDefault(entry.getKey(), "§f");
                        if (sb.length() > 0)
                            sb.append("§f/");
                        sb.append(color).append(entry.getValue());
                        break;
                    }
                }
            } else if (isBow) {
                for (Map.Entry<String, String> entry : BOW_ENCHANTS.entrySet()) {
                    if (line.contains(entry.getKey())) {
                        String color = BOW_COLORS.getOrDefault(entry.getKey(), "§f");
                        if (sb.length() > 0)
                            sb.append("§f/");
                        sb.append(color).append(entry.getValue());
                        break;
                    }
                }
            }
        }

        if (sb.length() == 0)
            return null;
        return sb.toString();
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!isToggled()) return;
        modifyTooltip(event.itemStack, event.toolTip);
        String customName = getCustomName(event.itemStack);
        if (customName != null && !event.toolTip.isEmpty()) {
            event.toolTip.set(0, customName);
        }
    }

    private static int detectTier(ItemStack stack, java.util.List<String> tooltip) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (tooltip != null) {
            lines.addAll(tooltip);
        }
        if (stack != null && stack.hasTagCompound()) {
            NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            if (display.hasKey("Name", 8)) {
                lines.add(display.getString("Name"));
            }
        }

        int tier = -1;
        for (String line : lines) {
            String cleanLine = StringUtils.stripControlCodes(line).toLowerCase();
            if (cleanLine.contains("fresh") || cleanLine.contains("used in the mystic well")) {
                tier = 1;
                continue;
            }
            if (cleanLine.contains("tier: iii") || cleanLine.contains("tier 3") || cleanLine.contains("tier iii")) {
                tier = 3;
                break;
            }
            if (cleanLine.contains("tier: ii") || cleanLine.contains("tier 2") || cleanLine.contains("tier ii")) {
                tier = 2;
                break;
            }
            if (cleanLine.contains("tier: i") || cleanLine.contains("tier 1")
                    || (cleanLine.contains("tier i") && !cleanLine.contains("tier ii") && !cleanLine.contains("tier iii"))) {
                tier = 1;
                break;
            }
        }
        return tier;
    }

    public static void modifyTooltip(ItemStack stack, java.util.List<String> tooltip) {
        if (!isEnabled())
            return;
        if (stack == null || tooltip == null)
            return;

        boolean isMystic = false;
        String name = stack.getItem().getUnlocalizedName().toLowerCase();
        if (stack.getItem() instanceof ItemSword && name.contains("gold"))
            isMystic = true;
        if (stack.getItem() instanceof ItemBow)
            isMystic = true;
        if (stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).armorType == 2
                && name.contains("leather"))
            isMystic = true;

        String displayName = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        if (displayName.contains("aqua") || displayName.contains("rage") || displayName.contains("sewer")
                || displayName.contains("dark"))
            return;

        Integer nonce = com.pitlite.utils.DenickUtils.extractNonceFromNBT(stack.getTagCompound());
        if (!isMystic && nonce == null)
            return;

        int tier = detectTier(stack, tooltip);

        if (tier == 1 || tier == 2) {
            if (nonce != null) {
                int colorIndex = Math.abs(nonce) % 5;
                String colorName;
                String colorCode;

                switch (colorIndex) {
                    case 0:
                        colorName = "Red";
                        colorCode = "§c";
                        break;
                    case 1:
                        colorName = "Yellow";
                        colorCode = "§e";
                        break;
                    case 2:
                        colorName = "Blue";
                        colorCode = "§9";
                        break;
                    case 3:
                        colorName = "Orange";
                        colorCode = "§6";
                        break;
                    case 4:
                        colorName = "Green";
                        colorCode = "§a";
                        break;
                    default:
                        return;
                }

                tooltip.add("");
                tooltip.add("§7Requires " + colorCode + colorName + " Pants§7 to Tier 3");
            }
        }
    }
}
