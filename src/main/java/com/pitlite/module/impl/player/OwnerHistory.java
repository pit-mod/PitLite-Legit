package com.pitlite.module.impl.player;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.DenickUtils;
import com.pitlite.utils.NotificationManager;
import com.pitlite.utils.PitMartService;
import com.pitlite.utils.Utils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OwnerHistory extends Module {

    private static OwnerHistory instance;

    private final BooleanSetting middleClickExpand = new BooleanSetting("Middle Click Expand", true);

    private final Set<Integer> expandedNonces = ConcurrentHashMap.newKeySet();
    private final Map<Integer, PitMartService.OwnerHistoryResult> expandedHistory = new ConcurrentHashMap<>();
    private final Set<Integer> loadingNonces = ConcurrentHashMap.newKeySet();

    private boolean middleWasDown;
    private long lastMiddleClickMs;

    public OwnerHistory() {
        super("Owner History", "Middle-click mystics to show owner history in tooltips or chat.", Category.PLAYER);
        addSettings(middleClickExpand);
        instance = this;
    }

    public static boolean handleGuiMiddleClick(GuiContainer gui, int mouseX, int mouseY) {
        if (instance == null || !instance.isActiveForMiddleClick()) {
            return false;
        }
        instance.onGuiMiddleClick(gui, mouseX, mouseY);
        return true;
    }

    private void onGuiMiddleClick(GuiContainer gui, int mouseX, int mouseY) {
        if (consumeMiddleClickDebounce()) {
            return;
        }

        Slot slot = resolveSlot(gui, mouseX, mouseY);
        if (slot == null || !slot.getHasStack()) {
            NotificationManager.show("\u00a77Hover a mystic item, then middle-click the slot.", 2500);
            return;
        }

        ItemStack stack = slot.getStack();
        Integer nonce = DenickUtils.extractNonceFromNBT(stack.getTagCompound());
        if (nonce == null) {
            NotificationManager.show("\u00a7cThat item has no nonce (not a tracked mystic).", 2500);
            return;
        }

        toggleExpand(nonce, true);
    }

    private boolean isActiveForMiddleClick() {
        return isToggled() && middleClickExpand.enabled;
    }

    private boolean consumeMiddleClickDebounce() {
        long now = System.currentTimeMillis();
        if (now - lastMiddleClickMs < 150L) {
            return true;
        }
        lastMiddleClickMs = now;
        return false;
    }

    @Override
    protected void onDisable() {
        clearExpanded();
        middleWasDown = false;
        super.onDisable();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiContainer)) {
            clearExpanded();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isActiveForMiddleClick()) {
            return;
        }

        boolean middleDown = Mouse.isButtonDown(2);
        if (middleDown && !middleWasDown) {
            if (mc.currentScreen instanceof GuiContainer) {
                onGuiMiddleClick((GuiContainer) mc.currentScreen, Utils.getMouseX(), Utils.getMouseY());
            } else if (mc.thePlayer != null) {
                onWorldMiddleClick();
            }
        }
        middleWasDown = middleDown;
    }

    private void onWorldMiddleClick() {
        if (consumeMiddleClickDebounce()) {
            return;
        }

        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null) {
            NotificationManager.show("\u00a77Hold a mystic item to middle-click owner history.", 2500);
            return;
        }

        Integer nonce = DenickUtils.extractNonceFromNBT(stack.getTagCompound());
        if (nonce == null) {
            NotificationManager.show("\u00a7cThat item has no nonce (not a tracked mystic).", 2500);
            return;
        }

        toggleExpand(nonce, false);
    }

    private void toggleExpand(int nonce, boolean inGui) {
        if (expandedNonces.contains(nonce)) {
            expandedNonces.remove(nonce);
            expandedHistory.remove(nonce);
            loadingNonces.remove(nonce);
            NotificationManager.show("\u00a77Owner history collapsed.", 2000);
            return;
        }

        expandedNonces.add(nonce);

        PitMartService.OwnerHistoryResult cached = PitMartService.peekCachedByNonce(nonce);
        if (cached != null && cached.status == PitMartService.FetchStatus.OK) {
            expandedHistory.put(nonce, cached);
            if (inGui) {
                NotificationManager.show("\u00a7aOwner history expanded \u00a77(hover item)", 2500);
            } else {
                printOwnerHistoryToChat(cached, nonce);
            }
            return;
        }

        loadingNonces.add(nonce);
        if (inGui) {
            NotificationManager.show("\u00a7aOwner history expanded \u00a77(hover item)", 2500);
        }
        fetchOwnerHistoryAsync(nonce, inGui);
    }

    private void fetchOwnerHistoryAsync(int nonce, boolean inGui) {
        CompletableFuture.supplyAsync(() -> PitMartService.fetchByNonceFast(nonce)).thenAccept(fast -> {
            if (fast == null || fast.status != PitMartService.FetchStatus.OK) {
                mc.addScheduledTask(() -> {
                    loadingNonces.remove(nonce);
                    expandedNonces.remove(nonce);
                    NotificationManager.show("\u00a7cNo owner history found for this item.", 3000);
                    NotificationManager.showInChat("\u00a77Item may not be indexed yet. Trade it or search pitpanda.rocks.");
                });
                return;
            }

            mc.addScheduledTask(() -> {
                loadingNonces.remove(nonce);
                expandedHistory.put(nonce, fast);
            });

            CompletableFuture.supplyAsync(() -> PitMartService.withResolvedUsernames(fast)).thenAccept(resolved -> {
                mc.addScheduledTask(() -> {
                    if (!expandedNonces.contains(nonce)) {
                        return;
                    }
                    expandedHistory.put(nonce, resolved);
                    if (inGui) {
                        NotificationManager.show("\u00a7aOwner history loaded \u00a77(hover item)", 2500);
                    } else {
                        printOwnerHistoryToChat(resolved, nonce);
                    }
                });
            });
        });
    }

    private void printOwnerHistoryToChat(PitMartService.OwnerHistoryResult result, int nonce) {
        NotificationManager.show("\u00a7aOwner history \u00a77(nonce " + nonce + ")", 3000);
        if (result.itemName != null && !result.itemName.isEmpty()) {
            NotificationManager.showInChat("\u00a77Item: " + StringUtils.stripControlCodes(result.itemName));
        }
        List<PitMartService.OwnerRecord> owners = result.owners;
        int startIndex = PitMartService.getDisplayStartIndex(result);
        for (int i = 0; i < owners.size(); i++) {
            NotificationManager.showInChat(PitMartService.formatOwnerLine(owners.get(i), startIndex + i));
        }
        if (result.totalOwners > owners.size()) {
            NotificationManager.showInChat("\u00a77... " + (result.totalOwners - owners.size()) + " older owners hidden");
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!isActiveForMiddleClick() || event.itemStack == null || event.toolTip == null) {
            return;
        }

        Integer nonce = DenickUtils.extractNonceFromNBT(event.itemStack.getTagCompound());
        if (nonce == null || !expandedNonces.contains(nonce)) {
            return;
        }

        removeOwnerHistorySection(event.toolTip);
        PitMartService.appendOwnerHistoryTooltip(event.toolTip, expandedHistory.get(nonce),
                loadingNonces.contains(nonce));
    }

    private static void removeOwnerHistorySection(List<String> tooltip) {
        int start = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            String clean = StringUtils.stripControlCodes(tooltip.get(i));
            if (clean.equalsIgnoreCase("Owner History")) {
                start = i;
                break;
            }
        }
        if (start <= 0) {
            return;
        }
        if (start > 0 && tooltip.get(start - 1).trim().isEmpty()) {
            start--;
        }
        Iterator<String> iterator = tooltip.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            iterator.next();
            if (index >= start) {
                iterator.remove();
            } else {
                index++;
            }
        }
    }

    private void clearExpanded() {
        expandedNonces.clear();
        expandedHistory.clear();
        loadingNonces.clear();
    }

    private Slot resolveSlot(GuiContainer gui, int mouseX, int mouseY) {
        Slot hovered = readTheSlot(gui);
        if (hovered != null && hovered.getHasStack()) {
            return hovered;
        }

        Slot slot = invokeGetSlotAtPosition(gui, mouseX, mouseY);
        if (slot != null) {
            return slot;
        }

        int guiLeft = readGuiOffset(gui, "guiLeft", "field_147003_i");
        int guiTop = readGuiOffset(gui, "guiTop", "field_147009_r");
        for (Slot candidate : gui.inventorySlots.inventorySlots) {
            int x = guiLeft + candidate.xDisplayPosition;
            int y = guiTop + candidate.yDisplayPosition;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return candidate;
            }
        }
        return null;
    }

    private static Slot readTheSlot(GuiContainer gui) {
        Object value = readField(gui, "theSlot", "field_147006_u");
        if (value instanceof Slot) {
            return (Slot) value;
        }
        return null;
    }

    private static Slot invokeGetSlotAtPosition(GuiContainer gui, int mouseX, int mouseY) {
        try {
            for (Method method : GuiContainer.class.getDeclaredMethods()) {
                if (method.getReturnType() != Slot.class || method.getParameterTypes().length != 2) {
                    continue;
                }
                method.setAccessible(true);
                Object result = method.invoke(gui, mouseX, mouseY);
                if (result instanceof Slot) {
                    return (Slot) result;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int readGuiOffset(GuiContainer gui, String deobfName, String obfName) {
        Object value = readField(gui, deobfName, obfName);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    private static Object readField(Object target, String deobfName, String obfName) {
        try {
            Field field = target.getClass().getDeclaredField(deobfName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
        }
        try {
            Field field = GuiContainer.class.getDeclaredField(deobfName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
        }
        try {
            Field field = GuiContainer.class.getDeclaredField(obfName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
        }
        return null;
    }
}
