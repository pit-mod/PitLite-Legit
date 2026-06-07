package com.pitlite;

import com.pitlite.gui.ClickGUI;
import com.pitlite.module.Module;
import com.pitlite.utils.KeybindRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class KeyHandler {
    private ClickGUI clickGUI;

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (Minecraft.getMinecraft().currentScreen instanceof ClickGUI) {
            return;
        }
        if (Keyboard.isCreated() && Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_RSHIFT) {
                if (clickGUI == null) {
                    clickGUI = new ClickGUI();
                }
                Minecraft.getMinecraft().displayGuiScreen(clickGUI);
            }

            if (key != Keyboard.KEY_NONE) {
                List<Module> bound = KeybindRegistry.getModulesForKey(key);
                for (Module m : bound) {
                    m.onKey(key);
                    m.toggle();
                }
            }
        }
    }
}
