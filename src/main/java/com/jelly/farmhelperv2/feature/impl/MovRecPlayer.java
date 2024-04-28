package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.FailsafeUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.OldRotationUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.*;

/*
    Credits to Yuro for this superb class
    https://github.com/onixiya1337/MovementRecorder
*/

public class MovRecPlayer implements IFeature {

    // region BOOLEANS, LISTS, ETC
    private static final List<Movement> movements = new ArrayList<>();
    static Minecraft mc = Minecraft.getMinecraft();
    private static MovRecPlayer instance;
    private static boolean isMovementPlaying = false;
    private static int currentDelay = 0;
    private static int playingIndex = 0;
    @Setter
    @Getter
    public static float yawDifference = 0;
    @Setter
    private List<Movement> selectedRecording = new ArrayList<>();
    private static final OldRotationUtils rotateBeforePlaying = new OldRotationUtils();
    private static final OldRotationUtils rotateDuringPlaying = new OldRotationUtils();
    private final HashMap<String, List<Movement>> recordings = new HashMap<>();
    // endregion

    // region CONSTRUCTOR

    public static MovRecPlayer getInstance() {
        if (instance == null) {
            instance = new MovRecPlayer();
        }
        return instance;
    }

    public MovRecPlayer() {
        List<String> resourceFiles;
        try {
            resourceFiles = getResourceFiles("/farmhelper/movrec");
            if (resourceFiles.isEmpty()) {
                FailsafeUtils.getInstance().sendNotification("Resource folder not found! Report this to #bug-reports!", TrayIcon.MessageType.WARNING);
                LogUtils.sendError("Resource folder not found! Report this to #bug-reports!");
                return;
            }
            for (String file : resourceFiles) {
                List<String> lines = new ArrayList<>();
                System.out.println("file: " + file);
                if (!file.endsWith(".movement")) continue;
                if (file.contains("/build/classes/")) {
                    file = file.split("/build/classes/java/main")[1];
                }
                InputStream inputStream = FarmHelper.class.getResourceAsStream(file);

                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    reader.close();
                } else {
                    LogUtils.sendError("Resource not found: " + file);
                    continue;
                }

                List<Movement> movements = new ArrayList<>();
                for (String line : lines) {
                    movements.add(getMovement(line));
                }
                System.out.println("Added " + file + " to recordings. Lines for movements: " + movements.size());
                recordings.put(file, movements);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void resetTimers() {
        rotateBeforePlaying.reset();
        rotateDuringPlaying.reset();
    }

    @Override
    public String getName() {
        return "Movement Recording Player";
    }

    @Override
    public boolean isRunning() {
        return isMovementPlaying;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public boolean isToggled() {
        return false;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        playingIndex = 0;
        currentDelay = 0;
        isMovementPlaying = false;
        selectedRecording.clear();
        resetTimers();
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

    // endregion

    public void playRandomRecording(String pattern) {
        List<String> matchingFiles = new ArrayList<>();
        for (String file : recordings.keySet()) {
            if (file.contains(pattern)) {
                matchingFiles.add(file);
            }
        }
        if (!matchingFiles.isEmpty()) {
            Random random = new Random();
            int randomIndex = random.nextInt(matchingFiles.size());
            String recordingName = matchingFiles.get(randomIndex)
                    .replace("/", "")
                    .replace("\\", "")
                    .replace(".movement", "")
                    .replace("farmhelpermovrec", "");
            LogUtils.sendDebug("[Movement Recorder] Selected recording: " + recordingName);
            selectedRecording = new ArrayList<>(recordings.get(matchingFiles.get(randomIndex)));
            start();
        }
    }

    @Override
    public void start() {
        if (selectedRecording.isEmpty()) {
            LogUtils.sendError("[Movement Recorder] No recording selected!");
            if (FailsafeManager.getInstance().triggeredFailsafe.isPresent())
                LogUtils.sendWarning("RIP bozo, recording name is empty! Send logs to #bug-reports!");
            return;
        }
        if (isMovementPlaying) {
            LogUtils.sendDebug("[Movement Recorder] The recording is playing already.");
            return;
        }
        movements.clear();
        playingIndex = 0;
        resetTimers();
        movements.addAll(selectedRecording);
        isMovementPlaying = true;
        Movement movement = movements.get(0);
//        yawDifference = AngleUtils.normalizeAngle(AngleUtils.getClosest() - movement.yaw);
//        LogUtils.sendSuccess("movement.yaw: " + movement.yaw + " yawDifference: " + yawDifference);
//        LogUtils.sendSuccess("easeTo: " + (movement.yaw + yawDifference));
        rotateBeforePlaying.easeTo(movement.yaw + yawDifference, movement.pitch, 500);
        IFeature.super.start();
    }

    @Override
    public void stop() {
        KeyBindUtils.stopMovement();
        IFeature.super.stop();
        if (isRunning()) {
            LogUtils.sendDebug("[Movement Recorder] Playing has been stopped.");
        }
    }

    @SubscribeEvent
    public void onTickPlayMovement(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (!isRunning())
            return;
        if (movements.isEmpty()) {
            if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
                LogUtils.sendError("[Movement Recorder] RIP bozo, the file is empty!");
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        if (!MacroHandler.getInstance().isMacroToggled()) {
            if (isRunning()) {
                LogUtils.sendDebug("[Movement Recorder] Macro has been disabled. Stopping playing.");
            }
            stop();
            resetStatesAfterMacroDisabled();
            return;
        }
        if (rotateBeforePlaying.rotating) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }

        Movement movement = movements.get(playingIndex);
        setPlayerMovement(movement);
//        LogUtils.sendSuccess("movement.yaw: " + movement.yaw + " yawDifference: " + yawDifference);
//        LogUtils.sendSuccess("easeTo: " + (movement.yaw + yawDifference));
        rotateDuringPlaying.easeTo(movement.yaw + yawDifference, movement.pitch, 49);

        if (currentDelay < movement.delay) {
            currentDelay++;
            if (currentDelay > 7
                    && movement.delay > 23
                    && Math.random() < 0.3
                    && FailsafeManager.getInstance().swapItemDuringRecording
                    && FailsafeManager.getInstance().triggeredFailsafe.isPresent())
                FailsafeManager.getInstance().selectNextItemSlot();
            return;
        }
        playingIndex++;
        currentDelay = 0;
        if (playingIndex >= movements.size()) {
            isMovementPlaying = false;
            LogUtils.sendDebug("[Movement Recorder] Playing has been finished.");
            stop();
            resetStatesAfterMacroDisabled();
        }
    }

    // region HELPER_METHODS

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

    @NotNull
    private static Movement getMovement(String line) {
        String[] split = line.split(";");
        return new Movement(
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
    }

    private void setPlayerMovement(Movement movement) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), movement.forward);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), movement.left);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), movement.backwards);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), movement.right);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), movement.sneak);
        mc.thePlayer.setSprinting(movement.sprint);
        if (mc.thePlayer.capabilities.allowFlying && mc.thePlayer.capabilities.isFlying != movement.fly) {
            mc.thePlayer.capabilities.isFlying = movement.fly;
            mc.thePlayer.sendPlayerAbilities();
            LogUtils.sendDebug("[Movement Recorder] Fly mode has been " + (movement.fly ? "enabled" : "disabled") + "!");
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), movement.jump);
        if (movement.attack && currentDelay == 0)
            KeyBindUtils.leftClick();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), movement.attack);
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        FileSystem fs;
        URL url = FarmHelper.class.getResource(path);
        if (url == null) {
            return filenames;
        }
        try {
            URI uri = url.toURI();
            Path myPath;
            if ("file".equals(uri.getScheme())) {
                myPath = Paths.get(Objects.requireNonNull(FarmHelper.class.getResource(path)).toURI());
            } else {
                fs = FileSystems.newFileSystem(uri, new HashMap<>());
                myPath = fs.getPath(path);
            }
            Iterator<Path> it = Files.walk(myPath).iterator();
            while (it.hasNext()) {
                Path filename = it.next();
                filenames.add(filename.toString().replace("\\", "/"));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return filenames;
    }

    // endregion
}
