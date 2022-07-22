package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

import static com.jelly.farmhelper.features.BanwaveChecker.getBanDiff;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] messages = new String[]
            {"What?", "yo huh?", "So whaats this?", "bedrock??", "ehhhhh??", "LOL what", "wtf is this", "why am i here",
                    "tf", "lmao what?", "????", "whatdahell", "what the frick", "UMMM???", "huh where am i", "damn wth",
                    "i don't get it", "What is this?", "WHy is bedrock here?", "can u explain?", "?!?!?!????", "why bedrocki",
                    "?!?!?!?!!!!!!" };
    public static Macro currentMacro;
    public static boolean isMacroing;
    Thread randomizingthread;
    Thread bzchillingthread;
    public static boolean caged = false;
    public static boolean resting = false;
    public static SugarcaneMacro sugarcaneMacro = new SugarcaneMacro();
    public static CropMacro cropMacro = new CropMacro();

    private final Rotation rotation = new Rotation();
    public static long startTime = 0;
    public static boolean randomizing = false;
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
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
            toggleMacro();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (isMacroing) {
            if (BlockUtils.bedrockCount() >= 2) {
                disableMacro();
                new Thread(cagedActing).start();
            }
            if (FarmHelper.tickCount == 1) {
                LogUtils.webhookStatus();
                ProfitUtils.updateProfitState();
                StatusUtils.updateStateString();
            }
            if (currentMacro != null && currentMacro.enabled) {
                currentMacro.onTick();
            }
        }

        if (isMacroing && FarmHelper.gameState.currentLocation == GameState.location.ISLAND && MiscConfig.randomization) {
            if (currentMacro instanceof SugarcaneMacro) {
                SugarcaneMacro.State state = ((SugarcaneMacro) currentMacro).currentState;
                if (state == SugarcaneMacro.State.RIGHT || state == SugarcaneMacro.State.LEFT) {
                    // 1/2 chance of starting every minute in ticks
                    if (Math.random() < 0.000416 * (1 + 0.05 * getBanDiff())) {
                        randomizingthread = new Thread(randomizememe);
                        randomizingthread.start();
                    }
                }
            } else if (currentMacro instanceof CropMacro) {
                CropMacro.State state = ((CropMacro) currentMacro).currentState;
                if (state == CropMacro.State.RIGHT || state == CropMacro.State.LEFT) {
                    // 1/2 chance of starting every minute in ticks
                    if (Math.random() < 0.000416 * (1 + 0.05 * getBanDiff())) {
                        randomizingthread = new Thread(randomizememe);
                        randomizingthread.start();
                    }
                }
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
            mc.displayGuiScreen(null);
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

    Runnable randomizememe = () -> {
        rotation.reset();
        isMacroing = false;
        randomizing = true;
        float currentyaw = mc.thePlayer.rotationYaw;
        float currentpitch = mc.thePlayer.rotationPitch;

        LogUtils.debugLog("Randomizing movements");
        rotation.easeTo((float) (360 * (Math.random())), (float) (15 * (Math.random())), 2000);
        while (rotation.rotating) {
            try {
                Thread.sleep(500);
                updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5,  false, true, false);
            } catch (Exception e) {}
        }
        KeyBindUtils.stopMovement();
        LogUtils.debugLog("Finished randomization");

        try {
            rotation.easeTo(currentyaw, currentpitch, 2000);
            Thread.sleep(2000);
        } catch (Exception ignored) {}
        randomizing = false;
        isMacroing = true;
        MacroHandler.enableCurrentMacro();
    };

    Runnable bazaarChilling = () -> {
        try {
            resting = true;
            UngrabUtils.ungrabMouse();
            LogUtils.scriptLog("Gonna chill for a bit (we don't want to get banned)");
            LogUtils.webhookLog("Gonna chill in hub for about 5 minutes");

            rotation.reset();
            rotation.easeTo(103f, -11f, 1000);
            Thread.sleep(1500);
            KeyBindUtils.updateKeys(true, false, false, false, false, true, false);
            long timeout = System.currentTimeMillis();
            boolean timedOut = false;
            while (BlockUtils.getRelativeBlock(0, 0, 1) != Blocks.spruce_stairs) {
                if ((System.currentTimeMillis() - timeout) > 10000) {
                    LogUtils.scriptLog("Couldn't find bz, gonna chill here");
                    timedOut = true;
                    break;
                }
            }
            stopMovement();

            if (!timedOut) {
                // about 5 minutes
                for (int i = 0; i < 15; i++) {
                    Thread.sleep(6000);
                    KeyBindUtils.rightClick();
                    Thread.sleep(3000);
                    InventoryUtils.clickOpenContainerSlot(11);
                    Thread.sleep(3000);
                    InventoryUtils.clickOpenContainerSlot(11);
                    Thread.sleep(3000);
                    InventoryUtils.clickOpenContainerSlot(10);
                    Thread.sleep(3000);
                    InventoryUtils.clickOpenContainerSlot(10);
                    Thread.sleep(3000);
                    mc.thePlayer.closeScreen();
                }
            } else {
                Thread.sleep(1000 * 60 * 5);
            }

            resting = false;
            mc.thePlayer.sendChatMessage("/is");
            Thread.sleep(6000);
            MacroHandler.enableMacro();
        } catch (Exception e) {}

    };
    Runnable cagedActing = () -> {
        caged = true;
        LogUtils.webhookLog("You just got caged bozo! Buy a lottery ticket! @everyone");
        if (randomizingthread != null) {
            randomizingthread.interrupt();
        }

        if (bzchillingthread != null) {
            bzchillingthread.interrupt();
        }

        int firstmsgindex = (int) Math.floor(Math.random() * (messages.length - 1));
        try {
            Thread.sleep(2000);
            mc.thePlayer.sendChatMessage("/ac " + messages[firstmsgindex]);
            Thread.sleep(1000);
            for (int i = 0; i < 3; i++) {
                rotation.easeTo((float) (270 * (Math.random())), (float) (20 * (Math.random() - 1)), (long) (800 * (Math.random() + 1)));
                while (rotation.rotating) {
                    if (i == 0) {
                        KeyBindUtils.updateKeys(Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, true, false);
                        Thread.sleep(500);
                        rotation.reset();
                        stopMovement();
                        Thread.sleep(2000);
                        mc.thePlayer.sendChatMessage("/ac " + messages[firstmsgindex + 1]);
                        Thread.sleep(1000);
                        break;
                    } else {
                        // around 0.3/0.6s long movements (the more random the best)
                        Thread.sleep((long) (100 * (Math.random() + 2)));
                        KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                    }
                }
            }
            stopMovement();
            Thread.sleep(1000);
            mc.thePlayer.sendChatMessage("/hub");
            Thread.sleep(5000);
            if (FarmHelper.gameState.currentLocation == GameState.location.HUB) {
                caged = false;
                bzchillingthread = new Thread(bazaarChilling);
                bzchillingthread.start();
            } else {
                Thread.sleep(1000 * 60 * 5);
                caged = false;
                MacroHandler.enableMacro();
            }

        } catch (Exception ignored) {}
    };
}