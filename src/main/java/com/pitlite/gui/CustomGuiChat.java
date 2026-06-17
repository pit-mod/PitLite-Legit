package com.pitlite.gui;

import com.pitlite.module.impl.player.StopYourAddiction;
import net.minecraft.client.gui.GuiChat;
import com.pitlite.commands.CommandHandler;
import com.pitlite.utils.ChatClickViewHelper;
import net.minecraft.util.IChatComponent;

public class CustomGuiChat extends GuiChat {

    public CustomGuiChat(String defaultText) {
        super(defaultText);
    }

    @Override
    protected boolean handleComponentClick(IChatComponent component) {
        if (ChatClickViewHelper.tryHandleClick(component)) {
            return true;
        }
        return super.handleComponentClick(component);
    }

    @Override
    public void sendChatMessage(String msg, boolean addToChat) {
        if (msg.startsWith(".")) {
            com.pitlite.module.Module commandModule = com.pitlite.PitLite.moduleManager.getModules().stream()
                    .filter(m -> m.getName().equals("Command")).findFirst().orElse(null);
            if (commandModule != null && commandModule.isToggled()) {
                if (CommandHandler.handleCommand(msg)) {
                    return;
                }
            }
        }
        if (StopYourAddiction.tryHandleOutgoingChat(msg)) {
            return;
        }
        super.sendChatMessage(msg, addToChat);
    }
}
