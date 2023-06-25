package com.jelly.farmhelper.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.hud.ProfitCalculatorHUD;
import com.jelly.farmhelper.hud.StatusHUD;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import com.jelly.farmhelper.features.Autosell;

import com.jelly.farmhelper.config.structs.Rewarp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// THIS IS RAT - CatalizCS
public class Config extends cc.polyfrost.oneconfig.config.Config {
	private transient static final Minecraft mc = Minecraft.getMinecraft();
	private transient static final String GENERAL = "General";
	private transient static final String MISCELLANEOUS = "Miscellaneous";
	private transient static final String FAILSAFE = "Fail Safes";
	private transient static final String VISITORS_MACRO = "Visitors Macro";
	private transient static final String DELAYS = "Delays";
	private transient static final String WEBHOOK = "Webhook";
	private transient static final String DEBUG = "Debug";

	private transient static final File configRewarpFile = new File("farmhelper_rewarp.json");

	public static List<Rewarp> rewarpList = new ArrayList<>();

	public String proxyAddress = "";
	public String proxyUsername = "";
	public String proxyPassword = "";
	public int proxyType = 0;
	public boolean connectAtStartup = false;


	// START GENERAL
     public enum VerticalMacroEnum {
		NORMAL_TYPE,
		MUSHROOM,
		MUSHROOM_ROTATE
     }

     public enum SMacroEnum {
         NORMAL_TYPE,
		 PUMPKIN_MELON,
         SUGAR_CANE,
		 CACTUS,
		 COCOA_BEANS,
		 COCOA_BEANS_RG,
     }

	public enum CropEnum {
	 	CARROT,
		NETHER_WART,
		POTATO,
		WHEAT,
		SUGAR_CANE,
		MELON,
		PUMPKIN,
		CACTUS,
		COCOA_BEANS,
		MUSHROOM,
	}

	public static void addRewarp(Rewarp rewarp) {
		rewarpList.add(rewarp);
		LogUtils.scriptLog("Added rewarp: " + rewarp.toString());
		saveRewarpConfig();
	}

	public static void removeRewarp(Rewarp rewarp) {
		rewarpList.remove(rewarp);
		LogUtils.scriptLog("Removed the closest rewarp: " + rewarp.toString());
		saveRewarpConfig();
	}

	public static void removeAllRewarps() {
		rewarpList.clear();
		LogUtils.scriptLog("Removed all saved rewarp positions");
		saveRewarpConfig();
	}

