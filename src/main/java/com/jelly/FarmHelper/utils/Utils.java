package com.jelly.FarmHelper.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import me.acattoXD.WartMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Utils {
  private static final Minecraft mc = Minecraft.getMinecraft();
  public static boolean isUngrabbed = false;
  private static MouseHelper oldMouseHelper;
  private static boolean doesGameWantUngrabbed;

  public static String formatNumber(int number) {
    String s = Integer.toString(number);
    return String.format("%,d", number);
  }

  public static String formatNumber(float number) {
    String s = Integer.toString(Math.round(number));
    return String.format("%,d", Math.round(number));
  }

  public static int nextInt(int upperbound) {
    Random r = new Random();
    return r.nextInt(upperbound);
  }

  public static String getScoreboardDisplayName(int line) {
    try {
      return mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(line).getDisplayName();
    } catch (Exception e) {
      LogUtils.debugLog("Error in getting scoreboard " + e);
      return "";
    }
  }

  public static List<String> getSidebarLines() {
    List<String> lines = new ArrayList<>();
    if (mc.theWorld == null) return lines;
    Scoreboard scoreboard = mc.theWorld.getScoreboard();
    if (scoreboard == null) return lines;

    ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
    if (objective == null) return lines;

    Collection<Score> scores = scoreboard.getSortedScores(objective);
    List<Score> list = scores.stream()
      .filter(input -> input != null && input.getPlayerName() != null && !input.getPlayerName()
        .startsWith("#"))
      .collect(Collectors.toList());

    if (list.size() > 15) {
      scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
    } else {
      scores = list;
    }

    for (Score score : scores) {
      ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
      lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
    }

    return lines;
  }

  public static String cleanSB(String scoreboard) {
    char[] nvString = StringUtils.stripControlCodes(scoreboard).toCharArray();
    StringBuilder cleaned = new StringBuilder();

    for (char c : nvString) {
      if ((int) c > 20 && (int) c < 127) {
        cleaned.append(c);
      }
    }

    return cleaned.toString();
  }

  public static void ScheduleRunnable(Runnable r, int delay, TimeUnit tu) {
    WartMacro.executor.schedule(r, delay, tu);
  }

  public static void ExecuteRunnable(Runnable r) {
    WartMacro.executor.execute(r);
  }

  public static void resetExecutor() {
    WartMacro.executor.shutdownNow();
    WartMacro.executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
  }

  public static void ungrabMouse() {
    Minecraft m = Minecraft.getMinecraft();
    if (isUngrabbed) return;
    m.gameSettings.pauseOnLostFocus = false;
    if (oldMouseHelper == null) oldMouseHelper = m.mouseHelper;
    doesGameWantUngrabbed = !Mouse.isGrabbed();
    oldMouseHelper.ungrabMouseCursor();
    m.inGameHasFocus = true;
    m.mouseHelper = new MouseHelper() {
      @Override
      public void mouseXYChange() {
      }
      @Override
      public void grabMouseCursor() {
        doesGameWantUngrabbed = false;
      }
      @Override
      public void ungrabMouseCursor() {
        doesGameWantUngrabbed = true;
      }
    };
    isUngrabbed = true;
  }

  // This is a function just to check if you're ratted in "me.acattoXD.Hiders"
  public static String getExecutor() {
    try {
      return new Exception().getStackTrace()[2].getClassName();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * This function performs all the steps required to regrab the mouse.
   */
  public static void regrabMouse() {
    if (!isUngrabbed) return;
    Minecraft m = Minecraft.getMinecraft();
    m.mouseHelper = oldMouseHelper;
    if (!doesGameWantUngrabbed) m.mouseHelper.grabMouseCursor();
    oldMouseHelper = null;
    isUngrabbed = false;
  }
}

