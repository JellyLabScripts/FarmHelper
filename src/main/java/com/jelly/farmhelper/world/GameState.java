package com.jelly.farmhelper.world;

import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.ScoreboardUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameState {
    private Minecraft mc = Minecraft.getMinecraft();

    public enum location {
        ISLAND,
        HUB,
        LOBBY,
        LIMBO,
        TELEPORTING
    }

    public IChatComponent header;
    public IChatComponent footer;
    public boolean cookie;
    public boolean godPot;

    public location currentLocation = location.ISLAND;
    public Clock teleporting = new Clock();

    private static final Pattern PATTERN_ACTIVE_EFFECTS = Pattern.compile(
      "\u00a7r\u00a7r\u00a77You have a \u00a7r\u00a7cGod Potion \u00a7r\u00a77active! \u00a7r\u00a7d([0-9]*?:?[0-9]*?:?[0-9]*)\u00a7r");

    public boolean frontWalkable;
    public boolean rightWalkable;
    public boolean backWalkable;
    public boolean leftWalkable;

    public double dx;
    public double dz;

    public int jacobCounter;

    public void update() {
        currentLocation = getLocation();
        checkFooter();
        updateWalkables();
        dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
        dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
        jacobCounter = getJacobCounter();
    }

    private location getLocation() {
        if (ScoreboardUtils.getScoreboardLines().size() == 0) {
            if (BlockUtils.countCarpet() > 0) {
                return location.LIMBO;
            }
            return location.TELEPORTING;
        }

        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("Village")) {
                return location.HUB;
            } else if (cleanedLine.contains("Island")) {
                return location.ISLAND;
            }
        }

        if (ScoreboardUtils.getScoreboardDisplayName(1).contains("SKYBLOCK")) {
            return location.TELEPORTING;
        } else {
            return location.LOBBY;
        }
    }

    private void checkFooter() {
        // LogUtils.debugFullLog("Looking for godpot/cookie");
        boolean foundGodPot = false;
        boolean foundCookieText = false;
        if (footer != null) {
            String formatted = footer.getFormattedText();
            for (String line : formatted.split("\n")) {
                Matcher activeEffectsMatcher = PATTERN_ACTIVE_EFFECTS.matcher(line);
                if (activeEffectsMatcher.matches()) {
                    foundGodPot = true;
                } else if (line.contains("\u00a7d\u00a7lCookie Buff")) {
                    foundCookieText = true;
                } else if (foundCookieText && line.contains("Not active! Obtain")) {
                    // LogUtils.debugLog("Cookie buff not active!");
                    foundCookieText = false;
                    cookie = false;
                } else if (foundCookieText) {
                    // LogUtils.debugFullLog("Cookie active!");
                    foundCookieText = false;
                    cookie = true;
                }
            }
            if (!foundGodPot) {
                // LogUtils.debugLog("God pot buff not active!");
                godPot = false;
            } else {
                // LogUtils.debugFullLog("God pot buff active!");
                godPot = true;
            }
        }
    }

    private void updateWalkables() {
        frontWalkable = BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, 1));
        rightWalkable = BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 0, 0));
        backWalkable = BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, -1));
        leftWalkable = BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 0, 0));
    }

    public static int getJacobCounter() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("with")) {
                return Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
            }
        }
        return 0;
    }
}
