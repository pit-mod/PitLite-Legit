package com.pitlite.module.impl.player;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

public class ChestStealer extends Module {

    private final NumberSetting minDelay = new NumberSetting("Min Delay", 1.0, 0.0, 20.0, 0);
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 2.0, 0.0, 20.0, 0);
    private final NumberSetting openDelay = new NumberSetting("Open Delay", 1.0, 0.0, 20.0, 0);
    private final BooleanSetting mysticOnly = new BooleanSetting("Mystic Items Only", false);
    private final BooleanSetting sewerStealer = new BooleanSetting("Sewer Stealer", true);

    private int clickDelay = 0;
    private int oDelay = 0;
    private boolean inChest = false;

    public ChestStealer() {
        super("ChestStealer", "Automatically steals items from chests", Category.PLAYER);
        addSettings(minDelay, maxDelay, openDelay, mysticOnly, sewerStealer);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clickDelay = 0;
        oDelay = 0;
        inChest = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        if (clickDelay > 0) clickDelay--;
        if (oDelay > 0) oDelay--;

        if (!(mc.currentScreen instanceof GuiChest)) {
            inChest = false;
            return;
        }

        GuiChest guiChest = (GuiChest) mc.currentScreen;
        if (!(guiChest.inventorySlots instanceof ContainerChest)) {
            inChest = false;
            return;
        }

        ContainerChest container = (ContainerChest) guiChest.inventorySlots;
        if (!inChest) {
            inChest = true;
            oDelay = (int) openDelay.value;
        }

        if (mc.playerController.getCurrentGameType() != net.minecraft.world.WorldSettings.GameType.SURVIVAL
                && mc.playerController.getCurrentGameType() != net.minecraft.world.WorldSettings.GameType.ADVENTURE) {
            return;
        }

        IInventory chestInventory = container.getLowerChestInventory();

        String name = chestInventory.getName();
        if (!name.equals("Chest") && !name.equals("Large Chest")
            && !name.equals(I18n.format("container.chest")) && !name.equals(I18n.format("container.chestDouble"))) {
            return;
        }

        if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
            return;
        }

        if (mysticOnly.enabled) {
            boolean tookPriority = false;
            for (int i = 0; i < chestInventory.getSizeInventory(); i++) {
                if (!container.getSlot(i).getHasStack()) continue;
                ItemStack stack = container.getSlot(i).getStack();
                if (isMysticItem(stack)) {
                    mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
                    tookPriority = true;
                }
            }

            if (tookPriority) {
                clickDelay = 1;
                return;
            }

            if (oDelay > 0 || clickDelay > 0) return;

            if (sewerStealer.enabled) {
                for (int i = 0; i < chestInventory.getSizeInventory(); i++) {
                    if (!container.getSlot(i).getHasStack()) continue;
                    ItemStack stack = container.getSlot(i).getStack();
                    if (isDiamondArmor(stack) || isExtraMysticItem(stack)) {
                        mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
                        clickDelay = 1;
                        return;
                    }
                }
            }
            return;
        }

        if (oDelay > 0 || clickDelay > 0) return;

        if (!mysticOnly.enabled) {
            if (takeBestGear(container, chestInventory)) return;

            for (int i = 0; i < chestInventory.getSizeInventory(); i++) {
                if (!container.getSlot(i).getHasStack()) continue;
                ItemStack stack = container.getSlot(i).getStack();

                if (!isTrashItem(stack)) {
                    shiftClick(container.windowId, i);
                    return;
                }
            }
        }
    }

    private boolean takeBestGear(ContainerChest container, IInventory chestInventory) {
        int bestSwordSlot = -1;
        double bestSwordDamage = getBestSwordDamageInInventory();
        int[] bestArmorSlots = {-1, -1, -1, -1};
        double[] bestArmorProtection = {
            getCurrentArmorProtection(0),
            getCurrentArmorProtection(1),
            getCurrentArmorProtection(2),
            getCurrentArmorProtection(3)
        };

        int bestBowSlot = -1;
        double bestBowDamage = getBestBowInInventory();

        for (int i = 0; i < chestInventory.getSizeInventory(); i++) {
            if (!container.getSlot(i).getHasStack()) continue;
            ItemStack stack = container.getSlot(i).getStack();
            Item item = stack.getItem();

            if (item instanceof ItemSword) {
                double damage = getAttackDamage(stack);
                if (damage > bestSwordDamage) {
                    bestSwordSlot = i;
                    bestSwordDamage = damage;
                }
            } else if (item instanceof ItemArmor) {
                int armorType = ((ItemArmor) item).armorType;
                double protection = getArmorProtection(stack);
                if (protection > bestArmorProtection[armorType]) {
                    bestArmorSlots[armorType] = i;
                    bestArmorProtection[armorType] = protection;
                }
            } else if (item instanceof ItemBow) {
                double damage = getBowPower(stack);
                if (damage > bestBowDamage) {
                    bestBowSlot = i;
                    bestBowDamage = damage;
                }
            }
        }

        if (bestSwordSlot != -1) {
            shiftClick(container.windowId, bestSwordSlot);
            return true;
        }

        for (int type = 0; type < 4; type++) {
            if (bestArmorSlots[type] != -1) {
                shiftClick(container.windowId, bestArmorSlots[type]);
                return true;
            }
        }

        if (bestBowSlot != -1) {
            shiftClick(container.windowId, bestBowSlot);
            return true;
        }

        return false;
    }

    private void shiftClick(int windowId, int slotId) {
        mc.playerController.windowClick(windowId, slotId, 0, 1, mc.thePlayer);

        int min = (int) minDelay.value;
        int max = (int) maxDelay.value;
        if (min > max) max = min;
        clickDelay = min == max ? min + 1 : ThreadLocalRandom.current().nextInt(min + 1, max + 2);
    }

    private boolean isTrashItem(ItemStack stack) {
        if (stack == null) return true;
        if (isExtraMysticItem(stack)) return false;

        Item item = stack.getItem();
        return !(item instanceof ItemSword || item instanceof ItemArmor || item instanceof ItemBow
                || item instanceof ItemPickaxe || item instanceof ItemAxe || item instanceof ItemSpade
                || item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemEnderPearl
                || item == Items.golden_apple || item == Items.arrow
                || item == Items.emerald || item == Items.diamond
                || item == Items.gold_ingot || item == Items.iron_ingot);
    }

    private boolean isMysticItem(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item == Items.golden_sword || item == Items.bow || item == Items.leather_leggings;
    }

    private boolean isDiamondArmor(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item == Items.diamond_helmet || item == Items.diamond_chestplate || item == Items.diamond_leggings || item == Items.diamond_boots;
    }

    private boolean isExtraMysticItem(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();

        if (item == Items.gold_ingot || item == Items.experience_bottle) return true;
        if (item == Items.leather_boots || item == Items.leather_leggings) return true;
        if (item == Items.milk_bucket) return true;

        if (item == Item.getItemFromBlock(Blocks.soul_sand)) return true;
        if (item == Item.getItemFromBlock(Blocks.prismarine) && stack.getMetadata() == 2) return true;

        if (stack.hasDisplayName()) return true;

        return false;
    }

    private double getAttackDamage(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return 0;
        double damage = ((ItemSword) stack.getItem()).getDamageVsEntity();
        damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25;
        damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.5;
        return damage;
    }

    private double getArmorProtection(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return 0;
        double protection = ((ItemArmor) stack.getItem()).damageReduceAmount;
        protection += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.5;
        return protection;
    }

    private double getBowPower(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBow)) return 0;
        return 1.0 + EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack) * 0.5;
    }

    private double getBestSwordDamageInInventory() {
        double best = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) {
                double damage = getAttackDamage(stack);
                if (damage > best) best = damage;
            }
        }
        return best;
    }

    private double getCurrentArmorProtection(int armorType) {
        ItemStack armorStack = mc.thePlayer.inventory.armorInventory[3 - armorType];
        return getArmorProtection(armorStack);
    }

    private double getBestBowInInventory() {
        double best = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBow) {
                double power = getBowPower(stack);
                if (power > best) best = power;
            }
        }
        return best;
    }
}
