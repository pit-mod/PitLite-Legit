package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class InventoryUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int findItem(Predicate<ItemStack> filter) {
        if (mc.thePlayer == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && filter.test(stack)) {
                if (i < 9) return i + 36; // Hotbar in ContainerPlayer
                return i; // Upper inventory in ContainerPlayer
            }
        }
        return -1;
    }

    public static List<Integer> getBestTransitSlots(int count, int avoidHotbarSlot) {
        List<Integer> sortedSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) sortedSlots.add(i);

        sortedSlots.sort(Comparator.comparingInt(slot -> {
            if (slot == avoidHotbarSlot) return 1000;
            ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
            if (stack == null) return 0; // Empty is best
            Item item = stack.getItem();
            if (item == Item.getItemFromBlock(Blocks.obsidian) || item == Item.getItemFromBlock(Blocks.cobblestone)) return 10;
            if (item instanceof ItemPotion) return 20;
            if (item instanceof ItemFood) return 30;
            return 100; // Important tools/weapons
        }));

        return sortedSlots.subList(0, Math.min(count, sortedSlots.size()));
    }

    public static boolean isDarkPants(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 2 || armor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) return false;
        if (armor.hasColor(stack)) {
            int color = armor.getColor(stack);
            if (color == 0 || color == 0x1D1D21 || color == 0x000000 || color == 0x191919) return true;
        }
        return StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase().contains("dark pant");
    }

    public static boolean isCombatSpade(ItemStack stack) {
        if (stack == null) return false;
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        return (stack.getItem() instanceof ItemSpade && (name.contains("combat spade") || name.contains("diamond shovel")));
    }

    public static boolean isDiamondLeggings(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        return armor.armorType == 2 && name.contains("diamond") && armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND;
    }

    public static boolean isDiamondBoots(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        return armor.armorType == 3 && name.contains("diamond") && armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND;
    }

    public static boolean isVenomPants(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 2) return false;
        
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        return name.contains("venom") || name.contains("poison") || hasLore(stack, "Venom");
    }

    public static boolean isEscapePod(ItemStack stack) {
        return hasLore(stack, "Escape Pod");
    }

    public static boolean isArmageddonBoots(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 3) return false;
        
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        return name.contains("armageddon") || hasLore(stack, "Armageddon");
    }

    public static boolean isNonArmaBoots(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 3) return false;
        return !isArmageddonBoots(stack);
    }

    public static int findBestNonArmaBoots() {
        if (mc.thePlayer == null) return -1;
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && isNonArmaBoots(stack)) {
                int score = getBootScore(stack);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = (i < 9) ? i + 36 : i;
                }
            }
        }
        return bestSlot;
    }

    private static int getBootScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return 0;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND) return 100;
        if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.IRON) return 80;
        if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.CHAIN) return 60;
        if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD) return 40;
        if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) return 20;
        return 1;
    }

    public static boolean isPhoenix(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return false;
        ItemArmor armor = (ItemArmor) stack.getItem();
        if (armor.armorType != 2) return false;
        
        String name = StringUtils.stripControlCodes(stack.getDisplayName()).toLowerCase();
        if (name.contains("phoenix")) return true;
        return hasLore(stack, "Phoenix");
    }

    public static boolean isMirrorPants(EntityPlayer player) {
        if (player == null) return false;
        ItemStack legs = player.getEquipmentInSlot(2);
        if (legs == null || !(legs.getItem() instanceof ItemArmor)) return false;
        return hasLore(legs, "Mirror");
    }

    public static boolean isGambleSword(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return false;
        return hasLore(stack, "Gamble");
    }

    public static boolean hasLore(ItemStack stack, String text) {
        if (stack == null || !stack.hasTagCompound()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("display")) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore")) {
                NBTTagList lore = display.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
                    if (line.contains(text.toLowerCase())) return true;
                }
            }
        }
        return tag.toString().toLowerCase().contains(text.toLowerCase());
    }

    private static final java.util.Map<ItemStack, Integer> LIVES_CACHE = new java.util.WeakHashMap<>();

    public static int getLives(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return -1;
        if (LIVES_CACHE.containsKey(stack)) return LIVES_CACHE.get(stack);

        int result = -1;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("display")) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore")) {
                NBTTagList lore = display.getTagList("Lore", 8);
                for (int i = 0; i < lore.tagCount(); i++) {
                    String line = StringUtils.stripControlCodes(lore.getStringTagAt(i)).toLowerCase();
                    if (line.contains("lives:")) {
                        try {
                            if (line.contains("/")) {
                                String livesPart = line.split("lives:")[1].trim();
                                String currentLives = livesPart.split("/")[0].trim();
                                result = Integer.parseInt(currentLives);
                                break;
                            } else {
                                String currentLives = line.split("lives:")[1].trim();
                                result = Integer.parseInt(currentLives);
                                break;
                            }
                        } catch (Exception e) {
                            result = -1;
                        }
                    }
                }
            }
        }
        LIVES_CACHE.put(stack, result);
        return result;
    }
}
