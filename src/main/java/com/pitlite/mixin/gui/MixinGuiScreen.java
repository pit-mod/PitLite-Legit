package com.pitlite.mixin.gui;

import com.pitlite.module.impl.player.OwnerHistory;
import com.pitlite.utils.ChatClickViewHelper;
import com.pitlite.utils.Utils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = {"handleComponentClick", "func_175276_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void pitlite$clickToView(IChatComponent component, CallbackInfoReturnable<Boolean> cir) {
        if (ChatClickViewHelper.tryHandleClick(component)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = {"handleMouseInput", "func_146274_d"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void pitlite$ownerHistoryMiddleClick(CallbackInfo ci) {
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 2) {
            return;
        }
        if (!((Object) this instanceof GuiContainer)) {
            return;
        }
        if (OwnerHistory.handleGuiMiddleClick((GuiContainer) (Object) this, Utils.getMouseX(), Utils.getMouseY())) {
            ci.cancel();
        }
    }
}
