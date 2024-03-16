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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Unique
    private final Map<Integer, String> farmHelperV2$cachedScoreboard = new HashMap<>();

    @Inject(method = "renderScoreboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScorePlayerTeam;formatPlayerName(Lnet/minecraft/scoreboard/Team;Ljava/lang/String;)Ljava/lang/String;", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci, Scoreboard scoreboard, Collection collection, int i, int j, int k, int l, int m, int n, Iterator iterator2, Score score2, ScorePlayerTeam scorePlayerTeam2) {
        String string2 = ScorePlayerTeam.formatPlayerName(scorePlayerTeam2, score2.getPlayerName());
        String clean = farmHelperV2$cleanSB(string2);
        if (!clean.equals(farmHelperV2$cachedScoreboard.get(n)) || !farmHelperV2$cachedScoreboard.containsKey(n)) {
            farmHelperV2$cachedScoreboard.put(n, clean);
            MinecraftForge.EVENT_BUS.post(new UpdateScoreboardLineEvent(clean));
        }
    }

    @Unique
    private static String farmHelperV2$cleanSB(String scoreboard) {
        StringBuilder cleaned = new StringBuilder();

        for (char c : StringUtils.stripControlCodes(scoreboard).toCharArray()) {
            if (c >= 32 && c < 127) {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
