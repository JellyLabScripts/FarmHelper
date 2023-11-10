package com.github.may2beez.farmhelperv2.util;

import com.github.may2beez.farmhelperv2.handler.RotationHandler;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Credits: https://github.com/onixiya1337/MovementRecorder
public class MovRecReader {
    private static final List<Movement> movements = new ArrayList<>();
    private static boolean isMovementPlaying = false;
    private static boolean isMovementReading = false;
    private static int currentDelay = 0;
    private static int playingIndex = 0;
    static Minecraft mc = Minecraft.getMinecraft();
    private static final RotationHandler rotateBeforePlaying = RotationHandler.getInstance();
    // TODO: Yuro fix this
    private static final RotationHandler rotateDuringPlaying = RotationHandler.getInstance();

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

    @SubscribeEvent
    public void onTickPlayMovement(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (!isMovementPlaying || isMovementReading)
            return;
        if (movements.isEmpty()) {
            LogUtils.sendError("The file is empty!");
            stopRecording();
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
                        40, null
                )
        );

        if (currentDelay < movement.delay) {
            currentDelay++;
            return;
        }
        playingIndex++;
        currentDelay = 0;
        if (playingIndex >= movements.size()) {
            isMovementPlaying = false;
            resetTimers();
            LogUtils.sendDebug("Playing has been finished.");
        }
    }

    public static void stopRecording() {
        playingIndex = 0;
        currentDelay = 0;
        resetTimers();
        KeyBindUtils.stopMovement();
        if (isMovementPlaying || isMovementReading) {
            isMovementPlaying = false;
            isMovementReading = false;
            LogUtils.sendSuccess("Playing has been stopped.");
            return;
        }
        LogUtils.sendError("No recording has been started.");
    }

    public static void playRecording(String name) {
        movements.clear();
        playingIndex = 0;
        resetTimers();
        if (isMovementPlaying) {
            LogUtils.sendError("The recording is playing already.");
            return;
        }
        isMovementReading = true;
        try {
            List<String> lines = java.nio.file.Files.readAllLines(new File(mc.mcDataDir + "\\movementrecorder\\" + name + ".movement").toPath());
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
                        Integer.parseInt(split[10]),
                        Integer.parseInt(split[11])
                );
                movements.add(movement);
            }
        } catch (Exception e) {
            LogUtils.sendError("An error occurred while playing the recording.");
            e.printStackTrace();
            isMovementReading = false;
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
        if (movement.attack) {
            KeyBindUtils.leftClick();
        }
    }


    public static boolean isPlaying() {
        return isMovementPlaying;
    }

    public static boolean isReading() {
        return isMovementReading;
    }
}
