package com.jelly.farmhelperv2.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jelly.farmhelperv2.event.UpdateScoreboardListEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static List<String> cachedScoreboardLines = new ArrayList<>();
    public static List<String> cachedCleanScoreboardLines = new ArrayList<>();

    public static List<String> getScoreboardLines(boolean clean) {
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

    private final List<String> previousScoreboard = new ArrayList<>();

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (mc.theWorld == null) return;
        long currentTime = System.currentTimeMillis();
        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        List<String> cleanScoreboardLines = new ArrayList<>();
        List<String> scoreboardLines = new ArrayList<>();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
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
            String playerName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            scoreboardLines.add(playerName);
            cleanScoreboardLines.add(cleanSB(playerName));
        }

        if (previousScoreboard.equals(cleanScoreboardLines)) return;

        previousScoreboard.clear();
        previousScoreboard.addAll(cleanScoreboardLines);
        cachedScoreboardLines = scoreboardLines;
        cachedCleanScoreboardLines = cleanScoreboardLines;
        MinecraftForge.EVENT_BUS.post(new UpdateScoreboardListEvent(scoreboardLines, cleanScoreboardLines, currentTime));
    }

    private static String cleanSB(String scoreboard) {
        StringBuilder cleaned = new StringBuilder();

        for (char c : StringUtils.stripControlCodes(scoreboard).toCharArray()) {
            if (c >= 32 && c < 127 || c == 'ൠ') {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
