package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

public class CobwebFailsafe extends Failsafe {
    private static CobwebFailsafe instance;

    public static CobwebFailsafe getInstance() {
        if (instance == null) {
            instance = new CobwebFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.COBWEB_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnCobwebFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnCobwebFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnCobwebFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnCobwebFailsafe;
    }

    @Override
    public void duringFailsafeTrigger() {
        switch (cobwebCheckState) {
            case NONE:
                if (cobwebsRemoved()) break;
                cobwebCheckState = CobwebCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                if (cobwebsRemoved()) break;
                MacroHandler.getInstance().pauseMacro();
                if (rotationBeforeReacting == null)
                    rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                positionBeforeReacting = mc.thePlayer.getPosition();
                cobwebCheckState = CobwebCheckState.PLAY_RECORDING;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case PLAY_RECORDING:
                if (cobwebsRemoved()) break;
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Start_");
                cobwebCheckState = CobwebCheckState.WAIT_BEFORE_SENDING_MESSAGE_1;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_1:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!FarmHelperConfig.sendFailsafeMessage) {
                    cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (!CustomFailsafeMessagesPage.customJacobMessages.isEmpty()
                        && GameStateHandler.getInstance().inJacobContest()
                        && Math.random() > CustomFailsafeMessagesPage.customJacobChance / 100.0) {
                    String[] customJacobMessages = CustomFailsafeMessagesPage.customJacobMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customJacobMessages);
                } else if (CustomFailsafeMessagesPage.customRotationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customRotationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                cobwebCheckState = CobwebCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(randomMessage.length() * 150L, 1000);
                break;
            case SEND_MESSAGE:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                if (FeatureManager.getInstance().isAnyOtherFeatureEnabled()) {
                    LogUtils.sendDebug("Ending failsafe earlier because another feature is enabled");
                    cobwebCheckState = CobwebCheckState.END;
                    break;
                }
                FailsafeManager.getInstance().rotation.easeTo(new RotationConfiguration(new Rotation((float) (rotationBeforeReacting.getYaw() + (Math.random() * 30 - 15)), (float) (Math.random() * 30 + 30)),
                        500, null));
                cobwebCheckState = CobwebCheckState.PLAY_RECORDING_2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case PLAY_RECORDING_2:
                if (cobwebsRemoved()) break;
                if (rotation.isRotating())
                    break;
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Continue_");
                cobwebCheckState = CobwebCheckState.WAIT_BEFORE_SENDING_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!FarmHelperConfig.sendFailsafeMessage || Math.random() < 0.3) {
                    cobwebCheckState = CobwebCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (CustomFailsafeMessagesPage.customContinueMessages.isEmpty()) {
                    randomContinueMessage = FailsafeManager.getRandomContinueMessage();
                } else {
                    String[] customContinueMessages = CustomFailsafeMessagesPage.customContinueMessages.split("\\|");
                    randomContinueMessage = FailsafeManager.getRandomMessage(customContinueMessages);
                }
                cobwebCheckState = CobwebCheckState.SEND_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(randomContinueMessage.length() * 150L, 1000);
                break;
            case SEND_MESSAGE_2:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomContinueMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomContinueMessage);
                cobwebCheckState = CobwebCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 10) {
                    BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                    cobwebCheckState = CobwebCheckState.GO_BACK_END;
                } else {
                    cobwebCheckState = CobwebCheckState.WARP_GARDEN;
                }
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || !BaritoneHandler.isWalkingToGoalBlock()) {
                    cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                FailsafeManager.getInstance().scheduleDelay(200);
                break;
            case ROTATE_TO_POS_BEFORE_2:
                if (MacroHandler.getInstance().isTeleporting()) break;
                if (rotation.isRotating()) break;
                FailsafeManager.getInstance().rotation.easeTo(new RotationConfiguration(new Rotation((float) (rotationBeforeReacting.getYaw() + (Math.random() * 30 - 15)), (float) (Math.random() * 30 + 30)),
                        500, null));
                cobwebCheckState = CobwebCheckState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WARP_GARDEN:
                MacroHandler.getInstance().triggerWarpGarden(true, false);
                cobwebCheckState = CobwebCheckState.ROTATE_TO_POS_BEFORE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(3000, 1000);
                break;
            case END:
                this.endOfFailsafeTrigger();
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();
        FailsafeManager.getInstance().restartMacroAfterDelay();
    }

    @Override
    public void resetStates() {
        cobwebCheckState = CobwebCheckState.NONE;
        rotationBeforeReacting = null;
        positionBeforeReacting = null;
        randomMessage = null;
        randomContinueMessage = null;
        rotation.reset();
        cobwebs.clear();
    }

    public boolean isTouchingCobwebBlock() {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = new BlockPos(mc.thePlayer.getPosition().add(x, y, z));
                    double distance = Math.sqrt(mc.thePlayer.getPositionEyes(1).distanceTo(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
                    if (mc.theWorld.getBlockState(pos).getBlock().equals(Blocks.web) && distance <= 1.5) {
                        if (cobwebs.stream().noneMatch(tuple -> tuple.getFirst().equals(pos))) {
                            cobwebs.add(new Tuple<>(pos, System.currentTimeMillis()));
                            LogUtils.sendDebug("[Failsafe] Cobweb added: " + pos);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean cobwebsRemoved() {
        cobwebs.removeIf(tuple -> {
            if (System.currentTimeMillis() - tuple.getSecond() > 120_000 || !mc.theWorld.getBlockState(tuple.getFirst()).getBlock().equals(Blocks.web)) {
                LogUtils.sendDebug("[Failsafe] Cobweb removed: " + tuple.getFirst());
                return true;
            }
            return false;
        });
        if (cobwebs.isEmpty()) {
            if (cobwebCheckState == CobwebCheckState.NONE || cobwebCheckState == CobwebCheckState.WAIT_BEFORE_START) {
                FailsafeManager.getInstance().emergencyQueue.remove(this);
                if (FailsafeManager.getInstance().emergencyQueue.isEmpty()) {
                    LogUtils.sendWarning("[Failsafe] Cobweb check failsafe was triggered but the admin removed the blocks immediately. §c§lDO NOT REACT§e TO THIS OR YOU WILL GET BANNED!");
                    if (FailsafeNotificationsPage.notifyOnCobwebFailsafe)
                        LogUtils.webhookLog("[Failsafe]\nCobweb check failsafe was triggered but the admin removed the blocks immediately. DO NOT REACT TO THIS OR YOU WILL GET BANNED!");
                }
                return true;
            }
            LogUtils.sendDebug("No cobwebs left!");
            cobwebCheckState = CobwebCheckState.GO_BACK_START;
            return true;
        }
        return false;
    }

    public boolean hasCobwebs() {
        return !cobwebs.isEmpty();
    }

    private final ArrayList<Tuple<BlockPos, Long>> cobwebs = new ArrayList<>();

    private CobwebCheckState cobwebCheckState = CobwebCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    String randomMessage;
    String randomContinueMessage;

    enum CobwebCheckState {
        NONE,
        WAIT_BEFORE_START,
        PLAY_RECORDING,
        WAIT_BEFORE_SENDING_MESSAGE_1,
        SEND_MESSAGE,
        ROTATE_TO_POS_BEFORE,
        PLAY_RECORDING_2,
        WAIT_BEFORE_SENDING_MESSAGE_2,
        SEND_MESSAGE_2,
        ROTATE_TO_POS_BEFORE_2,
        GO_BACK_START,
        GO_BACK_END,
        WARP_GARDEN,
        END,
    }
}
