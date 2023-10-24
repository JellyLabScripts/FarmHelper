package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BanInfoWS implements IFeature {
    private static BanInfoWS instance;

    public static BanInfoWS getInstance() {
        if (instance == null) {
            instance = new BanInfoWS();
        }
        return instance;
    }

    public BanInfoWS() {
        try {
            LogUtils.sendDebug("Connecting to analytics server...");
            client = createNewWebSocketClient();
            for (Map.Entry<String, JsonElement> header : getHeaders().entrySet()) {
                client.addHeader(header.getKey(), header.getValue().getAsString());
            }
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            client = null;
        }
    }

    private WebSocketClient client;

    @Setter
    private int bans = 0;

    @Getter
    @Setter
    private int minutes = 0;

    @Getter
    @Setter
    private int bansByMod = 0;

    @Override
    public String getName() {
        return "Banwave Checker";
    }

    @Override
    public boolean isRunning() {
        return isToggled();
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false; // it's running all the time
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.banwaveCheckerEnabled;
    }

    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    public boolean isBanwave() {
        return getBans() >= FarmHelperConfig.banwaveThreshold;
    }

    public int getBans() {
        switch (FarmHelperConfig.banwaveThresholdType) {
            case 0: {
                return bans;
            }
            case 1: {
                return bansByMod;
            }
            case 2: {
                return Math.max(bans, bansByMod);
            }
        }
        return 0;
    }

    private final Clock reconnectDelay = new Clock();

    @SubscribeEvent
    public void onTickReconnect(TickEvent.ClientTickEvent event) {
        if (reconnectDelay.isScheduled() && !reconnectDelay.passed()) return;

        if (client == null || client.isClosed() || !client.isOpen()) {
            try {
                reconnectDelay.reset();
                LogUtils.sendDebug("Connecting to analytics server...");
                client = createNewWebSocketClient();
                for (Map.Entry<String, JsonElement> header : getHeaders().entrySet()) {
                    client.addHeader(header.getKey(), header.getValue().getAsString());
                }
                client.connectBlocking();
                client.send("{\"message\":\"banwaveInfo\", \"mod\": \"farmHelper\"}");
            } catch (URISyntaxException | InterruptedException e) {
                e.printStackTrace();
                client = null;
                reconnectDelay.schedule(5_000);
            }
        }
    }

    public void playerBanned(int days, String reason, String banId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "gotBanned");
        jsonObject.addProperty("mod", "farmHelper");
        jsonObject.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        JsonObject additionalInfo = new JsonObject();
        additionalInfo.addProperty("days", days + 1);
        additionalInfo.addProperty("banId", banId);
        additionalInfo.addProperty("reason", reason);
        String config = FarmHelper.config.getJson();
        JsonObject configJson = FarmHelper.gson.fromJson(config, JsonObject.class);
        configJson.addProperty("proxyAddress", "REMOVED");
        configJson.addProperty("proxyUsername", "REM0VED");
        configJson.addProperty("proxyPassword", "REMOVED");
        configJson.addProperty("webHookURL", "REMOVED");
        configJson.addProperty("discordRemoteControlToken", "REMVOED");
        String configJsonString = FarmHelper.gson.toJson(configJson);
        additionalInfo.addProperty("config", configJsonString);
        jsonObject.add("additionalInfo", additionalInfo);
        client.send(jsonObject.toString());
    }

    public void sendAnalyticsData() {
        if (MacroHandler.getInstance().getAnalyticsTimer().getElapsedTime() <= 60_000) return; // ignore if macroing for less than 60 seconds
        System.out.println("Sending analytics data");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "analyticsData");
        jsonObject.addProperty("mod", "farmHelper");
        jsonObject.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        jsonObject.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID()); // that's public uuid bozos, not a token to login
        jsonObject.addProperty("modVersion", FarmHelper.VERSION);
        jsonObject.addProperty("timeMacroing", MacroHandler.getInstance().getAnalyticsTimer().getElapsedTime());
        JsonObject additionalInfo = new JsonObject();
        additionalInfo.addProperty("cropType", MacroHandler.getInstance().getCrop().toString());
        additionalInfo.addProperty("bps", ProfitCalculator.getInstance().getBPS());
        additionalInfo.addProperty("profit", ProfitCalculator.getInstance().getRealProfitString());
        jsonObject.add("additionalInfo", additionalInfo);
        client.send(jsonObject.toString());
    }

    private JsonObject getHeaders() {
        JsonObject handshake = new JsonObject();
        handshake.addProperty("reason", "WebSocketConnector");
        handshake.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID()); // that's public uuid bozos, not a token to login
        handshake.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        handshake.addProperty("modVersion", FarmHelper.VERSION);
        handshake.addProperty("mod", "farmHelper");
        return handshake;
    }

    private WebSocketClient createNewWebSocketClient() throws URISyntaxException {
        return new WebSocketClient(new URI("ws://may2beez.ddns.net:3000")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                LogUtils.sendDebug("Connected to analytics websocket server");
                Notifications.INSTANCE.send("FarmHelper INFO", "Connected to analytics websocket server");
                Multithreading.schedule(() -> client.send("{\"message\":\"banwaveInfo\", \"mod\": \"farmHelper\"}"), 1_000, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onMessage(String message) {
                JsonObject jsonObject = FarmHelper.gson.fromJson(message, JsonObject.class);
                String msg = jsonObject.get("message").getAsString();
                switch (msg) {
                    case "banwaveInfo": {
                        int bans = jsonObject.get("bansInLast15Minutes").getAsInt();
                        int minutes = jsonObject.get("bansInLast15MinutesTime").getAsInt();
                        int bansByMod = jsonObject.get("bansInLast15MinutesMod").getAsInt();
                        BanInfoWS.getInstance().setBans(bans);
                        BanInfoWS.getInstance().setMinutes(minutes);
                        BanInfoWS.getInstance().setBansByMod(bansByMod);
                        System.out.println("Banwave info received: " + bans + " bans in last " + minutes + " minutes, " + bansByMod + " by mod");
                        break;
                    }
                    case "playerGotBanned": {
                        String username = jsonObject.get("username").getAsString();
                        String days = jsonObject.get("days").getAsString();
                        String mod = jsonObject.get("mod").getAsString();
                        LogUtils.sendWarning("Player " + username + " got banned for " + days + " days while using " + mod);
                        Notifications.INSTANCE.send("FarmHelper INFO", "Player " + username + " got banned for " + days + " days while using " + mod);
                        break;
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                LogUtils.sendDebug("Disconnected from analytics server");
                LogUtils.sendDebug("Code: " + code + ", reason: " + reason + ", remote: " + remote);
                reconnectDelay.schedule(5_000);
            }

            @Override
            public void onError(Exception ex) {
                LogUtils.sendDebug("Error while connecting to analytics server. " + ex.getMessage());
                ex.printStackTrace();
            }
        };
    }
}
