package com.jelly.farmhelperv2.failsafe;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.BlockChangeEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.impl.*;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.BanInfoWS;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.AudioManager;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Below is the core logic of the FailsafeManager class
 * <p>
 * 1. When Failsafe instances (e.g., teleport failsafe) detect a possible emergency, it calls addPossibleDetection(),
 *    where the emergency is added to {@code emergencyQueue}.
 * <p>
 * 2. A delay is set, {@code chooseEmergencyDelay}, for choosing the emergency with the highest priority
 *   (in case there are multiple failsafes being triggered at the same time)
 * <p>
 * 3. Once an emergency is chosen, the failsafe is triggered (see {@code onTickChooseEmergency})
 * <p>
 * 4. The failsafe may call {@code restartMacroAfterDelay} after it has finished reacting
 * <p>
 * The macro may also schedule a delay into {@code onTickDelay} while reacting via {@code scheduleDelay} , which pauses onTick() events)
 */
public class FailsafeManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FailsafeManager instance;
    public final RotationHandler rotation = RotationHandler.getInstance();

    public static FailsafeManager getInstance() {
        if (instance == null) {
            instance = new FailsafeManager();
        }
        return instance;
    }

    /** List of failsafes registered at the constructor. */
    public final List<Failsafe> failsafes = new ArrayList<>();

    /** The queue of emergencies, added at possibleDetection()*/
    @Getter
    public final ArrayList<Failsafe> emergencyQueue = new ArrayList<>();

    /** Whether there is an active emergency*/
    @Getter
    @Setter
    private boolean isHavingEmergency = false;

    /** The delay of choosing the emergency with the highest priority*/
    @Getter
    public final Clock chooseEmergencyDelay = new Clock();

    /** The failsafe that is being triggered (Optional.empty() when there is no failsafe) */
    public Optional<Failsafe> triggeredFailsafe = Optional.empty();

    /** The delay of executing the failsafe*/
    @Getter
    private final Clock onTickDelay = new Clock();

    /** The delay of restarting the macro*/
    @Getter
    private final Clock restartMacroAfterFailsafeDelay = new Clock();

    /** Whether the player is notified of the failsafe information. Resets when the macro is stopped*/
    private boolean sentFailsafeInfo = false;

    /** Whether the player is notified of the failsafe information. Resets when the macro is stopped*/
    public boolean swapItemDuringRecording = false;


    /** Information of the last failsafe (for WebSocket)*/
    @Getter
    @Setter
    private String banInfoWSLastFailsafe = "";

    /** A request is sent to the server every 1 minute when the macro is toggled, ensuring proper response*/
    private final Clock checkFailsafeServerClock = new Clock();

    /** Whether the client can connect to the failsafe server. If not, the macro will be stopped*/
    private volatile boolean connectionSuccessful = true;

    public FailsafeManager() {
        failsafes.addAll(
                Arrays.asList(
                        BadEffectsFailsafe.getInstance(),
                        BanwaveFailsafe.getInstance(),
                        BedrockCageFailsafe.getInstance(),
                        CobwebFailsafe.getInstance(),
                        DirtFailsafe.getInstance(),
                        DisconnectFailsafe.getInstance(),
                        EvacuateFailsafe.getInstance(),
                        FullInventoryFailsafe.getInstance(),
                        GuestVisitFailsafe.getInstance(),
                        ItemChangeFailsafe.getInstance(),
                        JacobFailsafe.getInstance(),
                        KnockbackFailsafe.getInstance(),
                        LowerAvgBpsFailsafe.getInstance(),
                        RotationFailsafe.getInstance(),
                        TeleportFailsafe.getInstance(),
                        WorldChangeFailsafe.getInstance()
                )
        );
    }


    public void stopFailsafes() {
        triggeredFailsafe = Optional.empty();
        emergencyQueue.clear();
        sentFailsafeInfo = false;
        swapItemDuringRecording = false;
        chooseEmergencyDelay.reset();
        onTickDelay.reset();
        failsafes.forEach(Failsafe::resetStates);
    }

    public void resetAfterMacroDisable() {
        stopFailsafes();
        restartMacroAfterFailsafeDelay.reset();
        checkFailsafeServerClock.reset();
        connectionSuccessful = true;
    }

    /** Calls when there a block in the world is being changed*/
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockChange(BlockChangeEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onBlockChange(event));
    }

    /** Calls when there a packet is received*/
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) {
            if (triggeredFailsafe.get().equals(BedrockCageFailsafe.getInstance())) {
                BedrockCageFailsafe.getInstance().onReceivedPacketDetection(event);
            }
            return;
        }
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onReceivedPacketDetection(event));
    }

    /** Calls every tick when a failsafe is being triggered*/
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onTickDetection(event));
    }

    /** Calls when a chat message is received*/
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatDetection(ClientChatReceivedEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != 0) return;
        if (event.message == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;

        failsafes.forEach(failsafe -> failsafe.onChatDetection(event));
    }

    /** Calls when a world is unloaded*/
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldUnloadDetection(WorldEvent.Unload event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onWorldUnloadDetection(event));
    }

    /** Calls when the player is disconnected*/
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDisconnectDetection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        failsafes.forEach(failsafe -> failsafe.onDisconnectDetection(event));
    }

    /**
     * Adds possible detection to emergencyQueue.
     * Schedules a delay, and an emergency is chosen afterward.
     * Send failsafe information if it hasn't done yet
     * @param failsafe: the possible type of failsafe
     */
    public void addPossibleDetection(Failsafe failsafe) {
        if (emergencyQueue.contains(failsafe)) return;

        MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::saveState);
        emergencyQueue.add(failsafe);

        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(FarmHelperConfig.failsafeStopDelay + (long) (Math.random() * 500));

        LogUtils.sendDebug("[Failsafe] Emergency added: " + failsafe.getType().name());
        LogUtils.sendWarning("[Failsafe] Possible emergency: " + LogUtils.capitalize(failsafe.getType().name()));

        if (!sentFailsafeInfo) {
            sentFailsafeInfo = true;
            sendFailsafeInfo(failsafe);
        }
    }

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (!chooseEmergencyDelay.isScheduled()) return;
        if (!chooseEmergencyDelay.passed()) return;

        triggeredFailsafe = Optional.of(getHighestPriorityEmergency());
        if (triggeredFailsafe.get().getType() == EmergencyType.NONE) {
            LogUtils.sendDebug("[Failsafe] No emergency chosen! This shouldn't happen, so please contact the developer");
            stopFailsafes();
            return;
        }

        banInfoWSLastFailsafe = triggeredFailsafe.get().getType().name().toLowerCase().replace("_", " ");
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        isHavingEmergency = true;
        LogUtils.sendDebug("[Failsafe] Emergency chosen: " + StringUtils.stripControlCodes(triggeredFailsafe.get().getType().name()));

        FeatureManager.getInstance().disableCurrentlyRunning(Scheduler.getInstance());
        Scheduler.getInstance().pause();

        if (FarmHelperConfig.captureClipAfterFailsafe && !FarmHelperConfig.captureClipKeybind.getKeyBinds().isEmpty()) {
            startCaptureClip();
        }
    }

    @SubscribeEvent
    public void onFailsafeTriggered(TickEvent.ClientTickEvent event) {
        if (!triggeredFailsafe.isPresent()) {
            return;
        }
        if (onTickDelay.isScheduled() && !onTickDelay.passed()) {
            return;
        }
        triggeredFailsafe.get().duringFailsafeTrigger();
    }

    @SubscribeEvent
    public void onCheckFailsafeServer(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;

        if (!connectionSuccessful) {
            LogUtils.sendWarning("[Failsafe] Cannot connect to the server. Disabling the macro...");
            MacroHandler.getInstance().disableMacro();
        }

        if (checkFailsafeServerClock.isScheduled() && !checkFailsafeServerClock.passed()) return;

        checkFailsafeServerClock.schedule(60_000);
        tryToConnectToFailsafeServer();

    }

    @SubscribeEvent
    public void onTickRestartMacro(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!restartMacroAfterFailsafeDelay.isScheduled()) return;

        if (restartMacroAfterFailsafeDelay.passed()) {
            if (mc.currentScreen != null) {
                PlayerUtils.closeScreen();
            }
            if (FarmHelperConfig.alwaysTeleportToGarden) {
                MacroHandler.getInstance().triggerWarpGarden(true, false);
                MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.setSavedState(Optional.empty()));
            }
            restartMacroAfterFailsafeDelay.reset();
            Multithreading.schedule(() -> {
                LogUtils.sendDebug("[Failsafe] Restarting the macro...");
                if (MacroHandler.getInstance().isCurrentMacroPaused()) {
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.setSavedState(Optional.empty()));
                    MacroHandler.getInstance().resumeMacro();
                } else
                    MacroHandler.getInstance().enableMacro();
                FailsafeManager.getInstance().setHavingEmergency(false);
                FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().reset();
            }, FarmHelperConfig.alwaysTeleportToGarden ? 1_500 : 0, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        if (chooseEmergencyDelay.isScheduled()) {
            String text = "Failsafe in: " + LogUtils.formatTime(chooseEmergencyDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        } else if (restartMacroAfterFailsafeDelay.isScheduled()) {
            String text = "Restarting the macro in: " + LogUtils.formatTime(restartMacroAfterFailsafeDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.ORANGE);
        } else if (triggeredFailsafe.isPresent()
                && !FarmHelperConfig.streamerMode
                && !triggeredFailsafe.get().equals(BanwaveFailsafe.getInstance())
                && !triggeredFailsafe.get().equals(EvacuateFailsafe.getInstance())
                && !triggeredFailsafe.get().equals(GuestVisitFailsafe.getInstance())
                && !triggeredFailsafe.get().equals(JacobFailsafe.getInstance())
        ) {
            ArrayList<String> textLines = new ArrayList<>();
            textLines.add("§6" + StringUtils.stripControlCodes(triggeredFailsafe.get().getType().name()).replace("_", " "));
            textLines.add("§c§lYOU ARE DURING STAFF CHECK!");
            textLines.add("§cPRESS §6" + FarmHelperConfig.toggleMacro.getDisplay() + "§c TO DISABLE THE MACRO");
            textLines.add("§cDO §6§lNOT §cLEAVE! REACT!");
            RenderUtils.drawMultiLineText(textLines, event, Color.MAGENTA, 2f);
        }
    }


    public void restartMacroAfterDelay() {
        if (FarmHelperConfig.enableRestartAfterFailSafe) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.setSavedState(Optional.empty()));
            Multithreading.schedule(() -> {
                if (!MacroHandler.getInstance().isMacroToggled()) return;
                InventoryUtils.openInventory();
                LogUtils.sendDebug("[Failsafe] Finished " + (triggeredFailsafe.map(failsafe -> (failsafe.getType().label + " ")).orElse("")) + "failsafe");
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    if (FarmHelperConfig.restartAfterFailSafeDelay > 0) {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    } else {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro soon...");
                        restartMacroAfterFailsafeDelay.schedule(2000L + Math.random() * 1000L);
                    }
                }
            }, (long) (1_500 + Math.random() * 1_000), TimeUnit.MILLISECONDS);
        } else {
            LogUtils.sendWarning("[Failsafe] Restart after failsafe is disabled. Disabling the macro...");
            MacroHandler.getInstance().disableMacro();
        }
    }

    public void scheduleDelay(long delay) {
        onTickDelay.schedule(delay);
    }

    public void scheduleRandomDelay(long minDelay, long maxAdditionalDelay) {
        onTickDelay.schedule(minDelay + Math.random() * maxAdditionalDelay);
    }

    private void startCaptureClip() {
        if (FarmHelperConfig.clipCapturingType) {
            FailsafeUtils.captureClip();
            LogUtils.sendDebug("[Failsafe] Recording clip!");
        }
        Multithreading.schedule(() -> {
            FailsafeUtils.captureClip();
            LogUtils.sendDebug("[Failsafe] Clip captured!");
        }, FarmHelperConfig.captureClipDelay, TimeUnit.SECONDS);
    }

    private void sendFailsafeInfo(Failsafe failsafe) {
        Multithreading.schedule(() -> {
            Failsafe tempFailsafe = getHighestPriorityEmergency();

            if (tempFailsafe.getType() == EmergencyType.NONE) {
                LogUtils.sendDebug("[Failsafe] No emergency chosen! This shouldn't happen, please contact the developer.");
                stopFailsafes();
                return;
            }

            banInfoWSLastFailsafe = tempFailsafe.getType().name().toLowerCase().replace("_", " ");

            if (FarmHelperConfig.enableFailsafeSound && failsafe.shouldPlaySound()) {
                AudioManager.getInstance().playSound();
            }

            if (FarmHelperConfig.autoAltTab && failsafe.shouldAltTab()) {
                FailsafeUtils.bringWindowToFront();
            }

            Multithreading.schedule(() -> {
                if (FarmHelperConfig.autoAltTab && failsafe.shouldAltTab() && !Display.isActive()) {
                    FailsafeUtils.bringWindowToFrontUsingRobot();
                    System.out.println("Bringing window to front using Robot because Winapi failed as usual.");
                }
            }, 750, TimeUnit.MILLISECONDS);

            LogUtils.sendFailsafeMessage(tempFailsafe.getType().label, failsafe.shouldTagEveryone());
            BanInfoWS.getInstance().sendFailsafeInfo(tempFailsafe.getType());
            if (failsafe.shouldSendNotification())
                FailsafeUtils.getInstance().sendNotification(
                        StringUtils.stripControlCodes(tempFailsafe.getType().label),
                        TrayIcon.MessageType.WARNING);

        }, 800, TimeUnit.MILLISECONDS);
    }

    private Failsafe getHighestPriorityEmergency() {
        Failsafe highestPriority = emergencyQueue.get(0);
        for (Failsafe emergencyType : emergencyQueue) {
            if (emergencyType.getPriority() < highestPriority.getPriority()) {
                highestPriority = emergencyType;
            }
        }
        return highestPriority;
    }

    private void tryToConnectToFailsafeServer() {
        LogUtils.sendDebug("[Failsafe] Trying to connect to failsafe server");
        Executors.newSingleThreadExecutor().submit( () -> {
            connectionSuccessful = NetworkUtils.checkFailsafeServer();
            LogUtils.sendDebug("[Failsafe] Connection successful = " + connectionSuccessful);
        });
    }

    public enum EmergencyType {
        NONE(""),
        ROTATION_CHECK("You've been §lROTATED§r§d by a staff member!"),
        TELEPORT_CHECK("You've been §lTELEPORTED§r§d by a staff member!"),
        KNOCKBACK_CHECK("You've been §lKNOCKED OUT OF THE FARM§r§d by a staff member!"),
        DIRT_CHECK("You've been §lDIRT CHECKED§r§d by a staff member!"),
        COBWEB_CHECK("You've been §lCOBWEB CHECKED§r§d by a staff member!"),
        BAD_EFFECTS_CHECK("You've had §lBAD EFFECTS APPLIED§r§d by a staff member!"),
        FULL_INVENTORY("Your inventory is full. It might be a staff check or just an accident."),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!"),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!"),
        BEDROCK_CAGE_CHECK("You've been §lBEDROCK CAGED§r§d by a staff member!"),
        EVACUATE("Server is restarting! Evacuate!"),
        BANWAVE("A banwave has been detected!"),
        DISCONNECT("You've been §lDISCONNECTED§r§d from the server!"),
        LOWER_AVERAGE_BPS("Your BPS is lower than average!"),
        JACOB("You've extended the §lJACOB COUNTER§r§d!"),
        GUEST_VISIT("You've been §lVISITED§r§d by "
                + (!GuestVisitFailsafe.getInstance().lastGuestName.isEmpty() ? GuestVisitFailsafe.getInstance().lastGuestName : "a guest") + "!");

        final String label;

        EmergencyType(String s) {
            label = s;
        }
    }
}