package com.jelly.farmhelper.world;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.macros.MushroomMacro;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.ScoreboardUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameState {
    private final Minecraft mc = Minecraft.getMinecraft();

    public enum location {
        ISLAND,
        HUB,
        LOBBY,
        LIMBO,
        TELEPORTING
    }

    public enum EffectState{
        ON,
        INDETERMINABLE,
        OFF,
    }

    public static DiscordWebhook webhook;
    public IChatComponent header;
    public IChatComponent footer;
    public EffectState cookie;
    public EffectState godPot;

    public location currentLocation = location.ISLAND;
    public Clock teleporting = new Clock();
    public boolean wasInGarden = false;

    private static final Pattern PATTERN_ACTIVE_EFFECTS = Pattern.compile(
      "§r§r§7You have a §r§cGod Potion §r§7active! §r§d([0-9]*?:?[0-9]*?:?[0-9]*)§r");

    public boolean frontWalkable;
    public boolean rightWalkable;
    public boolean backWalkable;
    public boolean leftWalkable;
    public String serverIP;

    public double dx;
    public double dz;
    public double dy;

    public BlockPos blockInPos;
    public Block blockStandingOn;

    public int jacobCounter;

    public GameState() {
        webhook = new DiscordWebhook(FarmHelper.config.webHookURL);
        webhook.setUsername("Jelly - Farm Helper");
        webhook.setAvatarUrl("https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png");
    }

    public void update() {
        if(mc.getCurrentServerData() != null) {
            if (mc.getCurrentServerData().serverIP != null) {
                serverIP = mc.getCurrentServerData().serverIP;
            }
        }
        currentLocation = getLocation();
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("Island")) {
                wasInGarden = false;
            } else if (cleanedLine.contains("Garden") || cleanedLine.contains("Plot:")) {
                wasInGarden = true;
            }
        }
        checkFooter();
            updateWalkables();
        dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
        dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
        dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);
        blockInPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        blockStandingOn = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock();
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
            if (cleanedLine.contains("Village") || cleanedLine.contains("Bazaar") || cleanedLine.contains("Community")) {
                return location.HUB;
            } else if (cleanedLine.contains("Island") || cleanedLine.contains("Garden") || cleanedLine.contains("Plot:")) {
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
        //
        boolean foundGodPot = false;
        boolean foundCookieText = false;
        boolean loaded = false;

        if (footer != null) {
            String formatted = footer.getFormattedText();
            for (String line : formatted.split("\n")) {
                Matcher activeEffectsMatcher = PATTERN_ACTIVE_EFFECTS.matcher(line);
                if (activeEffectsMatcher.matches()) {
                    foundGodPot = true;
                } else if (line.contains("\u00a7d\u00a7lCookie Buff")) {
                    foundCookieText = true;
                } else if (foundCookieText && line.contains("Not active! Obtain")) {
                    foundCookieText = false;
                    cookie = EffectState.OFF;
                } else if (foundCookieText) {
                    foundCookieText = false;
                    cookie = EffectState.ON;;
                }
                if(line.contains("Active")) {
                    loaded = true;
                }
            }
            godPot = foundGodPot ? EffectState.ON : EffectState.OFF;
            if(!loaded){
                godPot = EffectState.INDETERMINABLE;
                cookie = EffectState.INDETERMINABLE;
            }
        }
    }

    private void updateWalkables() {
        float yaw = (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? MushroomMacro.closest90Yaw : mc.thePlayer.rotationYaw;
        frontWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, 1, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 1, 1, yaw)));
        rightWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 0, 0, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 1, 0, yaw)));
        backWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, -1, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 1, -1, yaw)));
        leftWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 0, 0, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 1, 0, yaw)));
    }

    public static int getJacobCounter() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("with") && !cleanedLine.contains("Elle")) {
                try {
                    return Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
