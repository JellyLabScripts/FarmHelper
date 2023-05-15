package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;
import com.jelly.farmhelper.config.enums.MacroEnum;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
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
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;


public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Macro currentMacro;
    public static boolean isMacroing;

    public static SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public static SShapeCropMacro sShapeCropMacro = new SShapeCropMacro();
    public static VerticalCropMacro verticalCropMacro = new VerticalCropMacro();
    public static CocoaBeanMacro cocoaBeanMacro = new CocoaBeanMacro();
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
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
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
        if(FarmConfig.farmType == FarmEnum.VERTICAL) {
            if (FarmConfig.cropType == MacroEnum.MUSHROOM || FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) {
                currentMacro = mushroomMacro;
            } else {
                currentMacro = verticalCropMacro;
            }
        } else {
            if (FarmConfig.cropType == MacroEnum.SUGARCANE) {
                currentMacro = sugarcaneMacro;
            } else if (FarmConfig.cropType == MacroEnum.COCOABEANS) {
                currentMacro = cocoaBeanMacro;
            } else {
                currentMacro = sShapeCropMacro;
            }
        }

        isMacroing = true;
        mc.thePlayer.closeScreen();

        LogUtils.scriptLog("Starting script");
        LogUtils.webhookLog("Starting script");
        if (AutoSellConfig.autoSell) LogUtils.scriptLog("Auto Sell is in BETA, lock important slots just in case");
        if (MiscConfig.ungrab) UngrabUtils.ungrabMouse();
        if (SchedulerConfig.scheduler) Scheduler.start();

        startTime = System.currentTimeMillis();
        ProfitCalculator.resetProfit();

        Failsafe.jacobWait.reset();
        startCounter = PlayerUtils.getCounter();
        enableCurrentMacro();
    }

    public static void disableMacro() {
        isMacroing = false;
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
        for (int x = -3; x < 3; x++) {
            for (int y = -3; y < 3; y++) {
                for (int z = -3; z < 3; z++) {
                    BlockPos pos = BlockUtils.getRelativeBlockPos(x, y, 1 + z,
                            FarmConfig.cropType == MacroEnum.MUSHROOM || FarmConfig.cropType == MacroEnum.SUGARCANE ? AngleUtils.getClosestDiagonal() - 45
                            : FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD ? AngleUtils.getClosest30() - 30
                            : AngleUtils.getClosest());
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (!(block instanceof BlockCrops || block instanceof BlockReed || block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockMelon || block instanceof  BlockPumpkin || block instanceof BlockMushroom || block instanceof BlockCactus)) continue;

                    if (closestCrop == null || mc.thePlayer.getPosition().distanceSq(pos.getX(), pos.getY(), pos.getZ()) < mc.thePlayer.getPosition().distanceSq(closestCrop.getRight().getX(), closestCrop.getRight().getY(), closestCrop.getRight().getZ())) {
                        closestCrop = Pair.of(block, pos);
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
                return CropEnum.NETHERWART;
            } else if (left.equals(Blocks.reeds)) {
                return CropEnum.SUGARCANE;
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