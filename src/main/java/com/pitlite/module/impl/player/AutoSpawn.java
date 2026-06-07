package com.pitlite.module.impl.player;

import com.pitlite.module.Category;
import com.pitlite.module.Module;

public class AutoSpawn extends Module {

    public AutoSpawn() {
        super("AutoSpawn", "Automatically types /spawn in chat", Category.MISC);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/spawn");
            toggle();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
