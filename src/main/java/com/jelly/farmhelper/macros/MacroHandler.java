package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static Macro currentMacro;
    public static boolean isMacroOn;
    public SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public CropMacro cropMacro = new CropMacro();

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
            if (FarmConfig.cropType == CropEnum.SUGARCANE) {
                currentMacro = sugarcaneMacro;
            } else {
                currentMacro = cropMacro;
            }

            if (isMacroOn) {
                disableMacro();
            } else {
                enableMacro();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (isMacroOn && mc.thePlayer != null && mc.theWorld != null) {
            // InventoryUtils.getInventoryDifference(mc.thePlayer.inventory.mainInventory);
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

    public static void enableMacro() {
        isMacroOn = true;
        LogUtils.scriptLog("Starting script");
        LogUtils.webhookLog("Starting script");
        if (AutoSellConfig.autoSell) LogUtils.scriptLog("Auto Sell is in BETA, lock important slots just in case");
        if (MiscConfig.ungrab) UngrabUtils.ungrabMouse();
        if (SchedulerConfig.scheduler) Scheduler.start();
        startTime = System.currentTimeMillis();
        ProfitUtils.resetProfit();
        startCounter = InventoryUtils.getCounter();
        enableCurrentMacro();
    }

    public static void disableMacro() {
        isMacroOn = false;
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
            startingUp = true;
            KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
            new Thread(startCurrent).start();
        }
    }

    static Runnable startCurrent = () -> {
        try {
            Thread.sleep(300);
            KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
            if (isMacroOn) currentMacro.toggle();
            startingUp = false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    };
}