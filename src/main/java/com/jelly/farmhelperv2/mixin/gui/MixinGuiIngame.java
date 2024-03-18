package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.event.UpdateScoreboardLineEvent;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Mixin(value = GuiIngame.class, priority = Integer.MAX_VALUE)
public class MixinGuiIngame {

    @Unique
    private final Map<Integer, String> farmHelperV2$cachedScoreboard = new HashMap<>();

    @Inject(method = "renderScoreboard", at = @At("HEAD"))
    private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        scores.removeIf(score -> score.getPlayerName().startsWith("#"));

        int index = 0;
        for (Score score : scores) {
            ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(score.getPlayerName());
            String string = ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.getPlayerName());
            String clean = farmHelperV2$cleanSB(string);
            if (!clean.equals(farmHelperV2$cachedScoreboard.get(index)) || !farmHelperV2$cachedScoreboard.containsKey(index)) {
                farmHelperV2$cachedScoreboard.put(index, clean);
                MinecraftForge.EVENT_BUS.post(new UpdateScoreboardLineEvent(clean));
            }
            index++;
            if (index > 15) break;
        }
    }

    @Unique
    private static String farmHelperV2$cleanSB(String scoreboard) {
        StringBuilder cleaned = new StringBuilder();

        for (char c : StringUtils.stripControlCodes(scoreboard).toCharArray()) {
            if (c >= 32 && c < 127 || c == 'ൠ') {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
