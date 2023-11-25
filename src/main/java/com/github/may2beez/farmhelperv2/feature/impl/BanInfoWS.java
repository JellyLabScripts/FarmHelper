package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.exceptions.AuthenticationException;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicHeader;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.spongepowered.asm.mixin.Unique;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public class BanInfoWS implements IFeature {
    private static BanInfoWS instance;

    public static BanInfoWS getInstance() {
        if (instance == null) {
            instance = new BanInfoWS();
        }
        return instance;
    }

    private HttpClient httpClient;

    public BanInfoWS() {
        try {
            LogUtils.sendDebug("Connecting to analytics server...");
            client = createNewWebSocketClient();
            Multithreading.schedule(() -> {
                JsonObject headers = getHeaders();
                if (headers == null) {
                    LogUtils.sendDebug("Failed to connect to analytics server. Retrying in 1 minute...");
                    return;
                }
                for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
                    client.addHeader(header.getKey(), header.getValue().getAsString());
                }
                client.connect();
            }, 0, TimeUnit.MILLISECONDS);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            client = null;
        }
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        StandardHttpRequestRetryHandler retryHandler = new StandardHttpRequestRetryHandler(3, true);
        this.httpClient = HttpClientBuilder.create().setRetryHandler(retryHandler).setDefaultRequestConfig(requestConfig).build();
    }

    private List<BasicHeader> getHttpClientHeaders() {
        List<BasicHeader> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent", "Farm Helper"));
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Accept", "application/json"));
        return headers;
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

    private int retryCount = 0;

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

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
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
        if (retryCount > 5) return;
        if (retryCount == 4) {
            LogUtils.sendWarning("Failed to connect to the analytics server 5 times. Restart Minecraft to try again.");
            reconnectDelay.reset();
            retryCount = 999;
            return;
        }

        if (client == null || client.isClosed() || !client.isOpen()) {
            try {
                reconnectDelay.reset();
                LogUtils.sendDebug("Connecting to analytics server...");
                client = createNewWebSocketClient();
                reconnectDelay.schedule(60_000L);
                Multithreading.schedule(() -> {
                    JsonObject headers = getHeaders();
                    if (headers == null) {
                        LogUtils.sendDebug("Failed to connect to analytics server. Retrying in 1 minute...");
                        return;
                    }
                    for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
                        client.addHeader(header.getKey(), header.getValue().getAsString());
                    }
                    client.connect();
                }, 0, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
                client = null;
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        Packet<?> packet = event.packet;
        if (packet instanceof S00PacketDisconnect) {
            String reason = ((S00PacketDisconnect) packet).func_149603_c().getFormattedText();
            System.out.println("S00PacketDisconnect");
            System.out.println(reason);
            processBanScreen(reason);
        } else if (packet instanceof S40PacketDisconnect) {
            String reason = ((S40PacketDisconnect) packet).getReason().getFormattedText();
            System.out.println("S40PacketDisconnect");
            System.out.println(reason);
            processBanScreen(reason);
        }
    }

    @Unique
    private final List<String> times = Arrays.asList(
            "23h 59m 59s",
            "23h 59m 58s",
            "23h 59m 57s",
            "23h 59m 56s"
    );

    @Unique
    private final List<String> days = Arrays.asList(
            "29d",
            "89d",
            "359d"
    );

    private void processBanScreen(String wholeReason) {
        Failsafe.getInstance().stop();
        ArrayList<String> multilineMessage = new ArrayList<>(Arrays.asList(wholeReason.split("\n")));
        System.out.println(multilineMessage);
        try {
            if (times.stream().noneMatch(time -> multilineMessage.get(0).contains(time)) || days.stream().noneMatch(day -> multilineMessage.get(0).contains(day)))
                return;

            String duration = StringUtils.stripControlCodes(multilineMessage.get(0)).replace("You are temporarily banned for ", "")
                    .replace(" from this server!", "").trim();
            String reason = StringUtils.stripControlCodes(multilineMessage.get(2)).replace("Reason: ", "").trim();
            int durationDays = Integer.parseInt(duration.split(" ")[0].replace("d", ""));
            String banId = StringUtils.stripControlCodes(multilineMessage.get(5)).replace("Ban ID: ", "").trim();
            BanInfoWS.getInstance().playerBanned(durationDays, reason, banId, wholeReason);
            LogUtils.webhookLog("[Banned]\\nBanned for " + durationDays + " days for " + reason, true);
            System.out.println("Banned");
            if (MacroHandler.getInstance().isMacroToggled()) {
                MacroHandler.getInstance().disableMacro();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerBanned(int days, String reason, String banId, String fullReason) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "gotBanned");
        jsonObject.addProperty("uuid", Minecraft.getMinecraft().getSession().getPlayerID());
        jsonObject.addProperty("mod", "farmHelper");
        jsonObject.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        jsonObject.addProperty("days", days + 1);
        jsonObject.addProperty("banId", banId);
        jsonObject.addProperty("reason", reason);
        jsonObject.addProperty("fullReason", fullReason);
        String config = FarmHelper.config.getJson();
        JsonObject configJson = FarmHelper.gson.fromJson(config, JsonObject.class);
        configJson.addProperty("proxyAddress", "REMOVED");
        configJson.addProperty("proxyUsername", "REM0VED");
        configJson.addProperty("proxyPassword", "REMOVED");
        configJson.addProperty("webHookURL", "REMOVED");
        configJson.addProperty("discordRemoteControlToken", "REMVOED");
        String configJsonString = FarmHelper.gson.toJson(configJson);
        jsonObject.addProperty("config", configJsonString);
        jsonObject.addProperty("lastIsland", GameStateHandler.getInstance().getLastLocation().getName());
        JsonObject mods = new JsonObject();
        collectMods(mods);
        jsonObject.add("mods", mods);

        try {
            String serverId = mojangAuthentication();
            jsonObject.addProperty("serverId", serverId);
        } catch (AuthenticationException e) {
            Multithreading.schedule(() -> playerBanned(days, reason, banId, fullReason), 250, TimeUnit.MILLISECONDS);
            return;
        }

        try {
            client.send(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            try {

                String serverId = mojangAuthentication();
                jsonObject.remove("config");
                jsonObject.addProperty("config", compress(configJsonString));
                jsonObject.addProperty("serverId", serverId);
                HashMap<String, String> headers = FarmHelper.gson.fromJson(jsonObject, HashMap.class);
                HttpPost post = new HttpPost("https://ws.may2bee.pl/ban");
                post.setHeaders(getHttpClientHeaders().stream().map(header -> new BasicHeader(header.getName(), header.getValue())).toArray(BasicHeader[]::new));
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    post.addHeader(header.getKey(), FarmHelper.gson.toJson(header.getValue()));
                }
                for (Header header : post.getAllHeaders()) {
                    System.out.println(header.getName() + ": " + header.getValue());
                }
                Multithreading.schedule(() -> {
                    HttpResponse response = null;
                    try {
                        response = httpClient.execute(post);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println(response);
                }, 0, TimeUnit.MILLISECONDS);

            } catch (AuthenticationException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // SKYSKIPPED BAN STATS

    private void collectMods(JsonObject obj) {
        boolean ctLoaded = false;
        boolean ssLoaded = false;
        boolean gtcLoaded = false;
        boolean auroraLoaded = false;
        JsonObject mods = new JsonObject();
        for (ModContainer mod : Loader.instance().getModList()) {
            mods.addProperty(mod.getName(), mod.getModId());
            switch (mod.getModId()) {
                case "chattriggers": {
                    ctLoaded = true;
                    break;
                }
                case "skyskipped": {
                    ssLoaded = true;
                    break;
                }
                case "gumtuneclient": {
                    gtcLoaded = true;
                    break;
                }
                case "bossbar_customizer": {
                    auroraLoaded = true;
                    break;
                }
            }
        }
        obj.add("mods", mods);

        boolean oringoLoaded = false;
        boolean sslLoaded = false;
        boolean gbLoaded = false;
        boolean pizzaLoaded = false;
        boolean cheetoLoaded = false;
        JsonArray modFiles = new JsonArray();
        for (File mod : Objects.requireNonNull(new File(Minecraft.getMinecraft().mcDataDir, "mods").listFiles())) {
            if (!mod.isFile()) return;
            String name = mod.getName();
            modFiles.add(new JsonPrimitive(name));

            if (name.contains("OringoClient")) {
                oringoLoaded = true;
            }
            if (name.contains("SkySkippedLoader")) {
                sslLoaded = true;
            }
            if (name.contains("GhosterBuster")) {
                gbLoaded = true;
            }
            if (name.contains("Pizza_Loader")) {
                pizzaLoaded = true;
            }
            if (name.contains("Cheeto")) {
                cheetoLoaded = true;
            }
        }
        obj.add("modFiles", modFiles);

        JsonArray ctModules = new JsonArray();
        if (ctLoaded) {
            for (File module : Objects.requireNonNull(new File(Minecraft.getMinecraft().mcDataDir, "config/ChatTriggers/modules").listFiles())) {
                if (!module.isFile()) return;
                String name = module.getName();
                ctModules.add(new JsonPrimitive(name));
            }
        }
        obj.add("ctModules", ctModules);

        JsonObject configFiles = new JsonObject();
        try {

            File oneConfigFolder = new File(Minecraft.getMinecraft().mcDataDir, "OneConfig/profiles/Default Profile");
            if (!oneConfigFolder.exists()) {
                oneConfigFolder = new File(Minecraft.getMinecraft().mcDataDir, "OneConfig/config");
            }

            File oringoFile = new File(Minecraft.getMinecraft().mcDataDir, "config/OringoClient/OringoClient.json");
            if (oringoLoaded && oringoFile.exists()) {
                configFiles.addProperty("oringo", compress(oringoFile));
            }

            // Add the SkySkipped config file to the JsonObject if it exists
            File skyskippedFile = new File(Minecraft.getMinecraft().mcDataDir, "config/skyskipped/config.json");
            if (ssLoaded && skyskippedFile.exists()) {
                configFiles.addProperty("skyskipped", compress(skyskippedFile));
            }

            File skyskippedLoaderFile = new File(Minecraft.getMinecraft().mcDataDir, "config/skyskippedloader/config.json");
            if (sslLoaded && skyskippedLoaderFile.exists()) {
                configFiles.addProperty("skyskipped loader", compress(skyskippedLoaderFile));
            }

            File farmhelperFile = new File(oneConfigFolder, "farmhelper/config.json");
            if (farmhelperFile.exists()) {
                configFiles.addProperty("farmhelper", compress(farmhelperFile));
            }

            File ghostbusterFile = new File(oneConfigFolder, "ghosterbuster9000/config.json");
            if (gbLoaded && ghostbusterFile.exists()) {
                configFiles.addProperty("ghostbuster", compress(ghostbusterFile));
            }

            File pizzaFile = new File(Minecraft.getMinecraft().mcDataDir, "config/pizzaclient/config.json");
            if (pizzaLoaded && pizzaFile.exists()) {
                configFiles.addProperty("pizza", compress(pizzaFile));
            }

            File gumtuneClientFile = new File(oneConfigFolder, "gumtuneclient.json");
            if (gtcLoaded && gumtuneClientFile.exists()) {
                configFiles.addProperty("gumtuneclient", compress(gumtuneClientFile));
            }

            File auroraFile = new File(Minecraft.getMinecraft().mcDataDir, "config/aurora.toml");
            if (auroraLoaded && auroraFile.exists()) {
                configFiles.addProperty("aurora", compress(auroraFile));
            }

            File cheetoFile = new File(Minecraft.getMinecraft().mcDataDir, "Cheeto/configs/Client.json");
            if (cheetoLoaded && cheetoFile.exists()) {
                configFiles.addProperty("cheeto", compress(cheetoFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
            obj.addProperty("configError", e.getMessage());
        }

        obj.add("configFiles", configFiles);

        JsonObject extraData = new JsonObject();
        JsonObject farmHelper = new JsonObject();
        farmHelper.addProperty("macroEnabled", MacroHandler.getInstance().isMacroToggled());
        extraData.add("farmHelper", farmHelper);
        obj.add("extraData", extraData);
    }

    private static String compress(File file) throws IOException {
        ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(rstBao);
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buffer = new byte[10240];
            for (int length = 0; (length = fis.read(buffer)) != -1; ) {
                zos.write(buffer, 0, length);
            }
        } finally {
            try {
                fis.close();
            } catch (IOException ignore) {
            }
            try {
                zos.close();
            } catch (IOException ignore) {
            }
        }
        IOUtils.closeQuietly(zos);
        return Base64.getEncoder().encodeToString(rstBao.toByteArray());
    }

    private static String compress(String json) throws IOException {
        ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(rstBao);
        zos.write(json.getBytes());
        IOUtils.closeQuietly(zos);
        return Base64.getEncoder().encodeToString(rstBao.toByteArray());
    }

    public enum AnalyticsState {
        START_SESSION,
        INFO,
        END_SESSION,
    }

    public void sendAnalyticsData() {
        sendAnalyticsData(AnalyticsState.INFO);
    }

    public void sendAnalyticsData(AnalyticsState state) {
        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.getAnalyticsClock().schedule(180_000)); // 3 minutes
        System.out.println("Sending analytics data...");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "analyticsData");
        jsonObject.addProperty("mod", "farmHelper");
        jsonObject.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        String INFO_FOR_SKIDDERS = "that's public uuid bozos, not a token to login";
        String INFO_FOR_SKIDDERS2 = "that's public uuid bozos, not a token to login";
        String INFO_FOR_SKIDDERS3 = "that's public uuid bozos, not a token to login";
        jsonObject.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID());
        jsonObject.addProperty("modVersion", FarmHelper.VERSION);
        jsonObject.addProperty("timeMacroing", MacroHandler.getInstance().getAnalyticsTimer().getElapsedTime());
        jsonObject.addProperty("type", state.name());
        JsonObject additionalInfo = new JsonObject();
        additionalInfo.addProperty("cropType", MacroHandler.getInstance().getCrop().toString());
        additionalInfo.addProperty("bps", ProfitCalculator.getInstance().getBPS());
        additionalInfo.addProperty("profit", ProfitCalculator.getInstance().getRealProfitString());
        additionalInfo.addProperty("profitPerHour", ProfitCalculator.getInstance().getProfitPerHourString());
        additionalInfo.addProperty("macroingTime", MacroHandler.getInstance().getMacroingTimer().getElapsedTime());
        additionalInfo.addProperty("fastBreak", FarmHelperConfig.fastBreak);
        additionalInfo.addProperty("farmType", FarmHelperConfig.getMacro().name());
        jsonObject.add("additionalInfo", additionalInfo);
        try {
            client.send(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JsonObject getHeaders() {
        JsonObject handshake = new JsonObject();
        handshake.addProperty("reason", "WebSocketConnector");
        handshake.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID()); // that's public uuid bozos, not a token to login
        handshake.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        handshake.addProperty("modVersion", FarmHelper.VERSION);
        handshake.addProperty("mod", "farmHelper");
        try {
            String serverId = mojangAuthentication();
            handshake.addProperty("serverId", serverId);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            retryCount++;
            reconnectDelay.schedule(60_000L);
            return null;
        }
        return handshake;
    }

    private String mojangAuthentication() throws AuthenticationException {
        String serverId = UUID.randomUUID().toString().replace("-", "");
        String commentForDecompilers =
                "This sends a request to Mojang's auth server, used for verification. This is how we verify you are the real user without your session details. This is the exact same system as Skytils and Optifine use.";
        Minecraft.getMinecraft().getSessionService().joinServer(Minecraft.getMinecraft().getSession().getProfile(), Minecraft.getMinecraft().getSession().getToken(), serverId);
        return serverId;
    }

    private WebSocketClient createNewWebSocketClient() throws URISyntaxException {
        return new WebSocketClient(new URI("ws://ws.may2bee.pl")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Multithreading.schedule(() -> {
                    if (client.isOpen() && client.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
                        LogUtils.sendDebug("Connected to analytics websocket server");
                        Notifications.INSTANCE.send("FarmHelper INFO", "Connected to analytics websocket server");
                    }
                }, 1_500, TimeUnit.MILLISECONDS);
                if (FarmHelperConfig.banwaveCheckerEnabled)
                    Multithreading.schedule(() -> client.send("{\"message\":\"banwaveInfo\", \"mod\": \"farmHelper\"}"), 1_000, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onMessage(String message) {
                JsonObject jsonObject = FarmHelper.gson.fromJson(message, JsonObject.class);
                String msg = jsonObject.get("message").getAsString();
                switch (msg) {
                    case "banwaveInfo": {
                        retryCount = 0;
                        int bans = jsonObject.get("bansInLast15Minutes").getAsInt();
                        int minutes = jsonObject.get("bansInLast15MinutesTime").getAsInt();
                        int bansByMod = jsonObject.get("bansInLast15MinutesMod").getAsInt();
                        BanInfoWS.getInstance().setBans(bans);
                        BanInfoWS.getInstance().setMinutes(minutes);
                        BanInfoWS.getInstance().setBansByMod(bansByMod);
                        System.out.println("Banwave info received: " + bans + " global staff bans in the last " + minutes + " minutes, " + bansByMod + " bans by this mod");
                        break;
                    }
                    case "playerGotBanned": {
                        String username = jsonObject.get("username").getAsString();
                        String days = jsonObject.get("days").getAsString();
                        String mod = jsonObject.get("mod").getAsString();
                        String reason = jsonObject.get("reason").getAsString();
                        LogUtils.sendWarning("Player " + username + " got banned for " + days + " days while using " + mod + " (reason: " + reason + ")");
                        Notifications.INSTANCE.send("FarmHelper INFO", "Player " + username + " got banned for " + days + " days while using " + mod);
                        break;
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                LogUtils.sendDebug("Disconnected from analytics server");
                LogUtils.sendDebug("Code: " + code + ", reason: " + reason + ", remote: " + remote);
                retryCount++;
                reconnectDelay.schedule(5_000L * (retryCount + 1));
            }

            @Override
            public void onError(Exception ex) {
                LogUtils.sendDebug("Error while connecting to analytics server. " + ex.getMessage());
                ex.printStackTrace();
            }
        };
    }
}
