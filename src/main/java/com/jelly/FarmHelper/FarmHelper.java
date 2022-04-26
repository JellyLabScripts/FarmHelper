package com.jelly.FarmHelper;

import com.jelly.FarmHelper.config.*;
import com.jelly.FarmHelper.config.enums.AngleEnum;
import com.jelly.FarmHelper.config.enums.CropEnum;
import com.jelly.FarmHelper.config.enums.FarmEnum;
import com.jelly.FarmHelper.config.interfaces.*;
import com.jelly.FarmHelper.gui.MenuGUI;
import com.jelly.FarmHelper.gui.ProfitGUI;
import com.jelly.FarmHelper.utils.*;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.Window;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.universal.UGraphics;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.omg.PortableServer.ThreadPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jelly.FarmHelper.utils.BlockUtils.isWalkable;
import static com.jelly.FarmHelper.utils.PlayerUtils.updateKeys;

@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper {
    public static final String MODID = "farmhelper";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "3.0-beta3.2";

    public static final Minecraft mc = Minecraft.getMinecraft();

    enum direction {
        RIGHT,
        LEFT,
        NONE
    }

    enum location {
        ISLAND,
        HUB,
        LOBBY,
        LIMBO,
        TELEPORTING
    }

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
    public static location currentLocation;
    public static direction lastDirection;
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
        lastDirection = direction.NONE;
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

        Utils.resetExecutor();
        mc.thePlayer.closeScreen();
        ProfitUtils.resetProfit();
        LogUtils.webhookLog("Started script");
        Utils.ScheduleRunnable(updateDeltaChange, 2, TimeUnit.SECONDS);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FarmHelperConfig.init();
        customKeyBinds[0] = new KeyBinding("Open GUI", ((Long) FarmHelperConfig.get("openGUIKeybind")).intValue(), "FarmHelper");
        customKeyBinds[1] = new KeyBinding("Toggle script", ((Long) FarmHelperConfig.get("startScriptKeybind")).intValue(), "FarmHelper");
        ClientRegistry.registerKeyBinding(customKeyBinds[0]);
        ClientRegistry.registerKeyBinding(customKeyBinds[1]);

        cropBlockStates.put(CropEnum.WHEAT, Blocks.wheat);
        cropBlockStates.put(CropEnum.CARROT, Blocks.carrots);
        cropBlockStates.put(CropEnum.NETHERWART, Blocks.nether_wart);
        cropBlockStates.put(CropEnum.POTATO, Blocks.potatoes);

        cropAgeRefs.put(CropEnum.WHEAT, BlockCrops.AGE);
        cropAgeRefs.put(CropEnum.CARROT, BlockCarrot.AGE);
        cropAgeRefs.put(CropEnum.NETHERWART, BlockNetherWart.AGE);
        cropAgeRefs.put(CropEnum.POTATO, BlockPotato.AGE);

        webhook = new DiscordWebhook(WebhookConfig.webhookURL);
        webhook.setUsername("Jelly - Farm Helper");
        webhook.setAvatarUrl("https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png");

        enabled = false;

        window = new Window();
        profitGUI = new ProfitGUI(window);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FarmHelper());
        MinecraftForge.EVENT_BUS.register(new MenuGUI());
    }

    @SubscribeEvent
    public void onOpenGui(final GuiOpenEvent event) {
        if (event.gui instanceof GuiDisconnected) {
            enabled = false;
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void render(RenderGameOverlayEvent event) {
        int gapY = 15;

        if (mc.theWorld != null && mc.thePlayer != null && ProfitCalculatorConfig.profitCalculator && event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            ((ProfitGUI) profitGUI).stats.forEach(UIComponent::hide);
            if (ProfitCalculatorConfig.runtime) ((ProfitGUI) profitGUI).stats.get(9).unhide(true);
            if (ProfitCalculatorConfig.counter) ((ProfitGUI) profitGUI).stats.get(8).unhide(true);
            if (ProfitCalculatorConfig.mushroomCount) {
                ((ProfitGUI) profitGUI).stats.get(7).unhide(true);
                ((ProfitGUI) profitGUI).stats.get(6).unhide(true);
            }
            if (ProfitCalculatorConfig.itemCount) {
                switch (FarmConfig.cropType) {
                    case NETHERWART:
                        ((ProfitGUI) profitGUI).stats.get(2).unhide(true);
                        break;
                    case CARROT:
                        ((ProfitGUI) profitGUI).stats.get(3).unhide(true);
                        break;
                    case POTATO:
                        ((ProfitGUI) profitGUI).stats.get(4).unhide(true);
                        break;
                    case WHEAT:
                        ((ProfitGUI) profitGUI).stats.get(5).unhide(true);
                        break;
                }
            }
            if (ProfitCalculatorConfig.profitHour) ((ProfitGUI) profitGUI).stats.get(1).unhide(true);
            if (ProfitCalculatorConfig.totalProfit) ((ProfitGUI) profitGUI).stats.get(0).unhide(true);

            //UGraphics.enableAlpha();
            window.draw();
            window.drawFloatingComponents();
            //UGraphics.disableAlpha();
        }

        if (event.type == RenderGameOverlayEvent.ElementType.TEXT && MiscConfig.debugMode) {
            mc.fontRendererObj.drawString("dx: " + Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 96, -1);
            mc.fontRendererObj.drawString("dy: " + Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 84, -1);

            mc.fontRendererObj.drawString("count " + (System.currentTimeMillis() - timeoutStart), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 72, -1);
            mc.fontRendererObj.drawString("selling: " + selling, 4, new ScaledResolution(mc).getScaledHeight() - 140 - 60, -1);

            mc.fontRendererObj.drawString("KeyBindW: " + (mc.gameSettings.keyBindForward.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 48, -1);
            mc.fontRendererObj.drawString("KeyBindS: " + (mc.gameSettings.keyBindBack.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 36, -1);
            mc.fontRendererObj.drawString("KeyBindA: " + (mc.gameSettings.keyBindLeft.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 24, -1);
            mc.fontRendererObj.drawString("KeyBindD: " + (mc.gameSettings.keyBindRight.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 12, -1);

            mc.fontRendererObj.drawString(BlockUtils.getFrontBlock() + " " + BlockUtils.getBackBlock().toString() + " " + BlockUtils.getRightBlock().toString() + " " + BlockUtils.getLeftBlock().toString(), 4, new ScaledResolution(mc).getScaledHeight() - 20, -1);
        }
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (customKeyBinds[0].isPressed()) {
            openedGUI = true;
            mc.displayGuiScreen(new MenuGUI());
        }
        if (customKeyBinds[1].isPressed()) {
            if (!enabled) {
                LogUtils.scriptLog("Starting script", EnumChatFormatting.GREEN);
                LogUtils.configLog();
            }
            toggle();
        }
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (enabled) {
            if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
                LogUtils.debugLog("Failed teleport - waiting");
                Utils.ScheduleRunnable(tpReset, 5, TimeUnit.SECONDS);
            }
            if (message.contains("This server is too laggy")) {
                bazaarLag = true;
            }
            if (message.contains("You were spawned in Limbo")) {
                teleporting = false;
                LogUtils.debugLog("Spawned in limbo");
            }
            if (message.contains("You are AFK.")) {
                teleporting = false;
                LogUtils.debugLog("AFK lobby");
            }
            if (message.contains("SkyBlock Lobby")) {
                LogUtils.debugLog("Island reset - Going to hub");
                teleporting = false;
                mc.thePlayer.sendChatMessage("/hub");
            }
//            if (message.contains("Warping you to your SkyBlock island...") || message.contains("Welcome to Hypixel SkyBlock!")) {
//                LogUtils.debugLog("Detected warp back to island");
//                Utils.ScheduleRunnable(tpReset, 2, TimeUnit.SECONDS);
//            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void OnTickPlayer(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (mc.currentScreen instanceof GuiIngameMenu) {
            // LogUtils.debugLog("Detected esc menu, stopping all threads");
            Utils.resetExecutor();
            updateKeys(false, false, false, false, false, false);
            return;
        }

        if (mc.currentScreen instanceof GuiInventory && buying) {
            LogUtils.debugLog("Detected inventory, stopping all threads");
            Utils.resetExecutor();
            updateKeys(false, false, false, false, false, false);
            buying = false;
            cookie = true;
            godPot = true;
            return;
        }

        if (buying && (System.currentTimeMillis() - timeoutStart) > 20000) {
            Utils.resetExecutor();
            LogUtils.debugLog("Failed to buy cookie or god pot for 20s, stopping");
            LogUtils.webhookLog("Failed to buy cookie or god pot for 20s, stopping");
            buying = false;
            cookie = true;
            godPot = true;
            KeyBinding.onTick(PlayerUtils.keybindAttack);
            buyCooldown = System.currentTimeMillis() + 60000;
            buyAttempts++;
            if (buyAttempts >= 3) {
                MiscConfig.autoCookie = false;
                MiscConfig.autoGodPot = false;
                LogUtils.debugLog("Failed to buy 3 times, turning off auto cookie/pot");
                LogUtils.webhookLog("Failed to buy 3 times, turning off auto cookie/pot");
            }
            return;
        }

        if (selling && checkFull && (System.currentTimeMillis() - timeoutStart) > 20000) {
            Utils.resetExecutor();
            LogUtils.debugLog("Failed to sell inventory, stopping");
            LogUtils.webhookLog("Failed to sell inventory, stopping");
            KeyBinding.onTick(PlayerUtils.keybindAttack);
            selling = false;
            sellCooldown = System.currentTimeMillis() + 60000;
            sellAttempts++;
            if (sellAttempts >= 3) {
                AutoSellConfig.autoSell = false;
                LogUtils.debugLog("Failed to sell 3 times, turning off auto sell");
                LogUtils.webhookLog("Failed to sell 3 times, turning off auto sell");
            }
        }

        if (mc.theWorld != null && mc.thePlayer != null && enabled) {
            Block blockIn = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock();
            double dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
            double dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
            double dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);
            currentLocation = getLocation();
            if (ProfitCalculatorConfig.profitCalculator) Utils.ExecuteRunnable(updateCounters);

            if (caged) {
                switch (currentLocation) {
                    case ISLAND:
                        LogUtils.debugFullLog("Bedrock cage - Still in island");
                        if (hubCaged && BlockUtils.bedrockCount() < 2) {
                            LogUtils.scriptLog("Dodged bedrock cage");
                            LogUtils.webhookLog("Dodged bedrock cage | Go buy a lottery ticket");
                            caged = false;
                            hubCaged = false;
                        }
                        break;
                    case HUB:
                        if (!hubCaged) {
                            hubCaged = true;
                            Utils.ExecuteRunnable(hubCage);
                        }
                        LogUtils.debugFullLog("Bedrock cage - At hub, going to buy from bazaar3");
                        break;
                    case LOBBY:
                        LogUtils.debugFullLog("Bedrock cage - At lobby going to hub");
                        break;
                    default:
                        LogUtils.debugFullLog("Bedrock cage - Teleporting somewhere");
                        break;
                }
                return;
            }
            if (currentLocation == location.TELEPORTING) {
                LogUtils.debugFullLog("Teleporting somewhere");
                updateKeys(false, false, false, false, false, false);
                teleporting = false;
            }
            if (currentLocation == location.LIMBO) {
                LogUtils.debugFullLog("Detected limbo/afk");
                updateKeys(false, false, false, false, false, false);
                if (!teleporting && jacobEnd < System.currentTimeMillis()) {
                    LogUtils.debugLog("Detected Limbo or AFK");
                    LogUtils.webhookLog("Detected Limbo or AFK");
                    LogUtils.debugLog("Attempting to teleport to lobby");
                    mc.thePlayer.sendChatMessage("/lobby");
                    teleporting = true;
                    Utils.ScheduleRunnable(tpReset, 20, TimeUnit.SECONDS);
                }
            }
            if (currentLocation == location.LOBBY) {
                LogUtils.debugFullLog("Detected lobby");
                updateKeys(false, false, false, false, false, false);
                if (!teleporting && jacobEnd < System.currentTimeMillis()) {
                    LogUtils.debugLog("Detected lobby");
                    LogUtils.webhookLog("Detected lobby");
                    LogUtils.debugLog("Attempting to teleport to skyblock");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    teleporting = true;
                }
            }
            if (currentLocation == location.HUB) {
                LogUtils.debugFullLog("Detected hub");
                if (mc.currentScreen instanceof GuiInventory) {
                    LogUtils.debugLog("Detected open inventory in hub, stopping threads");
                    Utils.resetExecutor();
                }
                if (!cookie && MiscConfig.autoCookie) {
                    if (!buying) {
                        LogUtils.debugLog("Starting cookie thread");
                        buying = true;
                        Utils.ExecuteRunnable(buyCookie);
                    }
                    LogUtils.debugFullLog("Waiting for cookie purchase in hub");
                    return;
                }
                if (!godPot && MiscConfig.autoGodPot) {
                    if (!buying) {
                        LogUtils.debugLog("Starting god potion thread");
                        buying = true;
                        Utils.ExecuteRunnable(buyGodPot);
                    }
                    LogUtils.debugFullLog("Waiting for god pot purchase in hub");
                    return;
                }
                updateKeys(false, false, false, false, false, false);
                buying = false;
                if (!teleporting && jacobEnd < System.currentTimeMillis()) {
                    LogUtils.debugLog("Detected hub");
                    LogUtils.webhookLog("Detected hub");
                    LogUtils.debugLog("Attempting to teleport to island");
                    mc.thePlayer.sendChatMessage("/warp home");
                    teleporting = true;
                }
                return;
            }
            if (currentLocation == location.ISLAND && !crouched) {
                LogUtils.debugLog("Back to island, holding shift");
                LogUtils.webhookLog("Back to island, restarting");
                updateKeys(false, false, false, false, false, true);
                teleporting = false;
                deltaZ = 1000;
                deltaX = 1000;
                setStuckCooldown(3);
                Utils.ScheduleRunnable(crouchReset, 500, TimeUnit.MILLISECONDS);
                return;
            }
            if (currentLocation == location.ISLAND) {
                if (dropping) {
                    updateKeys(false, false, false, false, false, false);
                    return;
                }
                if (!caged && BlockUtils.bedrockCount() > 1) {
                    LogUtils.scriptLog("Bedrock cage detected");
                    LogUtils.webhookLog("Bedrock cage detected | RIPBOZO -1 acc");
                    Utils.ExecuteRunnable(islandCage);
                    caged = true;
                    return;
                }
                if (JacobConfig.jacobFailsafe && jacobExceeded()) {
                    LogUtils.debugFullLog("In jacob failsafe, waiting for teleport");
                    updateKeys(false, false, false, false, false, false);
                    if (!teleporting) {
                        LogUtils.debugLog("Jacob Failsafe - Exceeded threshold, going to Lobby");
                        jacobEnd = getJacobEnd();
                        mc.thePlayer.sendChatMessage("/setspawn");
                        mc.thePlayer.sendChatMessage("/lobby");
                        teleporting = true;
                        Utils.ScheduleRunnable(tpReset, 20, TimeUnit.SECONDS);
                    }
                    return;
                }
                if (mc.currentScreen instanceof GuiContainer || mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiIngameMenu || mc.currentScreen instanceof MenuGUI) {
                    LogUtils.debugFullLog("In inventory/chat/pause, pausing");
                    updateKeys(false, false, false, false, false, false);
                    deltaX = 1000;
                    deltaZ = 1000;
                    return;
                }
                if (selling && checkFull) {
                    updateKeys(false, false, false, false, false, false);
                    LogUtils.debugFullLog("Waiting for inventory sell");
                } else if (falling) {
                    // Stopped falling
                    if (dy == 0) {
                        LogUtils.debugFullLog("Changing layer - Landed - Doing 180 and switching back to trench state");
                        LogUtils.webhookLog("Changed layer - Dropped");
                        if (!rotating) {
                            updateKeys(false, false, false, false, false, false);
                            rotating = true;
                            Utils.ExecuteRunnable(changeLayer);
                        }
                    } else {
                        LogUtils.debugFullLog("Changing layer - In air - Wait");
                        updateKeys(false, false, false, false, false, false);
                    }
                } else if (teleportPad) {
                    // Not glitching up/down
                    if (dy == 0 && (mc.thePlayer.posY % 1) == 0.8125) {
                        if (isWalkable(BlockUtils.getRightBlock(0.1875)) || isWalkable(BlockUtils.getLeftBlock(0.1875))) {
                            LogUtils.debugFullLog("End of farm - At exit pad - Switch to trench state");
                            LogUtils.webhookLog("Changed layer - TP Pad");
                            updateKeys(false, false, false, false, false, false);
                            teleportPad = false;
                        } else {
                            LogUtils.debugLog("Unknown case - id: 5");
                        }
                    }
                    // Glitching in exit pad
                    else if (!fixTpStuckFlag && blockIn == Blocks.end_portal_frame && (mc.thePlayer.posY % 1) < 0.8120) {
                        LogUtils.debugLog("End of farm - Stuck in exit pad - Hold jump in separate thread");
                        updateKeys(false, false, false, false, false, false);
                        fixTpStuckFlag = true;
                        Utils.ExecuteRunnable(fixTpStuck);
                    }

                }
                // In trenches walking along the layer
                else if (inTrenches) {
                    angleEnum = Math.round(AngleUtils.get360RotationYaw() / 90) < 4 ? AngleEnum.values()[Math.round(AngleUtils.get360RotationYaw() / 90)] : AngleEnum.A0;
                    playerYaw = AngleUtils.angleToValue(angleEnum);
                    if (FarmConfig.cropType.equals(CropEnum.NETHERWART)) {
                        mc.thePlayer.rotationPitch = 0;
                    } else {
                        mc.thePlayer.rotationPitch = (float) 2.8;
                    }
                    // Not falling
                    if (FarmConfig.farmType == FarmEnum.LAYERED) {
                        if (dy == 0) {
                            // If on solid block
                            if ((mc.thePlayer.posY % 1) == 0) {
                                if (!stuck && !dropping) {
                                    try {
                                        AngleUtils.hardRotate(playerYaw);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                // Cannot move forwards or backwards
                                if (!isWalkable(BlockUtils.getFrontBlock()) && !isWalkable(BlockUtils.getBackBlock())) {
                                    if (!checkFull && !selling && mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                                        LogUtils.debugLog("Inventory possibly full");
                                        checkFull = true;
                                        Utils.ExecuteRunnable(fullInventory);
                                    } else if (!checkFull && selling) {
                                        selling = false;
                                        setStuckCooldown(3);
                                        deltaX = 100;
                                        deltaZ = 100;
                                    }
                                    cached = false;
                                    if (newRow) {
                                        newRow = false;
                                        Utils.ExecuteRunnable(checkFooter);
                                        mc.thePlayer.sendChatMessage("/setspawn");
                                    }
                                    if (deltaX < 1 && deltaZ < 1) {
                                        LogUtils.debugLog("Start/Middle/End of row - Detected stuck");
                                        LogUtils.webhookLog("Start/Middle/End of row - Detected stuck");
                                        if (!stuck && stuckCooldown < System.currentTimeMillis()) {
                                            stuck = true;
                                            stuckFrequency();
                                            updateKeys(false, false, false, false, false, false);
                                            Utils.ExecuteRunnable(fixRowStuck);
                                        }
                                    } else if (isWalkable(BlockUtils.getRightBlock()) && !isWalkable(BlockUtils.getLeftBlock())) {
                                        // newRow = true;
                                        LogUtils.debugLog("Start of farm - Go right");
                                        lastDirection = direction.RIGHT;
                                        updateKeys(false, false, false, true, true, false);
                                    } else if (isWalkable(BlockUtils.getLeftBlock()) && !isWalkable(BlockUtils.getRightBlock())) {
                                        // newRow = true;
                                        LogUtils.debugLog("Start of farm - Go left");
                                        lastDirection = direction.LEFT;
                                        updateKeys(false, false, true, false, true, false);
                                    } else if (isWalkable(BlockUtils.getRightBlock()) && isWalkable(BlockUtils.getLeftBlock())) {
                                        // LogUtils.debugLog("Middle of row - Calculating which direction to go");
                                        // Calculate if not done in last tick
                                        // if (lastDirection == direction.NONE) {
                                        if (lastDirection == direction.NONE) {
                                            LogUtils.debugFullLog("Middle of row - No direction last tick, recalculating");
                                            lastDirection = calculateDirection();
                                        }
                                        if (lastDirection == direction.RIGHT) {
                                            LogUtils.debugLog("Middle of row - Go right");
                                            updateKeys(false, false, false, true, true, false);
                                        } else if (lastDirection == direction.LEFT) {
                                            LogUtils.debugLog("Middle of row - Go left");
                                            updateKeys(false, false, true, false, true, false);
                                        } else {
                                            LogUtils.debugLog("Middle of row - Cannot calculate [Multiple > 180]");
                                            updateKeys(false, false, false, false, false, false);
                                        }
                                    } else {
                                        LogUtils.debugLog("Unknown case - id: 4");
                                    }
                                } else if (deltaX < 1 && deltaZ < 1) {
                                    LogUtils.debugLog("Row switch - Detected stuck");
                                    LogUtils.webhookLog("Row switch - Detected stuck");
                                    if (!stuck && stuckCooldown < System.currentTimeMillis()) {
                                        stuck = true;
                                        stuckFrequency();
                                        updateKeys(false, false, false, false, false, false);
                                        Utils.ExecuteRunnable(fixSwitchStuck);
                                    }
                                }

                                // Can go forwards but not backwards

                                else if (isWalkable(BlockUtils.getFrontBlock()) && !isWalkable(BlockUtils.getBackBlock())) {
                                    lastDirection = direction.NONE;
                                    LogUtils.debugLog("End of row - Switching to next");
                                    LogUtils.webhookStatus();
                                    newRow = true;
                                    if (!cached && MiscConfig.resync) {
                                        cached = true;
                                        Utils.ExecuteRunnable(cacheRowAge);
                                        Utils.ScheduleRunnable(checkDesync, 4, TimeUnit.SECONDS);
                                    }
                                    if (mc.gameSettings.keyBindForward.isKeyDown()) {
                                        LogUtils.debugFullLog("End of row - Start of col - Pushed off - Keep Going forwards");
                                        updateKeys(true, false, false, false, false, false);
                                    } else if (pushedOffSide) {
                                        if (dx < 0.001 && dz < 0.001) {
                                            LogUtils.debugFullLog("End of row - Start of col - Pushed off - Stopped - Going forwards");
                                            updateKeys(true, false, false, false, false, false);
                                        } else {
                                            LogUtils.debugFullLog("End of row - Start of col - Pushed off - Waiting till stop");
                                            updateKeys(false, false, false, false, false, false);
                                        }
                                    } else {
                                        if (dx < 0.001 && dz < 0.001) {
                                            if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                                                pushedOffSide = true;
                                                if (!isWalkable(BlockUtils.getRightBlock())) {
                                                    LogUtils.debugFullLog("End of row - Start of col - Pushing off right edge");
                                                    updateKeys(false, false, true, false, false, true);
                                                } else {
                                                    LogUtils.debugFullLog("End of row - Start of col - Pushing off left edge");
                                                    updateKeys(false, false, false, true, false, true);
                                                }
                                            } else {
                                                if (!isWalkable(BlockUtils.getRightBlock())) {
                                                    LogUtils.debugFullLog("End of row - Start of col - Going to edge");
                                                    updateKeys(false, false, false, true, false, false);
                                                } else {
                                                    LogUtils.debugFullLog("End of row - Start of col - Going to edge");
                                                    updateKeys(false, false, true, false, false, false);
                                                }
                                            }
                                        } else {
                                            LogUtils.debugFullLog("Unknown case id: 7");
                                        }
                                    }
                                }

                                // Can go forwards and backwards
                                else if (isWalkable(BlockUtils.getFrontBlock()) && isWalkable(BlockUtils.getBackBlock())) {
                                    lastDirection = direction.NONE;
                                    pushedOffSide = false;
                                    LogUtils.debugFullLog("End of row - Middle of col - Go forwards");
                                    updateKeys(true, false, false, false, false, false);
                                }

                                // Can go backwards but not forwards
                                else if (isWalkable(BlockUtils.getBackBlock()) && !isWalkable(BlockUtils.getFrontBlock())) {
                                    lastDirection = direction.NONE;
                                    if (isWalkable(BlockUtils.getLeftBlock()) || isWalkable(BlockUtils.getRightBlock())) {
                                        if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                                            pushedOffFront = false;
                                            if (isWalkable(BlockUtils.getLeftBlock())) {
                                                LogUtils.debugFullLog("End of row - End of col - Keep going left");
                                                updateKeys(false, false, true, false, true, false);
                                            } else if (isWalkable(BlockUtils.getRightBlock())) {
                                                LogUtils.debugFullLog("End of row - End of col - Keep going right");
                                                updateKeys(false, false, false, true, true, false);
                                            }
                                        } else if (pushedOffFront) {
                                            if (dx < 0.001 && dz < 0.001) {
                                                if (!dropping && System.currentTimeMillis() > stuckCooldown && MiscConfig.dropStone) {
                                                    dropping = true;
                                                    Utils.ExecuteRunnable(dropStone);
                                                } else if (isWalkable(BlockUtils.getLeftBlock())) {
                                                    LogUtils.debugLog("End of row - End of col - Pushed - Go left");
                                                    updateKeys(false, false, true, false, true, false);
                                                } else if (isWalkable(BlockUtils.getRightBlock())) {
                                                    LogUtils.debugLog("End of row - End of col - Pushed - Go right");
                                                    updateKeys(false, false, false, true, true, false);
                                                }
                                            } else {
                                                LogUtils.debugFullLog("End of row - Start of col - Pushed off - Waiting till stop");
                                                updateKeys(false, false, false, false, false, true);
                                            }
                                        } else if (dx < 0.001 && dz < 0.001) {
                                            if (mc.gameSettings.keyBindForward.isKeyDown()) {
                                                LogUtils.debugFullLog("End of row - End of col - Edge - Pushing off");
                                                pushedOffFront = true;
                                                updateKeys(false, true, false, false, false, true);
                                            } else {
                                                LogUtils.debugFullLog("End of row - End of col - Maybe not edge - Going forwards");
                                                updateKeys(true, false, false, false, true, false);
                                            }
                                        } else {
                                            LogUtils.debugFullLog("End of row - End of col - Not at edge - Going forwards");
                                            updateKeys(true, false, false, false, true, false);
                                        }
                                    }
                                    // Can't go anywhere, end of layer
                                    else if (BlockUtils.getBelowBlock() == Blocks.end_portal_frame) {
                                        LogUtils.debugLog("End of farm - Above entrance tp");
                                        updateKeys(false, false, false, false, false, false);
                                        teleportPad = true;
                                    } else if (BlockUtils.getBelowBlock() == Blocks.air) {
                                        LogUtils.debugLog("Changing layer - About to fall");
                                        updateKeys(true, false, false, false, false, false);
                                        falling = true;
                                        setStuckCooldown(5);
                                    }
                                }
                            }
                            // Standing on tp pad
                            else if ((mc.thePlayer.posY % 1) == 0.8125) {
                                lastDirection = direction.NONE;
                                if (!isWalkable(BlockUtils.getFrontBlock(0.1875))) {
                                    // Cannot go left or right
                                    if (!isWalkable(BlockUtils.getLeftBlock(0.1875)) && !isWalkable(BlockUtils.getRightBlock(0.1875))) {
                                        LogUtils.debugLog("End of farm - On entrance pad, wait until on exit pad");
                                        updateKeys(false, false, false, false, false, false);
                                        teleportPad = true;
                                    } else if (isWalkable(BlockUtils.getRightBlock(0.1875))) {
                                        LogUtils.debugLog("Start of farm - At exit pad - Go right");
                                        updateKeys(false, false, false, true, true, false);
                                    } else if (isWalkable(BlockUtils.getLeftBlock(0.1875))) {
                                        LogUtils.debugLog("Start of farm - At exit pad - Go left");
                                        updateKeys(false, false, true, false, true, false);
                                    } else {
                                        LogUtils.debugLog("Unknown case - id: 3");
                                    }
                                }
                            }
                        } else if (isWalkable(blockIn) && isWalkable(BlockUtils.getBelowBlock()) && !isWalkable(BlockUtils.getFrontBlock()) && (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) < -0.2 && (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) > -0.8) {
                            updateKeys(false, false, false, false, false, false);
                            falling = true;
                            LogUtils.debugLog("If you see this - DM Polycrylate#2205. Did this log when falling/layer switch? Were you meant to layer switch but it didn't? Or did this run randomly");
                        } else {
                            // Potentially make it send false all keys for this case
                            LogUtils.debugLog("Unknown case - id: 2");
                        }
                    } else if (FarmConfig.farmType == FarmEnum.VERTICAL) {
                        if (dy == 0) {
                            if ((mc.thePlayer.posY % 1) == 0.8125) {//standing on tp pad
                                lastDirection = direction.NONE;
                                if (!isWalkable(BlockUtils.getFrontBlock(0.1875))) {
                                    // Cannot go left or right
                                    if (!isWalkable(BlockUtils.getLeftBlock(0.1875)) && !isWalkable(BlockUtils.getRightBlock(0.1875))) {
                                        LogUtils.debugLog("End of farm - On entrance pad, wait until on exit pad");
                                        updateKeys(false, false, false, false, false, false);
                                        teleportPad = true;
                                    } else if (isWalkable(BlockUtils.getRightBlock(0.1875))) {
                                        LogUtils.debugLog("Start of farm - At exit pad - Go right");
                                        updateKeys(false, false, false, true, true, false);
                                    } else if (isWalkable(BlockUtils.getLeftBlock(0.1875))) {
                                        LogUtils.debugLog("Start of farm - At exit pad - Go left");
                                        updateKeys(false, false, true, false, true, false);
                                    } else {
                                        LogUtils.debugLog("Unknown case - id: 3");
                                    }
                                }
                            } else {
                                try {
                                    AngleUtils.hardRotate(playerYaw);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (isWalkable(BlockUtils.getRightBlock()) && isWalkable(BlockUtils.getLeftBlock())) {
                                    if (lastDirection == direction.NONE) {
                                        LogUtils.debugFullLog("Middle of row - No direction last tick, recalculating");
                                        lastDirection = calculateDirection();
                                    }
                                    if (newRow) {
                                        newRow = false;
                                        mc.thePlayer.sendChatMessage("/setspawn");
                                    }
                                    if (lastDirection == direction.RIGHT) {
                                        LogUtils.debugLog("Middle of row - Go right");
                                        updateKeys(false, false, false, true, true, false);
                                    } else if (lastDirection == direction.LEFT) {
                                        LogUtils.debugLog("Middle of row - Go left");
                                        updateKeys(false, false, true, false, true, false);
                                    } else {
                                        LogUtils.debugLog("Middle of row - Cannot calculate [Multiple > 180]");
                                        updateKeys(false, false, false, false, false, false);
                                    }
                                } else if (!isWalkable(BlockUtils.getLeftBlock()) && isWalkable(BlockUtils.getRightBlock())) {
                                    if (!cached && MiscConfig.resync) {
                                        cached = true;
                                        Utils.ExecuteRunnable(cacheRowAge);
                                        Utils.ScheduleRunnable(checkDesync, 4, TimeUnit.SECONDS);
                                    }
                                    if (dx < 0.001 && dz < 0.001) {
                                        newRow = true;
                                        lastDirection = direction.RIGHT;
                                        updateKeys(false, false, false, true, true, false);
                                    }
                                } else if (isWalkable(BlockUtils.getLeftBlock()) && !isWalkable(BlockUtils.getRightBlock())) {
                                    cached = false;
                                    if (dx < 0.001 && dz < 0.001) {
                                        lastDirection = direction.LEFT;
                                        updateKeys(false, false, true, false, true, false);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LogUtils.debugLog("Unknown case - id: 1");
                }
            }
        }
    }

    // Runnables
    public static Runnable updateDeltaChange = new Runnable() {
        @Override
        public void run() {
            if (enabled) {
                deltaX = Math.abs(mc.thePlayer.posX - beforeX);
                deltaZ = Math.abs(mc.thePlayer.posZ - beforeZ);
                deltaY = Math.abs(mc.thePlayer.posY - beforeY);
                beforeX = mc.thePlayer.posX;
                beforeZ = mc.thePlayer.posZ;
                beforeY = mc.thePlayer.posY;
                Utils.ScheduleRunnable(updateDeltaChange, 2, TimeUnit.SECONDS);
            }
        }
    };

    public static Runnable updateCounters = () -> {
        if (enabled) {
            InventoryUtils.getInventoryDifference(mc.thePlayer.inventory.mainInventory);
            ProfitUtils.updateProfitState();
        }
    };

    public static Runnable fixTpStuck = () -> {
        try {
            KeyBinding.setKeyBindState(PlayerUtils.keyBindSpace, true);
            Thread.sleep(800);
            KeyBinding.setKeyBindState(PlayerUtils.keyBindSpace, false);
            Thread.sleep(300);
            deltaX = 100;
            deltaY = 100;
            setStuckCooldown(3);
            fixTpStuckFlag = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable fixRowStuck = () -> {
        try {
            Thread.sleep(20);
            KeyBinding.setKeyBindState(PlayerUtils.keybindS, true);
            Thread.sleep(500);
            KeyBinding.setKeyBindState(PlayerUtils.keybindS, false);
            Thread.sleep(200);
            KeyBinding.setKeyBindState(PlayerUtils.keybindW, true);
            Thread.sleep(500);
            KeyBinding.setKeyBindState(PlayerUtils.keybindW, false);
            Thread.sleep(200);
            deltaX = 100;
            deltaZ = 100;
            stuck = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable fixSwitchStuck = () -> {
        try {
            Thread.sleep(20);
            KeyBinding.setKeyBindState(PlayerUtils.keybindA, true);
            Thread.sleep(500);
            KeyBinding.setKeyBindState(PlayerUtils.keybindA, false);
            Thread.sleep(200);
            KeyBinding.setKeyBindState(PlayerUtils.keybindD, true);
            Thread.sleep(500);
            KeyBinding.setKeyBindState(PlayerUtils.keybindD, false);
            Thread.sleep(200);
            deltaX = 100;
            deltaZ = 100;
            stuck = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable changeLayer = () -> {
        try {
            Thread.sleep(250);
            angleEnum = angleEnum.ordinal() < 2 ? AngleEnum.values()[angleEnum.ordinal() + 2] : AngleEnum.values()[angleEnum.ordinal() - 2];
            playerYaw = AngleUtils.angleToValue(angleEnum);
            AngleUtils.smoothRotateClockwise(180, 1.2f);
            System.out.println("-------EXPECTED: " + AngleUtils.get360RotationYaw() + "ACTUAL--------" + playerYaw);
            Thread.sleep(100);
            KeyBinding.setKeyBindState(PlayerUtils.keybindW, true);
            Thread.sleep(200);
            KeyBinding.setKeyBindState(PlayerUtils.keybindW, false);
            setStuckCooldown(2);
            deltaX = 100;
            deltaZ = 100;
            rotating = false;
            falling = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable cacheRowAge = () -> {
        for (int i = 1; i < 10; i++) {
            BlockPos pos = new BlockPos(
                mc.thePlayer.posX + (i * BlockUtils.getUnitX()),
                mc.thePlayer.posY + 1,
                mc.thePlayer.posZ + (i * BlockUtils.getUnitZ())
            );
            Block checkBlock = mc.theWorld.getBlockState(pos).getBlock();
            LogUtils.debugFullLog("checking ------------ " + checkBlock);
            if (checkBlock.equals(cropBlockStates.get(FarmConfig.cropType))) {
                LogUtils.debugFullLog("cacheRowAge - Found row - Calculating age - Pos: " + pos);
                cachePos = pos;
                cacheAverageAge = BlockUtils.getAverageAge(pos);
                LogUtils.debugLog("Row Age - Calculated age: " + cacheAverageAge);
                return;
            }
        }
        LogUtils.debugLog("Row Age - No row found (Maybe changing layer?)");
        cachePos = null;
        cacheAverageAge = -1;
    };

    public static Runnable checkDesync = () -> {
        LogUtils.debugFullLog("checkDesync - Enter");
        while (dropping) {
            LogUtils.debugLog("checkDesync - Still dropping items, waiting");
            try {
                Thread.sleep(500);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(4000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        double lowestAverage = 4;
        double range = 0.25;
        if (FarmConfig.cropType == CropEnum.NETHERWART) {
            lowestAverage = 2.3;
        }
        if (cachePos == null || cacheAverageAge == -1) {
            LogUtils.debugLog("Desync - No cache (Wrong crop selected?)");
        } else if (cacheAverageAge >= lowestAverage) {
            double newAvg = BlockUtils.getAverageAge(cachePos);
            LogUtils.debugLog("Desync - Old: " + cacheAverageAge + ", New: " + newAvg);
            if (Math.abs(newAvg - cacheAverageAge) < range && !stuck) {
                LogUtils.debugLog("Desync detected, going to hub");
                LogUtils.webhookLog("Desync detected, going to hub");
                teleporting = false;
                mc.thePlayer.sendChatMessage("/hub");
            } else {
                LogUtils.debugLog("No desync detected");
            }
        } else {
            LogUtils.debugLog("Desync - Average age too low");
        }
    };

    public static Runnable tpReset = () -> teleporting = false;

    public static Runnable crouchReset = () -> crouched = true;

    public static Runnable islandCage = () -> {
        try {
            Thread.sleep(400);
            updateKeys(false, false, false, false, false, false);
            Thread.sleep(800);
            updateKeys(false, false, true, false, false, false);
            AngleUtils.sineRotateCW(45, 0.4f);
            Thread.sleep(100);
            updateKeys(false, false, false, false, false, false);
            Thread.sleep(1500);
            AngleUtils.sineRotateACW(84, 0.5f);
            updateKeys(false, false, false, true, false, false);
            Thread.sleep(100);
            updateKeys(false, false, false, false, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false, false);
            mc.thePlayer.sendChatMessage("/hub");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable hubCage = () -> {
        try {
            AngleUtils.smoothRotateAnticlockwise(77, 1);
            Thread.sleep(1000);
            updateKeys(true, false, false, false, false, false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
            while (BlockUtils.getFrontBlock() != Blocks.spruce_stairs) {
                LogUtils.debugFullLog("Not reached bazaar");
                Thread.sleep(50);
            }
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            updateKeys(false, false, false, false, false, true);
            Thread.sleep(300);
            bazaarLag = false;
            while (!(mc.thePlayer.openContainer instanceof ContainerChest) && !bazaarLag) {
                LogUtils.debugFullLog("Attempting to open gui");
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                Thread.sleep(600);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                Thread.sleep(600);
            }
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 0);
                Thread.sleep(1000);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 12);
                Thread.sleep(1000);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 10);
                Thread.sleep(1000);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 10);
                Thread.sleep(1000);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 12);
                Thread.sleep(1000);
                mc.thePlayer.closeScreen();
            }
            bazaarLag = false;
            Thread.sleep(3000);
            while (currentLocation == location.HUB) {
                mc.thePlayer.sendChatMessage("/is");
                Thread.sleep(10000);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable checkFooter = () -> {
        LogUtils.debugFullLog("Looking for godpot/cookie");
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
                    LogUtils.debugLog("Cookie buff not active!");
                    foundCookieText = false;
                    cookie = false;
                    if (MiscConfig.autoCookie && !buying && buyCooldown < System.currentTimeMillis() && enabled) {
                        LogUtils.debugLog("Going to buy cookie in 1 second");
                        timeoutStart = System.currentTimeMillis();
                        mc.thePlayer.sendChatMessage("/hub");
                    }
                } else if (foundCookieText) {
                    LogUtils.debugLog("Cookie active!");
                    foundCookieText = false;
                    cookie = true;
                }
            }
            if (!foundGodPot) {
                LogUtils.debugLog("God pot buff not active!");
                godPot = false;
                if (MiscConfig.autoGodPot && !buying && buyCooldown < System.currentTimeMillis() && enabled) {
                    LogUtils.debugLog("Going to buy god potion in 1 second");
                    timeoutStart = System.currentTimeMillis();
                    mc.thePlayer.sendChatMessage("/hub");
                }
            } else {
                LogUtils.debugLog("God pot buff active!");
                godPot = true;
            }
        }
    };

    public static Runnable dropStone = () -> {
        dropping = true;
        boolean right;
        int hoeSlot = mc.thePlayer.inventory.currentItem;
        try {
            int slotID = -1;

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null) {
                    if (stack.getDisplayName().contains("Stone")) {
                        slotID = i;
                    }
                }
            }

            if (slotID != -1) {
                LogUtils.debugLog("Found stone - dropping");

                if (slotID < 9) {
                    slotID = 36 + slotID;
                }
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, slotID, 0, 0);
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, slotID, 0, 6);
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, 35 + 7, 0, 0);
                Thread.sleep(300);
                if (isWalkable(BlockUtils.getRightBlock())) {
                    right = true;
                    AngleUtils.smoothRotateAnticlockwise(90, 2.5f);
                } else {
                    right = false;
                    AngleUtils.smoothRotateClockwise(90, 2.5f);
                }
                Thread.sleep(400);
                mc.thePlayer.inventory.currentItem = -1 + 7;
                Thread.sleep(400);
                mc.thePlayer.dropOneItem(true);
                LogUtils.debugLog("Dropped successfully");
                Thread.sleep(100);
                mc.thePlayer.inventory.currentItem = hoeSlot;
                if (right) {
                    AngleUtils.smoothRotateClockwise(90, 2.5f);
                    Thread.sleep(1000);
                    updateKeys(false, false, false, true, true, false);
                } else {
                    AngleUtils.smoothRotateAnticlockwise(90, 2.5f);
                    Thread.sleep(1000);
                    updateKeys(false, false, true, false, true, false);
                }
            } else {
                // Thread.sleep(100);
                LogUtils.debugLog("No stone - keep going");
                if (isWalkable(BlockUtils.getRightBlock())) {
                    updateKeys(false, false, false, true, true, false);
                } else {
                    updateKeys(false, false, true, false, true, false);
                }
            }
            deltaX = 1000;
            deltaY = 1000;
            setStuckCooldown(4);
            dropping = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable autoSell = () -> {
        try {
            timeoutStart = System.currentTimeMillis();
            selling = true;
            checkFull = true;
            int hoeSlot = mc.thePlayer.inventory.currentItem;

            Integer[] NPCSellSlots = {11, 16, 21, 23};
            Integer[] NPCSellSlotCounts = {0, 0, 0, 0};

            Integer[] BZSellSlots = {10, 13, 19};
            Integer[] BZSellSlotCounts = {0, 0, 0};

            if (!cookie) {
                LogUtils.debugLog("You need a cookie for auto sell!");
                KeyBinding.onTick(PlayerUtils.keybindAttack);
                Thread.sleep(100);
                checkFull = false;
                return;
            }

            InventoryUtils.sellInventory();

            if (InventoryUtils.findItemInventory("Large Enchanted Agronomy Sack") == -1) {
                LogUtils.debugLog("No sack detected, resuming");
                Thread.sleep(100);
                mc.thePlayer.inventory.currentItem = hoeSlot;
                Thread.sleep(100);
                KeyBinding.onTick(PlayerUtils.keybindAttack);
                Thread.sleep(100);
                checkFull = false;
                return;
            }
            System.out.println("autoSell1 - status: " +Thread.currentThread().isInterrupted());
            PlayerUtils.openSack();
            System.out.println("autoSell2 - status: " +Thread.currentThread().isInterrupted());
            // Count all items in sack NPC
            for (int i = 0; i < NPCSellSlots.length; i++) {
                InventoryUtils.waitForItem(NPCSellSlots[i], "");
                NPCSellSlotCounts[i] = InventoryUtils.countSack(NPCSellSlots[i]);
            }

            // Count all items in sack BZ
            for (int i = 0; i < BZSellSlots.length; i++) {
                InventoryUtils.waitForItem(BZSellSlots[i], "");
                BZSellSlotCounts[i] = InventoryUtils.countSack(BZSellSlots[i]);
            }

            // Claim items with counts
            for (int i = 0; i < NPCSellSlots.length; i++) {
                while (NPCSellSlotCounts[i] != 0) {
                    if (!(mc.currentScreen instanceof GuiContainer)) {
                        if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                        PlayerUtils.openSack();
                    }
                    while (mc.thePlayer.inventory.getFirstEmptyStack() != -1 && NPCSellSlotCounts[i] != 0) {
                        if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                        LogUtils.debugLog("Collecting");
                        InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, NPCSellSlots[i]);
                        InventoryUtils.waitForItem(NPCSellSlots[i], "");
                        Thread.sleep(100);
                        NPCSellSlotCounts[i] = InventoryUtils.countSack(NPCSellSlots[i]);
                    }
                    InventoryUtils.sellInventory();
                    timeoutStart = System.currentTimeMillis();
                }
            }

            // If any remaining in sack, sell to bazaar
            for (int i = 0; i < BZSellSlots.length; i++) {
                if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                if (BZSellSlotCounts[i] != 0) {
                    PlayerUtils.openBazaar();
                    InventoryUtils.waitForItemClick(11, "Selling whole inventory", 39, "Sell Sacks Now");
                    InventoryUtils.waitForItemClick(11, "Items sold!", 11, "Selling whole inventory");
                }
            }
            mc.thePlayer.closeScreen();
            mc.thePlayer.inventory.currentItem = hoeSlot;
            Thread.sleep(100);
            KeyBinding.onTick(PlayerUtils.keybindAttack);
            Thread.sleep(100);
            checkFull = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable fullInventory = () -> {
        int count = 0;
        int total = 0;
        int elapsed = 0;
        checkFull = true;
        try {
            while (elapsed < AutoSellConfig.fullTime * 1000) {
                if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    count++;
                }
                total++;
                elapsed += 10;
                Thread.sleep(10);
            }
            if (((float) count / total) > (AutoSellConfig.fullRatio / 100) && !selling && AutoSellConfig.autoSell && sellCooldown < System.currentTimeMillis()) {
                selling = true;
                LogUtils.webhookLog("Inventory full, Auto Selling!");
                timeoutStart = System.currentTimeMillis();
                Utils.ExecuteRunnable(autoSell);
            } else {
                checkFull = false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable buyGodPot = () -> {
        try {
            timeoutStart = System.currentTimeMillis();
            int hoeSlot = mc.thePlayer.inventory.currentItem;
            PlayerUtils.goToBlock(4, -95);
            PlayerUtils.goToBlock(3, -99);
            PlayerUtils.goToBlock(1, -101);
            AngleUtils.smoothRotateTo(106, 1);
            LogUtils.debugLog(String.valueOf(mc.currentScreen instanceof GuiContainer));
            KeyBinding.onTick(PlayerUtils.keybindUseItem);
            while (!(mc.currentScreen instanceof GuiContainer)) {
                if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                Thread.sleep(100);
                KeyBinding.onTick(PlayerUtils.keybindUseItem);
            }
            Thread.sleep(20);
            KeyBinding.setKeyBindState(PlayerUtils.keybindUseItem, false);
            if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                InventoryUtils.waitForItemClick(19, "God Potion", 4, "Bits Shop");
                LogUtils.debugLog("Now purchasing god potion");
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 19);
                Thread.sleep(100);
                while (InventoryUtils.findItemInventory("God Potion") == -1) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    if (mc.thePlayer.openContainer.getSlot(11).getStack().getDisplayName().contains("Confirm")) {
                        InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 11);
                        // InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 15);
                    }
                    LogUtils.debugLog("Looking for god pot in inventory");
                    Thread.sleep(100);
                }
                LogUtils.debugLog("Found god pot in inventory, switching");
                KeyBinding.unPressAllKeys();
                mc.thePlayer.closeScreen();
                Thread.sleep(20);
                KeyBinding.setKeyBindState(PlayerUtils.keybindUseItem, false);
                while (mc.currentScreen instanceof GuiContainer) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    LogUtils.debugLog("Waiting for shop to close");
                    Thread.sleep(100);
                }
                AngleUtils.smoothRotateClockwise(180, 0.6f);
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, InventoryUtils.findItemInventory("God Potion"));
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, 42);
                Thread.sleep(1000);
                LogUtils.debugLog("Done switching");
                mc.thePlayer.inventory.currentItem = 6;
                while (InventoryUtils.findItemInventory("God Potion") != -1 || !FarmHelper.godPot) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    if (mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getDisplayName().contains("God Potion")) {
                        KeyBinding.onTick(PlayerUtils.keybindUseItem);
                    }
                    Utils.ExecuteRunnable(checkFooter);
                    LogUtils.debugLog("Waiting for consume");
                    Thread.sleep(100);
                }
                LogUtils.debugLog("Consumed!");
                mc.thePlayer.inventory.currentItem = hoeSlot;
                Utils.ExecuteRunnable(checkFooter);
            } else {
                LogUtils.debugLog("Inventory full! Cannot buy God Pot, turning off Auto God Pot for this session");
                buying = false;
                MiscConfig.autoGodPot = false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static Runnable buyCookie = () -> {
        try {
            timeoutStart = System.currentTimeMillis();
            int hoeSlot = mc.thePlayer.inventory.currentItem;
            PlayerUtils.goToBlock(-3, -77);
            PlayerUtils.goToBlock(-31, -77);
            KeyBinding.onTick(PlayerUtils.keybindUseItem);
            while (!(mc.currentScreen instanceof GuiContainer)) {
                if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                KeyBinding.onTick(PlayerUtils.keybindUseItem);
                Thread.sleep(100);
            }
            if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                InventoryUtils.waitForItemClick(11, "Booster Cookie", 36, "Oddities");
                LogUtils.debugLog("Clicked category");
                Thread.sleep(500);
                InventoryUtils.waitForItemClick(10, "Buy Instantly", 11, "Booster Cookie");
                LogUtils.debugLog("Clicked cookie");
                Thread.sleep(500);
                InventoryUtils.waitForItemClick(10, "Buy only", 10, "Buy Instantly");
                LogUtils.debugLog("Clicked buy instantly");
                Thread.sleep(500);
                InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 10);
                LogUtils.debugLog("Clicked on buy cookie");
                while (InventoryUtils.findItemInventory("Booster Cookie") == -1 && InventoryUtils.findItemInventory("Booster Cookie") < 36) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    Thread.sleep(100);
                }
                LogUtils.debugLog("Found bought cookie in inventory");
                mc.thePlayer.closeScreen();
                while (mc.currentScreen instanceof GuiContainer) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    LogUtils.debugLog("Waiting for bazaar to close");
                    Thread.sleep(100);
                }
                AngleUtils.smoothRotateClockwise(180, 0.8f);
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, InventoryUtils.findItemInventory("Booster Cookie"));
                Thread.sleep(300);
                InventoryUtils.clickWindow(mc.thePlayer.inventoryContainer.windowId, 42);
                Thread.sleep(1000);
                mc.thePlayer.inventory.currentItem = 6;
                while (!(mc.currentScreen instanceof GuiContainer)) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    KeyBinding.onTick(PlayerUtils.keybindUseItem);
                    Thread.sleep(100);
                }
                LogUtils.debugLog("Opened cookie consume menu");
                while (InventoryUtils.findItemInventory("Booster Cookie") != -1 || !FarmHelper.cookie) {
                    if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                    InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, 11);
                    Utils.ExecuteRunnable(checkFooter);
                    Thread.sleep(100);
                }
                LogUtils.debugLog("Consumed cookie!");
                mc.thePlayer.closeScreen();
                Thread.sleep(100);
                mc.thePlayer.inventory.currentItem = hoeSlot;
                Utils.ExecuteRunnable(checkFooter);
            } else {
                LogUtils.debugLog("Inventory full! Cannot buy Cookie, turning off Auto Cookie for this session");
                buying = false;
                MiscConfig.autoCookie = false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static boolean jacobExceeded() {
        for (String line : Utils.getSidebarLines()) {
            String cleanedLine = Utils.cleanSB(line);
            if (cleanedLine.contains("Nether Wart")) {
                return getJacobCounter() > JacobConfig.netherWartCap;
            } else if (cleanedLine.contains("Mushroom")) {
                return getJacobCounter() > JacobConfig.mushroomCap;
            } else if (cleanedLine.contains("Carrot")) {
                return getJacobCounter() > JacobConfig.carrotCap;
            } else if (cleanedLine.contains("Potato")) {
                return getJacobCounter() > JacobConfig.potatoCap;
            } else if (cleanedLine.contains("Wheat")) {
                return getJacobCounter() > JacobConfig.wheatCap;
            }
        }
        return false;
    }

    public static int getJacobCounter() {
        for (String line : Utils.getSidebarLines()) {
            String cleanedLine = Utils.cleanSB(line);
            if (cleanedLine.contains("with")) {
                return Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
            }
        }
        return 0;
    }

    public static long getJacobEnd() {
        Pattern pattern = Pattern.compile("([0-9]|[1-2][0-9])m([0-9]|[1-5][0-9])s");
        for (String line : Utils.getSidebarLines()) {
            String cleanedLine = Utils.cleanSB(line);
            Matcher matcher = pattern.matcher(cleanedLine);
            if (matcher.find()) {
                LogUtils.debugLog("Jacob remaining time: " + matcher.group(1) + "m" + matcher.group(2) + "s");
                LogUtils.webhookLog("Reached jacob threshold - Resuming in " + matcher.group(1) + "m" + matcher.group(2) + "s");
                return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Long.parseLong(matcher.group(1))) + TimeUnit.SECONDS.toMillis(Long.parseLong(matcher.group(2)));
            }
        }
        LogUtils.debugLog("Failed to get Jacob remaining time");
        return 0;
    }

    public static direction calculateDirection() {
        if (FarmConfig.farmType == FarmEnum.LAYERED) {
            for (int i = 1; i < 180; i++) {
                if (!isWalkable(BlockUtils.getRightBlock(0, i))) {
                    if (isWalkable(BlockUtils.getRightColBlock(i - 1))) {
                        return direction.RIGHT;
                    } else {
                        LogUtils.debugFullLog("Failed right - " + BlockUtils.getRightColBlock(i - 1));
                        return direction.LEFT;
                    }
                } else if (!isWalkable(BlockUtils.getLeftBlock(0, i))) {
                    if (isWalkable(BlockUtils.getLeftColBlock(i - 1))) {
                        return direction.LEFT;
                    } else {
                        LogUtils.debugFullLog("Failed left - " + BlockUtils.getLeftColBlock(i - 1));
                        return direction.RIGHT;
                    }
                }
            }
            return direction.NONE;
        } else {
            for (int i = 0; i < 180; i++) {
                if (isWalkable(BlockUtils.getBlockAround(i, 0, -1))) {
                    return direction.RIGHT;
                }
                if (!isWalkable(BlockUtils.getBlockAround(i, 0, 0)))
                    break;

            }
            for (int i = 0; i > -180; i--) {
                if (isWalkable(BlockUtils.getBlockAround(i, 0, -1))) {
                    return direction.LEFT;
                }
                if (!isWalkable(BlockUtils.getBlockAround(i, 0, 0)))
                    break;

            }
        }
        return direction.NONE;
    }

    public static location getLocation() {
        // if (System.currentTimeMillis() % 1000 < 20) {
        if (Utils.getSidebarLines().size() == 0) {
            crouched = false;
            if (BlockUtils.countCarpet() > 0) {
                return location.LIMBO;
            }
            return location.TELEPORTING;
        }
        if (currentLocation == location.LIMBO) {
            Utils.ExecuteRunnable(tpReset);
        }
        for (String line : Utils.getSidebarLines()) {
            String cleanedLine = Utils.cleanSB(line);
            if (cleanedLine.contains("Village")) {
                crouched = false;
                return location.HUB;
            } else if (cleanedLine.contains("Island")) {
                return location.ISLAND;
            }
        }

        crouched = false;
        if (Utils.getScoreboardDisplayName(1).contains("SKYBLOCK")) {
            return location.TELEPORTING;
        } else {
            return location.LOBBY;
        }
        // }
        // return currentLocation;
    }

    public static void stuckFrequency() {
        if (System.currentTimeMillis() - lastStuck < 30000) {
            stuckCount++;
        } else {
            stuckCount = 1;
        }
        if (stuckCount >= 3) {
            LogUtils.debugLog("Stuck 3 times in succession - Going to lobby");
            LogUtils.webhookLog("Stuck 3 times in succession - Going to lobby");
            stuckCount = 1;
            teleporting = false;
            stuck = false;
            setStuckCooldown(5);
            mc.thePlayer.sendChatMessage("/lobby");
        }
        lastStuck = System.currentTimeMillis();
    }

    public static void setStuckCooldown(int seconds) {
        stuckCooldown = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
    }

    public static void toggle() {
        mc.thePlayer.closeScreen();
        if (enabled) {
            enabled = false;
            LogUtils.scriptLog("Stopped script");
            LogUtils.webhookLog("Stopped script");
            updateKeys(false, false, false, false, false, false);
            Utils.resetExecutor();
            mc.thePlayer.closeScreen();
        } else {
            enabled = true;
            initialize();
        }
        openedGUI = false;
    }
}