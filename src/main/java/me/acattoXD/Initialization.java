package me.acattoXD;

import com.jelly.FarmHelper.config.enums.AngleEnum;
import com.jelly.FarmHelper.config.enums.CropEnum;
import com.jelly.FarmHelper.config.interfaces.WebhookConfig;
import com.jelly.FarmHelper.utils.*;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.Window;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static me.acattoXD.WartMacro.updateDeltaChange;

public class Initialization {

    public static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean openedGUI = false;

    public static boolean enabled;
    public static boolean inTrenches;
    public static boolean falling;
    public static boolean teleportPad;
    public static boolean fixTpStuckFlag;
    public static boolean rotating;
    public static boolean pushedOffSide;
    public static boolean pushedOffFront;
    public static boolean teleporting;
    public static boolean newRow;
    public static boolean stuck;
    public static boolean cached;
    public static boolean crouched;
    public static boolean caged;
    public static boolean hubCaged;
    public static boolean bazaarLag;
    public static double cacheAverageAge;
    public static boolean selling;
    public static int stuckCount;
    public static int startCounter;
    public static int currentCounter;
    public static long jacobEnd;
    public static long lastStuck;
    public static long stuckCooldown;
    public static long startTime;
    public static long timeoutStart;
    public static long buyCooldown;
    public static long sellCooldown;
    public static int buyAttempts;
    public static int sellAttempts;
    public static boolean godPot;
    public static boolean cookie;
    public static boolean dropping;
    public static boolean checkFull;
    public static boolean buying;
    public static IChatComponent header;
    public static IChatComponent footer;
    public static BlockPos cachePos;
    public static WartMacro.location currentLocation;
    public static WartMacro.direction lastDirection;
    public static DiscordWebhook webhook;
    public static Map<CropEnum, Block> cropBlockStates = new HashMap<>();
    public static Map<CropEnum, PropertyInteger> cropAgeRefs = new HashMap<>();

    public static float playerYaw;
    public static AngleEnum angleEnum;
    public static int dropStack = Keyboard.KEY_Z;
    public static KeyBinding[] customKeyBinds = new KeyBinding[2];

    public static double deltaX;
    public static double deltaY;
    public static double deltaZ;
    public static double beforeX;
    public static double beforeY;
    public static double beforeZ;

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private static final Pattern PATTERN_ACTIVE_EFFECTS = Pattern.compile(
            "\u00a7r\u00a7r\u00a77You have a \u00a7r\u00a7cGod Potion \u00a7r\u00a77active! \u00a7r\u00a7d([0-9]*?:?[0-9]*?:?[0-9]*)\u00a7r");

    private static Window window;
    private static UIComponent profitGUI;


    static void initialize() {
        inTrenches = true;
        falling = false;
        teleportPad = false;
        fixTpStuckFlag = false;
        rotating = false;
        pushedOffSide = false;
        pushedOffFront = false;
        teleporting = false;
        newRow = true;
        buying = false;
        stuck = false;
        selling = false;
        checkFull = false;
        cached = false;
        crouched = true;
        cacheAverageAge = -1;
        cachePos = null;
        caged = false;
        hubCaged = false;
        bazaarLag = false;
        godPot = true;
        cookie = true;
        jacobEnd = System.currentTimeMillis();
        timeoutStart = System.currentTimeMillis();
        startCounter = InventoryUtils.getCounter();
        startTime = System.currentTimeMillis();
        stuckCount = 0;
        lastStuck = 0;
        stuckCooldown = System.currentTimeMillis();
        webhook = new DiscordWebhook(WebhookConfig.webhookURL);
        webhook.setUsername("Jelly - Farm Helper");
        webhook.setAvatarUrl("https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png");
        lastDirection = WartMacro.direction.NONE;
        angleEnum = AngleEnum.A0;
        playerYaw = AngleUtils.angleToValue(angleEnum);
        buyCooldown = System.currentTimeMillis();
        sellCooldown = System.currentTimeMillis();
        buyAttempts = 0;
        sellAttempts = 0;

        deltaX = 100;
        deltaY = 100;
        deltaZ = 100;
        beforeX = mc.thePlayer.posX;
        beforeY = mc.thePlayer.posY;
        beforeZ = mc.thePlayer.posZ;
        angleEnum = Math.round(AngleUtils.get360RotationYaw() / 90) < 4 ? AngleEnum.values()[Math.round(AngleUtils.get360RotationYaw() / 90)] : AngleEnum.A0;
        playerYaw = AngleUtils.angleToValue(angleEnum);


        try {
            AngleUtils.smoothRotateTo(AngleUtils.get360RotationYaw(playerYaw), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Utils.resetExecutor();
        mc.thePlayer.closeScreen();
        ProfitUtils.resetProfit();
        LogUtils.webhookLog("Started script");
        Utils.ScheduleRunnable(updateDeltaChange, 2, TimeUnit.SECONDS);
    }
}

