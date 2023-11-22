package com.github.may2beez.farmhelperv2.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.page.AutoSellNPCItemsPage;
import com.github.may2beez.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.github.may2beez.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.github.may2beez.farmhelperv2.config.struct.Rewarp;
import com.github.may2beez.farmhelperv2.feature.impl.*;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.handler.RotationHandler;
import com.github.may2beez.farmhelperv2.hud.DebugHUD;
import com.github.may2beez.farmhelperv2.hud.ProfitCalculatorHUD;
import com.github.may2beez.farmhelperv2.hud.StatusHUD;
import com.github.may2beez.farmhelperv2.util.BlockUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.PlayerUtils;
import com.github.may2beez.farmhelperv2.util.helper.AudioManager;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import com.github.may2beez.farmhelperv2.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// THIS IS RAT - CatalizCS
@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class FarmHelperConfig extends Config {
    private transient static final Minecraft mc = Minecraft.getMinecraft();
    private transient static final String GENERAL = "General";
    private transient static final String MISCELLANEOUS = "Miscellaneous";
    private transient static final String FAILSAFE = "Failsafe";
    private transient static final String SCHEDULER = "Scheduler";
    private transient static final String JACOBS_CONTEST = "Jacob's Contest";
    private transient static final String VISITORS_MACRO = "Visitors Macro";
    private transient static final String PESTS_DESTROYER = "Pests Destroyer";
    private transient static final String DISCORD_INTEGRATION = "Discord Integration";
    private transient static final String DELAYS = "Delays";
    private transient static final String HUD = "HUD";
    private transient static final String DEBUG = "Debug";
    private transient static final String EXPERIMENTAL = "Experimental";

    private transient static final File configRewarpFile = new File("farmhelper_rewarp.json");


    public static List<Rewarp> rewarpList = new ArrayList<>();

    public static boolean proxyEnabled = false;
    public static String proxyAddress = "";
    public static String proxyUsername = "";
    public static String proxyPassword = "";
    public static Proxy.ProxyType proxyType = Proxy.ProxyType.HTTP;

    public enum MacroEnum {
        S_V_NORMAL_TYPE,
        S_PUMPKIN_MELON,
        S_PUMPKIN_MELON_MELONGKINGDE,
        S_PUMPKIN_MELON_DEFAULT_PLOT,
        S_SUGAR_CANE,
        S_CACTUS,
        S_CACTUS_SUNTZU,
        S_COCOA_BEANS,
        S_COCOA_BEANS_LEFT_RIGHT,
        S_MUSHROOM,
        S_MUSHROOM_ROTATE
    }

    public enum CropEnum {
        NONE,
        CARROT,
        NETHER_WART,
        POTATO,
        WHEAT,
        SUGAR_CANE,
        MELON,
        PUMPKIN,
        PUMPKIN_MELON_UNKNOWN,
        CACTUS,
        COCOA_BEANS,
        MUSHROOM,
        MUSHROOM_ROTATE,
    }

    public static void addRewarp() {
        if (FarmHelperConfig.rewarpList.stream().anyMatch(rewarp -> rewarp.isTheSameAs(BlockUtils.getRelativeBlockPos(0, 0, 0)))) {
            LogUtils.sendError("Rewarp location has already been set!");
            return;
        }
        Rewarp newRewarp = new Rewarp(BlockUtils.getRelativeBlockPos(0, 0, 0));
        rewarpList.add(newRewarp);
        LogUtils.sendSuccess("Added rewarp: " + newRewarp);
        saveRewarpConfig();
    }

    public static void removeRewarp() {
        Rewarp closest = null;
        if (rewarpList.isEmpty()) {
            LogUtils.sendError("No rewarp locations set!");
            return;
        }
        double closestDistance = Double.MAX_VALUE;
        for (Rewarp rewarp : rewarpList) {
            double distance = rewarp.getDistance(BlockUtils.getRelativeBlockPos(0, 0, 0));
            if (distance < closestDistance) {
                closest = rewarp;
                closestDistance = distance;
            }
        }
        if (closest != null) {
            rewarpList.remove(closest);
            LogUtils.sendSuccess("Removed the closest rewarp: " + closest);
            saveRewarpConfig();
        }
    }

    public static void removeAllRewarps() {
        rewarpList.clear();
        LogUtils.sendSuccess("Removed all saved rewarp positions");
        saveRewarpConfig();
    }

    public static void saveRewarpConfig() {
        try {
            if (!configRewarpFile.exists())
                Files.createFile(configRewarpFile.toPath());

            Files.write(configRewarpFile.toPath(), FarmHelper.gson.toJson(rewarpList).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // START GENERAL

    @Dropdown(
            name = "Macro Type", category = GENERAL,
            description = "Farm Types",
            options = {
                    "S Shape / Vertical - Crops (Wheat, Carrot, Potato, NW)", // 0
                    "S Shape - Pumpkin/Melon", // 1
                    "S Shape - Pumpkin/Melon Melongkingde", // 2
                    "S Shape - Pumpkin/Melon Default Plot", // 3
                    "S Shape - Sugar Cane", // 4
                    "S Shape - Cactus", // 5
                    "S Shape - Cactus SunTzu Black Cat", // 6
                    "S Shape - Cocoa Beans", // 7
                    "S Shape - Cocoa Beans (Left/Right)", // 8
                    "S Shape - Mushroom (45°)", // 9
                    "S Shape - Mushroom (30° with rotations)", // 10
            }, size = 2
    )
    public static int macroType = 0;

    public static MacroEnum getMacro() {
        return MacroEnum.values()[macroType];
    }

    @Switch(
            name = "Rotate After Warped", category = GENERAL, subcategory = "Rotation",
            description = "Rotates the player after re-warping", size = 1
    )
    public static boolean rotateAfterWarped = false;

    @Switch(
            name = "Rotate After Drop", category = GENERAL, subcategory = "Rotation",
            description = "Rotates after the player falls down", size = 1
    )
    public static boolean rotateAfterDrop = false;

    @Switch(
            name = "Don't fix micro rotations after warp", category = GENERAL, subcategory = "Rotation",
            description = "The macro doesn't do micro-rotations after rewarp if the current yaw and target yaw are the same", size = 2
    )
    public static boolean dontFixAfterWarping = false;

    @Switch(
            name = "Custom Pitch", category = GENERAL, subcategory = "Rotation",
            description = "Set pitch to custom level after starting the macro"
    )
    public static boolean customPitch = false;

    @Number(
            name = "Custom Pitch Level", category = GENERAL, subcategory = "Rotation",
            description = "Set custom pitch level after starting the macro",
            min = -90.0F, max = 90.0F
    )
    public static float customPitchLevel = 0;

    @Switch(
            name = "Custom Yaw", category = GENERAL, subcategory = "Rotation",
            description = "Set yaw to custom level after starting the macro"
    )
    public static boolean customYaw = false;

    @Number(
            name = "Custom Yaw Level", category = GENERAL, subcategory = "Rotation",
            description = "Set custom yaw level after starting the macro",
            min = -180.0F, max = 180.0F
    )
    public static float customYawLevel = 0;

    @Switch(
            name = "Highlight rewarp points", category = GENERAL, subcategory = "Rewarp",
            description = "Highlights all rewarp points you have added",
            size = OptionSize.DUAL
    )
    public static boolean highlightRewarp = true;

    @Button(
            name = "Add Rewarp", category = GENERAL, subcategory = "Rewarp",
            description = "Adds a rewarp position",
            text = "Add Rewarp"
    )
    Runnable _addRewarp = FarmHelperConfig::addRewarp;
    @Info(
            text = "Don't forget to add rewarp points!",
            type = InfoType.WARNING,
            category = GENERAL,
            subcategory = "Rewarp"
    )
    public static boolean rewarpWarning;
    @Button(
            name = "Remove Rewarp", category = GENERAL, subcategory = "Rewarp",
            description = "Removes a rewarp position",
            text = "Remove Rewarp"
    )
    Runnable _removeRewarp = FarmHelperConfig::removeRewarp;
    @Button(
            name = "Remove All Rewarps", category = GENERAL, subcategory = "Rewarp",
            description = "Removes all rewarp positions",
            text = "Remove All Rewarps"
    )
    Runnable _removeAllRewarps = FarmHelperConfig::removeAllRewarps;

    // END GENERAL

    // START MISCELLANEOUS

    @KeyBind(
            name = "Toggle Farm Helper", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Toggles the macro on/off", size = 2
    )
    public static OneKeyBind toggleMacro = new OneKeyBind(Keyboard.KEY_GRAVE);
    @KeyBind(
            name = "Open GUI", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Opens Farm Helper configuration menu", size = 2
    )

    public static OneKeyBind openGuiKeybind = new OneKeyBind(Keyboard.KEY_F);

    @Info(
            text = "Freelock doesn't work properly with Oringo!", type = InfoType.WARNING,
            category = MISCELLANEOUS, subcategory = "Keybinds"
    )
    private int freelockWarning;

    @KeyBind(
            name = "Freelock", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Locks rotation, lets you freely look", size = 2
    )
    public static OneKeyBind freelockKeybind = new OneKeyBind(Keyboard.KEY_L);

    @KeyBind(
            name = "Plot Cleaning Helper", category = MISCELLANEOUS, subcategory = "Plot Cleaning Helper",
            description = "Toggles the plot cleaning helper on/off", size = 2
    )
    public static OneKeyBind plotCleaningHelperKeybind = new OneKeyBind(Keyboard.KEY_P);

    @Switch(
            name = "Automatically choose a tool to destroy the block", category = MISCELLANEOUS, subcategory = "Plot Cleaning Helper",
            description = "Automatically chooses the best tool to destroy the block"
    )
    public static boolean autoChooseTool = false;

    @DualOption(
            name = "AutoUpdater Version Type", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "The version type to use",
            left = "Release",
            right = "Pre-release",
            size = 2
    )
    public static boolean autoUpdaterDownloadBetaVersions = false;

    @Switch(
            name = "Performance Mode", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Set render distance to 2, set max fps to 15 and doesn't render crops"
    )
    public static boolean performanceMode = false;

    @Switch(
            name = "Mute The Game", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Mutes the game while farming"
    )
    public static boolean muteTheGame = false;

    @Switch(
            name = "Auto Cookie", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Automatically purchases and consumes a booster cookie"
    )
    public static boolean autoCookie = false;

    @Switch(
            name = "Hold left click when changing row", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Hold left click when change row"
    )
    public static boolean holdLeftClickWhenChangingRow = true;

    @Switch(
            name = "Auto Ungrab Mouse", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Automatically ungrabs your mouse, so you can safely alt-tab"
    )
    public static boolean autoUngrabMouse = true;

    @Info(
            text = "Priority getting God Pot is: Backpack -> Bits -> AH",
            type = InfoType.INFO, size = 2, category = MISCELLANEOUS, subcategory = "God Pot"
    )
    private static int godPotInfo;

    @Switch(
            name = "Auto God Pot", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "Automatically purchases and consumes a God Pot", size = 2
    )
    public static boolean autoGodPot = false;

    @Switch(
            name = "Get God Pot from Backpack", category = MISCELLANEOUS, subcategory = "God Pot", size = 2
    )
    public static boolean autoGodPotFromBackpack = true;

    @DualOption(
            name = "Storage Type", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "The storage type to get god pots from",
            left = "Backpack",
            right = "Ender Chest"
    )
    public static boolean autoGodPotStorageType = true;

    @Number(
            name = "Backpack Number", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "The backpack number, that contains god pots",
            min = 1, max = 18
    )
    public static int autoGodPotBackpackNumber = 1;

    @Switch(
            name = "Buy God Pot using Bits", category = MISCELLANEOUS, subcategory = "God Pot"
    )
    public static boolean autoGodPotFromBits = false;

    @Switch(
            name = "Get God Pot from Auction House", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "If the user doesn't have a cookie, it will go to the hub and buy from AH"
    )
    public static boolean autoGodPotFromAH = false;

    @Info(
            text = "Click ESC during Auto Sell, to stop it and pause for the next 15 minutes",
            category = MISCELLANEOUS, subcategory = "Auto Sell", type = InfoType.INFO, size = 2
    )
    public static boolean autoSellInfo;

    @Switch(
            name = "Enable Auto Sell", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Enables auto sell"
    )
    public static boolean enableAutoSell = false;

    @DualOption(
            name = "Market type", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The market type to sell crops to",
            left = "BZ",
            right = "NPC"
    )
    public static boolean autoSellMarketType = false;

    @Switch(
            name = "Sell Items In Sacks", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Sells items in your sacks and inventory"
    )
    public static boolean autoSellSacks = false;


    @DualOption(
            name = "Sacks placement",
            category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The sacks placement",
            left = "Inventory",
            right = "Sack of sacks"
    )
    public static boolean autoSellSacksPlacement = true;


    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Inventory Full Time", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The time to wait to test if inventory fullness ratio is still the same (or higher)",
            min = 1, max = 20
    )
    public static int inventoryFullTime = 6;

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Inventory Full Ratio", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "After reaching this ratio, the macro will start counting from 0 to Inventory Full Time. If the fullness ratio is still the same (or higher) after the time has passed, it will start selling items.",
            min = 1, max = 100
    )
    public static int inventoryFullRatio = 65;
    @Page(
            name = "Customize items sold to NPC", category = MISCELLANEOUS, subcategory = "Auto Sell", location = PageLocation.BOTTOM,
            description = "Click here to customize items that are sold to NPC automatically"
    )
    public AutoSellNPCItemsPage autoSellNPCItemsPage = new AutoSellNPCItemsPage();

    @Button(
            name = "Sell Inventory Now", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Sells crops in your inventory",
            text = "Sell Inventory Now"
    )
    Runnable autoSellFunction = () -> {
        mc.thePlayer.closeScreen();
        AutoSell.getInstance().enable(true);
    };


    @Switch(
            name = "Swap pet during Jacob's contest", category = MISCELLANEOUS, subcategory = "Pet Swapper",
            description = "Swaps pet to the selected pet during Jacob's contest. Selects the first one from the pet list."
    )
    public static boolean enablePetSwapper = false;

    @Slider(
            name = "Pet Swap Delay", category = MISCELLANEOUS, subcategory = "Pet Swapper",
            description = "The delay between clicking GUI during swapping the pet (in milliseconds)",
            min = 200, max = 3000
    )
    public static int petSwapperDelay = 1000;

    @Text(
            name = "Pet Name", placeholder = "Type your pet name here",
            category = MISCELLANEOUS, subcategory = "Pet Swapper"
    )
    public static String petSwapperName = null;


    @Switch(
            name = "Increase Cocoa Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm cocoa beans more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedCocoaBeans = true;

    @Switch(
            name = "Increase Crop Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm crops more efficient by making the hitboxes bigger"
    )
    public static boolean increasedCrops = true;

    @Switch(
            name = "Increase Nether Wart Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm nether warts more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedNetherWarts = true;


    @Switch(
            name = "Increase Mushroom Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm mushrooms more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedMushrooms = true;

    @Switch(
            name = "Pingless Cactus", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm cactus more efficiently at higher speeds by making the cactus pingless"
    )
    public static boolean pinglessCactus = true;


    @Switch(
            name = "Send analytic data", category = MISCELLANEOUS, subcategory = "Analytics",
            description = "Sends analytic data to the server to improve the macro and learn how to detect staff checks"
    )
    public static boolean sendAnalyticData = true;

    // END MISCELLANEOUS

    // START FAILSAFE


    @Switch(
            name = "Pop-up Notification", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Enable pop-up notification"
    )
    public static boolean popUpNotification = true;

    @Switch(
            name = "Fake Movements", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Tries to act like a real player by moving around"
    )
    public static boolean fakeMovements = true;

    @Switch(
            name = "Auto alt-tab when failsafe triggered", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically alt-tabs to the game when the dark times come"
    )
    public static boolean autoAltTab = true;

    @Switch(
            name = "Try to use jumping and flying in failsafes reactions", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Tries to use jumping and flying in failsafes reactions"
    )
    public static boolean tryToUseJumpingAndFlying = true;

    @Switch(
            name = "Check Desync", category = FAILSAFE, subcategory = "Desync",
            description = "If client desynchronization is detected, it activates a failsafe. Turn this off if the network is weak or if it happens frequently."
    )
    public static boolean checkDesync = true;


    @Slider(
            name = "Pause for X milliseconds after desync triggered", category = FAILSAFE, subcategory = "Desync",
            description = "The delay to pause after desync triggered (in milliseconds)",
            min = 3_000, max = 10_000
    )
    public static int desyncPauseDelay = 5_000;

    @Slider(
            name = "Failsafe Stop Delay", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The delay to stop the macro after failsafe has been triggered (in milliseconds)",
            min = 1_000, max = 7_500
    )
    public static int failsafeStopDelay = 2_000;

    @Switch(
            name = "Auto TP back on World Change", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically warps back to the garden on server reboot, server update, etc"
    )
    public static boolean autoTPOnWorldChange = true;

    @Switch(
            name = "Auto Evacuate on World update", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically evacuates the island on server reboot, server update, etc"
    )
    public static boolean autoEvacuateOnWorldUpdate = true;

    @Switch(
            name = "Auto reconnect on disconnect", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically reconnects to the server when disconnected"
    )
    public static boolean autoReconnect = true;

    @Slider(
            name = "Rotation Check Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The sensitivity of the rotation check; the lower the sensitivity, the more accurate the check is, but it will also increase the chance of getting false positives.",
            min = 1, max = 10
    )
    public static float rotationCheckSensitivity = 2;

    @Slider(
            name = "Teleport Check Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The minimum distance between the previous and teleported position to trigger failsafe",
            min = 0.5f, max = 20f
    )
    public static float teleportCheckSensitivity = 4;

    @Button(
            name = "Test failsafe", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Tests failsafe",
            text = "Test failsafe", size = 2
    )
    Runnable _testFailsafe = () -> {
        LogUtils.sendDebug("Testing failsafe...");
        Failsafe.getInstance().addEmergency(Failsafe.EmergencyType.TEST);
    };

    @Switch(
            name = "Enable Failsafe Trigger Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound", size = OptionSize.DUAL,
            description = "Makes a sound when a failsafe has been triggered"
    )
    public static boolean enableFailsafeSound = true;

    @DualOption(
            name = "Failsafe Sound Type", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The failsafe sound type to play when a failsafe has been triggered",
            left = "Minecraft",
            right = "Custom"
    )
    public static boolean failsafeSoundType = false;
    @Button(
            name = "", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Plays the selected sound",
            text = "Play"
    )
    Runnable _playFailsafeSoundButton = () -> AudioManager.getInstance().playSound();
    @Button(
            name = "", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Stops playing the selected sound",
            text = "Stop"
    )
    Runnable _stopFailsafeSoundButton = () -> AudioManager.getInstance().resetSound();

    @Dropdown(
            name = "Minecraft Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The Minecraft sound to play when a failsafe has been triggered",
            options = {
                    "Ping", // 0
                    "Anvil" // 1
            }
    )
    public static int failsafeMcSoundSelected = 1;

    @Dropdown(
            name = "Custom Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The custom sound to play when a failsafe has been triggered",
            options = {
                    "Custom", // 0
                    "Voice", // 1
                    "Metal Pipe", // 2
                    "AAAAAAAAAA", // 3
                    "Loud Buzz", // 4
            }
    )
    public static int failsafeSoundSelected = 1;

    @Number(
            name = "Number of times to play custom sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The number of times to play custom sound when a failsafe has been triggered",
            min = 1, max = 10
    )
    public static int failsafeSoundTimes = 13;

    @Info(
            text = "If you want to use your own WAV file, rename it to 'farmhelper_sound.wav' and put it in your Minecraft directory.",
            type = InfoType.WARNING,
            category = FAILSAFE,
            subcategory = "Failsafe Trigger Sound",
            size = 2
    )
    public static boolean customFailsafeSoundWarning;

    @Slider(
            name = "Failsafe Sound Volume (in %)", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The volume of the failsafe sound",
            min = 0, max = 100
    )
    public static float failsafeSoundVolume = 50.0f;

    @Switch(
            name = "Max out Master category sounds while pinging", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Maxes out the sounds while failsafe"
    )
    public static boolean maxOutMinecraftSounds = false;

    @Page(
            name = "Failsafe Notifications", category = FAILSAFE, subcategory = "Failsafe Notifications", location = PageLocation.BOTTOM,
            description = "Click here to customize failsafe notifications"
    )
    public FailsafeNotificationsPage failsafeNotificationsPage = new FailsafeNotificationsPage();


    @Switch(
            name = "Enable Restart After FailSafe", category = FAILSAFE, subcategory = "Restart After FailSafe",
            description = "Restarts the macro after a while when a failsafe has been triggered"
    )
    public static boolean enableRestartAfterFailSafe = true;
//
//    @Switch(
//            name = "Leave after failsafe triggered", category = FAILSAFE, subcategory = "Restart After FailSafe",
//            description = "Leaves the server after a failsafe has been triggered"
//    )
//    public static boolean leaveAfterFailSafe = false;

    @Slider(
            name = "Restart Delay", category = FAILSAFE, subcategory = "Restart After FailSafe",
            description = "The delay to restart after failsafe (in minutes)",
            min = 1, max = 60
    )
    public static int restartAfterFailSafeDelay = 5;

    @Switch(
            name = "Enable Banwave Checker", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "Checks for banwave and shows you the number of players banned in the last 15 minutes",
            size = 2
    )
    public static boolean banwaveCheckerEnabled = true;

    @Switch(
            name = "Leave/pause during banwave", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "Automatically disconnects from the server or pauses the macro when a banwave is detected"
    )
    public static boolean enableLeavePauseOnBanwave = false;

    @DualOption(
            name = "Banwave Action", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "The action taken when banwave detected",
            left = "Leave",
            right = "Pause"
    )
    public static boolean banwaveAction = false;

    @Dropdown(
            name = "Base Threshold on", category = FAILSAFE, subcategory = "Banwave Checker",
            options = {"Global bans", "FarmHelper bans", "Both"}, size = 2
    )
    public static int banwaveThresholdType = 0;

    @Slider(
            name = "Banwave Disconnect Threshold", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "The threshold to disconnect from the server on banwave",
            min = 1, max = 100
    )
    public static int banwaveThreshold = 50;

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Delay Before Reconnecting", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "The delay before reconnecting after leaving on banwave (in seconds)",
            min = 1, max = 20, size = 2
    )
    public static int delayBeforeReconnecting = 5;

    @Switch(
            name = "Don't leave during Jacob's Contest", category = FAILSAFE, subcategory = "Banwave Checker",
            description = "Prevents the macro from leaving during Jacob's Contest even when banwave detected"
    )
    public static boolean banwaveDontLeaveDuringJacobsContest = true;

    @Switch(
            name = "Enable AntiStuck", category = FAILSAFE, subcategory = "AntiStuck",
            description = "Prevents the macro from getting stuck in the same position"
    )
    public static boolean enableAntiStuck = true;

    @Switch(
            name = "Send Chat Message During Failsafe", category = FAILSAFE, subcategory = "Failsafe Messages",
            description = "Sends a chat message when a failsafe has been triggered"
    )
    public static boolean sendFailsafeMessage = true;
    @Page(
            name = "Custom Failsafe Messages", category = FAILSAFE, subcategory = "Failsafe Messages", location = PageLocation.BOTTOM,
            description = "Click here to edit custom failsafe messages"
    )
    public static CustomFailsafeMessagesPage customFailsafeMessagesPage = new CustomFailsafeMessagesPage();

    // END FAILSAFE

    // START SCHEDULER


    @Switch(
            name = "Enable Scheduler", category = SCHEDULER, subcategory = "Scheduler", size = OptionSize.DUAL,
            description = "Farms for X amount of minutes then takes a break for X amount of minutes"
    )
    public static boolean enableScheduler = false;

    @Slider(
            name = "Farming time (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How long to farm",
            min = 1, max = 300, step = 1
    )
    public static int schedulerFarmingTime = 30;

    @Slider(
            name = "Farming time randomness (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How much randomness to add to the farming time",
            min = 0, max = 15, step = 1
    )
    public static int schedulerFarmingTimeRandomness = 0;

    @Slider(
            name = "Break time (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How long to take a break",
            min = 1, max = 120, step = 1
    )
    public static int schedulerBreakTime = 5;

    @Slider(
            name = "Break time randomness (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How much randomness to add to the break time",
            min = 0, max = 15, step = 1
    )
    public static int schedulerBreakTimeRandomness = 0;

    @Switch(
            name = "Pause the scheduler during Jacob's Contest", category = SCHEDULER, subcategory = "Scheduler",
            description = "Pauses and delays the scheduler during Jacob's Contest"
    )
    public static boolean pauseSchedulerDuringJacobsContest = true;

    @Switch(
            name = "Open inventory on scheduler breaks", category = SCHEDULER, subcategory = "Scheduler",
            description = "Opens inventory on scheduler breaks"
    )
    public static boolean openInventoryOnSchedulerBreaks = true;

    @Switch(
            name = "Enable leave timer", category = SCHEDULER, subcategory = "Leave Timer",
            description = "Leaves the server after the timer has ended"
    )
    public static boolean leaveTimer = false;

    @Slider(
            name = "Leave time", category = SCHEDULER, subcategory = "Leave Timer",
            description = "The time to leave the server (in minutes)",
//            min = 15, max = 720, step = 15
            min = 1, max = 20, step = 1
    )
    public static int leaveTime = 60;

    // END SCHEDULER

    // START JACOB


    @Switch(
            name = "Enable Jacob Failsafes", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "Stops farming once a crop threshold has been met"
    )
    public static boolean enableJacobFailsafes = false;

    @DualOption(
            name = "Jacob Failsafe Action", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The action to take when a failsafe has been triggered",
            left = "Leave",
            right = "Pause"
    )
    public static boolean jacobFailsafeAction = true;

    @Slider(
            name = "Nether Wart Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The nether wart cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobNetherWartCap = 800000;

    @Slider(
            name = "Potato Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The potato cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobPotatoCap = 830000;

    @Slider(
            name = "Carrot Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The carrot cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobCarrotCap = 860000;

    @Slider(
            name = "Wheat Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The wheat cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobWheatCap = 265000;

    @Slider(
            name = "Sugar Cane Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The sugar cane cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobSugarCaneCap = 575000;

    @Slider(
            name = "Mushroom Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The mushroom cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobMushroomCap = 250000;

    @Slider(
            name = "Melon Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The melon cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobMelonCap = 1234000;

    @Slider(
            name = "Pumpkin Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The pumpkin cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobPumpkinCap = 240000;

    @Slider(
            name = "Cocoa Beans Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The cocoa beans cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobCocoaBeansCap = 725000;

    @Slider(
            name = "Cactus Beans Cap", category = JACOBS_CONTEST, subcategory = "Jacob's Contest",
            description = "The cactus cap",
            min = 10000, max = 2000000, step = 10000
    )
    public static int jacobCactusCap = 470000;

    // END JACOB

    // START VISITORS_MACRO

    @Info(
            text = "Visitors Macro tends to move your mouse because of opening GUIs frequently. Be aware of that.",
            type = InfoType.WARNING,
            category = VISITORS_MACRO,
            subcategory = "Visitors Macro",
            size = 2
    )
    public static boolean visitorsMacroWarning;


    @Switch(
            name = "Enable visitors macro", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "Enables visitors macro"
    )
    public static boolean visitorsMacro = false;

    @Slider(
            name = "Minimum Visitors to start the macro", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "The minimum amount of visitors to start the macro",
            min = 1, max = 5
    )
    public static int visitorsMacroMinVisitors = 5;

    @Switch(
            name = "Autosell before serving visitors", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "Automatically sells crops before serving visitors"
    )
    public static boolean visitorsMacroAutosellBeforeServing = false;


    @Switch(
            name = "Pause the visitors macro during Jacob's contests", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "Pauses the visitors macro during Jacob's contests"
    )
    public static boolean pauseVisitorsMacroDuringJacobsContest = true;

    @Switch(
            name = "Only Accept Profitable Visitors", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "Only accepts visitors that are profitable"
    )
    public static boolean onlyAcceptProfitableVisitors = false;
    @Button(
            name = "Start the macro manually", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "Triggers the visitors macro",
            text = "Trigger now"
    )
    public static Runnable triggerVisitorsMacro = () -> {
        VisitorsMacro.getInstance().setManuallyStarted(true);
        VisitorsMacro.getInstance().start();
    };

    @Slider(
            name = "The minimum amount of coins to start the macro (in thousands)", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "The minimum amount of coins you need to have in your purse to start the visitors macro",
            min = 1_000, max = 20_000
    )
    public static int visitorsMacroMinMoney = 2_000;

    @Slider(
            name = "Price Manipulation Detection Multiplier", category = VISITORS_MACRO, subcategory = "Visitors Macro",
            description = "How much does Instant Buy price need to be higher than Instant Sell price to detect price manipulation",
            min = 1.25f, max = 4f
    )
    public static float visitorsMacroPriceManipulationMultiplier = 1.75f;
    @Info(
            text = "If you put your compactors in the hotbar, they will be temporarily disabled.",
            type = InfoType.WARNING,
            category = VISITORS_MACRO,
            subcategory = "Visitors Macro",
            size = 2
    )
    public static boolean infoCompactors;
    @Info(
            text = "Cookie buff is required!",
            type = InfoType.ERROR,
            category = VISITORS_MACRO,
            subcategory = "Visitors Macro"
    )
    public static boolean infoCookieBuffRequired;
    @Info(
            text = "Desk position must be set before using the visitors macro!",
            type = InfoType.ERROR,
            category = VISITORS_MACRO,
            subcategory = "Visitors Macro"
    )
    public static boolean infoDeskNotSet;


    @Switch(
            name = "Accept uncommon visitors", category = VISITORS_MACRO, subcategory = "Rarity",
            description = "Whether to accept visitors that are uncommon rarity"
    )
    public static boolean visitorsAcceptUncommon = true;

    @Switch(
            name = "Accept rare visitors", category = VISITORS_MACRO, subcategory = "Rarity",
            description = "Whether to accept visitors that are rare rarity"
    )
    public static boolean visitorsAcceptRare = true;

    @Switch(
            name = "Accept legendary visitors", category = VISITORS_MACRO, subcategory = "Rarity",
            description = "Whether to accept visitors that are legendary rarity"
    )
    public static boolean visitorsAcceptLegendary = true;

    @Switch(
            name = "Accept mythic visitors", category = VISITORS_MACRO, subcategory = "Rarity",
            description = "Whether to accept visitors that are mythic rarity"
    )
    public static boolean visitorsAcceptMythic = true;

    @Switch(
            name = "Accept special visitors", category = VISITORS_MACRO, subcategory = "Rarity",
            description = "Whether to accept visitors that are special rarity"
    )
    public static boolean visitorsAcceptSpecial = true;


    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Visitors Desk X", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
            description = "Visitors desk X coordinate",
            min = -30000000, max = 30000000
    )
    public static int visitorsDeskPosX = 0;
    @Button(
            name = "Set Visitor's Desk", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
            description = "Sets the visitor's desk position",
            text = "Set Visitor's Desk"
    )
    Runnable setVisitorDesk = () -> {
        BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        visitorsDeskPosX = pos.getX();
        visitorsDeskPosY = pos.getY();
        visitorsDeskPosZ = pos.getZ();
        save();
        LogUtils.sendSuccess("Visitors desk position has been set");
    };

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Visitors Desk Y", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
            description = "Visitors desk Y coordinate",
            min = -30000000, max = 30000000
    )
    public static int visitorsDeskPosY = 0;
    @Button(
            name = "Reset Visitor's Desk", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
            description = "Resets the visitor's desk position",
            text = "Reset Visitor's Desk"
    )
    Runnable resetVisitorDesk = () -> {
        visitorsDeskPosX = 0;
        visitorsDeskPosY = 0;
        visitorsDeskPosZ = 0;
        save();
        LogUtils.sendSuccess("Visitors desk position has been reset");
    };

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Visitors Desk Z", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
            description = "Visitors desk Z coordinate",
            min = -30000000, max = 30000000
    )
    public static int visitorsDeskPosZ = 0;


    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "SpawnPos X", category = VISITORS_MACRO, subcategory = "Spawn Position",
            description = "The X coordinate of the spawn",
            min = -30000000, max = 30000000

    )
    public static int spawnPosX = 0;
    @Button(
            name = "Set SpawnPos", category = VISITORS_MACRO, subcategory = "Spawn Position",
            description = "Sets the spawn position to your current position",
            text = "Set SpawnPos"
    )
    Runnable _setSpawnPos = PlayerUtils::setSpawnLocation;

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "SpawnPos Y", category = VISITORS_MACRO, subcategory = "Spawn Position",
            description = "The Y coordinate of the spawn",
            min = -30000000, max = 30000000
    )
    public static int spawnPosY = 0;
    @Button(
            name = "Reset SpawnPos", category = VISITORS_MACRO, subcategory = "Spawn Position",
            description = "Resets the spawn position",
            text = "Reset SpawnPos"
    )
    Runnable _resetSpawnPos = () -> {
        spawnPosX = 0;
        spawnPosY = 0;
        spawnPosZ = 0;
        save();
        LogUtils.sendSuccess("Spawn position has been reset!");
    };

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "SpawnPos Z", category = VISITORS_MACRO, subcategory = "Spawn Position",
            description = "The Z coordinate of the spawn",
            min = -30000000, max = 30000000
    )
    public static int spawnPosZ = 0;


    @Switch(
            name = "Draw visitors desk location", category = VISITORS_MACRO, subcategory = "Drawings",
            description = "Draws the visitors desk location"
    )
    public static boolean drawVisitorsDeskLocation = true;


    @Switch(
            name = "Draw spawn location", category = VISITORS_MACRO, subcategory = "Drawings",
            description = "Draws the spawn location"
    )
    public static boolean drawSpawnLocation = true;

    // END VISITORS_MACRO

    // START PESTS_DESTROYER

    @Switch(
            name = "Enable Pests Destroyer", category = PESTS_DESTROYER, subcategory = "Pests Destroyer",
            description = "Destroys pests"
    )
    public static boolean enablePestsDestroyer = false;

    @Slider(
            name = "Start killing pests at X pests", category = PESTS_DESTROYER, subcategory = "Pests Destroyer",
            description = "The amount of pests to start killing pests",
            min = 1, max = 8
    )
    public static int startKillingPestsAt = 3;

    @Switch(
            name = "Pests ESP", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "Draws a box around pests"
    )
    public static boolean pestsESP = true;
    @Color(
            name = "ESP Color", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "The color of the pests ESP"
    )
    public static OneColor pestsESPColor = new OneColor(0, 255, 217, 171);

    @Switch(
            name = "Tracers to Pests", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "Draws a line to pests"
    )
    public static boolean pestsTracers = true;

    @Color(
            name = "Tracers Color", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "The color of the pests tracers"
    )
    public static OneColor pestsTracersColor = new OneColor(0, 255, 217, 171);

    @Switch(
            name = "Highlight borders of Plot with pests", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "Highlights the borders of the plot with pests"
    )
    public static boolean highlightPlotWithPests = true;

    @Color(
            name = "Plot Highlight Color", category = PESTS_DESTROYER, subcategory = "Drawings",
            description = "The color of the plot highlight"
    )
    public static OneColor plotHighlightColor = new OneColor(0, 255, 217, 40);

    @Switch(
            name = "Send Webhook log if pests detection number has been exceeded", category = PESTS_DESTROYER, subcategory = "Logs",
            description = "Sends a webhook log if pests detection number has been exceeded"
    )
    public static boolean sendWebhookLogIfPestsDetectionNumberExceeded = true;

    @Switch(
            name = "Ping @everyone", category = PESTS_DESTROYER, subcategory = "Logs",
            description = "Pings @everyone on pests detection number exceeded"
    )
    public static boolean pingEveryoneOnPestsDetectionNumberExceeded = false;

    @Switch(
            name = "Send notification if pests detection number has been exceeded", category = PESTS_DESTROYER, subcategory = "Logs",
            description = "Sends a notification if pests detection number has been exceeded"
    )
    public static boolean sendNotificationIfPestsDetectionNumberExceeded = true;

    // END PESTS_DESTROYER

    // START DISCORD_INTEGRATION


    @Switch(
            name = "Enable Webhook Messages", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Allows to send messages via Discord webhooks"
    )
    public static boolean enableWebHook = false;

    @Switch(
            name = "Send Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends all messages about the macro, staff checks, etc"
    )
    public static boolean sendLogs = false;

    @Switch(
            name = "Send Status Updates", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends messages about the macro, such as profits, harvesting crops, etc"
    )
    public static boolean sendStatusUpdates = false;

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Status Update Interval (in minutes)", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "The interval between sending messages about status updates",
            min = 1, max = 60
    )
    public static int statusUpdateInterval = 5;

    @Switch(
            name = "Send Visitors Macro Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends messages about the visitors macro, such as which visitor got rejected or accepted and with what items"
    )
    public static boolean sendVisitorsMacroLogs = true;

    @Switch(
            name = "Ping everyone on Visitors Macro Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Pings everyone on Visitors Macro Logs"
    )
    public static boolean pingEveryoneOnVisitorsMacroLogs = false;

    @Text(
            name = "WebHook URL", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "The URL to use for the webhook",
            placeholder = "https://discord.com/api/webhooks/...",
            secure = true
    )
    public static String webHookURL = "";

    @Switch(
            name = "Enable Remote Control", category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "Enables remote control via Discord messages"
    )
    public static boolean enableRemoteControl = false;

    @Text(
            name = "Discord Remote Control Bot Token",
            category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The bot token to use for remote control",
            secure = true
    )
    public static String discordRemoteControlToken;

    @Text(
            name = "Discord Remote Control Address",
            category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The address to use for remote control. If you are unsure what to put there, leave \"localhost\".",
            placeholder = "localhost"
    )
    public static String discordRemoteControlAddress = "localhost";

    @cc.polyfrost.oneconfig.config.annotations.Number(
            name = "Remote Control Port", category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The port to use for remote control. Change this if you have port conflicts.",
            min = 1, max = 65535
    )
    public static int remoteControlPort = 21370;
    @Info(
            text = "If you want to use the remote control feature, you need to put Farm Helper JDA Dependency inside your mods folder.",
            type = InfoType.ERROR,
            category = DISCORD_INTEGRATION,
            subcategory = "Remote Control",
            size = 2
    )
    public static boolean infoRemoteControl;

    // END DISCORD_INTEGRATION

    // START DELAYS

    @Slider(
            name = "Time between changing rows", category = DELAYS, subcategory = "Changing rows",
            description = "The minimum time to wait before changing rows (in milliseconds)",
            min = 150, max = 2000
    )
    public static float timeBetweenChangingRows = 400f;

    @Slider(
            name = "Additional random time between changing rows", category = DELAYS, subcategory = "Changing rows",
            description = "The maximum time to wait before changing rows (in milliseconds)",
            min = 0, max = 2000
    )
    public static float randomTimeBetweenChangingRows = 200f;

    public static long getRandomTimeBetweenChangingRows() {
        return (long) (timeBetweenChangingRows + (float) Math.random() * randomTimeBetweenChangingRows);
    }

    public static long getMaxTimeBetweenChangingRows() {
        return (long) (timeBetweenChangingRows + randomTimeBetweenChangingRows);
    }

    @Slider(
            name = "Rotation Time", category = DELAYS, subcategory = "Rotations",
            description = "The time it takes to rotate the player",
            min = 200f, max = 2000f
    )
    public static float rotationTime = 500f;

    @Slider(
            name = "Additional random Rotation Time", category = DELAYS, subcategory = "Rotations",
            description = "The maximum random time added to the delay time it takes to rotate the player (in seconds)",
            min = 0f, max = 2000f
    )
    public static float rotationTimeRandomness = 300;

    public static long getRandomRotationTime() {
        return (long) (rotationTime + (float) Math.random() * rotationTimeRandomness);
    }

    @Slider(
            name = "Pests Destroyer Rotation Time", category = DELAYS, subcategory = "Pests Destroyer",
            description = "The time it takes to rotate the player",
            min = 50f, max = 750
    )
    public static float pestsKillerRotationTime = 200f;

    @Slider(
            name = "Additional random Pests Destroyer Rotation Time", category = DELAYS, subcategory = "Pests Destroyer",
            description = "The maximum random time added to the delay time it takes to rotate the player (in seconds)",
            min = 0f, max = 750
    )
    public static float pestsKillerRotationTimeRandomness = 150;

    public static long getRandomPestsKillerRotationTime() {
        return (long) (pestsKillerRotationTime + (float) Math.random() * pestsKillerRotationTimeRandomness);
    }

    @Slider(
            name = "GUI Delay", category = DELAYS, subcategory = "GUI Delays",
            description = "The delay between clicking during GUI macros (in miliseconds)",
            min = 250f, max = 2000f
    )
    public static float visitorsMacroGuiDelay = 400f;

    @Slider(
            name = "Additional random GUI Delay", category = DELAYS, subcategory = "GUI Delays",
            description = "The maximum random time added to the delay time between clicking during GUI macros (in miliseconds)",
            min = 0f, max = 2000f
    )
    public static float visitorsMacroGuiDelayRandomness = 350f;

    public static long getRandomGUIMacroDelay() {
        return (long) (visitorsMacroGuiDelay + (float) Math.random() * visitorsMacroGuiDelayRandomness);
    }

    @Slider(
            name = "Plot Cleaning Helper Rotation Time", category = DELAYS, subcategory = "Plot Cleaning Helper",
            description = "The time it takes to rotate the player",
            min = 20f, max = 500f
    )
    public static float plotCleaningHelperRotationTime = 50;

    @Slider(
            name = "Additional random Plot Cleaning Helper Rotation Time", category = DELAYS, subcategory = "Plot Cleaning Helper",
            description = "The maximum random time added to the delay time it takes to rotate the player (in seconds)",
            min = 0f, max = 500f
    )

    public static float plotCleaningHelperRotationTimeRandomness = 50;

    public static long getRandomPlotCleaningHelperRotationTime() {
        return (long) (plotCleaningHelperRotationTime + (float) Math.random() * plotCleaningHelperRotationTimeRandomness);
    }

    @Slider(
            name = "Rewarp Delay", category = DELAYS, subcategory = "Rewarp",
            description = "The delay between rewarping (in milliseconds)",
            min = 250f, max = 2000f
    )
    public static float rewarpDelay = 400f;

    @Slider(
            name = "Additional random Rewarp Delay", category = DELAYS, subcategory = "Rewarp",
            description = "The maximum random time added to the delay time between rewarping (in milliseconds)",
            min = 0f, max = 2000f
    )
    public static float rewarpDelayRandomness = 350f;

    public static long getRandomRewarpDelay() {
        return (long) (rewarpDelay + (float) Math.random() * rewarpDelayRandomness);
    }

    // END DELAYS

    // START HUD

    @HUD(
            name = "Status HUD", category = HUD
    )
    public static StatusHUD statusHUD = new StatusHUD();
    @HUD(
            name = "Profit Calculator HUD", category = HUD, subcategory = " "
    )
    public static ProfitCalculatorHUD profitHUD = new ProfitCalculatorHUD();

    // END HUD

    // START DEBUG

    @KeyBind(
            name = "Debug Keybind", category = DEBUG
    )
    public static OneKeyBind debugKeybind = new OneKeyBind(Keyboard.KEY_H);

    @Switch(
            name = "Debug Mode", category = DEBUG, subcategory = "Debug",
            description = "Prints to chat what the bot is currently executing. Useful if you are having issues."
    )
    public static boolean debugMode = false;

    @Switch(
            name = "Hide Logs (Not Recommended)", category = DEBUG, subcategory = "Debug",
            description = "Hides all logs from the console. Not recommended."
    )
    public static boolean hideLogs = false;
    @HUD(
            name = "Debug HUD", category = DEBUG, subcategory = " "
    )
    public static DebugHUD debugHUD = new DebugHUD();

    // END DEBUG

    // START EXPERIMENTAL


    @Switch(
            name = "Enable Fast Break (DANGEROUS)", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Fast Break is very risky and using it will most likely result in a ban. Proceed with caution."
    )
    public static boolean fastBreak = false;
    @Info(
            text = "Fast Break will most likely ban you. Use at your own risk.",
            type = InfoType.ERROR,
            category = EXPERIMENTAL,
            subcategory = "Fast Break"
    )
    public static boolean fastBreakWarning;

    @Slider(
            name = "Fast Break Speed", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Fast Break speed",
            min = 1, max = 3
    )
    public static int fastBreakSpeed = 1;

    @Switch(
            name = "Disable Fast Break during banwave", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Disables Fast Break during banwave"
    )
    public static boolean disableFastBreakDuringBanWave = true;

    @Switch(
            name = "Disable Fast Break during Jacob's contest", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Disables Fast Break during Jacob's contest"
    )
    public static boolean disableFastBreakDuringJacobsContest = true;

    // END EXPERIMENTAL


    @Number(name = "Config Version", category = EXPERIMENTAL, subcategory = "Experimental", min = 0, max = 1337)
    public static int configVersion = 1;

    @Switch(
            name = "Shown Welcome GUI", category = EXPERIMENTAL, subcategory = "Experimental"
    )
    public static boolean shownWelcomeGUI = false;

    public FarmHelperConfig() {
        super(new Mod("Farm Helper", ModType.HYPIXEL, "/farmhelper/icon-mod/icon.png"), "/farmhelper/config.json");
        initialize();

        this.addDependency("macroType", "Macro Type", () -> !MacroHandler.getInstance().isMacroToggled());

        this.addDependency("customPitchLevel", "customPitch");
        this.addDependency("customYawLevel", "customYaw");

        this.addDependency("inventoryFullTime", "enableAutoSell");
        this.addDependency("autoSellMarketType", "enableAutoSell");
        this.addDependency("autoSellSacks", "enableAutoSell");
        this.addDependency("autoSellSacksPlacement", "enableAutoSell");
        this.addDependency("autoSellFunction", "enableAutoSell");


        this.addDependency("petSwapperDelay", "enablePetSwapper");
        this.addDependency("petSwapperName", "enablePetSwapper");

        this.addDependency("autoUngrabMouse", "This feature doesn't work properly on Mac OS!", () -> !Minecraft.isRunningOnMac);

        this.addDependency("failsafeSoundType", "Play Button", () -> enableFailsafeSound && !AudioManager.getInstance().isSoundPlaying());
        this.addDependency("_playFailsafeSoundButton", "enableFailsafeSound");
        this.addDependency("_stopFailsafeSoundButton", "enableFailsafeSound");
        this.hideIf("_playFailsafeSoundButton" , () -> AudioManager.getInstance().isSoundPlaying());
        this.hideIf("_stopFailsafeSoundButton" , () -> !AudioManager.getInstance().isSoundPlaying());
        this.addDependency("failsafeMcSoundSelected", "Minecraft Sound", () -> !failsafeSoundType && enableFailsafeSound);
        this.addDependency("failsafeSoundSelected", "Custom Sound", () -> failsafeSoundType && enableFailsafeSound);
        this.addDependency("failsafeSoundVolume", "Custom Sound", () -> failsafeSoundType && enableFailsafeSound);
        this.addDependency("maxOutMinecraftSounds", "Minecraft Sound", () -> !failsafeSoundType && enableFailsafeSound);
        this.hideIf("customFailsafeSoundWarning", () -> !failsafeSoundType || !enableFailsafeSound || failsafeSoundSelected != 0);
        this.addDependency("leaveAfterFailSafe", "enableRestartAfterFailSafe");
        this.addDependency("restartAfterFailSafeDelay", "enableRestartAfterFailSafe");
        this.addDependency("sendFailsafeMessage", "fakeMovements");
        this.addDependency("rewarpAt3FailesAntistuck", "enableAntiStuck");

        this.addDependency("schedulerFarmingTime", "enableScheduler");
        this.addDependency("schedulerFarmingTimeRandomness", "enableScheduler");
        this.addDependency("schedulerBreakTime", "enableScheduler");
        this.addDependency("schedulerBreakTimeRandomness", "enableScheduler");
        this.addDependency("pauseSchedulerDuringJacobsContest", "enableScheduler");

        this.addDependency("jacobNetherWartCap", "enableJacobFailsafes");
        this.addDependency("jacobPotatoCap", "enableJacobFailsafes");
        this.addDependency("jacobCarrotCap", "enableJacobFailsafes");
        this.addDependency("jacobWheatCap", "enableJacobFailsafes");
        this.addDependency("jacobSugarCaneCap", "enableJacobFailsafes");
        this.addDependency("jacobMushroomCap", "enableJacobFailsafes");
        this.addDependency("jacobMelonCap", "enableJacobFailsafes");
        this.addDependency("jacobPumpkinCap", "enableJacobFailsafes");
        this.addDependency("jacobCocoaBeansCap", "enableJacobFailsafes");
        this.addDependency("jacobCactusCap", "enableJacobFailsafes");
        this.addDependency("jacobFailsafeAction", "enableJacobFailsafes");

        this.addDependency("pauseVisitorsMacroDuringJacobsContest", "visitorsMacro");
        this.addDependency("onlyAcceptProfitableVisitors", "visitorsMacro");
        this.addDependency("triggerVisitorsMacro", "visitorsMacro");
        this.addDependency("visitorsMacroPriceManipulationMultiplier", "visitorsMacro");
        this.addDependency("visitorsAcceptUncommon", "visitorsMacro");
        this.addDependency("visitorsAcceptRare", "visitorsMacro");
        this.addDependency("visitorsAcceptLegendary", "visitorsMacro");
        this.addDependency("visitorsAcceptMythic", "visitorsMacro");
        this.addDependency("visitorsAcceptSpecial", "visitorsMacro");
        this.addDependency("visitorsMacroAction", "visitorsMacro");
        this.addDependency("visitorsMacroAutosellBeforeServing", "visitorsMacro");
        this.addDependency("visitorsMacroMinMoney", "visitorsMacro");

        this.addDependency("sendVisitorsMacroLogs", "visitorsMacro");
        this.addDependency("sendVisitorsMacroLogs", "enableWebHook");
        this.addDependency("pingEveryoneOnVisitorsMacroLogs", "visitorsMacro");
        this.addDependency("pingEveryoneOnVisitorsMacroLogs", "sendVisitorsMacroLogs");
        this.addDependency("pingEveryoneOnVisitorsMacroLogs", "enableWebHook");


        this.hideIf("infoCookieBuffRequired", () -> GameStateHandler.getInstance().inGarden() || GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE);
        this.hideIf("infoDeskNotSet", () -> GameStateHandler.getInstance().inGarden() || PlayerUtils.isDeskPosSet());

        this.addDependency("sendLogs", "enableWebHook");
        this.addDependency("sendStatusUpdates", "enableWebHook");
        this.addDependency("statusUpdateInterval", "enableWebHook");
        this.addDependency("webHookURL", "enableWebHook");
        this.addDependency("enableRemoteControl", "Enable Remote Control", () -> Loader.isModLoaded("farmhelperjdadependency"));
        this.addDependency("discordRemoteControlAddress", "enableRemoteControl");
        this.addDependency("remoteControlPort", "enableRemoteControl");


        this.hideIf("infoRemoteControl", () -> Loader.isModLoaded("farmhelperjdadependency"));
        this.hideIf("failsafeSoundTimes", () -> true);

        this.addDependency("debugMode", "Debug Mode", () -> !hideLogs);
        this.addDependency("hideLogs", "Hide Logs (Not Recommended)", () -> !debugMode);

        this.addDependency("fastBreakSpeed", "fastBreak");
        this.addDependency("disableFastBreakDuringBanWave", "fastBreak");
        this.addDependency("disableFastBreakDuringJacobsContest", "fastBreak");

        this.addDependency("autoGodPotFromBackpack", "autoGodPot");
        this.addDependency("autoGodPotFromBits", "autoGodPot");
        this.addDependency("autoGodPotFromAH", "autoGodPot");

        this.hideIf("autoGodPotBackpackNumber", () -> !autoGodPotFromBackpack);
        this.hideIf("autoGodPotStorageType", () -> !autoGodPotFromBackpack);

        this.addDependency("banwaveAction", "enableLeavePauseOnBanwave");
        this.addDependency("banwaveThreshold", "enableLeavePauseOnBanwave");
        this.addDependency("banwaveThresholdType", "enableLeavePauseOnBanwave");
        this.addDependency("delayBeforeReconnecting", "enableLeavePauseOnBanwave");
        this.addDependency("banwaveDontLeaveDuringJacobsContest", "enableLeavePauseOnBanwave");

        this.addDependency("sendWebhookLogIfPestsDetectionNumberExceeded", "enableWebHook");
        this.addDependency("pingEveryoneOnPestsDetectionNumberExceeded", "sendWebhookLogIfPestsDetectionNumberExceeded");
        this.addDependency("pingEveryoneOnPestsDetectionNumberExceeded", "enableWebHook");

        this.addDependency("leaveTime", "leaveTimer");

        this.hideIf("shownWelcomeGUI", () -> true);

        this.hideIf("configVersion", () -> true);

        registerKeyBind(openGuiKeybind, this::openGui);
        registerKeyBind(toggleMacro, () -> MacroHandler.getInstance().toggleMacro());
//        registerKeyBind(debugKeybind, () -> {
//        });
        registerKeyBind(freelockKeybind, () -> Freelock.getInstance().toggle());
        registerKeyBind(plotCleaningHelperKeybind, () -> PlotCleaningHelper.getInstance().toggle());
        save();
    }

    public String getJson() {
        String json = gson.toJson(this);
        if (json == null || json.equals("{}")) {
            json = nonProfileSpecificGson.toJson(this);
        }
        return json;
    }
}
