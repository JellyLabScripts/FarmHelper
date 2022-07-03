package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static Macro currentMacro;
    public static boolean isMacroing;
    public static SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public static CropMacro cropMacro = new CropMacro();

    public static long startTime = 0;
    public static int startCounter = 0;
    public static boolean startingUp;

    @SubscribeEvent
    public void onChatMessageReceived(ClientChatReceivedEvent e) {
        if (currentMacro != null && currentMacro.enabled && mc.thePlayer != null && mc.theWorld != null && e.message != null) {
            currentMacro.onChatMessageReceived(e.message.getUnformattedText());
        }
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
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
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
            toggleMacro();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (isMacroing && mc.thePlayer != null && mc.theWorld != null) {
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
        if (isMacroing) {
            disableMacro();
        } else {
            enableMacro();
        }
    }

    public static void enableMacro() {
        if (FarmConfig.cropType == CropEnum.SUGARCANE) {
            currentMacro = sugarcaneMacro;
        } else {
            currentMacro = cropMacro;
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
        startCounter = InventoryUtils.getCounter();
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
            mc.displayGuiScreen((GuiScreen) null);
            Field f = null;
            f = FieldUtils.getDeclaredField(mc.getClass(), "leftClickCounter",true);
            try {
                f.set(mc, 10000);
            }catch (Exception e){
                e.printStackTrace();
            }
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