package com.pitlite.mixin.item;

import com.pitlite.module.impl.player.MysticRename;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Shadow(aliases = "func_82833_r", remap = false)
    public abstract String getDisplayName();

    @Inject(method = {"getDisplayName", "func_82833_r"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetDisplayName(CallbackInfoReturnable<String> cir) {
        String customName = MysticRename.getCustomName((ItemStack) (Object) this);
        if (customName != null) {
            cir.setReturnValue(customName);
        }
    }
}
