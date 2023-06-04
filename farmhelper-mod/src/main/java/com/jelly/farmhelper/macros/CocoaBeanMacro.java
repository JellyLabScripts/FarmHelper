package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class CocoaBeanMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        DROPPING,
        SWITCH_ROW,
        SWITCH_SIDE,
        RIGHT,
        KEEP_RIGHT,
        LEFT,
        LEFT_KEEP,
        NONE
    }

    private final Rotation rotation = new Rotation();
    public State currentState;
    private double layerY;
    private boolean antistuckActive;
    private float yaw;
    private float pitch;
    private int axeEquipFails = 0;
    private int notmovingticks = 0;
    private final Clock waitForChangeDirection = new Clock();
    private final Clock lastTp = new Clock();

    private boolean isTping = false;

    @Override
    public void onEnable() {
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        antistuckActive = false;
        Antistuck.stuck = false;
        Antistuck.cooldown.schedule(1000);
        pitch = -70f + (float) (Math.random() * 0.6);
        yaw = AngleUtils.getClosest();
        rotation.easeTo(yaw, pitch, 500);
        isTping = false;
        lastTp.reset();
    }

    @Override
    public void onDisable() {
        updateKeys(false, false, false, false, false);
    }

    private BlockPos beforeTeleportationPos = null;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 1) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            layerY = mc.thePlayer.posY;
            lastTp.schedule(1_000);
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
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
    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @Override
    public void onOverlayRender(RenderGameOverlayEvent event) {
        if (!enabled) return;
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && currentState != null) {
            FontUtils.drawScaledString("State: " + currentState.name(), 1, 300, 100, true);
        }
    }

    @Override
    public void onTick() {
        if(mc.thePlayer == null || mc.theWorld == null) return;

        checkForTeleport();

        if (isTping) return;

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && mc.thePlayer.rotationPitch != pitch) {
            yaw = AngleUtils.getClosestDiagonal();
            rotation.easeTo(yaw, pitch, 500);
            KeyBindUtils.stopMovement();
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            return;
        }

        if (Antistuck.stuck) {
            LogUtils.debugFullLog("Antistuck");
            if (!antistuckActive) {
                LogUtils.webhookLog("Stuck, trying to fix");
                LogUtils.debugLog("Stuck, trying to fix");
                antistuckActive = true;
                switch (currentState) {
                    case RIGHT:
                    case LEFT:
                        new Thread(fixRowStuck).start();
                        break;
                    case SWITCH_ROW:
                        //
                }
            } else {
                if (notmovingticks > 500) {
                    antistuckActive = false;
                    notmovingticks = 0;
                }
                notmovingticks++;
            }
            return;
        }

        updateState();

        switch (currentState) {
            case RIGHT:
                LogUtils.debugLog("On right row, going back");
                updateKeys(false, false, true, false, findAndEquipAxe());
                return;
            case KEEP_RIGHT:
                LogUtils.debugLog("On right row, going back");
                updateKeys(false, true, true, false, findAndEquipAxe());
                return;
            case LEFT:
                LogUtils.debugLog("On left row, going forwards");
                updateKeys(true, false, false, false, findAndEquipAxe());
                return;
            case LEFT_KEEP:
                LogUtils.debugLog("On left row, going forwards");
                updateKeys(true, false, false, true, findAndEquipAxe());
                return;
            case SWITCH_ROW:
            case SWITCH_SIDE:
                LogUtils.debugLog("Finished row, going right");
                updateKeys(false, true, true, false, false);
        }
    }

    private void updateState() {
        State lastState = currentState;

        if (!isRewarpLocationSet() && !lastTp.isScheduled() && !isTping &&
                FarmHelper.gameState.dx < 0.01 && FarmHelper.gameState.dz < 0.01 && FarmHelper.gameState.dy < 0.01) {
            triggerWarpGarden();
            return;
        } else if (isRewarpLocationSet() && isStandingOnRewarpLocation()) {
            triggerWarpGarden();
            return;
        }

        if (layerY - mc.thePlayer.posY > 1 || currentState == State.DROPPING || isDropping()) {
            currentState = State.DROPPING;
        } else if (currentState == State.SWITCH_ROW) {
            if (BlockUtils.getRelativeBlock(-1, 0, 1).getMaterial().isSolid()) {
                currentState = State.LEFT;
            }
        } else if (currentState == State.SWITCH_SIDE) {
            if (BlockUtils.getRelativeBlock(1, 0, 0).getMaterial().isSolid()) {
                currentState = State.RIGHT;
            }
        } else if (currentState == State.KEEP_RIGHT && !BlockUtils.getRelativeBlock(0, 0, -1).equals(Blocks.air)) {
            currentState = State.SWITCH_ROW;
        } else if (currentState == State.LEFT_KEEP && BlockUtils.getRelativeBlock(-1, 0, 1).equals(Blocks.air)) {
            currentState = State.SWITCH_SIDE;
        } else if (BlockUtils.getRelativeBlock(-1, 0, 0).getMaterial().isSolid()) {
            if ((currentState == State.NONE || (currentState == State.LEFT && lastState == State.LEFT))) {
                currentState = State.LEFT_KEEP;
            }
        } else if (BlockUtils.getRelativeBlock(1, 0, 0).getMaterial().isSolid()) {
            if ((currentState == State.NONE || (currentState == State.RIGHT && lastState == State.RIGHT))) {
                currentState = State.KEEP_RIGHT;
            }
        } else {
            currentState = State.NONE;
        }

        if (lastState != currentState) {
            //TODO: Further test set spawn
            if(currentState == State.KEEP_RIGHT || currentState == State.LEFT_KEEP) {
                PlayerUtils.attemptSetSpawn();
            }
            rotation.reset();
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

    public Runnable fixRowStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            updateKeys(true, false, false, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            antistuckActive = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public boolean findAndEquipAxe() {
        int axeSlot = PlayerUtils.getAxeSlot();
        if (axeSlot == -1) {
            axeEquipFails = axeEquipFails + 1;
            if (axeEquipFails > 10) {
                LogUtils.webhookLog("No Hoe Detected 10 times, Quitting");
                LogUtils.debugLog("No Hoe Detected 10 times, Quitting");
                mc.theWorld.sendQuittingDisconnectingPacket();
            } else {
                LogUtils.webhookLog("No Hoe Detected");
                LogUtils.debugLog("No Hoe Detected");
                mc.thePlayer.sendChatMessage("/hub");
            }
            return false;
        } else {
            mc.thePlayer.inventory.currentItem = axeSlot;
            axeEquipFails = 0;
            return true;
        }
    }

    private static boolean isDropping(){

        return  (BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 1).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(1, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(-1, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 0).equals(Blocks.air));
    }
}
