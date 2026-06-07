package com.pitlite.loader;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.Name("PitLite Mixin Loader")
@IFMLLoadingPlugin.TransformerExclusions({"com.pitlite.loader"})
public class PitLiteMixinLoader implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        try {
            org.spongepowered.asm.launch.MixinBootstrap.init();
            org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.pitlite.json");
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
