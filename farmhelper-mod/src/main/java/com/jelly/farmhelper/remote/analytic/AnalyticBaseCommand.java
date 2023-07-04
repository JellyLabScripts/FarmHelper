package com.jelly.farmhelper.remote.analytic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.RemoteControlHandler;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import org.json.simple.JSONObject;

abstract public class AnalyticBaseCommand {
    private static Minecraft mc = Minecraft.getMinecraft();

    public static JsonObject gatherMacroingData() {
        JsonObject obj = new JsonObject();
        obj.addProperty("hoeCounter", PlayerUtils.getCounter());
        obj.addProperty("isMacroing", MacroHandler.isMacroing);
        obj.addProperty("startTime", MacroHandler.startTime);
        obj.addProperty("isBanwaveWaiting", BanwaveChecker.banwaveOn && mc.currentScreen instanceof GuiDisconnected);
        return obj;
    }

    public boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }
    public static void send(String content) {
        RemoteControlHandler.analytic.send(content);
    }

    public static void send(JsonObject content) {
        RemoteControlHandler.analytic.send(content.toString());
    }

    private static JsonObject sanitizeConfig(JSONObject obj) {
        // removes any sensitive data

        // remotecontrolconfig
        obj.remove("websocketPassword");
        obj.remove("websocketIP");
        obj.remove("webhookURL");

        // proxyconfig
        obj.remove("proxyAddress");
        obj.remove("proxyPassword");
        obj.remove("proxyUsername");

        return new Gson().fromJson(obj.toString(), JsonObject.class);
    }
}
