package com.pitlite.commands;

import com.pitlite.gui.ClickGUI;
import com.pitlite.gui.CustomGuiChat;
import com.pitlite.utils.ConfigManager;
import com.pitlite.utils.DenickManager;
import com.pitlite.utils.DenickService;
import com.pitlite.utils.FriendManager;
import com.pitlite.utils.KOSManager;
import com.pitlite.utils.LobbyPlayerIndex;
import com.pitlite.utils.MojangCache;
import com.pitlite.utils.NotificationManager;
import com.pitlite.utils.PitMartService;
import com.pitlite.utils.TruceManager;
import com.pitlite.utils.DenickUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandHandler {
    private static final String PREFIX = ".";
    private static final String CONFIRM_CLICK_COMMAND = "/pitlite_confirm";

    private enum PlayerListType {
        KOS,
        FRIEND,
        TRUCE
    }

    private enum PendingClear {
        NONE,
        KOS,
        FRIENDS,
        TRUCE,
        ALL
    }

    private static PendingClear pendingClear = PendingClear.NONE;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChat && !(event.gui instanceof CustomGuiChat)) {
            event.gui = new CustomGuiChat("");
        }
    }

    public static boolean handleCommand(String message) {
        if (message.startsWith(PREFIX)) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(message);

            String[] args = message.substring(PREFIX.length()).trim().split(" ");
            if (args.length == 0 || args[0].isEmpty()) return true;

            String command = args[0].toLowerCase();

            switch (command) {
                case "pitlite":
                case "pitlegit":
                    if (args.length >= 2) {
                        handlePitLiteSubcommand(args);
                    } else {
                        openClickGui();
                    }
                    break;
                case "export":
                    if (args.length >= 2) {
                        com.pitlite.utils.ConfigPresets.exportPreset(args[1]);
                    } else {
                        NotificationManager.show("\u00a7cUsage: .export <presetName>", 3000);
                    }
                    break;
                case "import":
                    if (args.length >= 2) {
                        com.pitlite.utils.ConfigPresets.importPreset(args[1]);
                    } else {
                        NotificationManager.show("\u00a7cUsage: .import <presetName>", 3000);
                    }
                    break;
                case "kos":
                case "e":
                    handlePlayerListCommand(args, PlayerListType.KOS);
                    break;
                case "friend":
                case "f":
                    handlePlayerListCommand(args, PlayerListType.FRIEND);
                    break;
                case "truce":
                case "t":
                    handlePlayerListCommand(args, PlayerListType.TRUCE);
                    break;
                case "confirm":
                    handleConfirm();
                    break;
                case "clear":
                    if (args.length >= 2 && "all".equalsIgnoreCase(args[1])) {
                        requestClearAll();
                    } else {
                        NotificationManager.show("§cUsage: .clear all", 3000);
                    }
                    break;
                case "denick":
                case "d":
                case "dn":
                case "enick":
                    handleDenickCommand(copyArgsFrom(args, 1));
                    break;
                case "owners":
                case "oh":
                case "ownerhistory":
                    handleOwnerHistoryCommand(copyArgsFrom(args, 1));
                    break;
                default:
                    NotificationManager.show("§cUnknown command.", 3000);
                    break;
            }
            return true;
        }
        return false;
    }

    private static void handleOwnerHistoryCommand(String[] args) {
        if (args.length >= 1 && looksLikeMongoId(args[0])) {
            fetchAndPrintOwnerHistoryById(args[0]);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            NotificationManager.show("\u00a7cHold a mystic item or pass a PitPanda item id.", 3000);
            return;
        }

        NBTTagCompound tag = held.getTagCompound();
        Integer nonce = DenickUtils.extractNonceFromNBT(tag);
        if (nonce == null) {
            NotificationManager.show("\u00a7cThat item has no nonce (not a tracked mystic).", 3000);
            return;
        }

        NotificationManager.show("\u00a77Fetching owner history...", 2500);
        CompletableFuture.supplyAsync(() -> PitMartService.fetchByNonce(nonce))
                .thenAccept(result -> mc.addScheduledTask(() -> printOwnerHistoryResult(result, nonce)));
    }

    private static void fetchAndPrintOwnerHistoryById(String mongoId) {
        Minecraft mc = Minecraft.getMinecraft();
        NotificationManager.show("\u00a77Fetching owner history...", 2500);
        CompletableFuture.supplyAsync(() -> PitMartService.fetchByMongoId(mongoId))
                .thenAccept(result -> mc.addScheduledTask(() -> printOwnerHistoryResult(result, -1)));
    }

    private static void printOwnerHistoryResult(PitMartService.OwnerHistoryResult result, int fallbackNonce) {
        if (result == null || result.owners.isEmpty()) {
            NotificationManager.show("\u00a7cNo owner history found.", 3000);
            if (result != null && result.status == PitMartService.FetchStatus.ERROR) {
                NotificationManager.showInChat("\u00a77Could not reach item database. Check your connection.");
            } else {
                NotificationManager.showInChat("\u00a77Item may not be indexed yet. Trade it or search pitpanda.rocks.");
            }
            return;
        }

        int nonce = result.nonce > 0 ? result.nonce : fallbackNonce;
        NotificationManager.show("\u00a7aOwner history \u00a77(nonce " + nonce + ")", 3000);
        if (result.itemName != null && !result.itemName.isEmpty()) {
            NotificationManager.showInChat("\u00a77Item: " + result.itemName);
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

    private static boolean looksLikeMongoId(String value) {
        return value != null && value.length() == 24 && value.matches("[0-9a-fA-F]+");
    }

    private static void handleDenickCommand(String[] args) {
        if (args.length == 0) {
            printDenickUsage();
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                showDenickList();
                break;
            case "clear":
                if (DenickManager.isEmpty()) {
                    NotificationManager.show("§9Denick list is already empty.", 3000);
                    return;
                }
                DenickManager.clear();
                ConfigManager.saveConfig();
                NotificationManager.show("§9Cleared all denicked players.", 3000);
                break;
            case "remove":
                if (args.length < 2) {
                    printDenickUsage();
                    return;
                }
                if (!DenickManager.contains(args[1])) {
                    NotificationManager.show("§c" + args[1] + " is not in the denick list.", 3000);
                    return;
                }
                DenickManager.removeByNick(args[1]);
                ConfigManager.saveConfig();
                NotificationManager.show("§9Removed " + args[1] + " from the denick list.", 3000);
                break;
            default:
                DenickService.denickByName(args[0]);
                break;
        }
    }

    private static void showDenickList() {
        if (DenickManager.isEmpty()) {
            NotificationManager.show("§9Denick list is empty.", 3000);
            return;
        }

        NotificationManager.show("\u00a79Denick list \u00a77(" + DenickManager.getAll().size() + ")", 3000);
        NotificationManager.showInChat("\u00a79--- Denick List ---");
        for (Map.Entry<String, String> entry : DenickManager.getAll().entrySet()) {
            NotificationManager.showInChat("\u00a79  " + entry.getKey() + " \u00a77-> \u00a7a" + entry.getValue());
        }
    }

    private static void printDenickUsage() {
        NotificationManager.show("\u00a7cInvalid denick command", 2500);
        NotificationManager.showInChat("\u00a7c.denick <player> | .denick list | .denick remove <nick> | .denick clear");
    }

    private static String[] copyArgsFrom(String[] args, int start) {
        if (start >= args.length) {
            return new String[0];
        }
        String[] out = new String[args.length - start];
        System.arraycopy(args, start, out, 0, out.length);
        return out;
    }

    private static void handlePlayerListCommand(String[] args, PlayerListType type) {
        if (args.length < 2) {
            printListUsage(type);
            return;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "list":
                showList(type);
                break;
            case "clear":
                requestClear(type);
                break;
            case "add":
                if (args.length < 3) {
                    printListUsage(type);
                    return;
                }
                addPlayerToList(args[2], type);
                break;
            case "remove":
                if (args.length < 3) {
                    printListUsage(type);
                    return;
                }
                removePlayerFromList(args[2], type);
                break;
            default:
                printListUsage(type);
                break;
        }
    }

    private static void showList(PlayerListType type) {
        Map<String, String> players;
        String header;
        String prefix;

        switch (type) {
            case FRIEND:
                players = FriendManager.getFriends();
                header = "§a--- Friends List ---";
                prefix = "§a";
                break;
            case TRUCE:
                players = TruceManager.getTrucePlayers();
                header = TruceManager.COLOR_CODE + "--- Truce List ---";
                prefix = TruceManager.COLOR_CODE;
                break;
            default:
                players = KOSManager.getKosPlayers();
                header = "§c--- KOS List ---";
                prefix = "§c";
                break;
        }

        if (players.isEmpty()) {
            NotificationManager.show(prefix + listLabel(type) + " list is empty.", 3000);
            return;
        }

        NotificationManager.show(prefix + listLabel(type) + " list \u00a77(" + players.size() + ")", 3000);
        NotificationManager.showInChat(header);
        for (Map.Entry<String, String> entry : players.entrySet()) {
            NotificationManager.showInChat(prefix + "  " + entry.getValue() + " \u00a77(" + entry.getKey() + ")");
        }
    }

    public static void tryConfirmFromClick() {
        handleConfirm();
    }

    private static void requestClearAll() {
        boolean kosEmpty = KOSManager.getKosPlayers().isEmpty();
        boolean friendsEmpty = FriendManager.getFriends().isEmpty();
        boolean truceEmpty = TruceManager.getTrucePlayers().isEmpty();

        if (kosEmpty && friendsEmpty && truceEmpty) {
            NotificationManager.show("§eAll lists are already empty.", 3000);
            return;
        }

        pendingClear = PendingClear.ALL;
        sendClearConfirmationWarning("This will clear your KOS, friends, and truce lists.");
    }

    private static void requestClear(PlayerListType type) {
        switch (type) {
            case FRIEND:
                if (FriendManager.getFriends().isEmpty()) {
                    NotificationManager.show("§aFriends list is already empty.", 3000);
                    return;
                }
                pendingClear = PendingClear.FRIENDS;
                sendClearConfirmationWarning("This will remove all friends.");
                break;
            case TRUCE:
                if (TruceManager.getTrucePlayers().isEmpty()) {
                    NotificationManager.show(TruceManager.COLOR_CODE + "Truce list is already empty.", 3000);
                    return;
                }
                pendingClear = PendingClear.TRUCE;
                sendClearConfirmationWarning("This will remove all truce entries.");
                break;
            default:
                if (KOSManager.getKosPlayers().isEmpty()) {
                    NotificationManager.show("§cKOS list is already empty.", 3000);
                    return;
                }
                pendingClear = PendingClear.KOS;
                sendClearConfirmationWarning("This will remove all KOS entries.");
                break;
        }
    }

    private static void sendClearConfirmationWarning(String actionDescription) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        ChatComponentText message = new ChatComponentText("[PitLite] §eWarning: " + actionDescription + " Type or click ");
        message.appendSibling(createClickableConfirmText());
        message.appendSibling(new ChatComponentText(" §eto proceed."));
        mc.thePlayer.addChatMessage(message);
    }

    private static IChatComponent createClickableConfirmText() {
        ChatComponentText confirm = new ChatComponentText(".confirm");
        confirm.setChatStyle(new ChatStyle()
                .setColor(EnumChatFormatting.RED)
                .setBold(true)
                .setUnderlined(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CONFIRM_CLICK_COMMAND))
                .setChatHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("§cClick to confirm"))));
        return confirm;
    }

    private static void handleConfirm() {
        switch (pendingClear) {
            case KOS:
                KOSManager.clear();
                ConfigManager.saveConfig();
                pendingClear = PendingClear.NONE;
                NotificationManager.show("§cCleared all KOS entries.", 3000);
                break;
            case FRIENDS:
                FriendManager.clear();
                ConfigManager.saveConfig();
                pendingClear = PendingClear.NONE;
                NotificationManager.show("§aCleared all friends.", 3000);
                break;
            case TRUCE:
                TruceManager.clear();
                ConfigManager.saveConfig();
                pendingClear = PendingClear.NONE;
                NotificationManager.show(TruceManager.COLOR_CODE + "Cleared all truce entries.", 3000);
                break;
            case ALL:
                KOSManager.clear();
                FriendManager.clear();
                TruceManager.clear();
                ConfigManager.saveConfig();
                pendingClear = PendingClear.NONE;
                NotificationManager.show("§cCleared all lists (KOS, friends, and truce).", 3000);
                break;
            default:
                NotificationManager.show("§cNothing to confirm.", 3000);
                break;
        }
    }

    private static void printListUsage(PlayerListType type) {
        String cmd = commandLabel(type);
        NotificationManager.show("\u00a7cInvalid " + listLabel(type) + " command", 2500);
        NotificationManager.showInChat("\u00a7c" + cmd + " add|remove|list|clear  \u00a77|  .clear all");
    }

    private static String commandLabel(PlayerListType type) {
        switch (type) {
            case FRIEND:
                return ".friend (or .f)";
            case TRUCE:
                return ".truce (or .t)";
            default:
                return ".kos (or .e)";
        }
    }

    private static String listLabel(PlayerListType type) {
        switch (type) {
            case FRIEND:
                return "Friends";
            case TRUCE:
                return "Truce";
            default:
                return "KOS";
        }
    }

    private static void addPlayerToList(String name, PlayerListType type) {
        if (isOnList(name, type)) {
            NotificationManager.show(getAlreadyOnListMessage(name, type), 3000);
            return;
        }

        String uuid = resolveUuid(name);
        if (uuid == null) {
            NotificationManager.show("§cCould not resolve UUID for " + name + ".", 3000);
            return;
        }

        String displayName = resolveDisplayName(name);

        switch (type) {
            case FRIEND:
                FriendManager.add(displayName, uuid);
                NotificationManager.show("§aAdded " + displayName + " to friends list.", 3000);
                break;
            case TRUCE:
                TruceManager.add(displayName, uuid);
                NotificationManager.show(TruceManager.COLOR_CODE + "Added " + displayName + " to truce list.", 3000);
                break;
            default:
                KOSManager.add(displayName, uuid);
                NotificationManager.show("§cAdded " + displayName + " to KOS list.", 3000);
                break;
        }
        ConfigManager.saveConfig();
    }

    private static void removePlayerFromList(String name, PlayerListType type) {
        if (!isOnList(name, type)) {
            NotificationManager.show(getNotOnListMessage(name, type), 3000);
            return;
        }

        switch (type) {
            case FRIEND:
                FriendManager.removeByName(name);
                NotificationManager.show("§aRemoved " + name + " from friends list.", 3000);
                break;
            case TRUCE:
                TruceManager.removeByName(name);
                NotificationManager.show(TruceManager.COLOR_CODE + "Removed " + name + " from truce list.", 3000);
                break;
            default:
                KOSManager.removeByName(name);
                NotificationManager.show("§cRemoved " + name + " from KOS list.", 3000);
                break;
        }
        ConfigManager.saveConfig();
    }

    private static boolean isOnList(String name, PlayerListType type) {
        switch (type) {
            case FRIEND:
                return FriendManager.isFriendByName(name);
            case TRUCE:
                return TruceManager.isTruceByName(name);
            default:
                return KOSManager.isKOSByName(name);
        }
    }

    private static String getAlreadyOnListMessage(String name, PlayerListType type) {
        switch (type) {
            case FRIEND:
                return "§a" + name + " is already in your friends list.";
            case TRUCE:
                return TruceManager.COLOR_CODE + name + " is already in your truce list.";
            default:
                return "§c" + name + " is already in the KOS list.";
        }
    }

    private static String getNotOnListMessage(String name, PlayerListType type) {
        switch (type) {
            case FRIEND:
                return "§a" + name + " is not in your friends list.";
            case TRUCE:
                return TruceManager.COLOR_CODE + name + " is not in your truce list.";
            default:
                return "§c" + name + " is not in the KOS list.";
        }
    }

    private static String resolveDisplayName(String name) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld != null) {
            EntityPlayer player = mc.theWorld.getPlayerEntityByName(name);
            if (player != null) {
                return player.getName();
            }
        }

        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null
                        && info.getGameProfile().getName() != null
                        && info.getGameProfile().getName().equalsIgnoreCase(name)) {
                    return info.getGameProfile().getName();
                }
            }
        }

        return name;
    }

    private static String resolveUuid(String name) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld != null) {
            EntityPlayer player = LobbyPlayerIndex.findByName(name);
            if (player != null) {
                return player.getUniqueID().toString();
            }
        }

        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null
                        && info.getGameProfile().getName() != null
                        && info.getGameProfile().getName().equalsIgnoreCase(name)) {
                    return info.getGameProfile().getId().toString();
                }
            }
        }

        String cached = MojangCache.getCachedUuid(name);
        if (cached != null) {
            return cached;
        }
        return MojangCache.fetchUuid(name);
    }

    private static void openClickGui() {
        new Thread(() -> {
            try {
                Thread.sleep(50);
                Minecraft.getMinecraft().addScheduledTask(() ->
                        Minecraft.getMinecraft().displayGuiScreen(new ClickGUI())
                );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handlePitLiteSubcommand(String[] args) {
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "export":
                if (args.length >= 3) {
                    com.pitlite.utils.ConfigPresets.exportPreset(args[2]);
                } else {
                    NotificationManager.show("\u00a7cUsage: .pitlite export <name>", 3000);
                }
                break;
            case "import":
                if (args.length >= 3) {
                    com.pitlite.utils.ConfigPresets.importPreset(args[2]);
                } else {
                    NotificationManager.show("\u00a7cUsage: .pitlite import <name>", 3000);
                }
                break;
            case "gui":
                openClickGui();
                break;
            default:
                openClickGui();
                break;
        }
    }
}
