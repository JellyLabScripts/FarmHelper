package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;

public class Macro {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled;

    public void toggle(){
        enabled = !enabled;
        if(enabled){
            onEnable();
        } else {
            onDisable();
        }
    }
    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick(){
    }

    public void onRender(){
    }

    public void onChatMessageReceived(String msg){

    }




}
