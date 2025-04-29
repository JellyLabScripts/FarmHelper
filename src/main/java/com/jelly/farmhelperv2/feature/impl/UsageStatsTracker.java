package com.jelly.farmhelperv2.feature.impl;

import com.google.gson.*;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static com.jelly.farmhelperv2.feature.impl.BanInfoWS.fileNamePrefix;
import static com.jelly.farmhelperv2.feature.impl.BanInfoWS.statsDirectory;


public class UsageStatsTracker implements IFeature {
    private static UsageStatsTracker instance;
    public  static UsageStatsTracker getInstance() {
        return instance == null ? (instance = new UsageStatsTracker()) : instance;
    }

    private long baseTodayMillis = 0;
    private long baseTotalMillis = 0;
    private LocalDate todayDate = LocalDate.now();
    private long sessionMillis = 0;
    private long lastTickMillis = System.currentTimeMillis();
    private boolean running = false;

    private UsageStatsTracker() { loadFromFile(); }

    public String getTodayString() {
        return fmt(baseTodayMillis + sessionMillis);
    }
    public String getTotalString() {
        return fmt(baseTotalMillis + sessionMillis);
    }

    public long  getTodayMillis() { return baseTodayMillis + sessionMillis; }

    private long rollingMillis(int days) {
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        long sum = 0;
        for (JsonElement e : readArray()) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("timestamp").getAsLong() >= cutoff)
                sum += o.get("timeMacroing").getAsLong();
        }
        return sum + sessionMillis;
    }

    public long get7dMillis() { return rollingMillis(7);  }
    public long get30dMillis() { return rollingMillis(30); }
    public String get7dString() { return fmt(get7dMillis()); }
    public String get30dString() { return fmt(get30dMillis()); }

    @Override public String  getName() { return "User Stats Tracker"; }
    @Override public boolean isRunning() { return running; }
    @Override public boolean shouldPauseMacroExecution() { return false; }
    @Override public boolean shouldStartAtMacroStart() { return false; }
    @Override public boolean isToggled() { return true;  }
    @Override public boolean shouldCheckForFailsafes() { return false; }
    @Override public void resetStatesAfterMacroDisabled() {}
    @Override public void start() { running = true;  lastTickMillis = System.currentTimeMillis(); }
    @Override public void resume() { start(); }
    @Override public void stop() { running = false ;}

    public void tick(boolean macroRunning) {
        long now = System.currentTimeMillis();
        long delta = now - lastTickMillis;
        lastTickMillis = now;
        if (!todayDate.equals(LocalDate.now())) {
            todayDate      = LocalDate.now();
            baseTodayMillis = 0;
        }
        if (macroRunning && delta > 0 && delta < 5000) {
            sessionMillis += delta;
        }
    }

    private void loadFromFile() {
        JsonArray arr = readArray();
        LocalDate today = LocalDate.now();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            long ms = o.get("timeMacroing").getAsLong();
            baseTotalMillis += ms;
            long ts = o.get("timestamp").getAsLong();
            LocalDate d = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.equals(today)) baseTodayMillis += ms;
        }
    }

    private String fmt(long ms) {
        long h = TimeUnit.MILLISECONDS.toHours(ms);
        long m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        return String.format("%dh %02dm", h, m);
    }

    private File statsFile() {
        String uuid = Minecraft.getMinecraft().getSession().getPlayerID().replace("-", "");
        return new File(statsDirectory, fileNamePrefix + uuid + ".dat");
    }

    private JsonArray readArray() {
        File f = statsFile();
        if (!f.exists()) return new JsonArray();
        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(f));
             InputStreamReader isr = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {
            JsonElement el = new JsonParser().parse(isr);
            return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            LogUtils.sendDebug("Error Reading Usage Stats Array");
//            e.printStackTrace();
        }
        return new JsonArray();
    }
}