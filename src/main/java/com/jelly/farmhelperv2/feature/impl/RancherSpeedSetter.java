package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.concurrent.TimeUnit;


public final class RancherSpeedSetter {
    /** @return true if need to change speed (async started), false otherwise  */
    public static boolean runIfNeeded(Runnable after) {
        if (!FarmHelperConfig.customFarmingSpeed) return false;

        int current = InventoryUtils.getRancherBootSpeed();
        if (current == -1) return false;
        if (current == FarmHelperConfig.farmingSpeed) return false;

        INSTANCE.start(after);
        return true;
    }

    private enum Stage { NONE, START, INPUT, CONFIRM, END }

    private static final RancherSpeedSetter INSTANCE = new RancherSpeedSetter();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Clock clock = new Clock();
    private Stage stage = Stage.NONE;
    private Runnable callback;
    private boolean enabled;
    private boolean worldChanging;

    private RancherSpeedSetter() {}

    private void start(Runnable after) {
        if (enabled) return;
        this.callback = after;
        this.stage = Stage.START;
        this.enabled = true;
        this.worldChanging = false;
        clock.reset();
        MinecraftForge.EVENT_BUS.register(this);
        LogUtils.sendDebug("[Rancher Speed Setter]: starting (target " + FarmHelperConfig.farmingSpeed + ")");
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) { worldChanging = true; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (!enabled || e.phase == TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null || worldChanging) { finish(); return; }
        if (clock.isScheduled() && !clock.passed()) return;

        switch (stage) {
            case START: {
                mc.thePlayer.sendChatMessage("/setmaxspeed");
                stage = Stage.INPUT;
                clock.schedule(750);
                break;
            }
            case INPUT: {
                if (mc.currentScreen == null) break;
                SignUtils.setTextToWriteOnString(String.valueOf(FarmHelperConfig.farmingSpeed));
                stage = Stage.CONFIRM;
                clock.schedule(750);
                break;
            }
            case CONFIRM: {
                if (mc.currentScreen == null) break;
                SignUtils.confirmSign();
                stage = Stage.END;
                clock.schedule(750);
                break;
            }
            case END: {
                LogUtils.sendSuccess("Rancher's Boots Speed set to " + FarmHelperConfig.farmingSpeed + ".");
                PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), false, false);
                finish();
                break;
            }
            default: break;
        }
    }

    private void finish() {
        enabled = false;
        stage = Stage.NONE;
        clock.reset();
        MinecraftForge.EVENT_BUS.unregister(this);
        if (callback != null) {
            Runnable run = callback;
            callback = null;
            Multithreading.schedule(run, 0, TimeUnit.MILLISECONDS);
        }
    }
}