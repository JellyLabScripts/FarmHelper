package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.config.Config.CropEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.*;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import java.awt.*;


public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Macro<?> currentMacro;
    public static boolean isMacroing;

    public static SugarcaneMacroNew sugarcaneMacro = new SugarcaneMacroNew();
    public static SShapeCropMacroNew sShapeCropMacro = new SShapeCropMacroNew();
    public static VerticalCropMacroNew verticalCropMacro = new VerticalCropMacroNew();
    public static CocoaBeanMacroNew cocoaBeanMacro = new CocoaBeanMacroNew();
    public static MushroomMacroNew mushroomMacro = new MushroomMacroNew();

    private final Rotation rotation = new Rotation();
    public static long startTime = 0;
    public static boolean randomizing = false;
    public static long startCounter = 0;
    public static boolean startingUp;
    public static CropEnum crop;

    @SubscribeEvent(receiveCanceled = true)
    public void onChatMessageReceived(ClientChatReceivedEvent e) {
        if(isMacroing) {
            if(e.message == null)
                return;
        }
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null && e.message != null) {
            currentMacro.onChatMessageReceived(e.message.getUnformattedText());
        }
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
        }
        if (mc.thePlayer == null && mc.theWorld == null) return;
        if (currentMacro != null && currentMacro.enabled) {
            currentMacro.onLastRender();
        }
        if (FarmHelper.config.highlightRewarp && Config.rewarpList != null && LocationUtils.currentIsland == LocationUtils.Island.GARDEN) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 2000) / 2000, 1, 1);
            Color chromaLowerAlpha = new Color(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 120);

            for (Rewarp rewarp : Config.rewarpList) {
                RenderUtils.drawBlockBox(new BlockPos(rewarp.x, rewarp.y, rewarp.z), chromaLowerAlpha);
            }
        }
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent event) {
        if (currentMacro != null && mc.thePlayer != null && mc.theWorld != null) {
            currentMacro.onOverlayRender(event);
        }
    }

    @SubscribeEvent
    public void onPacketReceived(ReceivePacketEvent event) {
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null) {
            currentMacro.onPacketReceived(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;


        if (isMacroing) {
            if (FarmHelper.tickCount == 1) {
                LogUtils.webhookStatus();

                StatusUtils.updateStateString();
            }
            if (currentMacro != null && currentMacro.enabled && LocationUtils.currentIsland == LocationUtils.Island.GARDEN) {
                if (!VisitorsMacro.isEnabled() && !PetSwapper.isEnabled()) {
                    currentMacro.onTick();
                }
            }
        }

    }

    public static void toggleMacro() {
        toggleMacro(false);
    }

    public static void toggleMacro(boolean force) {
        FailsafeNew.restartAfterFailsafeCooldown.reset();
        if (FailsafeNew.emergency) {
            FailsafeNew.resetFailsafes();
            disableMacro();
            LogUtils.sendWarning("Farm manually and DO NOT restart the macro too soon! The staff might still be spectating you for a while!");
        } else if (isMacroing || (VisitorsMacro.triggeredManually && VisitorsMacro.isEnabled())) {
            disableMacro();
        } else {
            if (VisitorsMacro.isPlayerInsideBarn()) {
                if (VisitorsMacro.isEnabled()) {
                    VisitorsMacro.stopMacro();
                } else if (VisitorsMacro.canEnableMacro(true)) {
                    VisitorsMacro.enableMacro(true);
                } else {
                    LogUtils.sendError("You are inside barn, but not on desk position. Can't run visitors macro!");
                }
            } else {
                enableMacro(force);
            }
        }
    }

    public static void enableMacro() {
        enableMacro(false);
    }

    public static void enableMacro(boolean force) {
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) {
            LogUtils.sendError("You must be in the garden to start the macro!");
            if (!force)
                return;
        }
        if (!FarmHelper.config.macroType) {
            currentMacro = verticalCropMacro;
        } else {
            if (FarmHelper.config.SShapeMacroType == SMacroEnum.SUGAR_CANE.ordinal()) {
                currentMacro = sugarcaneMacro;
            } else if (FarmHelper.config.SShapeMacroType == SMacroEnum.COCOA_BEANS.ordinal()) {
                currentMacro = cocoaBeanMacro;
            } else if (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() ||
                FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                currentMacro = mushroomMacro;
            }
            else {
                currentMacro = sShapeCropMacro;
            }
        }

        isMacroing = true;
        mc.thePlayer.closeScreen();

        LogUtils.sendSuccess("Starting script");
        LogUtils.webhookLog("Starting script");
        if (FarmHelper.config.enableAutoSell) LogUtils.sendWarning("Auto Sell is in BETA, lock important slots just in case");
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
        if (FarmHelper.config.enableScheduler) Scheduler.start();
        if (FarmHelper.config.visitorsMacro && FarmHelper.config.onlyAcceptProfitableVisitors) LogUtils.sendDebug("Visitors macro will only accept offers containing any of these products: " + String.join(", ", VisitorsMacro.profitRewards));
        if (FarmHelper.config.enablePetSwapper && GameState.inJacobContest() && !PetSwapper.hasPetChangedDuringThisContest) {
            PetSwapper.startMacro(false);
            PetSwapper.hasPetChangedDuringThisContest = true;
        } else if (FarmHelper.config.enablePetSwapper && !GameState.inJacobContest() && PetSwapper.hasPetChangedDuringThisContest) {
            PetSwapper.startMacro(true);
            PetSwapper.hasPetChangedDuringThisContest = false;
        }

        startTime = System.currentTimeMillis();
        ProfitCalculator.resetProfit();
        ProfitCalculator.startingPurse = -1;

//        startCounter = PlayerUtils.getCounter();
        enableCurrentMacro();
    }

    public static void disableMacro() {
        isMacroing = false;
        if (currentMacro != null)
            currentMacro.savedLastState = false;
        disableCurrentMacro();
        LogUtils.sendSuccess("Disabling script");
        LogUtils.webhookLog("Disabling script");
        UngrabUtils.regrabMouse();
        StatusUtils.updateStateString();
        VisitorsMacro.stopMacro();
        Autosell.disableOnly();
        PetSwapper.reset();
        FailsafeNew.resetFailsafes();
        Utils.disablePingAlert();
    }

    public static void disableCurrentMacro() {
        disableCurrentMacro(false);
    }

    public static void disableCurrentMacro(boolean saveLastState) {
        LogUtils.sendDebug("Disabling current macro");
        if (currentMacro != null && currentMacro.enabled) {
            if (saveLastState) {
                currentMacro.saveLastStateBeforeDisable();
            } else {
                currentMacro.enabled = false;
                currentMacro.onDisable();
            }
        }
    }

    public static void enableCurrentMacro() {
        if (currentMacro != null && !currentMacro.enabled && !startingUp) {
            mc.displayGuiScreen(null);
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
            startingUp = true;
            CropUtils.itemChangedByStaff = false;
            CropUtils.changeItemEveryClock.reset();
            KeyBindUtils.updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            if (currentMacro.savedLastState) {
                currentMacro.restoreState();
            }
            new Thread(startCurrent).start();
        }
    }

    static Runnable startCurrent = () -> {
        try {
            Thread.sleep(300);
            KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
            if (isMacroing) {
                currentMacro.enabled = true;
                currentMacro.onEnable();
            }
            startingUp = false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    };

    public static CropEnum getFarmingCrop() {
        Pair<Block, BlockPos> closestCrop = null;
        boolean foundCropUnderMouse = false;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block instanceof BlockCrops || block instanceof BlockReed || block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockMelon || block instanceof  BlockPumpkin || block instanceof BlockMushroom || block instanceof BlockCactus) {
                closestCrop = Pair.of(block, pos);
                foundCropUnderMouse = true;
            }
        }

        if (!foundCropUnderMouse) {
            for (int x = -3; x < 3; x++) {
                for (int y = -3; y < 3; y++) {
                    for (int z = 0; z < 3; z++) {
                        BlockPos pos = BlockUtils.getRelativeBlockPos(x, y, 1 + z,
                            ((FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) ||
                                (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.SUGAR_CANE.ordinal()) ?
                                AngleUtils.getClosestDiagonal() - 45 :
                                AngleUtils.getClosest()));
                        Block block = mc.theWorld.getBlockState(pos).getBlock();
                        if (!(block instanceof BlockCrops || block instanceof BlockReed || block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockMelon || block instanceof BlockPumpkin || block instanceof BlockMushroom || block instanceof BlockCactus))
                            continue;

                        if (closestCrop == null || mc.thePlayer.getPosition().distanceSq(pos.getX(), pos.getY(), pos.getZ()) < mc.thePlayer.getPosition().distanceSq(closestCrop.getRight().getX(), closestCrop.getRight().getY(), closestCrop.getRight().getZ())) {
                            closestCrop = Pair.of(block, pos);
                        }
                    }
                }
            }
        }

        if (closestCrop != null) {
            Block left = closestCrop.getLeft();
            if (left.equals(Blocks.wheat)) {
                return CropEnum.WHEAT;
            } else if (left.equals(Blocks.carrots)) {
                return CropEnum.CARROT;
            } else if (left.equals(Blocks.potatoes)) {
                return CropEnum.POTATO;
            } else if (left.equals(Blocks.nether_wart)) {
                return CropEnum.NETHER_WART;
            } else if (left.equals(Blocks.reeds)) {
                return CropEnum.SUGAR_CANE;
            } else if (left.equals(Blocks.cocoa)) {
                return CropEnum.COCOA_BEANS;
            } else if (left.equals(Blocks.melon_block)) {
                return CropEnum.MELON;
            } else if (left.equals(Blocks.pumpkin)) {
                return CropEnum.PUMPKIN;
            } else if (left.equals(Blocks.red_mushroom)) {
                return CropEnum.MUSHROOM;
            } else if (left.equals(Blocks.brown_mushroom)) {
                return CropEnum.MUSHROOM;
            } else if (left.equals(Blocks.cactus)) {
                return CropEnum.CACTUS;
            }
        }
        LogUtils.sendError("Can't detect crop type! Defaulting to wheat.");
        return CropEnum.WHEAT;
    }
}