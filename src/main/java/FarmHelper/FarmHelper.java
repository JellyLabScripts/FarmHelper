package FarmHelper;

import FarmHelper.config.AngleEnum;
import FarmHelper.config.Config;
import FarmHelper.config.CropEnum;
import FarmHelper.gui.GUI;
import FarmHelper.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper {
    public static final String MODID = "nwmath";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "1.0";

    Minecraft mc = Minecraft.getMinecraft();

    enum direction {
        RIGHT,
        LEFT,
        NONE
    }

    enum location {
        ISLAND,
        HUB,
        LOBBY
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
    public static double cacheAverageAge;
    public static BlockPos cachePos;
    public static location currentLocation;
    public static direction lastDirection;
    public static Map<CropEnum, Block> cropBlockStates = new HashMap<>();
    public static Map<CropEnum, PropertyInteger> cropAgeRefs = new HashMap<>();

    public int playerYaw;
    public int keybindA = mc.gameSettings.keyBindLeft.getKeyCode();
    public int keybindD = mc.gameSettings.keyBindRight.getKeyCode();
    public int keybindW = mc.gameSettings.keyBindForward.getKeyCode();
    public int keybindS = mc.gameSettings.keyBindBack.getKeyCode();
    public int keybindAttack = mc.gameSettings.keyBindAttack.getKeyCode();
    public int keybindUseItem = mc.gameSettings.keyBindUseItem.getKeyCode();
    public int keyBindSpace = mc.gameSettings.keyBindJump.getKeyCode();
    public int keyBindShift = mc.gameSettings.keyBindSneak.getKeyCode();
    static KeyBinding[] customKeyBinds = new KeyBinding[2];

    public double deltaX;
    public double deltaY;
    public double deltaZ;
    public double beforeX;
    public double beforeY;
    public double beforeZ;

    void initialize() {
        inTrenches = true;
        falling = false;
        teleportPad = false;
        fixTpStuckFlag = false;
        rotating = false;
        pushedOffSide = false;
        pushedOffFront = false;
        teleporting = false;
        newRow = false;
        stuck = false;
        cached = false;
        cacheAverageAge = -1;
        cachePos = null;
        lastDirection = direction.NONE;
        playerYaw = Utils.angleToValue(Config.Angle);

        deltaX = 100;
        deltaY = 100;
        deltaZ = 100;
        beforeX = mc.thePlayer.posX;
        beforeY = mc.thePlayer.posY;
        beforeZ = mc.thePlayer.posZ;

        Utils.ScheduleRunnable(updateDeltaChange, 2, TimeUnit.SECONDS);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        customKeyBinds[0] = new KeyBinding("Open GUI", Keyboard.KEY_RSHIFT, "FarmHelper");
        customKeyBinds[1] = new KeyBinding("Toggle script", Keyboard.KEY_GRAVE, "FarmHelper");
        ClientRegistry.registerKeyBinding(customKeyBinds[0]);
        ClientRegistry.registerKeyBinding(customKeyBinds[1]);
        try {
            Config.readConfig();
        } catch (Exception e) {
            Config.writeConfig();
        }

        cropBlockStates.put(CropEnum.WHEAT, Blocks.wheat);
        cropBlockStates.put(CropEnum.CARROT, Blocks.carrots);
        cropBlockStates.put(CropEnum.NETHERWART, Blocks.nether_wart);
        cropBlockStates.put(CropEnum.POTATO, Blocks.potatoes);

        cropAgeRefs.put(CropEnum.WHEAT, BlockCrops.AGE);
        cropAgeRefs.put(CropEnum.CARROT, BlockCarrot.AGE);
        cropAgeRefs.put(CropEnum.NETHERWART, BlockNetherWart.AGE);
        cropAgeRefs.put(CropEnum.POTATO, BlockPotato.AGE);

        enabled = false;
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FarmHelper());
        MinecraftForge.EVENT_BUS.register(new GUI());
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
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT && Config.debug && !Config.compactDebug) {
            mc.fontRendererObj.drawString("dx: " + Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 96, -1);
            mc.fontRendererObj.drawString("dz: " + Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 84, -1);

            mc.fontRendererObj.drawString("X Vector: " + Utils.getUnitX(), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 72, -1);
            mc.fontRendererObj.drawString("Z Vector: " + Utils.getUnitZ(), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 60, -1);

            mc.fontRendererObj.drawString("KeyBindW: " + (mc.gameSettings.keyBindForward.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 48, -1);
            mc.fontRendererObj.drawString("KeyBindS: " + (mc.gameSettings.keyBindBack.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 36, -1);
            mc.fontRendererObj.drawString("KeyBindA: " + (mc.gameSettings.keyBindLeft.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 24, -1);
            mc.fontRendererObj.drawString("KeyBindD: " + (mc.gameSettings.keyBindRight.isKeyDown() ? "Pressed" : "Not pressed"), 4, new ScaledResolution(mc).getScaledHeight() - 140 - 12, -1);

            mc.fontRendererObj.drawString(Utils.getFrontBlock() + " " + Utils.getBackBlock().toString() + " " + Utils.getRightBlock().toString() + " " + Utils.getLeftBlock().toString(), 4, new ScaledResolution(mc).getScaledHeight() - 20, -1);
        }
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event){
        if (customKeyBinds[0].isPressed()) {
            openedGUI = true;
            mc.displayGuiScreen(new GUI());
        }
        if (customKeyBinds[1].isPressed()) {
            if (!enabled) {
                Utils.scriptLog("Starting script", EnumChatFormatting.GREEN);
                Utils.configLog();
            }
            toggle();
        }
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (enabled) {
            if (message.contains("You were spawned in Limbo")) {
                Utils.debugLog("Spawned in limbo");
            }
            if (message.contains("You are already playing")) {
                Utils.ScheduleRunnable(tpReset, 2, TimeUnit.SECONDS);
            }
            if (message.contains("Cannot join SkyBlock") || message.contains("Please don't spam the command!")) {
                Utils.debugLog("Failed to teleport to SkyBlock");
                Utils.ScheduleRunnable(tpReset, 4, TimeUnit.SECONDS);
            }
            if (message.contains("DYNAMIC") || message.contains("Couldn't warp you")) {
                Utils.debugLog("Failed to teleport to island");
                Utils.ScheduleRunnable(tpReset, 2, TimeUnit.SECONDS);
            }
            if (message.contains("You cannot join SkyBlock from here!")) {
                teleporting = false;
            }
            if (message.contains("SkyBlock Lobby")) {
                Utils.debugLog("Island reset - Going to hub");
                teleporting = false;
                mc.thePlayer.sendChatMessage("/hub");
            }
            if (message.contains("Warping you to your SkyBlock island...") || message.contains("Welcome to Hypixel SkyBlock!")) {
                Utils.debugLog("Detected warp back to island");
                Utils.ScheduleRunnable(tpReset, 2, TimeUnit.SECONDS);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void OnTickPlayer(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (mc.theWorld != null && mc.thePlayer != null && enabled) {
            Block blockIn = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock();
            double dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
            double dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
            double dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);
            currentLocation = getLocation();

            if (currentLocation == location.LOBBY) {
                Utils.debugFullLog("Detected lobby");
                updateKeys(false, false, false, false, false, false);
                if (!teleporting) {
                    Utils.debugLog("Detected lobby");
                    Utils.debugLog("Attempting to teleport to skyblock");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    teleporting = true;
                }
            }
            else if (currentLocation == location.HUB) {
                Utils.debugFullLog("Detected hub");
                updateKeys(false, false, false, false, false, false);
                if (!teleporting) {
                    Utils.debugLog("Detected hub");
                    Utils.debugLog("Attempting to teleport to island");
                    mc.thePlayer.sendChatMessage("/warp home");
                    teleporting = true;
                }
            }
            else if (currentLocation == location.ISLAND && teleporting) {
                Utils.debugLog("Back to island, holding shift");
                updateKeys(false, false, false, false, false, true);
            }
            else if (currentLocation == location.ISLAND) {
                if (falling) {
                    // Stopped falling
                    if (dy == 0) {
                        Utils.debugFullLog("Changing layer - Landed - Doing 180 and switching back to trench state");
                        if (!rotating) {
                            updateKeys(false, false, false, false, false, false);
                            rotating = true;
                            Utils.ExecuteRunnable(changeLayer);
                        }
                    } else {
                        Utils.debugFullLog("Changing layer - In air - Wait");
                        updateKeys(false, false, false, false, false, false);
                    }
                } else if (teleportPad) {
                    // Not glitching up/down
                    if (dy == 0 && (mc.thePlayer.posY % 1) == 0.8125) {
                        if (isWalkable(Utils.getRightBlock(0.1875)) || isWalkable(Utils.getLeftBlock(0.1875))) {
                            Utils.debugFullLog("End of farm - At exit pad - Switch to trench state");
                            updateKeys(false, false, false, false, false, false);
                            teleportPad = false;
                        } else {
                            Utils.debugLog("Unknown case - id: 5");
                        }
                    }
                    // Glitching in exit pad
                    else if (!fixTpStuckFlag) {
                        Utils.debugLog("End of farm - Stuck in exit pad - Hold jump in separate thread");
                        updateKeys(false, false, false, false, false, false);
                        fixTpStuckFlag = true;
                        Utils.ExecuteRunnable(fixTpStuck);
                    }
                }
                // In trenches walking along the layer
                else if (inTrenches) {
                    Config.Angle = Math.round(Utils.get360RotationYaw() / 90) < 4 ? AngleEnum.values()[Math.round(Utils.get360RotationYaw() / 90)] : AngleEnum.A0;
                    playerYaw = Utils.angleToValue(Config.Angle);
                    if (Config.CropType.equals(CropEnum.NETHERWART)) {
                        mc.thePlayer.rotationPitch = 0;
                    } else {
                        mc.thePlayer.rotationPitch = 5;
                    }
                    Utils.hardRotate(playerYaw);
                    // Not falling
                    if (dy == 0) {
                        // If on solid block
                        if ((mc.thePlayer.posY % 1) == 0) {
                            // Cannot move forwards or backwards
                            if (!isWalkable(Utils.getFrontBlock()) && !isWalkable(Utils.getBackBlock())) {
                                cached = false;
                                if (newRow) {
                                    newRow = false;
                                    mc.thePlayer.sendChatMessage("/setspawn");
                                }
                                if (deltaX < 1 && deltaZ < 1) {
                                    Utils.debugLog("Start/Middle/End of row - Detected stuck");
                                    if (!stuck) {
                                        stuck = true;
                                        updateKeys(false, false, false, false, false, false);
                                        Utils.ExecuteRunnable(fixRowStuck);
                                    }
                                }
                                else if (isWalkable(Utils.getRightBlock()) && !isWalkable(Utils.getLeftBlock())) {
                                    Utils.debugLog("Start of farm - Go right");
                                    updateKeys(false, false, false, true, true, false);
                                } else if (isWalkable(Utils.getLeftBlock()) && !isWalkable(Utils.getRightBlock())) {
                                    Utils.debugLog("Start of farm - Go left");
                                    updateKeys(false, false, true, false, true, false);
                                } else if (isWalkable(Utils.getRightBlock()) && isWalkable(Utils.getLeftBlock())) {
                                    // Utils.debugLog("Middle of row - Calculating which direction to go");
                                    // Calculate if not done in last tick
                                    // if (lastDirection == direction.NONE) {
                                    if (true) {
                                        // Utils.debugLog("Middle of row - No direction last tick, recalculating");
                                        lastDirection = calculateDirection();
                                    }
                                    if (lastDirection == direction.RIGHT) {
                                        Utils.debugLog("Middle of row - Go right");
                                        updateKeys(false, false, false, true, true, false);
                                    } else if (lastDirection == direction.LEFT) {
                                        Utils.debugLog("Middle of row - Go left");
                                        updateKeys(false, false, true, false, true, false);
                                    } else {
                                        Utils.debugLog("Middle of row - Cannot calculate [Multiple > 180]");
                                        updateKeys(false, false, false, false, false, false);
                                    }
                                } else {
                                    Utils.debugLog("Unknown case - id: 4");
                                }
                            }

                            // Can go forwards but not backwards
                            else if (isWalkable(Utils.getFrontBlock()) && !isWalkable(Utils.getBackBlock())) {
                                Utils.debugLog("End of row - Switching to next");
                                newRow = true;
                                  if (!cached && Config.resync) {
                                    cached = true;
                                    Utils.ExecuteRunnable(cacheRowAge);
                                    Utils.ScheduleRunnable(checkDesync, 4, TimeUnit.SECONDS);
                                }
                                if (mc.gameSettings.keyBindForward.isKeyDown()) {
                                    Utils.debugFullLog("End of row - Start of col - Pushed off - Keep Going forwards");
                                    updateKeys(true, false, false, false, false, false);
                                } else if (pushedOffSide) {
                                    if (dx == 0 && dz == 0) {
                                        Utils.debugFullLog("End of row - Start of col - Pushed off - Stopped - Going forwards");
                                        updateKeys(true, false, false, false, false, false);
                                    } else {
                                        Utils.debugFullLog("End of row - Start of col - Pushed off - Waiting till stop");
                                        updateKeys(false, false, false, false, false, false);
                                    }
                                } else {
                                    if (dx == 0 && dz == 0) {
                                        if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                                            pushedOffSide = true;
                                            if (!isWalkable(Utils.getRightBlock())) {
                                                Utils.debugFullLog("End of row - Start of col - Pushing off right edge");
                                                updateKeys(false, false, true, false, false, true);
                                            } else {
                                                Utils.debugFullLog("End of row - Start of col - Pushing off left edge");
                                                updateKeys(false, false, false, true, false, true);
                                            }
                                        } else {
                                            if (!isWalkable(Utils.getRightBlock())) {
                                                Utils.debugFullLog("End of row - Start of col - Going to edge");
                                                updateKeys(false, false, false, true, false, false);
                                            } else {
                                                Utils.debugFullLog("End of row - Start of col - Going to edge");
                                                updateKeys(false, false, true, false, false, false);
                                            }
                                        }
                                    } else {
                                        Utils.debugFullLog("Unknown case id: 7");
                                    }
                                }
                            }

                            // Can go forwards and backwards
                            else if (isWalkable(Utils.getFrontBlock()) && isWalkable(Utils.getBackBlock())) {
                                pushedOffSide = false;
                                if (Utils.getUnitX() != 0) {
                                    // if (1.4 * dx >= distanceToTurn() || !mc.gameSettings.keyBindForward.isKeyDown()) {
                                    if (false) {
                                        Utils.debugFullLog("End of row - Middle of col - Close to turn, coasting");
                                        updateKeys(false, false, false, false, false, false);
                                    } else {
                                        Utils.debugFullLog("End of row - Middle of col - Go forwards");
                                        updateKeys(true, false, false, false, false, false);
                                    }
                                } else {
                                    // if (1.4 * dz >= distanceToTurn() || !mc.gameSettings.keyBindForward.isKeyDown()) {
                                    if (false) {
                                        Utils.debugFullLog("End of row - Middle of col - Close to turn, coasting");
                                        updateKeys(false, false, false, false, false, false);
                                    } else {
                                        Utils.debugFullLog("End of row - Middle of col - Go forwards");
                                        updateKeys(true, false, false, false, false, false);
                                    }
                                }
                            }

                            // Can go backwards but not forwards
//                            else if (isWalkable(Utils.getBackBlock()) && !isWalkable(Utils.getFrontBlock())) {
//                                distanceToTurn();
//                                if (isWalkable(Utils.getLeftBlock()) || isWalkable(Utils.getRightBlock())) {
//                                    if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
//                                        pushedOffFront = false;
//                                        if (isWalkable(Utils.getLeftBlock())) {
//                                            Utils.debugLog("End of row - End of col - Keep going left");
//                                            updateKeys(false, false, true, false, true, false);
//                                        } else if (isWalkable(Utils.getRightBlock())) {
//                                            Utils.debugLog("End of row - End of col - Keep going right");
//                                            updateKeys(false, false, false, true, true, false);
//                                        }
//                                    } else if (dx == 0 && dz == 0) {
//                                        if (isWalkable(Utils.getLeftBlock())) {
//                                            Utils.debugLog("End of row - End of col - Aligned - Go left");
//                                            updateKeys(false, false, true, false, true, false);
//                                        } else if (isWalkable(Utils.getRightBlock())) {
//                                            Utils.debugLog("End of row - End of col - Aligned - Go right");
//                                            updateKeys(false, false, false, true, true, false);
//                                        }
//                                    } else {
//                                        if (Utils.getUnitX() != 0) {
//                                            if (1.4 * dx >= distanceToTurn() || !mc.gameSettings.keyBindForward.isKeyDown()) {
//                                                Utils.debugLog("End of row - End of col - Close to turn, coasting");
//                                                updateKeys(false, false, false, false, false, false);
//                                            } else {
//                                                Utils.debugLog("End of row - End of col - Go forwards");
//                                                updateKeys(true, false, false, false, false, false);
//                                            }
//                                        } else {
//                                            if (1.4 * dz >= distanceToTurn() || !mc.gameSettings.keyBindForward.isKeyDown()) {
//                                                Utils.debugLog("End of row - End of col - Close to turn, coasting");
//                                                updateKeys(false, false, false, false, false, false);
//                                            } else {
//                                                Utils.debugLog("End of row - End of col - Go forwards");
//                                                updateKeys(true, false, false, false, false, false);
//                                            }
//                                        }
//                                    }
//                                }
//                                // Can't go anywhere, end of layer
//                                else if (Utils.getBelowBlock() == Blocks.end_portal_frame) {
//                                    Utils.debugLog("End of farm - Above entrance tp");
//                                    updateKeys(false, false, false, false, false, false);
//                                    teleportPad = true;
//                                } else if (Utils.getBelowBlock() == Blocks.air) {
//                                    Utils.debugLog("Changing layer - about to fall");
//                                    updateKeys(true, false, false, false, false, false);
//                                    falling = true;
//                                }
//                            }
                        else if (isWalkable(Utils.getBackBlock()) && !isWalkable(Utils.getFrontBlock())) {
                            distanceToTurn();
                            if (isWalkable(Utils.getLeftBlock()) || isWalkable(Utils.getRightBlock())) {
                                if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                                    pushedOffFront = false;
                                    if (isWalkable(Utils.getLeftBlock())) {
                                        Utils.debugFullLog("End of row - End of col - Keep going left");
                                        updateKeys(false, false, true, false, true, false);
                                    }
                                    else if (isWalkable(Utils.getRightBlock())) {
                                        Utils.debugFullLog("End of row - End of col - Keep going right");
                                        updateKeys(false, false, false, true, true, false);
                                    }
                                }
                                else if (pushedOffFront) {
                                    if (dx == 0 && dz == 0) {
                                        if (isWalkable(Utils.getLeftBlock())) {
                                            Utils.debugLog("End of row - End of col - Pushed - Go left");
                                            updateKeys(false, false, true, false, true, false);
                                        }
                                        else if (isWalkable(Utils.getRightBlock())) {
                                            Utils.debugLog("End of row - End of col - Pushed - Go right");
                                            updateKeys(false, false, false, true, true, false);
                                        }
                                    } else {
                                        Utils.debugFullLog("End of row - Start of col - Pushed off - Waiting till stop");
                                        updateKeys(false, false, false, false, false, true);
                                    }
                                }
                                else if (dx == 0 && dz == 0) {
                                    if (mc.gameSettings.keyBindForward.isKeyDown()) {

                                        Utils.debugFullLog("End of row - End of col - Edge - Pushing off");
                                        pushedOffFront = true;
                                        updateKeys(false, true, false, false, false, true);
                                    } else {
                                        Utils.debugFullLog("End of row - End of col - Maybe not edge - Going forwards");
                                        updateKeys(true, false, false, false, true, false);
                                    }
                                } else {
                                    Utils.debugFullLog("End of row - End of col - Not at edge - Going forwards");
                                    updateKeys(true, false, false, false, true, false);
                                }
                            }
                            // Can't go anywhere, end of layer
                            else if (Utils.getBelowBlock() == Blocks.end_portal_frame) {
                                Utils.debugLog("End of farm - Above entrance tp");
                                updateKeys(false, false, false, false, false, false);
                                teleportPad = true;
                            } else if (Utils.getBelowBlock() == Blocks.air) {
                                Utils.debugLog("Changing layer - About to fall");
                                updateKeys(true, false, false, false, false, false);
                                falling = true;
                            }
                        }
                        }
                        // Standing on tp pad
                        else if ((mc.thePlayer.posY % 1) == 0.8125) {
                            if (!isWalkable(Utils.getFrontBlock(0.1875))) {
                                // Cannot go left or right
                                if (!isWalkable(Utils.getLeftBlock(0.1875)) && !isWalkable(Utils.getRightBlock(0.1875))) {
                                    Utils.debugLog("End of farm - On entrance pad, wait until on exit pad");
                                    updateKeys(false, false, false, false, false, false);
                                    teleportPad = true;
                                } else if (isWalkable(Utils.getRightBlock(0.1875))) {
                                    Utils.debugLog("Start of farm - At exit pad - Go right");
                                    updateKeys(false, false, false, true, true, false);
                                } else if (isWalkable(Utils.getLeftBlock(0.1875))) {
                                    Utils.debugLog("Start of farm - At exit pad - Go left");
                                    updateKeys(false, false, true, false, true, false);
                                } else {
                                    Utils.debugLog("Unknown case - id: 3");
                                }
                            }
                        }
                    } else if (blockIn == Blocks.air && dy > 0.3) {
                        Utils.debugFullLog("Changing layer - falling, wait till land - dy:" + dy + ", y: " + mc.thePlayer.posY + ", prevY: " + mc.thePlayer.lastTickPosY);
                        updateKeys(false, false, false, false, false, false);
                        falling = true;
                    } else {
                        // Potentially make it send false all keys for this case
                        Utils.debugLog("Unknown case - id: 2");
                    }
                } else {
                    Utils.debugLog("Unknown case - id: 1");
                }
            }
        }
    }

    // Runnables
    Runnable updateDeltaChange = new Runnable() {
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

    Runnable fixTpStuck = () -> {
        try {
            KeyBinding.setKeyBindState(keyBindSpace, true);
            Thread.sleep(1500);
            KeyBinding.setKeyBindState(keyBindSpace, false);
            Thread.sleep(1500);
            fixTpStuckFlag = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    Runnable fixRowStuck = () -> {
        try {
            Thread.sleep(20);
            KeyBinding.setKeyBindState(keybindS, true);
            Thread.sleep(1000);
            KeyBinding.setKeyBindState(keybindS, false);
            Thread.sleep(500);
            KeyBinding.setKeyBindState(keybindW, true);
            Thread.sleep(1000);
            KeyBinding.setKeyBindState(keybindW, false);
            Thread.sleep(500);
            deltaX = 100;
            deltaZ = 100;
            stuck = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    Runnable changeLayer = () -> {
        try {
            Thread.sleep(250);
            Config.Angle = Config.Angle.ordinal() < 2 ? AngleEnum.values()[Config.Angle.ordinal() + 2] : AngleEnum.values()[Config.Angle.ordinal() - 2];
            playerYaw = Utils.angleToValue(Config.Angle);
            Utils.smoothRotateClockwise(180);
            Thread.sleep(2000);
            KeyBinding.setKeyBindState(keybindW, true);
            Thread.sleep(750);
            KeyBinding.setKeyBindState(keybindW, false);
            deltaX = 100;
            deltaZ = 100;
            rotating = false;
            falling = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    Runnable cacheRowAge = () -> {
        for (int i = 1; i < 10; i++) {
            BlockPos pos = new BlockPos(
                mc.thePlayer.posX + (i * Utils.getUnitX()),
                mc.thePlayer.posY + 1,
                mc.thePlayer.posZ + (i * Utils.getUnitZ())
            );
            Block checkBlock = mc.theWorld.getBlockState(pos).getBlock();
            if (checkBlock == cropBlockStates.get(Config.CropType)) {
                Utils.debugFullLog("cacheRowAge - Found row - Calculating age - Pos: " + pos);
                cachePos = pos;
                cacheAverageAge = getAverageAge(pos);
                Utils.debugLog("Row Age - Calculated age: " + cacheAverageAge);
                return;
            }
        }
        Utils.debugLog("Row Age - No row found (Maybe changing layer?)");
        cachePos = null;
        cacheAverageAge = -1;
    };

    Runnable checkDesync = () -> {
        Utils.debugFullLog("checkDesync - Enter");
        if (cachePos == null || cacheAverageAge == -1) {
            Utils.debugLog("Desync - No cache (Wrong crop selected?)");
        }
        else if (cacheAverageAge >= 2) {
            double newAvg = getAverageAge(cachePos);
            Utils.debugLog("Desync - Old: " + newAvg + ", New: " + cacheAverageAge);
            if (Math.abs(newAvg - cacheAverageAge) < 0.5) {
                Utils.debugLog("Desync detected, going to hub");
                teleporting = false;
                mc.thePlayer.sendChatMessage("/hub");
            } else {
                Utils.debugLog("No desync detected");
            }
        }
        else {
            Utils.debugLog("Desync - Average age too low");
        }
    };

    Runnable tpReset = () -> {
        teleporting = false;
    };

    void updateKeys(boolean wBool, boolean sBool, boolean aBool, boolean dBool, boolean atkBool, boolean shiftBool) {
        // Commented due to direction push back interfering with last direction
        // if (aBool) {
        //     lastDirection = direction.LEFT;
        // }
        // else if (dBool) {
        //     lastDirection = direction.RIGHT;
        // }
        // else {
        //     lastDirection = direction.NONE;
        // }
        KeyBinding.setKeyBindState(keybindW, wBool);
        KeyBinding.setKeyBindState(keybindS, sBool);
        KeyBinding.setKeyBindState(keybindA, aBool);
        KeyBinding.setKeyBindState(keybindD, dBool);
        KeyBinding.setKeyBindState(keybindAttack, atkBool);
        KeyBinding.setKeyBindState(keyBindShift, shiftBool);
    }

    boolean isWalkable(Block block) {
        return block == Blocks.air || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.dark_oak_fence_gate || block == Blocks.acacia_fence_gate || block == Blocks.birch_fence_gate || block == Blocks.oak_fence_gate || block == Blocks.jungle_fence_gate || block == Blocks.spruce_fence_gate;
    }

    direction calculateDirection() {
        for (int i = 1; i < 180; i++) {
            if (!isWalkable(Utils.getRightBlock(0, i))) {
                if (isWalkable(Utils.getRightColBlock(i - 1))) {
                    return direction.RIGHT;
                } else {
                    return direction.LEFT;
                }
            } else if (!isWalkable(Utils.getLeftBlock(0, i))) {
                if (isWalkable(Utils.getLeftColBlock(i - 1))) {
                    return direction.LEFT;
                } else {
                    return direction.RIGHT;
                }
            }
        }
        return direction.NONE;
    }

    location getLocation() {
        for (String line : Utils.getSidebarLines()) {
            String cleanedLine = Utils.cleanSB(line);
            if (cleanedLine.contains("Village")) {
                return location.HUB;
            } else if (cleanedLine.contains("Island")) {
                return location.ISLAND;
            }
        }
        return location.LOBBY;
    }

    double getAverageAge(BlockPos pos) {
        Utils.debugFullLog("getAverageAge - enter");
        IBlockState current;
        double total = 0;
        double count = 0;
        do {
            current = mc.theWorld.getBlockState(new BlockPos(pos.getX() + (count * Utils.getUnitX()), pos.getY(), pos.getZ() + (count * Utils.getUnitZ())));
            if (current.getBlock() == cropBlockStates.get(Config.CropType)) {
                Utils.debugFullLog("getAverageAge - current: " + current.getBlock());
                Utils.debugFullLog("getAverageAge - age: " + current.getValue(cropAgeRefs.get(Config.CropType)));
                total += current.getValue(cropAgeRefs.get(Config.CropType));
                count += 1;
            }
        } while (current.getBlock() == cropBlockStates.get(Config.CropType));
        return total/count;
    }

    double distanceToTurn() {
        // Find end coord
        double distance = 1000;
//        for (int i = 1; i < 9; i++) {
//            if (!isWalkable(Utils.getFrontBlock(0, i))) {
//                if (Utils.getFrontBlock(-1, i - 1) == Blocks.air || Utils.getFrontBlock(-1, i - 1) == Blocks.end_portal_frame) {
//                    return 1000;
//                }
//                else if (Utils.getUnitX() != 0) {
//                    double midpoint = ((int) Math.floor(mc.thePlayer.posX)) + (Utils.getUnitX() * (i - 1)) + 0.5;
//                    distance = Math.abs(mc.thePlayer.posX - midpoint);
//
//                } else {
//                    double midpoint = ((int) Math.floor(mc.thePlayer.posZ)) + (Utils.getUnitZ() * (i - 1)) + 0.5;
//                    distance = Math.abs(mc.thePlayer.posZ - midpoint);
//                }
//                Utils.debugLog("Distance to stop: " + distance);
//                return distance;
//            }
//        }
        return distance;
    }

    void toggle(){
        mc.thePlayer.closeScreen();
        if (enabled) {
            Utils.scriptLog("Stopped script");
            updateKeys(false, false, false, false, false, false);
        } else {
            initialize();
        }
         enabled = !enabled;
         openedGUI = false;
    }
}