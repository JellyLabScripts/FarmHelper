package com.jelly.farmhelper.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.hud.ProfitCalculatorHUD;
import com.jelly.farmhelper.hud.StatusHUD;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.util.BlockPos;
import org.json.simple.JSONObject;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import com.jelly.farmhelper.features.Autosell;

import com.jelly.farmhelper.config.structs.Rewarp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// THIS IS RAT - CatalizCS
public class Config extends cc.polyfrost.oneconfig.config.Config {
	private transient static final Minecraft mc = Minecraft.getMinecraft();
	private transient static final String GENERAL = "General";
	private transient static final String FAILSAFE = "Fail Safes";
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
         PUMPKIN_MELON,
         MUSHROOM,
		MUSHROOM_ROTATE
     }

     public enum SMacroEnum {
         NORMAL_TYPE,
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
		LogUtils.scriptLog("Removed closest rewarp: " + rewarp.toString());
		saveRewarpConfig();
	}

	public static void removeAllRewarps() {
		rewarpList.clear();
		LogUtils.scriptLog("Removed all rewarp points");
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
		right = "S Shape"
	)
	public boolean macroType = false;
	@Switch(
		name = "Debug Mode", category = GENERAL, subcategory = "Macro",
		description = "Enable debug mode"
	)
	public boolean debugMode = false;
	@Dropdown(
		name = "Vertical Farm", category = GENERAL, subcategory = "Macro",
		description = "The macro to use for farming",
		options = {
			"Normal Type", // 0
			"Pumpkin/Melon", // 1
			"Mushroom", // 2
			"Mushroom ROTATE", // 3 (this shit outdated)
		}
	)
    public int VerticalMacroType = 0;
	@Dropdown(
		name = "S Shape Farm", category = GENERAL, subcategory = "Macro",
		description = "The macro to use for farming",
		options = {
			"Normal Type", // 0
			"Sugar Cane", // 1
			"Cactus", // 2
			"Cocoa Beans", // 3
			"Cocoa Beans (RoseGold version)"
		}
	)
    public int SShapeMacroType = 0;
	@Switch(
		name = "Ladder Design", category = GENERAL, subcategory = "Macro",
		description = "Use ladder design"
	)
	public boolean ladderDesign = false;
	@Switch(
		name = "Auto Ungrab Mouse", category = GENERAL, subcategory = "Macro",
		description = "Automatically ungrab the mouse when macroing"
	)
	public boolean autoUngrabMouse = true;
	@Switch(
		name = "Rotate After Warped", category = GENERAL, subcategory = "Macro",
		description = "Rotate after warped"
	)
	public boolean rotateAfterWarped = false;
	@Switch(
		name = "Rotate After Drop", category = GENERAL, subcategory = "Macro",
		description = "Rotate after drop"
	)
	public boolean rotateAfterDrop = false;

	@Switch(
		name = "Xray Mode", category = GENERAL, subcategory = "Miscellaneous",
		description = "Enable xray mode"
	)
	public boolean xrayMode = false;
	@Switch(
		name = "Mute Game", category = GENERAL, subcategory = "Miscellaneous",
		description = "Mute game"
	)
	public boolean muteGame = false;
	@Switch(
        name = "Auto GodPot", category = GENERAL, subcategory = "Miscellaneous",
        description = "Automatically godpot"
	)
	public boolean autoGodPot = false;
	@Switch(
        name = "Auto Cookie", category = GENERAL, subcategory = "Miscellaneous",
        description = "Automatically cookie"
	)
	public boolean autoCookie = false;
	@Switch(
		name = "Fast Change Direction Cane", category = GENERAL, subcategory = "Miscellaneous",
		description = "Fast change direction cane"
	)
	public boolean fastChangeDirectionCane = false;
	@Switch(
		name = "Count RNG to $/Hr in Profit Calculator", category = GENERAL, subcategory = "Miscellaneous",
		description = "Count RNG to $/Hr"
	)
	public boolean countRNGToDollarPerHour = false;
    @Switch(
        name = "Fast Break", category = GENERAL, subcategory = "Miscellaneous",
        description = "Fast break(gonna crazy asf, so be careful)"
    )
    public boolean fastBreak = false;
    @Slider(
        name = "Fast Break Speed", category = GENERAL, subcategory = "Miscellaneous",
        description = "Fast break speed, 1 its for cowards, 3 its for man",
        min = 1, max = 3, step = 1
    )
    public int fastBreakSpeed = 1;

	@Switch(
		name = "Enable", category = GENERAL, subcategory = "Visitors Macro",
		description = "Enable visitors macro"
	)
	public boolean visitorsMacro = false;
	@Switch(
		name = "Only Accept Profit Visitors", category = GENERAL, subcategory = "Visitors Macro",
		description = "Only accept visitors that are profitable"
	)
	public boolean onlyAcceptProfitVisitors = false;
	@Number(
		name = "Visitors Macro Money Threshold", category = GENERAL, subcategory = "Visitors Macro",
		description = "Visitors Macro Money Threshold",
		min = 1, max = 20, step = 1
	)
	public int visitorsMacroMoneyThreshold = 1;

	@Switch(
		name = "Enable Remote Control", category = GENERAL, subcategory = "Remote Control",
		description = "Enable remote control"
	)
	public boolean enableRemoteControl = false;
	@Text(
		name = "WebSocket IP", category = GENERAL, subcategory = "Remote Control",
		description = "The IP to use for the WebSocket server",
		secure = false, multiline = false

	)
	public String webSocketIP = "";
	@Text(
		name = "WebSocket Password", category = GENERAL, subcategory = "Remote Control",
		description = "The password to use for the WebSocket server",
		secure = true, multiline = false
	)
	public String webSocketPassword = "";
	@Switch(
        name = "Enable WebHook", category = GENERAL, subcategory = "Webhook",
        description = "Enable WebHook"
	)
	public boolean enableWebHook = false;
	@Switch(
		name = "Send Logs", category = GENERAL, subcategory = "Webhook",
		description = "Send logs to WebHook"
	)
	public boolean sendLogs = false;
	@Switch(
		name = "Send Status Updates", category = GENERAL, subcategory = "Webhook",
		description = "Send status updates to WebHook"
	)
	public boolean sendStatusUpdates = false;
	@Number(
		name = "Status Update Interval", category = GENERAL, subcategory = "Webhook",
		description = "The interval to send status updates to WebHook (in seconds)",
		min = 1, max = 60, step = 1
	)
	public int statusUpdateInterval = 5;
	@Text(
		name = "WebHook URL", category = GENERAL, subcategory = "Webhook",
		description = "The WebHook URL to use for the WebHook",
		placeholder = "https://discord.com/api/webhooks/...",
		secure = true, multiline = false
	)
	public String webHookURL = "";

	@Switch(
		name = "Enable Auto Sell", category = GENERAL, subcategory = "Auto Sell",
		description = "Enable auto sell"
	)
	public boolean enableAutoSell = false;
	@Switch(
		name = "Sell To NPC" , category = GENERAL, subcategory = "Auto Sell",
		description = "Sell to NPC"
	)
	public boolean sellToNPC = false;
	@Number(
		name = "Inventory Full Time", category = GENERAL, subcategory = "Auto Sell",
		description = "The time to wait for inventory to be full (in seconds)",
		min = 1, max = 20, step = 1
	)
	public int inventoryFullTime = 6;
	@Number(
		name = "Inventory Full Ratio", category = GENERAL, subcategory = "Auto Sell",
		description = "The ratio to wait for inventory to be full (in percentage)",
		min = 1, max = 100, step = 1
	)
	public int inventoryFullRatio = 65;
	@Button(
		name = "Sell Inventory Now", category = GENERAL, subcategory = "Auto Sell",
		description = "Sell inventory now",
		text = "Sell Now"
	)
	Runnable autoSellFunction = () -> {
		mc.thePlayer.closeScreen();
		Autosell.enable();
	};

	// END GENERAL

	// START FAIL SAFES
	@Switch(
		name = "Enable Scheduler", category = GENERAL, subcategory = "Scheduler",
		description = "Enable scheduler"
	)
	public boolean enableScheduler = false;
	@Number(
		name = "Farm time (in minutes)", category = GENERAL, subcategory = "Scheduler",
		description = "The time to farm (in minutes)",
		min = 1, max = 500, step = 3
	)
	public int farmTime = 30;
	@Number(
		name = "Sleep time (in minutes)", category = GENERAL, subcategory = "Scheduler",
		description = "The time to sleep (in minutes)",
		min = 1, max = 120, step = 3
	)
	public int sleepTime = 10;


	@Switch(
		name = "Pop-up Notification", category = FAILSAFE,
		description = "Enable pop-up notification"
	)
	public boolean popUpNotification = true;
	@Switch(
		name = "Fake Movements", category = FAILSAFE,
		description = "Enable fake movements let you have a chance to not get banned"
	)
	public boolean fakeMovements = true;
	@Switch(
		name = "Ping Sound", category = FAILSAFE,
		description = "Enable ping sound"
	)
	public boolean pingSound = true;
	@Switch(
		name = "Auto alt-tab on FailSafe Activated", category = FAILSAFE,
		description = "Automatically alt-tab on failsafe activated"
	)
	public boolean autoAltTabOnFailSafeActivated = true;
	@Slider(
		name = "Rotation Check Sensitivity", category = FAILSAFE,
		description = "The sensitivity of rotation check, the low the sensitivity, the more accurate the check is, but it will also increase the chance of false positive",
		min = 1, max = 10, step = 1
	)
	public float rotationCheckSensitivity = 2;
	@Switch(
		name = "Check DeSync", category = FAILSAFE,
		description = "Check desync, if desync is detected, it will activate failsafe, turn this off if the network is weak or if it happens frequently"
	)
	public boolean checkDeSync = true;
	@Switch(
		name = "Auto TP On World Change", category = FAILSAFE,
		description = "Automatically teleport to spawn on world change like server reboot, server update, etc"
	)
	public boolean autoTPOnWorldChange = true;
	@Switch(
		name = "Enable", category = FAILSAFE, subcategory = "Restart After FailSafe",
		description = "Enable restart after failsafe"
	)
	public boolean enableRestartAfterFailSafe = false;
	@Number(
		name = "Restart Delay", category = FAILSAFE, subcategory = "Restart After FailSafe",
		description = "The delay to restart after failsafe (in seconds)",
		min = 0, max = 600, step = 3
	)
	public int restartAfterFailSafeDelay = 30;
	@Switch(
		name = "Enable", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "Enable auto set spawn"
	)
	public boolean enableAutoSetSpawn = false;
	@Switch(
		name = "Set Spawn Before Evacuate", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "Set spawn before evacuate"
	)
	public boolean setSpawnBeforeEvacuate = false;
	@Number(
		name = "Set Spawn min Delay", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "The min delay to set spawn (in seconds)",
		min = 1, max = 120, step = 1
	)
	public int autoSetSpawnMinDelay = 60;
	@Number(
		name = "Set Spawn max Delay", category = FAILSAFE, subcategory = "Auto Set Spawn",
		description = "The max delay to set spawn (in seconds)",
		min = 1, max = 120, step = 1
	)
	public int autoSetSpawnMaxDelay = 25;

	@Switch(
		name = "Enable", category = FAILSAFE, subcategory = "Leave On Banwave",
		description = "Enable leave on banwave"
	)
	public boolean enableLeaveOnBanwave = false;
	@Slider(
		name = "Banwave Threshold", category = FAILSAFE, subcategory = "Leave On Banwave",
		description = "The banwave threshold",
		min = 1, max = 100, step = 1
	)
	public int banwaveThreshold = 50;
	@Number(
		name = "Delay Before Reconnect", category = FAILSAFE, subcategory = "Leave On Banwave",
		description = "The delay before reconnect (in seconds)",
		min = 1, max = 20, step = 1
	)
	public int delayBeforeReconnect = 5;
	// END FAIL SAFES

	// START JACOB
	@Switch(
		name = "Enable Jacob Failsafes", category = FAILSAFE, subcategory = "Jacob",
		description = "Enable Jacob failsafes"
	)
	public boolean enableJacobFailsafes = true;
	@Slider(
		name = "Nether Wart Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The nether wart cap",
		min = 0, max = 1000000, step = 10000
	)
	public int netherWartCap = 400000;
	@Slider(
		name = "Potato Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The potato cap",
		min = 0, max = 1000000, step = 10000
	)
	public int potatoCap = 400000;
	@Slider(
		name = "Carrot Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The carrot cap",
		min = 0, max = 1000000, step = 10000
	)
	public int carrotCap = 400000;
	@Slider(
		name = "Wheat Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The wheat cap",
		min = 0, max = 1000000, step = 1
	)
	public int wheatCap = 400000;
	@Slider(
		name = "Sugar Cane Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The sugar cane cap",
		min = 0, max = 1000000, step = 10000
	)
	public int sugarCaneCap = 400000;
	@Slider(
		name = "Mushroom Cap", category = FAILSAFE, subcategory = "Jacob",
		description = "The mushroom cap",
		min = 0, max = 1000000, step = 10000
	)
	public int mushroomCap = 200000;
	// END JACOB

	@Button(
		name = "Set Visitor's Desk", category = DEBUG, subcategory = "Visitor's Desk",
		description = "Set the visitor's desk position",
		text = "Set Visitor's Desk"
	)
	Runnable setVisitorDesk = () -> {
		BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
		visitorsDeskPosX = pos.getX();
		visitorsDeskPosY = pos.getY();
		visitorsDeskPosZ = pos.getZ();
		save();
		LogUtils.scriptLog("Visitors Desk Position Set. BlockPos: " + pos);
	};

	@Button(
		name = "Reset Visitor's Desk", category = DEBUG, subcategory = "Visitor's Desk",
		description = "Reset the visitor's desk position",
		text = "Reset Visitor's Desk"
	)
	Runnable resetVisitorDesk = () -> {
		visitorsDeskPosX = 0;
		visitorsDeskPosY = 0;
		visitorsDeskPosZ = 0;
		save();
		LogUtils.scriptLog("Visitors Desk Position Reset");
	};
	@Number(
		name = "Visitor's Desk X", category = DEBUG, subcategory = "Visitor's Desk",
		description = "The visitor's desk X coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int visitorsDeskPosX = 0;
	@Number(
		name = "Visitor's Desk Y", category = DEBUG, subcategory = "Visitor's Desk",
		description = "The visitor's desk Y coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int visitorsDeskPosY = 0;
	@Number(
		name = "Visitor's Desk Z", category = DEBUG, subcategory = "Visitor's Desk",
		description = "The visitor's desk Z coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int visitorsDeskPosZ = 0;
	@KeyBind(
		name = "Visitor's Desk Keybind", category = DEBUG, subcategory = "Visitor's Desk",
		description = "The visitor's desk keybind"
	)
	public OneKeyBind visitorsDeskKeybind = new OneKeyBind(0);



	@Number(
		name = "SpawnPos X", category = DEBUG, subcategory = "Spawn Location",
		description = "The SpawnPos X coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int spawnPosX = 0;
	@Number(
		name = "SpawnPos Y", category = DEBUG, subcategory = "Spawn Location",
		description = "The SpawnPos Y coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int spawnPosY = 0;
	@Number(
		name = "SpawnPos Z", category = DEBUG, subcategory = "Spawn Location",
		description = "The SpawnPos Z coordinate",
		min = -30000000, max = 30000000, step = 1
	)
	public int spawnPosZ = 0;
	@Button(
		name = "Set SpawnPos", category = DEBUG, subcategory = "Spawn Location",
		description = "Set the SpawnPos position",
		text = "Set SpawnPos"
	)
	Runnable _setSpawnPos = () -> {
		BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
		spawnPosX = pos.getX();
		spawnPosY = pos.getY();
		spawnPosZ = pos.getZ();
		save();
		LogUtils.scriptLog("SpawnPos Position Set. BlockPos: " + pos);
	};

	@Button(
		name = "Add Rewarp", category = DEBUG, subcategory = "Rewarp",
		description = "Add a rewarp position",
		text = "Add Rewarp"
	)
	Runnable _addRewarp = () -> {
		BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
		rewarpList.add(new Rewarp(pos.getX(), pos.getY(), pos.getZ()));
		save();
		LogUtils.scriptLog("Rewarp Position Added. BlockPos: " + pos);
	};

	@Button(
		name = "Remove Rewarp", category = DEBUG, subcategory = "Rewarp",
		description = "Remove a rewarp position",
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
		name = "Remove All Rewarps", category = DEBUG, subcategory = "Rewarp",
		description = "Remove all rewarp positions",
		text = "Remove All Rewarps"
	)
	Runnable _removeAllRewarps = () -> {
		removeAllRewarps();
		LogUtils.scriptLog("All rewarp positions removed");
	};

	@KeyBind(
		name = "Toggle Farm Helper", category = DEBUG, subcategory = "Keybinds",
		description = "Toggle the farm helper"
	)
	public OneKeyBind toggleMacro = new OneKeyBind(Keyboard.KEY_GRAVE);

	@HUD(
		name = "Farm Helper Status", category = DEBUG, subcategory = "HUD"
	)
	public StatusHUD statusHUD = new StatusHUD();
	@HUD(
		name = "Farm Helper Profit Calculator", category = DEBUG, subcategory = "HUD"
	)
	public ProfitCalculatorHUD profitHUD = new ProfitCalculatorHUD();

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

		this.addDependency("sellToNPC", "enable auto sell", () -> {
			return this.enableAutoSell;
		});
		this.addDependency("inventoryFullTime", "sell to npc", () -> {
			return this.enableAutoSell;
		});
		this.addDependency("inventoryFullRatio", "sell to npc", () -> {
			return this.enableAutoSell;
		});

		this.addDependency("farmTime", "enable scheduler", () -> {
			return this.enableScheduler;
		});
		this.addDependency("sleepTime", "scheduler time", () -> {
			return this.enableScheduler;
		});

		this.addDependency("netherWartCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("potatoCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("carrotCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("wheatCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("sugarCaneCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("mushroomCap", "enable jacob failsafes", () -> {
			return this.enableJacobFailsafes;
		});
		this.addDependency("_setSpawnPos", "debug mode",() -> {
			return this.debugMode;
		});

		save();
	}
}
