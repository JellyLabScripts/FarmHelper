package com.jelly.farmhelperv2.failsafe;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.impl.*;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.AudioManager;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.awt.*;
        import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FailsafeManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FailsafeManager instance;

    public static FailsafeManager getInstance() {
        if (instance == null) {
            instance = new FailsafeManager();
        }
        return instance;
    }
    public final List<Failsafe> failsafes = new ArrayList<>();
    public Optional<Failsafe> triggeredFailsafe = Optional.empty();
    @Getter
    public final ArrayList<Failsafe> emergencyQueue = new ArrayList<>();
    @Getter
    public final Clock chooseEmergencyDelay = new Clock();
    private final Clock onTickDelay = new Clock();
    @Getter
    private final Clock restartMacroAfterFailsafeDelay = new Clock();
    public final RotationHandler rotation = RotationHandler.getInstance();
    private boolean sendingFailsafeInfo = false;
    private static final String[] FAILSAFE_MESSAGES = new String[]{
            "WHAT", "what?", "what", "what??", "what???", "wut?", "?", "what???", "yo huh", "yo huh?", "yo?",
            "ehhhhh??", "eh", "yo", "ahmm", "ehh", "LOL what", "lol :skull:", "bro wtf was that?", "lmao",
            "lmfao", "wtf is this", "wtf", "WTF", "wtf is this?", "wtf???", "tf", "tf?", "wth",
            "lmao what?", "????", "??", "???????", "???", "UMMM???", "umm", "ummm???", "damn wth",
            "Damn", "damn wtf", "damn", "hmmm", "hm", "sus", "hmm", "ok??", "ok?", "give me a rest",
            "again lol", "again??", "ok damn", "seriously?", "seriously????", "seriously", "really?", "really",
            "are you kidding me?", "are you serious?", "are you fr???", "not again",
            "give me a break", "youre kidding right?", "youre joking", "youre kidding me",
            "you must be joking", "seriously bro?", "cmon now", "cmon", "this is too much", "stop messing with me"};
    private static final String[] FAILSAFE_CONTINUE_MESSAGES = new String[]{
            "can i keep farming?", "lemme farm ok?", "leave me alone next time ok?"};


    @Getter
    @Setter
    private boolean hadEmergency = false;

    public FailsafeManager() {
        failsafes.addAll(
                Arrays.asList(
                        new BanwaveFailsafe(),
                        new BedrockCageFailsafe(),
                        new DirtFailsafe(),
                        new DisconnectFailsafe(),
                        new EvacuateFailsafe(),
                        new GuestVisitFailsafe(),
                        new ItemChangeFailsafe(),
                        new JacobFailsafe(),
                        new LowerAvgBpsFailsafe(),
                        new RotationFailsafe(),
                        new TeleportFailsafe(),
                        new WorldChangeFailsafe()
                )
        );
    }

    public void stopFailsafes() {
        triggeredFailsafe = Optional.empty();
        emergencyQueue.clear();
        sendingFailsafeInfo = false;
        chooseEmergencyDelay.reset();
        onTickDelay.reset();
    }

    public void resetAfterMacroDisable() {
        stopFailsafes();
        restartMacroAfterFailsafeDelay.reset();
    }

    @SubscribeEvent
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onReceivedPacketDetection(event));
    }

    @SubscribeEvent
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onTickDetection(event));
    }

    public void possibleDetection(Failsafe failsafe) {
        if (emergencyQueue.contains(failsafe)) return;

        MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::saveState);
        emergencyQueue.add(failsafe);
        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(FarmHelperConfig.failsafeStopDelay + (long) (Math.random() * 500));
        LogUtils.sendDebug("[Failsafe] Emergency added: " + failsafe.getType().name());
        LogUtils.sendWarning("[Failsafe] Probability of emergency: " + LogUtils.capitalize(failsafe.getType().name()));
        if (!sendingFailsafeInfo) {
            sendingFailsafeInfo = true;
            Multithreading.schedule(() -> {
                Failsafe tempFailsafe = getHighestPriorityEmergency();
                if (tempFailsafe.getType() == EmergencyType.NONE) {
                    // Should never happen, but yeh...
                    LogUtils.sendDebug("[Failsafe] No emergency chosen!");
                    stopFailsafes();
                    return;
                }

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
                if (failsafe.shouldSendNotification())
                    FailsafeUtils.getInstance().sendNotification(StringUtils.stripControlCodes(tempFailsafe.getType().label), TrayIcon.MessageType.WARNING);
            }, 800, TimeUnit.MILLISECONDS);
        }
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

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (!chooseEmergencyDelay.isScheduled()) return;
        if (chooseEmergencyDelay.passed()) {
            triggeredFailsafe = Optional.of(getHighestPriorityEmergency());
            if (triggeredFailsafe.get().getType() == EmergencyType.NONE) {
                // Should never happen, but yeh...
                LogUtils.sendDebug("[Failsafe] No emergency chosen!");
                stopFailsafes();
                return;
            }
            emergencyQueue.clear();
            chooseEmergencyDelay.reset();
            hadEmergency = true;
            LogUtils.sendDebug("[Failsafe] Emergency chosen: " + StringUtils.stripControlCodes(triggeredFailsafe.get().getType().name()));
            FeatureManager.getInstance().disableCurrentlyRunning(null);
            if (!Scheduler.getInstance().isFarming()) {
                Scheduler.getInstance().farmingTime();
            }
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
    public void onTickRestartMacro(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!restartMacroAfterFailsafeDelay.isScheduled()) return;

        if (restartMacroAfterFailsafeDelay.passed()) {
            if (mc.currentScreen != null) {
                PlayerUtils.closeScreen();
            }
            if (FarmHelperConfig.alwaysTeleportToGarden) {
                MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::triggerWarpGarden);
            }
            restartMacroAfterFailsafeDelay.reset();
            Multithreading.schedule(() -> {
                LogUtils.sendDebug("[Failsafe] Restarting the macro...");
                MacroHandler.getInstance().enableMacro();
                FailsafeManager.getInstance().setHadEmergency(false);
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
        } else if (triggeredFailsafe.isPresent()) {
            ArrayList<String> textLines = new ArrayList<>();
            textLines.add("§3" + StringUtils.stripControlCodes(triggeredFailsafe.get().getType().name()).replace("_", " "));
            textLines.add("§cYOU ARE DURING STAFF CHECK!");
            textLines.add("§dPRESS §3" + FarmHelperConfig.toggleMacro.getDisplay() + "§d TO DISABLE THE MACRO");
            textLines.add("§dDO §cNOT §dLEAVE! REACT!");
            RenderUtils.drawMultiLineText(textLines, event, Color.MAGENTA, 3f);
        }
    }

    public void restartMacroAfterDelay() {
        if (FarmHelperConfig.enableRestartAfterFailSafe) {
            MacroHandler.getInstance().pauseMacro();
            Multithreading.schedule(() -> {
                InventoryUtils.openInventory();
                LogUtils.sendDebug("[Failsafe] Finished " + (triggeredFailsafe.map(failsafe -> (failsafe.getType().label + " ")).orElse("")) + "failsafe");
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                    restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                }
            }, (long) (1_500 + Math.random() * 1_000), TimeUnit.MILLISECONDS);
        } else {
            MacroHandler.getInstance().disableMacro();
        }
    }

    public boolean firstCheckReturn() {
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (!MacroHandler.getInstance().isMacroToggled()) return true;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return true;
        return FeatureManager.getInstance().shouldIgnoreFalseCheck();
    }

    public void scheduleDelay(long delay) {
        onTickDelay.schedule(delay);
    }

    public void scheduleRandomDelay(long minDelay, long maxAdditionalDelay) {
        onTickDelay.schedule(minDelay + Math.random() * maxAdditionalDelay);
    }

    public static String getRandomMessage(String[] messages) {
        if (messages.length > 1) {
            return messages[(int) (Math.random() * (messages.length - 1))];
        } else {
            return messages[0];
        }
    }

    public static String getRandomMessage() {
        return getRandomMessage(FAILSAFE_MESSAGES);
    }

    public static String getRandomContinueMessage() {
        return getRandomMessage(FAILSAFE_CONTINUE_MESSAGES);
    }

    public void randomMoveAndRotate() {
        long rotationTime = FarmHelperConfig.getRandomRotationTime();
        this.rotation.easeTo(
                new RotationConfiguration(
                        new Rotation(
                                mc.thePlayer.rotationYaw + randomValueBetweenExt(-180, 180, 45),
                                randomValueBetweenExt(-20, 40, 5)),
                        rotationTime, null));
        scheduleDelay(rotationTime - 50);
        double randomKey = Math.random();
        if (!mc.thePlayer.onGround && FarmHelperConfig.tryToUseJumpingAndFlying) {
            if (randomKey <= 0.3) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.6) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else {
                KeyBindUtils.stopMovement();
            }
        } else {
            if (randomKey <= 0.175 && GameStateHandler.getInstance().isFrontWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.35 && GameStateHandler.getInstance().isLeftWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.525 && GameStateHandler.getInstance().isRightWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.70 && GameStateHandler.getInstance().isBackWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            }
            if (randomKey > 0.7) {
                KeyBindUtils.stopMovement();
                if (FarmHelperConfig.tryToUseJumpingAndFlying) {
                    mc.thePlayer.jump();
                    Multithreading.schedule(() -> {
                        if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.allowEdit && mc.thePlayer.capabilities.allowFlying && !mc.thePlayer.capabilities.isFlying) {
                            mc.thePlayer.capabilities.isFlying = true;
                            mc.thePlayer.sendPlayerAbilities();
                        }
                    }, (long) (250 + Math.random() * 250), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private float randomValueBetweenExt(float min, float max, float minFromZero) {
        double random = Math.random();
        if (random < 0.5) {
            // should return value between (min, -minFromZero)
            return (float) (min + Math.random() * (minFromZero - min));
        } else {
            // should return value between (minFromZero, max)
            return (float) (minFromZero + Math.random() * (max - minFromZero));
        }
    }

    public enum EmergencyType {
        NONE(""),
        ROTATION_CHECK("You've got§l ROTATED§r§d by staff member!"),
        TELEPORT_CHECK("You've got§l TELEPORTED§r§d by staff member!"),
        DIRT_CHECK("You've got§l DIRT CHECKED§r§d by staff member!"),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!"),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!"),
        BEDROCK_CAGE_CHECK("You've got§l BEDROCK CAGED§r§d by staff member!"),
        EVACUATE("Server is restarting! Evacuate!"),
        BANWAVE("Banwave has been detected!"),
        DISCONNECT("You've been§l DISCONNECTED§r§d from the server!"),
        LOWER_AVERAGE_BPS("Your BPS is lower than average!"),
        JACOB("You've extended the §lJACOB COUNTER§r§d!"),
        GUEST_VISIT("You've got§l VISITED§r§d by a guest!");

        final String label;

        EmergencyType(String s) {
            label = s;
        }
    }
}