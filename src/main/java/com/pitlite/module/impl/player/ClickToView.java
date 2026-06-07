package com.pitlite.module.impl.player;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.utils.ChatClickViewHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClickToView extends Module {

    private static ClickToView instance;

    public ClickToView() {
        super("Click to View", "Click chat names to run /view instead of opening social options.", Category.PLAYER);
        instance = this;
    }

    public static boolean isActive() {
        return instance != null && instance.isToggled();
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isToggled() || event.type == 2) {
            return;
        }
        ChatClickViewHelper.rewriteSocialClicks(event.message);
    }
}
