package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.*;
import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.FailsafeUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.mojang.authlib.exceptions.AuthenticationException;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.compress.utils.IOUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.spongepowered.asm.mixin.Unique;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BanInfoWS implements IFeature {
    private static BanInfoWS instance;
    private final Clock reconnectDelay = new Clock();
    @Getter
    private long lastReceivedPacket = System.currentTimeMillis();
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
    private WebSocketClient client;

    @Getter
    @Setter
    private int staffBans = 0;

    @Getter
    @Setter
    private int minutes = 0;

    @Getter
    @Setter
    private int bansByMod = 0;

    @Getter
    private boolean receivedBanwaveInfo = false;

    public static final File statsDirectory = new File(Minecraft.getMinecraft().mcDataDir, "farmhelper");
    public static final String fileNamePrefix = "fh_stats_";

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
            if (client != null)
                client.close();
        }
    }

    public static BanInfoWS getInstance() {
        if (instance == null) {
            instance = new BanInfoWS();
        }
        return instance;
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
        return getAllBans() >= FarmHelperConfig.banwaveThreshold;
    }

    public int getAllBans() {
        switch (FarmHelperConfig.banwaveThresholdType) {
            case 0: {
                return staffBans;
            }
            case 1: {
                return bansByMod;
            }
            case 2: {
                return bansByMod + staffBans;
            }
        }
        return 0;
    }

    @SubscribeEvent
    public void onCheckIfDisconnected(TickEvent.ClientTickEvent event) {
        if (client != null && client.isOpen() && System.currentTimeMillis() - lastReceivedPacket > 120_000L) {
            LogUtils.sendDebug("Disconnected from analytics server (no packets received in 2 minutes)");
            client.close();
            reconnectDelay.schedule(1_000);
        }
    }

    @SubscribeEvent
    public void onTickReconnect(TickEvent.ClientTickEvent event) {
        if (!reconnectDelay.isScheduled() || !reconnectDelay.passed()) return;

        if (client.isClosed() && !client.isOpen()) {
            try {
                reconnectDelay.reset();
                receivedBanwaveInfo = false;
                LogUtils.sendDebug("Connecting to analytics server...");
                Multithreading.schedule(() -> {
                    lastReceivedPacket = System.currentTimeMillis();
                    JsonObject headers = getHeaders();
                    if (headers == null) {
                        LogUtils.sendDebug("Failed to connect to analytics server. Retrying in 1 minute...");
                        return;
                    }
                    for (Map.Entry<String, JsonElement> header : headers.entrySet()) {
                        client.addHeader(header.getKey(), header.getValue().getAsString());
                    }
                    client.reconnect();
                }, 0, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        Packet<?> packet = event.packet;
        if (packet instanceof S40PacketDisconnect) {
            String reason = ((S40PacketDisconnect) packet).getReason().getFormattedText();
            processBanScreen(reason);
        }
    }

    // SKYSKIPPED BAN STATS

    private void processBanScreen(String wholeReason) {
        FailsafeManager.getInstance().stopFailsafes();
        ArrayList<String> multilineMessage = new ArrayList<>(Arrays.asList(wholeReason.split("\n")));
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
            if (FarmHelperConfig.captureClipAfterFailsafe && !FarmHelperConfig.captureClipKeybind.getKeyBinds().isEmpty()) {
                Multithreading.schedule(() -> {
                    FailsafeUtils.captureClip();
                    LogUtils.sendDebug("[Failsafe] Clip captured!");
                }, FarmHelperConfig.captureClipDelay, TimeUnit.SECONDS);
            }
            if (MacroHandler.getInstance().isMacroToggled()) {
                MacroHandler.getInstance().disableMacro();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerBanned(int days, String reason, String banId, String fullReason) {
        if (System.currentTimeMillis() - GameStateHandler.getInstance().getLastTimeInGarden() > 15_000L) return;
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
            Multithreading.schedule(() -> playerBanned(days, reason, banId, fullReason), 1337, TimeUnit.MILLISECONDS);
            return;
        }

        try {
            client.send(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            if (!mod.isFile()) continue;
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
                if (!module.isFile()) continue;
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
        farmHelper.addProperty("pestsDestroyerEnabled", FarmHelperConfig.enablePestsDestroyer);
        farmHelper.addProperty("crop", MacroHandler.getInstance().getCrop().name());
        farmHelper.addProperty("macroType", FarmHelperConfig.getMacro().name());
        farmHelper.addProperty("fastBreak", FarmHelperConfig.fastBreak);
        farmHelper.addProperty("lastFailsafes", FailsafeManager.getInstance().getBanInfoWSLastFailsafe());
        farmHelper.addProperty("longestSessionLast7D", getLongestSessionLast7D());
        farmHelper.addProperty("autoCookie", FarmHelperConfig.autoCookie);
        farmHelper.addProperty("autoGodPot", FarmHelperConfig.autoGodPot);
        extraData.add("farmHelper", farmHelper);
        obj.add("extraData", extraData);
        System.out.println("lastFailsafe: " + FailsafeManager.getInstance().getBanInfoWSLastFailsafe());
    }

    public void saveStats() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID());
        jsonObject.addProperty("modVersion", FarmHelper.VERSION);
        jsonObject.addProperty("timeMacroing", MacroHandler.getInstance().getAnalyticsTimer().getElapsedTime());
        jsonObject.addProperty("fastBreak", FarmHelperConfig.fastBreak);
        jsonObject.addProperty("timestamp", System.currentTimeMillis());

        JsonArray jsonArray = readJsonArrayFromFile();
        jsonArray.add(jsonObject);
        writeJsonArrayToFile(jsonArray);
    }

    public long getLongestSessionLast7D() {
        long sevenDaysAgo = System.currentTimeMillis() - 604800000L; // 7 days in milliseconds
        JsonArray jsonArray = readJsonArrayFromFile();
        JsonArray updatedJsonArray = new JsonArray();
        long longestSessionLength = 0L;

        for (JsonElement element : jsonArray) {
            JsonObject session = element.getAsJsonObject();
            long sessionTimestamp = session.get("timestamp").getAsLong();
            if (sessionTimestamp >= sevenDaysAgo) {
                long sessionLength = session.get("timeMacroing").getAsLong();
                if (sessionLength > longestSessionLength) {
                    longestSessionLength = sessionLength;
                }
                updatedJsonArray.add(session);
            }
        }

        writeJsonArrayToFile(updatedJsonArray); // Optimize by writing only if needed
        return longestSessionLength;
    }

    private JsonArray readJsonArrayFromFile() {
        String playerUUID = Minecraft.getMinecraft().getSession().getPlayerID().replace("-", "");
        File statsFile = new File(statsDirectory, fileNamePrefix + playerUUID + ".dat");

        if (statsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(statsFile);
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 InputStreamReader isr = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {
                JsonElement parsedElement = new JsonParser().parse(isr);
                if (parsedElement.isJsonArray()) {
                    return parsedElement.getAsJsonArray();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new JsonArray(); // return an empty array if file doesn't exist or error occurs
    }

    private void writeJsonArrayToFile(JsonArray jsonArray) {
        String playerUUID = Minecraft.getMinecraft().getSession().getPlayerID().replace("-", "");
        File statsFile = new File(statsDirectory, fileNamePrefix + playerUUID + ".dat");

        try {
            if (!statsDirectory.exists()) {
                statsDirectory.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(statsFile);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
                osw.write(jsonArray.toString());
                osw.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFailsafeInfo(FailsafeManager.EmergencyType type) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "staffCheck");
        jsonObject.addProperty("mod", "farmHelper");
        jsonObject.addProperty("username", Minecraft.getMinecraft().getSession().getUsername());
        jsonObject.addProperty("id", Minecraft.getMinecraft().getSession().getPlayerID());
        jsonObject.addProperty("modVersion", FarmHelper.VERSION);
        jsonObject.addProperty("checkType", type.name());
        JsonObject additionalInfo = new JsonObject();
        additionalInfo.addProperty("cropType", MacroHandler.getInstance().getCrop().toString());
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
            reconnectDelay.schedule(6_000L);
            return null;
        }
        return handshake;
    }

    private String mojangAuthentication() throws AuthenticationException {
        Random r1 = new Random();
        Random r2 = new Random(System.identityHashCode(new Object()));
        BigInteger random1Bi = new BigInteger(128, r1);
        BigInteger random2Bi = new BigInteger(128, r2);
        BigInteger serverBi = random1Bi.xor(random2Bi);
        String serverId = serverBi.toString(16);
        String commentForDecompilers =
                "This sends a request to Mojang's auth server, used for verification. This is how we verify you are the real user without your session details. This is the exact same system as Skytils and Optifine use.";
        try {
            Minecraft.getMinecraft().getSessionService().joinServer(Minecraft.getMinecraft().getSession().getProfile(), Minecraft.getMinecraft().getSession().getToken(), serverId);
        } catch (AuthenticationException e) {
            throw new AuthenticationException("Failed to authenticate with Mojang's servers. " + e.getMessage());
        }
        return serverId;
    }

    private WebSocketClient createNewWebSocketClient() throws URISyntaxException {
        return new WebSocketClient(new URI("ws://ws.may2bee.pl")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
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
                        BanInfoWS.getInstance().setStaffBans(bans);
                        BanInfoWS.getInstance().setMinutes(minutes);
                        BanInfoWS.getInstance().setBansByMod(bansByMod);
                        lastReceivedPacket = System.currentTimeMillis();
                        if (!receivedBanwaveInfo) {
                            if (client.isOpen() && client.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
                                LogUtils.sendDebug("Connected to analytics websocket server");
                                LogUtils.sendNotification("Farm Helper", "Connected to analytics websocket server");
                            }
                        }
                        receivedBanwaveInfo = true;
                        break;
                    }
                    case "playerGotBannedWithStats": {
                        String username = jsonObject.get("username").getAsString();
                        String days = jsonObject.get("days").getAsString();
                        String reason = jsonObject.get("reason").getAsString();
                        boolean macroEnabled = jsonObject.get("macroEnabled").getAsBoolean();
                        boolean fastBreak = jsonObject.get("fastBreak").getAsBoolean();
                        String crop = jsonObject.get("crop").getAsString().toLowerCase().replace("_", " ");
                        long longestSession7D = jsonObject.get("longestSession7D").getAsLong();
                        String lastFailsafe = jsonObject.get("lastFailsafe").getAsString();
                        LogUtils.sendWarning("User §c" + username + "§e got banned for " + days + " days"
                                + (macroEnabled ? " while " + (fastBreak ? "§c§nfastbreaking§r§e " : "farming ") + crop + "." : ".")
                                + "\n§ePossible reason: §c" + reason + "§e."
                                + "\n§eLongest session in the last 7 days: §c" + LogUtils.formatTime(longestSession7D)
                                + (!lastFailsafe.isEmpty() ? "\n§eLast failsafe: §c" + lastFailsafe : ""));
                        // LogUtils.sendNotification("Farm Helper", "User " + username + " got banned for " + days + " days");
                        break;
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                LogUtils.sendDebug("Disconnected from analytics server");
                LogUtils.sendDebug("Code: " + code + ", reason: " + reason + ", remote: " + remote);
                if (!reconnectDelay.isScheduled())
                    reconnectDelay.schedule(5_000L);
            }

            @Override
            public void onError(Exception ex) {
                LogUtils.sendDebug("Error while connecting to analytics server. " + ex.getMessage());
                ex.printStackTrace();
            }
        };
    }

    public void loadStatsOnInit() {
        getLongestSessionLast7D();
    }
}
