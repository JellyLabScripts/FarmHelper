package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.config.Config.CropEnum;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class MushroomMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        LEFT,
        RIGHT,
        NONE
    }

    static State currentState;
    static State prevState;

    public static float closest90Yaw;
    float pitch;
    float yaw;

    Rotation rotation = new Rotation();

    private final Clock lastTp = new Clock();
    private boolean isTping = false;
    private boolean rotated = false;

    private final Clock waitForChangeDirection = new Clock();
    private final Clock waitBetweenTp = new Clock();

    private boolean stuck = false;

    @Override
    public void onEnable() {
        lastTp.reset();
        waitForChangeDirection.reset();
        waitBetweenTp.reset();
        pitch = (float) (Math.random() * 2 - 1); // -1 - 1
        CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;
        yaw = AngleUtils.getClosestDiagonal();
        closest90Yaw = AngleUtils.getClosest();
        prevState = null;
        beforeTeleportationPos = null;
        currentState = State.NONE;
        rotated = false;
        if (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) {
            rotation.easeTo(yaw, pitch, 500);
            rotated = true;
        }
        mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(CropEnum.MUSHROOM);
        isTping = false;
        stuck = false;
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    private State stateBeforeFailsafe = null;

    @Override
    public void saveLastStateBeforeDisable() {
        stateBeforeFailsafe = currentState;
        super.saveLastStateBeforeDisable();
    }

    @Override
    public void restoreState() {
        currentState = stateBeforeFailsafe;
        super.restoreState();
    }

    @Override
    public void onChatMessageReceived(String msg) {
        super.onChatMessageReceived(msg);
        if (msg.contains("Warped from the ") && msg.contains(" to the ")) {
            lastTp.schedule(1000);
            isTping = false;
            LogUtils.debugLog("Tped");
            waitBetweenTp.schedule(10000);
        }
    }

    private void triggerWarpGarden() {
        KeyBindUtils.stopMovement();
        if (waitForChangeDirection.isScheduled() && beforeTeleportationPos == null) {
            waitForChangeDirection.reset();
        }
        if (!waitForChangeDirection.isScheduled()) {
            LogUtils.debugLog("Should TP");
            long waitTime = (long) (Math.random() * 750 + 500);
            waitForChangeDirection.schedule(waitTime);
            beforeTeleportationPos = mc.thePlayer.getPosition();
        } else if (waitForChangeDirection.passed()) {
            mc.thePlayer.sendChatMessage(FarmHelper.gameState.wasInGarden ? "/warp garden" : "/is");
            isTping = true;
            waitForChangeDirection.reset();
        }
    }

    private BlockPos beforeTeleportationPos = null;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 1) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.schedule(1_000);
            currentState = State.NONE;
            rotated = false;
            waitForChangeDirection.reset();
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
    }

    @Override
    public void triggerTpCooldown() {
        lastTp.schedule(1500);
    }

    @Override
    public void onTick() {

        if (mc.thePlayer == null || mc.theWorld == null || stuck)
            return;

        checkForTeleport();

        if (isTping) return;

        if (rotation.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (!rotated) {
            KeyBindUtils.stopMovement();
        }

        if (waitBetweenTp.isScheduled() && waitBetweenTp.passed()) {
            waitBetweenTp.reset();
        }

        if (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
            if (currentState == State.RIGHT && !rotated) {
                pitch = (float) (Math.random() * 2 - 1); // -1 - 1
                rotation.easeTo(closest90Yaw + 30, pitch, 400);
                rotated = true;
                return;
            } else if (currentState == State.LEFT && !rotated) {
                pitch = (float) (Math.random() * 2 - 1); // -1 - 1
                rotation.easeTo(closest90Yaw - 30, pitch, 400);
                rotated = true;
                return;
            }
        }

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating) {
            yaw = AngleUtils.getClosestDiagonal();
            closest90Yaw = AngleUtils.getClosest();
            pitch = (float) (Math.random() * 2 - 1); // -1 - 1
            if (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal())
                rotation.easeTo(yaw, pitch, (long) (600 + Math.random() * 200));
            else if (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                if (currentState == State.RIGHT) {
                    rotation.easeTo(closest90Yaw + 30, pitch, 400);
                } else if (currentState == State.LEFT) {
                    rotation.easeTo(closest90Yaw - 30, pitch, 400);
                }
            }
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (lastTp.isScheduled() && lastTp.passed()) {
            lastTp.reset();
            currentState = calculateDirection();
        }

        LogUtils.debugLog("Current state: " + currentState);

        if ((FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) && !Failsafe.emergency && !rotation.rotating && !isTping && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FarmHelper.config.rotationCheckSensitivity || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity) && rotated) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        } else if ((FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) && !Failsafe.emergency && !rotation.rotating && !isTping && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), closest90Yaw + (currentState == State.LEFT ? -30 : 30)) > FarmHelper.config.rotationCheckSensitivity || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity) && rotated) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }


        if (beforeTeleportationPos != null) {
            LogUtils.debugLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            triggerWarpGarden();
            return;
        }

        if (Failsafe.emergency) {
            LogUtils.debugLog("Blocking changing movement due to emergency");
            return;
        }

        if(Antistuck.stuck){
            stuck = true;
            new Thread(fixRowStuck).start();
        }

        switch (currentState) {
            case RIGHT:
                LogUtils.debugLog("Going RIGHT");
                updateKeys((FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) || !mushroom45DegreeLeftSide(), false, (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) && mushroom45DegreeLeftSide(), false, true);
                break;
            case LEFT:
                LogUtils.debugLog("Going LEFT");
                updateKeys((FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal()) || mushroom45DegreeLeftSide(), false, false, (FarmHelper.config.macroType && FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) && mushroom45DegreeLeftSide(), true);
                break;
            default:
                LogUtils.debugLog("Error: dir == direction.NONE");
                stopMovement();
                changeStateTo(calculateDirection());
                break;
        }

        updateState();
    }

    private boolean mushroom45DegreeLeftSide() {
        return closest90Yaw - yaw == 45 || closest90Yaw - yaw == -315;
    }

    private void updateState() {
        if (!isRewarpLocationSet()) {
            LogUtils.scriptLog("Your rewarp position is not set!");
        } else if (isRewarpLocationSet() && isStandingOnRewarpLocation()) {
            triggerWarpGarden();
        }

        if (gameState.rightWalkable && !gameState.frontWalkable) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    changeStateTo(calculateDirection());
                    waitForChangeDirection.reset();
                    LogUtils.debugLog("Change direction to RIGHT");
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        } else if (gameState.leftWalkable && !gameState.frontWalkable) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    changeStateTo(State.LEFT);
                    waitForChangeDirection.reset();
                    LogUtils.debugLog("Change direction to LEFT");
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        }
    }

    @Override
    public void onLastRender() {
        if (rotation.rotating)
            rotation.update();
    }

    public Runnable fixRowStuck = () -> {
        try {
            LogUtils.scriptLog("Activated antistuck");
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, true, false, false);
            Thread.sleep(200);
            updateKeys(true, false, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, false, true, false);
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            stuck = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    private void changeStateTo(State newState) {
        State currState = currentState;
        currentState = newState;
        if (currState != currentState) {
            System.out.println("Changed direction to " + currentState + " from " + currState);
            prevState = currState;
            waitForChangeDirection.reset();
            rotated = false;
        }
    }

    State calculateDirection() {

        boolean f1 = true, f2 = true;

        if (rightCropIsReady()) {
            return State.RIGHT;
        } else if (leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, 0, 0, closest90Yaw)) && f1) {
                return State.RIGHT;
            }
            if (!isWalkable(getRelativeBlock(i, 1, 0, closest90Yaw)))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, 0, 0, closest90Yaw)) && f2) {
                return State.LEFT;
            }
            if (!isWalkable(getRelativeBlock(-i, 1, 0, closest90Yaw)))
                f2 = false;
        }
        System.out.println("No direction found");
        return State.NONE;
    }
}
