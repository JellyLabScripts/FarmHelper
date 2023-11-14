package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.handler.RotationHandler;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/*
    Credits to Yuro for this superb class
    https://github.com/onixiya1337/MovementRecorder
*/

public class MovRecPlayer implements IFeature {
    private static MovRecPlayer instance;
    public static MovRecPlayer getInstance() {
        if (instance == null) {
            instance = new MovRecPlayer();
        }
        return instance;
    }

    private static final List<Movement> movements = new ArrayList<>();
    private static boolean isMovementPlaying = false;
    private static boolean isMovementReading = false;
    private static boolean attackKeyPressed = false;
    private static int currentDelay = 0;
    private static int playingIndex = 0;
    static Minecraft mc = Minecraft.getMinecraft();
    private static final RotationHandler rotateBeforePlaying = RotationHandler.getInstance();
    private static final RotationHandler rotateDuringPlaying = RotationHandler.getInstance();

    @Setter
    public String recordingName = "";

    public static class Movement {
        private final boolean forward;
        private final boolean left;
        private final boolean backwards;
        private final boolean right;
        private final boolean sneak;
        private final boolean sprint;
        private final boolean fly;
        private final boolean jump;
        private final boolean attack;
        private final float yaw;
        private final float pitch;
        private final int delay;

        public Movement(boolean forward, boolean left, boolean backwards, boolean right, boolean sneak, boolean sprint, boolean fly,
                        boolean jump, boolean attack, float yaw, float pitch, int delay) {
            this.forward = forward;
            this.left = left;
            this.backwards = backwards;
            this.right = right;
            this.sneak = sneak;
            this.sprint = sprint;
            this.fly = fly;
            this.jump = jump;
            this.attack = attack;
            this.yaw = yaw;
            this.pitch = pitch;
            this.delay = delay;
        }
    }

    @Override
    public String getName() {
        return "Movement Recording Player";
    }

