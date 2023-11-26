package com.jelly.farmhelperv2.transformer;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class FMLCore implements IFMLLoadingPlugin {
    public FMLCore() {
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{NetworkManagerTransformer.class.getName()};
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
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