	public static void saveRewarpConfig() {
		try {
			Files.write(configRewarpFile.toPath(), FarmHelper.gson.toJson(rewarpList).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@DualOption(
		name = "Macro Type", category = GENERAL, subcategory = "Macro",
		description = "The macro to use for farming",
		left = "Vertical",
		right = "S Shape",
		size = OptionSize.DUAL
	)
	public boolean macroType = false;
	@Dropdown(
		name = "Vertical Farm", category = GENERAL, subcategory = "Macro",
		description = "Vertical farm type",
		options = {
			"Wheat/Potato/Carrot/Nether Wart", // 0
			"Mushroom (45°)", // 1
			"Mushroom (30° with rotations)", // 2
		}
	)
    public int VerticalMacroType = 0;
	@Dropdown(
		name = "S Shape Farm", category = GENERAL, subcategory = "Macro",
		description = "S Shape farm type",
		options = {
			"Wheat/Potato/Carrot/Nether Wart", // 0
			"Pumpkin/Melon", // 1
			"Sugar Cane", // 2
			"Cactus", // 3
			"Cocoa Beans", // 4
			"Cocoa Beans (RoseGold version)" // 5
		}
	)
    public int SShapeMacroType = 0;
	@Switch(
			name = "Auto Ungrab Mouse", category = GENERAL, subcategory = "Macro",
			description = "Automatically unfocuses your mouse, so you can safely alt-tab"
	)
	public boolean autoUngrabMouse = true;
	@Switch(
		name = "Ladder Design", category = GENERAL, subcategory = "Macro",
		description = "Select this if you're using ladder design"
	)
	public boolean ladderDesign = false;
	@Switch(
			name = "Rotate After Warped", category = GENERAL, subcategory = "Macro",
			description = "Rotates the player after re-warping"
	)
	public boolean rotateAfterWarped = false;
	@Switch(
		name = "Rotate After Drop", category = GENERAL, subcategory = "Macro",
		description = "Rotates after the player falls down"
	)
	public boolean rotateAfterDrop = false;

	@KeyBind(
			name = "Toggle Farm Helper", category = GENERAL, subcategory = "Keybinds",
			description = "Toggles the macro on/off"
	)
	public OneKeyBind toggleMacro = new OneKeyBind(Keyboard.KEY_GRAVE);

	@Button(
			name = "Add Rewarp", category = GENERAL, subcategory = "Rewarp",
			description = "Adds a rewarp position",
			text = "Add Rewarp"
	)
	Runnable _addRewarp = () -> {
		BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
		rewarpList.add(new Rewarp(pos.getX(), pos.getY(), pos.getZ()));
		save();
		LogUtils.scriptLog("Rewarp position has been added. BlockPos: " + pos);
	};
	@Info(
			text = "Don't forget to add rewarp points!",
			type = InfoType.WARNING,
			category = GENERAL,
			subcategory = "Rewarp"
	)
	public static boolean ignored;
	@Button(
			name = "Remove Rewarp", category = GENERAL, subcategory = "Rewarp",
			description = "Removes a rewarp position",
			text = "Remove Rewarp"
	)
	Runnable _removeRewarp = () -> {
		Rewarp closest = null;
		if (rewarpList.size() == 0) {
			LogUtils.scriptLog("No rewarp locations set");
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
			removeRewarp(closest);
		}
	};
	@Button(
			name = "Remove All Rewarps", category = GENERAL, subcategory = "Rewarp",
			description = "Removes all rewarp positions",
			text = "Remove All Rewarps"
	)
	Runnable _removeAllRewarps = () -> {
		removeAllRewarps();
		LogUtils.scriptLog("All rewarp positions has been removed");
	};

	// END GENERAL

	// START MISCELLANEOUS
	@Switch(
			name = "Xray Mode", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Hides blocks to reduce resource usage"
	)
	public boolean xrayMode = false;
	@Switch(
			name = "Mute The Game", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Mutes the game while farming"
	)
	public boolean muteTheGame = false;
	@Switch(
			name = "Auto GodPot", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Automatically purchases and consumes a God Pot"
	)
	public boolean autoGodPot = false;
	@Switch(
			name = "Auto Cookie", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Automatically purchases and consumes a booster cookie"
	)
	public boolean autoCookie = false;
	@Switch(
			name = "Fast Change Direction Cane", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Fast change direction cane"
	)
	public boolean fastChangeDirectionCane = false;
	@Switch(
			name = "Count RNG to $/Hr in Profit Calculator", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Count RNG to $/Hr"
	)
	public boolean countRNGToProfitCalc = false;
	@Switch(
			name = "Fast Break (DANGEROUS)", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Fast break is very risky and most likely will result in a ban"
	)
	public boolean fastBreak = false;
	@Switch(
			name = "Debug Mode", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Prints to chat what the bot is currently executing. Useful if you are having issues."
	)
	public boolean debugMode = false;
	@Slider(
			name = "Fast Break Speed", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Fast break speed",
			min = 1, max = 3
	)
	public int fastBreakSpeed = 1;
	@Switch(
			name = "Highlight rewarp points", category = MISCELLANEOUS, subcategory = "Miscellaneous",
			description = "Highlights all rewarp points you have added"
	)
	public boolean highlightRewarp = true;

	@Switch(
			name = "Enable Auto Sell", category = MISCELLANEOUS, subcategory = "Auto Sell",
			description = "Enables auto sell"
	)
	public boolean enableAutoSell = false;
	@Switch(
			name = "Sell To NPC" , category = MISCELLANEOUS, subcategory = "Auto Sell",
			description = "Automatically sells crops to NPC or Bazaar"
	)
	public boolean sellToNPC = false;
	@Number(
			name = "Inventory Full Time", category = MISCELLANEOUS, subcategory = "Auto Sell",
			description = "The time to wait for inventory to be full (in seconds)",
			min = 1, max = 20
	)
	public int inventoryFullTime = 6;
	@Number(
			name = "Inventory Full Ratio", category = MISCELLANEOUS, subcategory = "Auto Sell",
			description = "The ratio to wait for inventory to be full (in percentage)",
			min = 1, max = 100
	)
	public int inventoryFullRatio = 65;
	@Button(
			name = "Sell Inventory Now", category = MISCELLANEOUS, subcategory = "Auto Sell",
			description = "Sells crops in your inventory",
			text = "Sell Inventory Now"
	)
	Runnable autoSellFunction = () -> {
		mc.thePlayer.closeScreen();
		Autosell.enable();
	};

	@Switch(
			name = "Increase Cocoa Hitboxes", category = MISCELLANEOUS, subcategory = "Bigger Hitboxes",
			description = "Allows you to farm cocoa beans more efficient on higher speeds by making the hitboxes bigger"
	)
	public boolean increasedCocoaBeans = false;
	@Switch(
			name = "Increase Crop Hitboxes", category = MISCELLANEOUS, subcategory = "Bigger Hitboxes",
			description = "Allows you to farm crops more efficient by making the hitboxes bigger"
	)
	public boolean increasedCrops = false;
	@Switch(
			name = "Increase Nether Wart Hitboxes", category = MISCELLANEOUS, subcategory = "Bigger Hitboxes",
			description = "Allows you to farm nether warts more efficient on higher speeds by making the hitboxes bigger"
	)
	public boolean increasedNetherWarts = false;

	// END MISCELLANEOUS

	// START DELAYS

	@Slider(
			name = "Stop Script Delay Time", category = DELAYS, subcategory = "Delays",
			description = "The time to wait before stopping the script (in milliseconds)",
			min = 1, max = 10000
	)
	public int delayedStopScriptTime = 1000;
	@Slider(
			name = "Stop Script Delay Random Time", category = DELAYS, subcategory = "Delays",
			description = "The maximum random time added to the delay time before stopping the script (in milliseconds)",
			min = 1, max = 2000
	)
	public int delayedStopScriptTimeRandomness = 1000;
	@Slider(
			name = "Rotation Time", category = DELAYS, subcategory = "Delays",
			description = "The time it takes to rotate the player (in milliseconds)",
			min = 1, max = 2000
	)
	public int rotationTime = 500;
	@Slider(
			name = "Rotation Random Time", category = DELAYS, subcategory = "Delays",
			description = "The maximum random time added to the delay time it takes to rotate the player (in milliseconds)",
			min = 1, max = 2000
	)
	public int rotationTimeRandomness = 200;

	// END DELAYS

	// START VISITORS_MACRO

	@Switch(
		name = "Enable", category = VISITORS_MACRO, subcategory = "Visitors Macro",
		description = "Enables visitors macro"
	)
	public boolean visitorsMacro = false;
	@Switch(
		name = "Only Accept Profitable Visitors", category = VISITORS_MACRO, subcategory = "Visitors Macro",
		description = "Only accepts visitors that are profitable"
	)
	public boolean onlyAcceptProfitableVisitors = false;
	@Number(
		name = "Visitors Macro Coins Threshold", category = VISITORS_MACRO, subcategory = "Visitors Macro",
		description = "The maximum amount of coins to be considered profitable",
		min = 1, max = 20
	)
	public int visitorsMacroCoinsThreshold = 1;


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
		LogUtils.scriptLog("Visitors desk position has been set. BlockPos: " + pos);
	};

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
		LogUtils.scriptLog("Visitors desk position has been reset");
	};
	@Number(
			name = "Visitors Desk X", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
			description = "Visitors desk X coordinate",
			min = -30000000, max = 30000000
	)
	public int visitorsDeskPosX = 0;
	@Number(
			name = "Visitors Desk Y", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
			description = "Visitors desk Y coordinate",
			min = -30000000, max = 30000000
	)
	public int visitorsDeskPosY = 0;
	@Number(
			name = "Visitors Desk Z", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
			description = "Visitors desk Z coordinate",
			min = -30000000, max = 30000000
	)
	public int visitorsDeskPosZ = 0;
	@KeyBind(
			name = "Visitors Desk Keybind", category = VISITORS_MACRO, subcategory = "Visitor's Desk",
			description = "Visitors desk keybind"
	)
	public OneKeyBind visitorsDeskKeybind = new OneKeyBind(0);

	// END VISITORS_MACRO

	// START WEBHOOK

	@Switch(
        name = "Enable webhook messages", category = WEBHOOK, subcategory = "Discord Webhook",
        description = "Allows to send messages via Discord webhooks"
	)
	public boolean enableWebHook = false;
	@Switch(
		name = "Send Logs", category = WEBHOOK, subcategory = "Discord Webhook",
		description = "Sends all messages about the macro, spams a lot of messages"
	)
	public boolean sendLogs = false;
	@Switch(
		name = "Send Status Updates", category = WEBHOOK, subcategory = "Discord Webhook",
		description = "Sends messages about the macro, such as when it started, stopped, etc"
	)
	public boolean sendStatusUpdates = false;
	@Number(
		name = "Status Update Interval (in minutes)", category = WEBHOOK, subcategory = "Discord Webhook",
		description = "The interval between sending messages about status updates",
		min = 1, max = 60
	)
	public int statusUpdateInterval = 5;
	@Text(
		name = "WebHook URL", category = WEBHOOK, subcategory = "Discord Webhook",
		description = "The URL to use for the webhook",
		placeholder = "https://discord.com/api/webhooks/...",
		secure = true, multiline = false
	)
	public String webHookURL = "";

	@Switch(
			name = "Enable (BROKEN)", category = WEBHOOK, subcategory = "Remote Control",
			description = "Enables remote control via Discord messages"
	)
	public boolean enableRemoteControl = false;
	@Info(
			text = "You don't need to configure this. It's for advanced users only.",
			type = InfoType.INFO,
			category = WEBHOOK,
			subcategory = "Remote Control"
	)
	public static boolean ignored2;
	@Text(
			name = "WebSocket IP (DANGEROUS)", category = WEBHOOK, subcategory = "Remote Control",
			description = "The IP to use for the WebSocket server",
			secure = false, multiline = false

	)
	public String webSocketIP = "";
	@Text(
			name = "WebSocket Password", category = WEBHOOK, subcategory = "Remote Control",
			description = "The password to use for the WebSocket server",
			secure = true, multiline = false
	)
	public String webSocketPassword = "";

	// END WEBHOOK

	// START FAILSAFE

	@Switch(
		name = "Pop-up Notification", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "Enable pop-up notification"
	)
	public boolean popUpNotification = true;
	@Switch(
		name = "Fake Movements", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "Tries to act like a real player by moving around"
	)
	public boolean fakeMovements = true;
	@Switch(
		name = "Ping Sound", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "Makes a ping sound when a failsafe has been triggered"
	)
	public boolean pingSound = true;
	@Switch(
		name = "Auto alt-tab when failsafe triggered", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "Automatically alt-tabs to the game when the dark times come"
	)
	public boolean autoAltTab = true;
	@Switch(
		name = "Check Desync", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "If client desynchronization is detected, it activates a failsafe. Turn this off if the network is weak or if it happens frequently."
	)
	public boolean checkDesync = true;
	@Switch(
		name = "Auto TP On World Change", category = FAILSAFE, subcategory = "Miscellaneous",
		description = "Automatically warps back to garden on server reboot, server update, etc"
	)
	public boolean autoTPOnWorldChange = true;
	@Slider(
			name = "Rotation Check Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
			description = "The sensitivity of rotation check, the lower the sensitivity, the more accurate the check is, but it will also increase the chance of getting false positives",
			min = 1, max = 10
	)
	public float rotationCheckSensitivity = 2;
	@Switch(
			name = "Send failsafe message", category = FAILSAFE, subcategory = "Miscellaneous",
			description = "Sends a message to the chat when a failsafe has been triggered"
	)
	public boolean sendFailsafeMessage = true;
	@Text(
			name = "Custom failsafe message", category = FAILSAFE, subcategory = "Miscellaneous",
			description = "Custom failsafe message",
			placeholder = "Leave empty to use a random message"
	)
	public static String customFailsafeMessage = "";

	@Switch(
			name = "Enable Scheduler", category = FAILSAFE, subcategory = "Scheduler", size = OptionSize.DUAL,
			description = "Farms for X amount of minutes then takes a break for X amount of minutes"
	)
	public boolean enableScheduler = false;
	@Slider(
			name = "Farming time (in minutes)", category = FAILSAFE, subcategory = "Scheduler",
			description = "How long to farm",
			min = 1, max = 300, step = 1
	)
	public int schedulerFarmingTime = 30;
	@Slider(
			name = "Farming time randomness (in minutes)", category = FAILSAFE, subcategory = "Scheduler",
			description = "How much randomness to add to the farming time",
			min = 0, max = 15, step = 1
	)
	public int schedulerFarmingTimeRandomness = 0;
	@Slider(
			name = "Break time (in minutes)", category = FAILSAFE, subcategory = "Scheduler",
			description = "How long to take a break",
			min = 1, max = 120, step = 1
	)
	public int schedulerBreakTime = 5;
	@Slider(
			name = "Break time randomness (in minutes)", category = FAILSAFE, subcategory = "Scheduler",
			description = "How much randomness to add to the break time",
			min = 0, max = 15, step = 1
	)
	public int schedulerBreakTimeRandomness = 0;

	@Switch(
		name = "Enable", category = FAILSAFE, subcategory = "Restart After FailSafe",
		description = "Restarts the macro after a while when a failsafe has been triggered"
	)
	public boolean enableRestartAfterFailSafe = false;
	@Slider(
		name = "Restart Delay", category = FAILSAFE, subcategory = "Restart After FailSafe",
		description = "The delay to restart after failsafe (in seconds)",
		min = 0, max = 600
	)
	public int restartAfterFailSafeDelay = 30;
	@Switch(
		name = "Enable", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "Enables auto set spawn"
	)
	public boolean enableAutoSetSpawn = false;
	@Switch(
		name = "Set Spawn Before Evacuation", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "Set spawn before evacuate"
	)
	public boolean setSpawnBeforeEvacuate = false;
	@Number(
		name = "Set Spawn min delay", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "The minimum delay between setting a new spawn (in seconds)",
		min = 1, max = 120
	)
	public int autoSetSpawnMinDelay = 15;
	@Number(
		name = "Set Spawn max delay", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "The maximum delay between setting a new (in seconds)",
		min = 1, max = 120
	)
	public int autoSetSpawnMaxDelay = 25;

	@Switch(
			name = "Enable Banwave Checker", category = FAILSAFE, subcategory = "Banwave Checker",
			description = "Checks for banwave and shows you the number of players banned in the last 15 minutes"
	)
	public boolean banwaveCheckerEnabled = true;
	@Switch(
		name = "Leave during banwave", category = FAILSAFE, subcategory = "Banwave Checker",
		description = "Automatically disconnects from the server when banwave detected"
	)
	public boolean enableLeaveOnBanwave = false;
	@Slider(
		name = "Banwave Disconnect Threshold", category = FAILSAFE, subcategory = "Banwave Checker",
		description = "The threshold to disconnect from the server on banwave",
		min = 1, max = 100
	)
	public int banwaveThreshold = 50;
	@Number(
		name = "Delay Before Reconnecting", category = FAILSAFE, subcategory = "Banwave Checker",
		description = "The delay before reconnecting after leaving on banwave (in seconds)",
		min = 1, max = 20
	)
	public int delayBeforeReconnecting = 5;

	// END FAILSAFE

	// START JACOB

	@Switch(
		name = "Enable Jacob Failsafes", category = FAILSAFE, subcategory = "Jacob",
		description = "Stops farming once a crop threshold has been met"
	)
	public boolean enableJacobFailsafes = true;
	@Slider(
		name = "Nether Wart Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The nether wart cap",
		min = 0, max = 1000000, step = 10000
	)
	public int jacobNetherWartCap = 400000;
	@Slider(
		name = "Potato Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The potato cap",
		min = 0, max = 1000000, step = 10000
	)
	public int jacobPotatoCap = 400000;
	@Slider(
		name = "Carrot Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The carrot cap",
		min = 0, max = 1000000, step = 10000
	)
	public int jacobCarrotCap = 400000;
	@Slider(
		name = "Wheat Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The wheat cap",
		min = 0, max = 1000000
	)
	public int jacobWheatCap = 400000;
	@Slider(
		name = "Sugar Cane Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The sugar cane cap",
		min = 0, max = 1000000, step = 10000
	)
	public int jacobSugarCaneCap = 400000;
	@Slider(
		name = "Mushroom Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The mushroom cap",
		min = 0, max = 1000000, step = 10000
	)
	public int jacobMushroomCap = 200000;

	// END JACOB

	// START DEBUG

	@HUD(
		name = "Farm Helper Status", category = DEBUG, subcategory = "HUD"
	)
	public StatusHUD statusHUD = new StatusHUD();
	@HUD(
		name = "Farm Helper Profit Calculator", category = DEBUG, subcategory = "HUD"
	)
	public ProfitCalculatorHUD profitHUD = new ProfitCalculatorHUD();
	public int spawnPosX = 0;
	public int spawnPosY = 0;
	public int spawnPosZ = 0;

	// END DEBUG

	public Config() {
		super(new Mod("Farm Helper", ModType.HYPIXEL), "/farmhelper/config.json");
		initialize();

		this.addDependency("VerticalMacroType", "Macro Type", () -> {
			return !this.macroType;
		});
		this.addDependency("SShapeMacroType", "Macro Type", () -> {
			return this.macroType;
		});

        this.addDependency("fastBreakSpeed", "Fast Break", () -> {
            return this.fastBreak;
        });

		this.addDependency("sellToNPC", "Enable Auto Sell", () -> {
			return this.enableAutoSell;
		});
		this.addDependency("inventoryFullTime", "Sell to NPC", () -> {
			return this.enableAutoSell;
		});
		this.addDependency("inventoryFullRatio", "Sell to NPC", () -> {
			return this.enableAutoSell;
		});

		this.addDependency("customFailsafeMessage", "Enable Failsafe Custom Messages", () -> {
			return this.sendFailsafeMessage;
		});
		this.addDependency("schedulerFarmingTime", "Enable Scheduler", () -> {
			return this.enableScheduler;
		});
		this.addDependency("schedulerFarmingTimeRandomness", "Enable Scheduler", () -> {
			return this.enableScheduler;
		});
		this.addDependency("schedulerBreakTime", "Enable Scheduler", () -> {
			return this.enableScheduler;
		});
		this.addDependency("schedulerBreakTimeRandomness", "Enable Scheduler", () -> {
			return this.enableScheduler;
		});
		this.addDependency("jacobNetherWartCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("jacobPotatoCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("jacobCarrotCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("jacobWheatCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("jacobSugarCaneCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("jacobMushroomCap", "Enable Jacob Failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("onlyAcceptProfitableVisitors", "Enable Visitors Macro",() -> {
			return this.visitorsMacro;
		});
		this.addDependency("visitorsMacroCoinsThreshold", "Enable Visitors Macro",() -> {
			return this.visitorsMacro;
		});
		this.addDependency("sendLogs", "Enable webhook messages",() -> {
			return this.enableWebHook;
		});
		this.addDependency("sendStatusUpdates", "Enable webhook messages",() -> {
			return this.enableWebHook;
		});
		this.addDependency("statusUpdateInterval", "Enable webhook messages",() -> {
			return this.enableWebHook;
		});
		this.addDependency("webHookURL", "Enable webhook messages",() -> {
			return this.enableWebHook;
		});
		this.addDependency("webSocketIP", "Enable Remote Control",() -> {
			return this.enableRemoteControl;
		});
		this.addDependency("webSocketPassword", "Enable Remote Control",() -> {
			return this.enableRemoteControl;
		});
		this.addDependency("restartAfterFailSafeDelay", "Enable Restart After FailSafe",() -> {
			return this.enableRestartAfterFailSafe;
		});
		this.addDependency("enableLeaveOnBanwave", "Enable Banwave Checker",() -> {
			return this.banwaveCheckerEnabled;
		});
		this.addDependency("banwaveThreshold", "Enable Leave On Banwave",() -> {
			return this.enableLeaveOnBanwave;
		});
		this.addDependency("delayBeforeReconnecting", "Enable Leave On Banwave",() -> {
			return this.enableLeaveOnBanwave;
		});
		this.addDependency("setSpawnBeforeEvacuate", "Enable Auto Set Spawn",() -> {
			return this.enableAutoSetSpawn;
		});
		this.addDependency("autoSetSpawnMinDelay", "Enable Auto Set Spawn",() -> {
			return this.enableAutoSetSpawn;
		});
		this.addDependency("autoSetSpawnMaxDelay", "Enable Auto Set Spawn",() -> {
			return this.enableAutoSetSpawn;
		});

		save();
	}
}
