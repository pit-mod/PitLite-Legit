package com.pitlite.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DenickUtils {
    public static Integer extractNonceFromNBT(NBTTagCompound tag) {
        if (tag == null) return null;
        if (tag.hasKey("ExtraAttributes")) {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra.hasKey("nonce")) return extra.getInteger("nonce");
            if (extra.hasKey("Nonce")) return extra.getInteger("Nonce");
        }
        for (String key : tag.getKeySet()) {
            net.minecraft.nbt.NBTBase base = tag.getTag(key);
            if (base instanceof NBTTagCompound) {
                Integer nonce = extractNonceFromNBT((NBTTagCompound) base);
                if (nonce != null) return nonce;
            } else if (base instanceof net.minecraft.nbt.NBTTagList) {
                net.minecraft.nbt.NBTTagList list = (net.minecraft.nbt.NBTTagList) base;
                for (int i = 0; i < list.tagCount(); ++i) {
                    NBTTagCompound compound = list.getCompoundTagAt(i);
                    Integer nonce = extractNonceFromNBT(compound);
                    if (nonce != null) return nonce;
                }
            }
        }
        if (tag.hasKey("nonce")) return tag.getInteger("nonce");
        if (tag.hasKey("Nonce")) return tag.getInteger("Nonce");
        return null;
    }

    public static boolean isUntrackableDenickPants(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        if (!(item instanceof ItemArmor) || ((ItemArmor) item).armorType != 2) {
            return false;
        }

        if (containsUntrackablePantsKeyword(getItemDisplayText(stack))) {
            return true;
        }

        if (stack.hasTagCompound()) {
            NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            if (display.hasKey("Name", 8)) {
                return containsUntrackablePantsKeyword(StringUtils.stripControlCodes(display.getString("Name")));
            }
        }
        return false;
    }

    private static boolean containsUntrackablePantsKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("aqua")
                || lower.contains("rage")
                || lower.contains("dark");
    }

    private static String getItemDisplayText(ItemStack stack) {
        return StringUtils.stripControlCodes(stack.getDisplayName());
    }

    public static boolean isDenickMysticItem(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !stack.hasTagCompound()) {
            return false;
        }
        if (extractNonceFromNBT(stack.getTagCompound()) == null) {
            return false;
        }

        Item item = stack.getItem();
        if (item instanceof ItemBow) {
            return true;
        }
        if (item instanceof ItemSword && item == Items.golden_sword) {
            return true;
        }
        if (item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            if (armor.armorType == 0 && item == Items.golden_helmet) {
                return true;
            }
            if (armor.armorType == 2) {
                if (isUntrackableDenickPants(stack)) {
                    return false;
                }
                return item.getUnlocalizedName().toLowerCase().contains("leather");
            }
        }
        return false;
    }

    public static String getMysticEquipmentFingerprint(EntityPlayer player) {
        if (player == null) {
            return "";
        }

        StringBuilder fingerprint = new StringBuilder();
        appendMysticSlot(fingerprint, "hand", player.getCurrentEquippedItem());
        appendMysticSlot(fingerprint, "pants", player.getEquipmentInSlot(2));
        appendMysticSlot(fingerprint, "helmet", player.getEquipmentInSlot(4));
        return fingerprint.toString();
    }

    public static List<Integer> collectDenickNonces(EntityPlayer player) {
        List<Integer> nonces = new ArrayList<>();
        if (player == null) {
            return nonces;
        }
        collectMysticNonce(player.getCurrentEquippedItem(), nonces);
        collectMysticNonce(player.getEquipmentInSlot(2), nonces);
        collectMysticNonce(player.getEquipmentInSlot(4), nonces);
        return nonces;
    }

    private static void appendMysticSlot(StringBuilder fingerprint, String slot, ItemStack stack) {
        if (!isDenickMysticItem(stack)) {
            return;
        }
        Integer nonce = extractNonceFromNBT(stack.getTagCompound());
        if (nonce == null) {
            return;
        }
        if (fingerprint.length() > 0) {
            fingerprint.append('|');
        }
        fingerprint.append(slot).append(':').append(nonce);
    }

    private static void collectMysticNonce(ItemStack stack, List<Integer> nonces) {
        if (!isDenickMysticItem(stack)) {
            return;
        }
        Integer nonce = extractNonceFromNBT(stack.getTagCompound());
        if (nonce != null && !nonces.contains(nonce)) {
            nonces.add(nonce);
        }
    }
}