    @Override
    public boolean isRunning() {
        return isMovementReading || isMovementPlaying;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    public void playRandomRecording(String pattern) {
        File recordingDir = new File(mc.mcDataDir, "movementrecorder");
        File[] files = recordingDir.listFiles((dir, name) -> name.contains(pattern) && name.endsWith(".movement"));
        String filename = "";

        if (files != null && files.length > 0) {
            List<File> matchingFiles = new ArrayList<>(Arrays.asList(files));

            Random random = new Random();
            int randomIndex = random.nextInt(matchingFiles.size());
            LogUtils.sendDebug("[Movement Recorder] Selected recording: " + matchingFiles.get(randomIndex).getName());
            filename = matchingFiles.get(randomIndex).getName();
        }
        if (filename.isEmpty()) {
            LogUtils.sendError("[Movement Recorder] No recording found!");
            if (Failsafe.getInstance().isRunning()) {
                LogUtils.sendWarning("[Movement Recorder] Your recording is probably corrupted! Try to record it again. Switching to built-in failsafe mechanism instead.");
                Failsafe.getInstance().resetCustomMovement();
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        MovRecPlayer movRecPlayer = new MovRecPlayer();
        movRecPlayer.setRecordingName(filename);
        movRecPlayer.start();
    }

    @Override
    public void start() {
        if (recordingName.isEmpty()) {
            LogUtils.sendError("[Movement Recorder] No recording selected!");
            if (Failsafe.getInstance().isRunning()) {
                LogUtils.sendWarning("[Movement Recorder] Something wrong happened. Switching to built-in failsafe mechanism instead.");
                Failsafe.getInstance().resetCustomMovement();
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        if (isMovementPlaying) {
            LogUtils.sendDebug("[Movement Recorder] The recording is playing already.");
            return;
        }
        movements.clear();
        playingIndex = 0;
        resetTimers();
        isMovementReading = true;
        try {
            List<String> lines = java.nio.file.Files.readAllLines(new File(mc.mcDataDir + "\\movementrecorder\\" + recordingName).toPath());
            for (String line : lines) {
                if (!isMovementReading) return;
                String[] split = line.split(";");
                Movement movement = new Movement(
                        Boolean.parseBoolean(split[0]),
                        Boolean.parseBoolean(split[1]),
                        Boolean.parseBoolean(split[2]),
                        Boolean.parseBoolean(split[3]),
                        Boolean.parseBoolean(split[4]),
                        Boolean.parseBoolean(split[5]),
                        Boolean.parseBoolean(split[6]),
                        Boolean.parseBoolean(split[7]),
                        Boolean.parseBoolean(split[8]),
                        Float.parseFloat(split[9]),
                        Float.parseFloat(split[10]),
                        Integer.parseInt(split[11])
                );
                movements.add(movement);
            }
        } catch (Exception e) {
            LogUtils.sendError("[Movement Recorder] An error occurred while playing the recording.");
            e.printStackTrace();
            if (Failsafe.getInstance().isRunning()) {
                LogUtils.sendWarning("[Movement Recorder] Your recording is corrupted! Try to record it again. Switching to built-in failsafe mechanism instead.");
                Failsafe.getInstance().resetCustomMovement();
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        isMovementReading = false;
        isMovementPlaying = true;
        Movement movement = movements.get(0);
        rotateBeforePlaying.easeTo(
                new RotationConfiguration(
                        new Rotation(movement.yaw, movement.pitch),
                        500, null
                )
        );
    }

    @Override
    public void stop() {
        KeyBindUtils.stopMovement();
        if (isMovementPlaying || isMovementReading) {
            LogUtils.sendDebug("[Movement Recorder] Playing has been stopped.");
            return;
        }
        LogUtils.sendDebug("[Movement Recorder] No recording has been started.");
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        playingIndex = 0;
        currentDelay = 0;
        isMovementPlaying = false;
        isMovementReading = false;
        attackKeyPressed = false;
        recordingName = "";
        resetTimers();
    }

    @Override
    public boolean isToggled() {
        return false;
    }

    @SubscribeEvent
    public void onTickPlayMovement(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (!isMovementPlaying || isMovementReading)
            return;
        if (movements.isEmpty()) {
            LogUtils.sendError("[Movement Recorder] The file is empty!");
            if (Failsafe.getInstance().isRunning()) {
                LogUtils.sendWarning("[Movement Recorder] Your recording is corrupted! Try to record it again. Switching to built-in failsafe mechanism instead.");
                Failsafe.getInstance().resetCustomMovement();
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        if (!MacroHandler.getInstance().isMacroToggled()) {
            LogUtils.sendDebug("[Movement Recorder] Macro has been disabled. Stopping playing.");
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        if (rotateBeforePlaying.isRotating()) {
            KeyBindUtils.stopMovement();
            return;
        }

        Movement movement = movements.get(playingIndex);
        setPlayerMovement(movement);
        rotateBeforePlaying.easeTo(
                new RotationConfiguration(
                        new Rotation(movement.yaw, movement.pitch),
                        49, null
                )
        );

        if (currentDelay < movement.delay) {
            currentDelay++;
            return;
        }
        attackKeyPressed = false;
        playingIndex++;
        currentDelay = 0;
        if (playingIndex >= movements.size()) {
            isMovementPlaying = false;
            resetTimers();
            LogUtils.sendDebug("[Movement Recorder] Playing has been finished.");
            stop();
            resetStatesAfterMacroDisabled();
        }
    }

    private static void resetTimers() {
        rotateBeforePlaying.reset();
        rotateDuringPlaying.reset();
    }

    private void setPlayerMovement(Movement movement) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), movement.forward);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), movement.left);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), movement.backwards);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), movement.right);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), movement.sneak);
        mc.thePlayer.setSprinting(movement.sprint);
        if (mc.thePlayer.capabilities.allowFlying && mc.thePlayer.capabilities.isFlying != movement.fly)
            mc.thePlayer.capabilities.isFlying = movement.fly;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), movement.jump);
        if (movement.attack && !attackKeyPressed) {
            KeyBindUtils.leftClick();
            attackKeyPressed = true;
        }
    }
}
