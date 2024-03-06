package com.jelly.farmhelperv2.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static List<String> cachedScoreboardLines = new ArrayList<>();
    private static List<String> cachedCleanScoreboardLines = new ArrayList<>();
    private static long lastUpdateTimestamp = 0;

    public static List<String> getScoreboardLines(boolean clean) {
        long currentTime = System.currentTimeMillis();
        if (!cachedScoreboardLines.isEmpty() && currentTime - lastUpdateTimestamp < 50) {
            return (clean ? cachedCleanScoreboardLines : cachedScoreboardLines);
        }
        cachedScoreboardLines.clear();
        cachedCleanScoreboardLines.clear();
        if (mc.theWorld == null) return (clean ? cachedCleanScoreboardLines : cachedScoreboardLines);
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return (clean ? cachedCleanScoreboardLines : cachedScoreboardLines);

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return (clean ? cachedCleanScoreboardLines : cachedScoreboardLines);

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
            cachedScoreboardLines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
            cachedCleanScoreboardLines.add(cleanSB(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName())));
        }

        lastUpdateTimestamp = currentTime;
        return (clean ? cachedCleanScoreboardLines : cachedScoreboardLines);
    }

    public static String getScoreboardTitle() {
        if (mc.theWorld == null) return "";
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return "";

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return "";

        return StringUtils.stripControlCodes(objective.getDisplayName());
    }

    public static String cleanSB(String scoreboard) {
        StringBuilder cleaned = new StringBuilder();

        for (char c : StringUtils.stripControlCodes(scoreboard).toCharArray()) {
            if (c > 20 && c < 127) {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }

    public static String getScoreboardDisplayName(int line) {
        try {
            return mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(line).getDisplayName();
        } catch (Exception e) {
            LogUtils.sendDebug("Error in getting scoreboard " + e);
            return "";
        }
    }
}
