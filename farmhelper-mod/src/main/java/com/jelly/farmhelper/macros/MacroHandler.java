package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;
import com.jelly.farmhelper.config.interfaces.*;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Macro currentMacro;
    public static boolean isMacroing;

    public static SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public static LayeredCropMacro layeredCropMacro = new LayeredCropMacro();
    public static VerticalCropMacro verticalCropMacro = new VerticalCropMacro();
    public static CocoaBeanMacro cocoaBeanMacro = new CocoaBeanMacro();

    private final Rotation rotation = new Rotation();
    public static long startTime = 0;
    public static boolean randomizing = false;
    public static long startCounter = 0;
    public static boolean startingUp;

    @SubscribeEvent
    public void onChatMessageReceived(ClientChatReceivedEvent e) {
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
                ProfitUtils.updateProfitState();
                StatusUtils.updateStateString();
            }
            if (currentMacro != null && currentMacro.enabled) {
                currentMacro.onTick();
            }
        }

    }
    public static void toggleMacro(){
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
        System.out.println(FarmConfig.farmType + " " + FarmConfig.cropType);
        if(FarmConfig.farmType == FarmEnum.VERTICAL){
            currentMacro = verticalCropMacro;
        } else {
            if (FarmConfig.cropType == CropEnum.SUGARCANE) {
                currentMacro = sugarcaneMacro;
            } else if (FarmConfig.cropType == CropEnum.COCOA_BEANS) {
                currentMacro = cocoaBeanMacro;
            } else {
                currentMacro = layeredCropMacro;
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
        ProfitUtils.resetProfit();

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
    }

    public static void disableCurrentMacro() {
        if (currentMacro.enabled) {
            currentMacro.toggle();
        }
    }

    public static void enableCurrentMacro() {
        if (!currentMacro.enabled && !startingUp) {
            mc.inGameHasFocus = true;
            mc.displayGuiScreen(null);
            startingUp = true;
            KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
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


}