package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.BlockChangeEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.LagDetector;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

public class DirtFailsafe extends Failsafe {
    private static DirtFailsafe instance;

    public static DirtFailsafe getInstance() {
        if (instance == null) {
            instance = new DirtFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.DIRT_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnDirtFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnDirtFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnDirtFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnDirtFailsafe;
    }

    @Override
    public void onBlockChange(BlockChangeEvent event) {
        if (FailsafeManager.getInstance().firstCheckReturn()) return;
        if (event.update.getBlock().equals(Blocks.air) && !CropUtils.isCrop(event.old.getBlock())) {
            LogUtils.sendDebug("[Failsafe] Block destroyed: " + event.pos);
            blocksDestroyedByPlayer.add(new Tuple<>(event.pos, System.currentTimeMillis()));
        }
        blocksDestroyedByPlayer.removeIf(tuple -> System.currentTimeMillis() - tuple.getSecond() > 10000);
        if ((event.old.getBlock().equals(Blocks.air) || CropUtils.isCrop(event.old.getBlock()) || event.old.getBlock().equals(Blocks.water) || event.old.getBlock().equals(Blocks.flowing_water)) &&
                event.update.getBlock() != null && !event.update.getBlock().equals(Blocks.air) &&
                !CropUtils.isCrop(event.update.getBlock()) && event.update.getBlock().isCollidable() &&
                !event.update.getBlock().equals(Blocks.trapdoor) && !event.update.getBlock().equals(Blocks.ladder) &&
                !event.update.getBlock().equals(Blocks.water) && !event.update.getBlock().equals(Blocks.flowing_water) &&
                event.update.getBlock().isFullCube()) { // If old block was air or crop and new block is not air, crop, trapdoor, water or flowing water
            if (blocksDestroyedByPlayer.stream().anyMatch(tuple -> tuple.getFirst().equals(event.pos))) {
                LogUtils.sendDebug("[Failsafe] Block destroyed by player and resynced by hypixel: " + event.pos);
                return;
            }
            if (LagDetector.getInstance().wasJustLagging()) {
                LogUtils.sendDebug("[Failsafe] Lag detected, ignoring block change: " + event.pos);
                return;
            }
            LogUtils.sendWarning("[Failsafe] Someone put a block on your garden! Block: " + event.update.getBlock() + " Pos: " + event.pos);
            dirtBlocks.add(new Tuple<>(event.pos, System.currentTimeMillis()));
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
            // just in case something in the hand keeps opening the screen
            if (FailsafeManager.getInstance().swapItemDuringRecording && mc.thePlayer.inventory.currentItem > 1)
                FailsafeManager.getInstance().selectNextItemSlot();
            return;
        }
        switch (dirtCheckState) {
            case NONE:
                if (blocksRemoved()) break;
                dirtCheckState = DirtCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WAIT_BEFORE_START:
                if (blocksRemoved()) break;
                MacroHandler.getInstance().pauseMacro();
                maxReactions = (int) Math.round(3 + Math.random() * 3);
                LogUtils.sendDebug("[Failsafe] Minimum reactions: " + maxReactions);
                if (BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.dirt))
                    dirtOnLeft = true;
                else if (BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.dirt))
                    dirtOnLeft = false;
                LogUtils.sendDebug("[Failsafe] Dirt on left: " + dirtOnLeft);
                LogUtils.sendDebug("[Failsafe] Yaw difference: " + AngleUtils.getClosest());
                MovRecPlayer.setYawDifference(AngleUtils.getClosest());
                FailsafeManager.getInstance().swapItemDuringRecording = Math.random() < 0.2;
                positionBeforeReacting = mc.thePlayer.getPosition();
                rotationBeforeReacting = new Rotation(mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch);
                dirtCheckState = DirtCheckState.PLAY_RECORDING;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case PLAY_RECORDING:
                if (blocksRemoved()) break;
                if (dirtOnLeft)
                    MovRecPlayer.getInstance().playRandomRecording("DIRT_CHECK_Left_Start_");
                else
                    MovRecPlayer.getInstance().playRandomRecording("DIRT_CHECK_Right_Start_");
                dirtCheckState = DirtCheckState.WAIT_BEFORE_SENDING_MESSAGE;
                break;
            case WAIT_BEFORE_SENDING_MESSAGE:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!FarmHelperConfig.sendFailsafeMessage) {
                    dirtCheckState = DirtCheckState.KEEP_PLAYING;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (!CustomFailsafeMessagesPage.customJacobMessages.isEmpty()
                        && GameStateHandler.getInstance().inJacobContest()
                        && Math.random() > CustomFailsafeMessagesPage.customJacobChance / 100.0) {
                    String[] customJacobMessages = CustomFailsafeMessagesPage.customJacobMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customJacobMessages);
                } else if (CustomFailsafeMessagesPage.customDirtMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customDirtMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                dirtCheckState = DirtCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(randomMessage.length() * 150L, 1000);
                break;
            case SEND_MESSAGE:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                dirtCheckState = DirtCheckState.KEEP_PLAYING;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case KEEP_PLAYING:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (FailsafeManager.getInstance().swapItemDuringRecording && Math.random() > 0.6)
                    FailsafeManager.getInstance().swapItemDuringRecording = false;
                if (blocksRemoved()) break;
                String tempRecordingName = "DIRT_CHECK_";
                if (dirtOnLeft)
                    tempRecordingName += "Left_";
                else
                    tempRecordingName += "Right_";
                if (Math.random() < 0.2) {
                    dirtCheckState = DirtCheckState.WAIT_BEFORE_SENDING_MESSAGE;
                    FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                    break;
                } else if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    tempRecordingName += "JumpBoost_";
                } else if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    tempRecordingName += "Fly_";
                } else {
                    tempRecordingName += "OnGround_";
                }
                if (maxReactions > 0) {
                    LogUtils.sendDebug("[Failsafe] Playing recording: " + tempRecordingName);
                    MovRecPlayer.getInstance().playRandomRecording(tempRecordingName);
                    maxReactions--;
                } else {
                    FlyPathFinderExecutor.getInstance().findPath(new Vec3(positionBeforeReacting.getX() + 0.5, positionBeforeReacting.getY() + 3, positionBeforeReacting.getZ() + 0.5), true, true);
                    dirtCheckState = DirtCheckState.GO_BACK_END;
                }
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (FailsafeManager.getInstance().swapItemDuringRecording)
                    FailsafeManager.getInstance().swapItemDuringRecording = false;
                if (FlyPathFinderExecutor.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 1) {
                    dirtCheckState = DirtCheckState.ROTATE_TO_POS_BEFORE;
                    break;
                }
                BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                dirtCheckState = DirtCheckState.GO_BACK_END;
                break;
            case GO_BACK_END:
                if (FlyPathFinderExecutor.getInstance().isRunning())
                    break;
                if (BaritoneHandler.isWalkingToGoalBlock()) {
                    LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                } else
                    dirtCheckState = DirtCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                if (FailsafeManager.getInstance().rotation.isRotating()) break;
                FailsafeManager.getInstance().rotation.easeTo(new RotationConfiguration(new Rotation((float) (rotationBeforeReacting.getYaw() + (Math.random() * 30 - 15)), (float) (Math.random() * 30 + 30)),
                        500, null));
                dirtCheckState = DirtCheckState.END_DIRT_CHECK;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case END_DIRT_CHECK:
                endOfFailsafeTrigger();
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
        blocksDestroyedByPlayer.clear();
        dirtBlocks.clear();
        dirtCheckState = DirtCheckState.NONE;
        positionBeforeReacting = null;
        rotationBeforeReacting = null;
        randomMessage = null;
        dirtOnLeft = false;
        maxReactions = 3;
    }

    public boolean isTouchingDirtBlock() {
        for (Tuple<BlockPos, Long> tuple : dirtBlocks) {
            BlockPos dirtBlock = tuple.getFirst();
            double distance = Math.sqrt(mc.thePlayer.getPositionEyes(1).distanceTo(new Vec3(dirtBlock.getX() + 0.5, dirtBlock.getY() + 0.5, dirtBlock.getZ() + 0.5)));
            LogUtils.sendDebug(distance + " " + dirtBlock);
            if (distance <= 1.5) {
                return true;
            }
        }
        return false;
    }

    private boolean blocksRemoved() {
        dirtBlocks.removeIf(tuple -> {
            if (System.currentTimeMillis() - tuple.getSecond() > 120_000 || !mc.theWorld.getBlockState(tuple.getFirst()).getBlock().isCollidable() || BlockUtils.canWalkThrough(tuple.getFirst()) || mc.theWorld.getBlockState(tuple.getFirst()).getBlock().equals(Blocks.air) || CropUtils.isCrop(mc.theWorld.getBlockState(tuple.getFirst()).getBlock()) || mc.theWorld.getBlockState(tuple.getFirst()).getBlock().equals(Blocks.water) || mc.theWorld.getBlockState(tuple.getFirst()).getBlock().equals(Blocks.flowing_water)) {
                if (blocksDestroyedByPlayer.stream().anyMatch(block -> block.getFirst().equals(tuple.getFirst()))) {
                    LogUtils.sendDebug("[Failsafe] Dirt block removed by player: " + tuple.getFirst());
                    return false;
                }
                LogUtils.sendDebug("[Failsafe] Dirt block removed: " + tuple.getFirst());
                return true;
            }
            return false;
        });
        if (dirtBlocks.isEmpty()) {
            if (dirtCheckState == DirtCheckState.NONE || dirtCheckState == DirtCheckState.WAIT_BEFORE_START) {
                FailsafeManager.getInstance().emergencyQueue.remove(this);
                if (FailsafeManager.getInstance().emergencyQueue.isEmpty()) {
                    LogUtils.sendWarning("[Failsafe] Dirt check failsafe was triggered but the admin removed the blocks immediately. §c§lDO NOT REACT§e TO THIS OR YOU WILL GET BANNED!");
                    if (FailsafeNotificationsPage.notifyOnDirtFailsafe)
                        LogUtils.webhookLog("[Failsafe]\nDirt check failsafe was triggered but the admin removed the blocks immediately. DO NOT REACT TO THIS OR YOU WILL GET BANNED!");
                }
                return true;
            }
            LogUtils.sendDebug("No dirt blocks left!");
            dirtCheckState = DirtCheckState.GO_BACK_START;
            return true;
        }
        return false;
    }

    public boolean hasDirtBlocks() {
        return !dirtBlocks.isEmpty();
    }

    private final ArrayList<Tuple<BlockPos, Long>> dirtBlocks = new ArrayList<>();
    private final ArrayList<Tuple<BlockPos, Long>> blocksDestroyedByPlayer = new ArrayList<>();

    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    private boolean dirtOnLeft = false;
    private int maxReactions = 3;
    private DirtCheckState dirtCheckState = DirtCheckState.NONE;
    String randomMessage;

    enum DirtCheckState {
        NONE,
        WAIT_BEFORE_START,
        PLAY_RECORDING,
        WAIT_BEFORE_SENDING_MESSAGE,
        SEND_MESSAGE,
        KEEP_PLAYING,
        GO_BACK_END,
        GO_BACK_START,
        ROTATE_TO_POS_BEFORE,
        END_DIRT_CHECK
    }
}
