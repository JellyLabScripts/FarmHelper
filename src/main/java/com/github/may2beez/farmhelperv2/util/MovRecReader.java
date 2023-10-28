package com.github.may2beez.farmhelperv2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Credits: https://github.com/onixiya1337/MovementRecorder
public class MovRecReader {
    private static List<Movement> movements = new ArrayList<>();
    private static boolean isMovementPlaying = false;
    private static boolean isMovementReading = false;
    private static int currentDelay = 0;
    private static int playingIndex = 0;
    static Minecraft mc = Minecraft.getMinecraft();
    private static RotationUtils rotateBeforePlaying = new RotationUtils();
    private static RotationUtils rotateDuringPlaying = new RotationUtils();

    public static class Movement {
        private final boolean forward;
        private final boolean left;
        private final boolean backwards;
        private final boolean right;
        private final boolean sneak;
        private final boolean sprint;
        private final boolean jump;
        private final boolean attack;
        private final float yaw;
        private final float pitch;
        private int delay;

        public Movement(boolean forward, boolean left, boolean backwards, boolean right, boolean sneak, boolean sprint, boolean jump,
                        boolean attack, float yaw, float pitch, int delay) {
            this.forward = forward;
            this.left = left;
            this.backwards = backwards;
            this.right = right;
            this.sneak = sneak;
            this.sprint = sprint;
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
        if (rotateBeforePlaying.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        Movement movement = movements.get(playingIndex);
        setPlayerMovement(movement);
        rotateDuringPlaying.easeTo(movement.yaw, movement.pitch, 49);

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

    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent event) {
        if (rotateDuringPlaying.rotating) {
            rotateDuringPlaying.update();
            return;
        }
        if (rotateBeforePlaying.rotating) {
            rotateBeforePlaying.update();
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
                        Float.parseFloat(split[8]),
                        Float.parseFloat(split[9]),
                        Integer.parseInt(split[10])
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
        rotateBeforePlaying.easeTo(movement.yaw, movement.pitch, 500);
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
