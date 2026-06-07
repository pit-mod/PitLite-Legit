package com.pitlite.mixin.gui;

import com.pitlite.module.impl.player.LowLivesWarning;
import com.pitlite.utils.InventoryUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    @Inject(method = {"drawSlot", "func_146977_a"}, at = @At("HEAD"), remap = false)
    private void pitlite$lowLivesHighlight(Slot slot, CallbackInfo ci) {
        if (!LowLivesWarning.isBackgroundEnabled() || slot == null || !slot.getHasStack()) {
            return;
        }
        int lives = InventoryUtils.getLives(slot.getStack());
        if (lives == -1) {
            return;
        }
        int color = 0;
        if (lives < 3) {
            color = 0x80FF0000;
        } else if (lives < 5) {
            color = 0x80FFA500;
        }
        if (color != 0) {
            Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                    slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, color);
        }
    }
}
