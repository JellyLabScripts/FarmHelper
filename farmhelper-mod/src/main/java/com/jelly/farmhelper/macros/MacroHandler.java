package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.VerticalMacroEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.config.Config.CropEnum;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.features.VisitorsMacro;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
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

    public static Macro currentMacro;
    public static boolean isMacroing;

    public static SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public static SShapeCropMacro sShapeCropMacro = new SShapeCropMacro();
    public static VerticalCropMacro verticalCropMacro = new VerticalCropMacro();
    public static CocoaBeanMacro cocoaBeanMacro = new CocoaBeanMacro();
    public static CocoaBeanRGMacro cocoaBeanRGMacro = new CocoaBeanRGMacro();
    public static MushroomMacro mushroomMacro = new MushroomMacro();

    private final Rotation rotation = new Rotation();
    public static long startTime = 0;
    public static boolean randomizing = false;
    public static long startCounter = 0;
    public static boolean startingUp;
    public static CropEnum crop;

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
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
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null) {
            currentMacro.onLastRender();
        }
        if (FarmHelper.config.highlightRewarp) {Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 2000) / 2000, 1, 1);
            Color chromaLowerAlpha = new Color(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 120);

            for (Rewarp rewarp : FarmHelper.config.rewarpList) {
                RenderUtils.drawBlockBox(new BlockPos(rewarp.x, rewarp.y, rewarp.z), chromaLowerAlpha);
            }
        }
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent event) {
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null) {
            currentMacro.onOverlayRender(event);
        }
    }

    @SubscribeEvent
    public void onPacketReceived(ReceivePacketEvent event) {
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null) {
            currentMacro.onPacketReceived(event);
        }
    }


    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        Keyboard.enableRepeatEvents(false);
        if (FarmHelper.config.toggleMacro.isActive()) {
            toggleMacro();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_J)) {
            //debug
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;


        if (isMacroing) {
            if (FarmHelper.tickCount == 1) {
                LogUtils.webhookStatus();

                StatusUtils.updateStateString();
            }
            if (currentMacro != null && currentMacro.enabled) {
                if (!VisitorsMacro.isEnabled()) {
                    currentMacro.onTick();
                }
            }
        }

    }
    public static void toggleMacro(){
        Failsafe.restartAfterFailsafeCooldown.reset();
        if(Failsafe.emergency) {
            Failsafe.stopAllFailsafeThreads();
            disableMacro();
            LogUtils.scriptLog("Do not restart macro too soon and farm yourself. The staff might still be spectating for 1-2 minutes");
        } else if (isMacroing) {
            disableMacro();
        } else {
            enableMacro();
        }
    }
    public static void enableMacro() {
        if(!FarmHelper.config.macroType) {
            currentMacro = verticalCropMacro;
        } else {
            if (FarmHelper.config.SShapeMacroType == SMacroEnum.SUGAR_CANE.ordinal()) {
                currentMacro = sugarcaneMacro;
            } else if (FarmHelper.config.SShapeMacroType == SMacroEnum.COCOA_BEANS.ordinal()) {
                currentMacro = cocoaBeanMacro;
            } else if (FarmHelper.config.SShapeMacroType == SMacroEnum.COCOA_BEANS_RG.ordinal()) {
                currentMacro = cocoaBeanRGMacro;
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

        LogUtils.scriptLog("Starting script");
        LogUtils.webhookLog("Starting script");
        if (FarmHelper.config.enableAutoSell) LogUtils.scriptLog("Auto Sell is in BETA, lock important slots just in case");
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
        if (FarmHelper.config.enableScheduler) Scheduler.start();
        if (FarmHelper.config.visitorsMacro && FarmHelper.config.onlyAcceptProfitableVisitors) LogUtils.scriptLog("Macro will only accept offers containing any of these products: " + String.join(", ", VisitorsMacro.profitRewards));

        startTime = System.currentTimeMillis();
        ProfitCalculator.resetProfit();
        ProfitCalculator.startingPurse = -1;

        Failsafe.jacobWait.reset();
        startCounter = PlayerUtils.getCounter();
        enableCurrentMacro();
    }

    public static void disableMacro() {
        isMacroing = false;
        if (currentMacro != null)
            currentMacro.savedLastState = false;
        disableCurrentMacro();
        LogUtils.scriptLog("Disabling script");
        LogUtils.webhookLog("Disabling script");
        UngrabUtils.regrabMouse();
        StatusUtils.updateStateString();
        VisitorsMacro.stopMacro();
    }

    public static void disableCurrentMacro() {
        disableCurrentMacro(false);
    }

    public static void disableCurrentMacro(boolean saveLastState) {
        LogUtils.debugLog("Disabling current macro");
        if (currentMacro != null && currentMacro.enabled) {
            if (saveLastState) {
                currentMacro.saveLastStateBeforeDisable();
            } else {
                currentMacro.toggle();
            }
        }
    }

    public static void enableCurrentMacro() {
        if (!currentMacro.enabled && !startingUp) {
            mc.inGameHasFocus = true;
            mc.displayGuiScreen(null);
            startingUp = true;
            CropUtils.itemChangedByStaff = false;
            KeyBindUtils.updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            new Thread(startCurrent).start();
        }
    }

    static Runnable startCurrent = () -> {
        try {
            Thread.sleep(300);
            KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
            if (isMacroing) currentMacro.toggle();
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
        LogUtils.scriptLog("Can't detect crop type, defaulting to wheat", EnumChatFormatting.RED);
        return CropEnum.WHEAT;
    }
}