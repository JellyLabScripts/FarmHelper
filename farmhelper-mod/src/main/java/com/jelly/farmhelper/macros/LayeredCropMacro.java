package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

//TODO: Add drop rotation detection
public class LayeredCropMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        DROPPING,
        TP_PAD,
        SWITCH_START,
        SWITCH_MID,
        SWITCH_END,
        RIGHT,
        LEFT,
        STONE_THROW,
        NONE
    }

    enum StoneThrowState {
        ROTATE_AWAY,
        LIFT,
        SWITCH,
        DROP,
        ROTATE_BACK,
        NONE
    }

    private final Rotation rotation = new Rotation();
    public State currentState;
    private StoneThrowState stoneState;
    private double layerY;
    private boolean antistuckActive;
    private float yaw;
    private float pitch;
    private final Clock stoneDropTimer = new Clock();
    private int hoeSlot;
    private int hoeEquipFails = 0;
    private int notmovingticks = 0;

    private final Clock tpCoolDown = new Clock();
    private boolean isTping = false;
    private final Clock waitForChangeDirection = new Clock();


    @Override
    public void onEnable() {
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        antistuckActive = false;
        Antistuck.stuck = false;
        Antistuck.cooldown.schedule(1000);
        if (FarmConfig.cropType == CropEnum.NETHERWART || FarmConfig.cropType == CropEnum.CACTUS) {
            pitch = (float) (0f + Math.random() * 0.5f);
        } else {
            pitch = (float) (2.8f + Math.random() * 0.5f);
        }
        yaw = AngleUtils.getClosest();
        tpCoolDown.reset();
        waitForChangeDirection.reset();
        rotation.easeTo(yaw, pitch, 500);
        isTping = false;
    }

    @Override
    public void onChatMessageReceived(String msg) {
        super.onChatMessageReceived(msg);
        if (msg.contains("Warped from the ") && msg.contains(" to the ")) {
            tpCoolDown.schedule(1500);
            isTping = false;
            LogUtils.debugLog("Tped");
        }
    }

    @Override
    public void onDisable() {
        updateKeys(false, false, false, false, false);
    }

    @Override
    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @Override
    public void onPacketReceived(ReceivePacketEvent event) {
        if(rotation.rotating && event.packet instanceof S08PacketPlayerPosLook &&
                (currentState == State.DROPPING || (currentState == State.TP_PAD && tpCoolDown.passed()))) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
        }
    }

    @Override
    public void onTick() {

        if (rotation.rotating && currentState != State.TP_PAD) {
            updateKeys(false, false, false, false, false);
            return;
        }

        if (isTping) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (tpCoolDown.isScheduled() && tpCoolDown.passed()) {
            tpCoolDown.reset();
        } else if (tpCoolDown.isScheduled() && !tpCoolDown.passed()) {
            return;
        }

        if(currentState != State.DROPPING && currentState != State.STONE_THROW) {

            boolean flag = AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FailsafeConfig.rotationSens
                    || Math.abs(mc.thePlayer.rotationPitch - pitch) > FailsafeConfig.rotationSens;


            if (currentState == State.TP_PAD && !tpCoolDown.passed()) {
                if(flag && (Math.round(AngleUtils.get360RotationYaw()) % 90 != 0 || mc.thePlayer.rotationPitch != pitch)) {
                    if (tpCoolDown.getRemainingTime() < 500) {
                        yaw = AngleUtils.getClosest();
                        rotation.easeTo(yaw, pitch, 500);
                        return;
                    }
                }
            } else if(flag) {
                rotation.reset();
                Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
                return;
            }
        }


        if (Antistuck.stuck && currentState != State.STONE_THROW) {
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
                    case SWITCH_START:
                    case SWITCH_MID:
                        new Thread(fixSwitchStuck).start();
                        break;
                    case SWITCH_END:
                        new Thread(fixCornerStuck).start();
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
        System.out.println(currentState);

        switch (currentState) {
            case TP_PAD:
//                if(!tpCoolDown.passed()) {
//                    LogUtils.debugLog("Waiting for cd");
//                    KeyBindUtils.stopMovement();
//                    break;
//                }
//                tpCoolDown.reset();

                if(mc.thePlayer.capabilities.isFlying || (!getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) && !mc.thePlayer.onGround)) {
                    LogUtils.debugLog("Pressing shift");
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
                } else if (mc.thePlayer.posY % 1 > 0 && mc.thePlayer.posY % 1 < 0.8125f) {
                    LogUtils.debugLog("Pressing space");
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
                } else {
                    if(mc.gameSettings.keyBindRight.isKeyDown()) {
                        LogUtils.debugLog("On top of pad, keep going right");
                        updateKeys(false, false, true, false, true, false, false);
                    } else if(mc.gameSettings.keyBindLeft.isKeyDown()) {
                        LogUtils.debugLog("On top of pad, keep going left");
                        updateKeys(false, false, false, true, true, false, false);
                    } else if (isWalkable(getRelativeBlock(1, 0.1875f, 0))) {
                        LogUtils.debugLog("On top of pad, go right");
                        updateKeys(false, false, true, false, false, false, false);
                    } else if (isWalkable(getRelativeBlock(-1, 0.1875f, 0))) {
                        LogUtils.debugLog("On top of pad, go left");
                        updateKeys(false, false, false, true, false, false, false);
                    } else if (isWalkable(getRelativeBlock(0, 0.1875f, 1))) {
                        LogUtils.debugLog("On top of pad, go forward");
                        updateKeys(true, false, false, false, false, false, false);
                    } else {
                        LogUtils.debugLog("On top of pad, cant detect where to go");
                        updateKeys(false, false, false, false, false, false, false);
                    }
                }

                return;
            case DROPPING:
                if (layerY - mc.thePlayer.posY > 1) {
                    if (!rotation.completed && FarmConfig.farmType == FarmEnum.LAYERED) {
                        if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating 180");
                            rotation.reset();
                            yaw = AngleUtils.get360RotationYaw(yaw + 180);
                            rotation.easeTo(yaw, pitch, (long) (1000 + Math.random() * 1000));
                        }
                        LogUtils.debugFullLog("Waiting Rotating 180");
                        updateKeys(false, false, false, false, false);
                    } else {
                        if (mc.thePlayer.posY % 1 == 0) {
                            LogUtils.debugLog("Dropped, resuming");
                            layerY = mc.thePlayer.posY;
                            rotation.reset();
                            currentState = State.NONE;
                            PlayerUtils.attemptSetSpawn();
                        } else {
                            LogUtils.debugFullLog("Falling");
                        }
                        updateKeys(false, false, false, false, false);
                    }
                } else {
                    if (BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(0, 1, 1).equals(Blocks.air)) {
                        updateKeys(true, false, false, false, false);
                    } else if (BlockUtils.getRelativeBlock(1, -1, 0).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.air)) {
                        updateKeys(false, false, true, false, true);
                    } else if (BlockUtils.getRelativeBlock(-1, -1, 0).equals(Blocks.air)
                                    && BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)
                                    && BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.air)) {
                        updateKeys(false, false, false, true, true);
                    }
                        LogUtils.debugLog("Not at drop yet, keep going");
                }

                return;
            case RIGHT:
                LogUtils.debugLog("Middle of row, going right");
                updateKeys(FarmConfig.cropType != CropEnum.CACTUS && shouldWalkForwards(), FarmConfig.cropType == CropEnum.CACTUS && shouldPushBack(), true, false, findAndEquipHoe());
                return;
            case LEFT:
                LogUtils.debugLog("Middle of row, going left");
                updateKeys(FarmConfig.cropType != CropEnum.CACTUS && shouldWalkForwards(), FarmConfig.cropType == CropEnum.CACTUS && shouldPushBack(), false, true, findAndEquipHoe());
                return;
            case SWITCH_START:
                LogUtils.debugFullLog("Continue forwards");
                updateKeys(true, false, false, false, false);
                return;
            case SWITCH_MID:
                LogUtils.debugLog("Middle of switch, keep going forwards");
                updateKeys(true, false, false, false, false);
                return;
            case SWITCH_END:
                if (gameState.rightWalkable) {
                    LogUtils.debugFullLog("Continue going right");
                    updateKeys(false, false, true, false, findAndEquipHoe());
                } else if (gameState.leftWalkable) {
                    LogUtils.debugFullLog("Continue going left");
                    updateKeys(false, false, false, true, findAndEquipHoe());
                }
                return;
            case STONE_THROW:
                updateKeys(false, false, false, false, false);
                int stoneSlot = PlayerUtils.getSlotForItem("Stone");
                switch (stoneState) {
                    case ROTATE_AWAY:
                        if (rotation.completed) {
                            stoneState = StoneThrowState.LIFT;
                            rotation.reset();
                            return;
                        } else if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating away");
                            hoeSlot = mc.thePlayer.inventory.currentItem;
                            mc.thePlayer.inventory.currentItem = 6;
                            if (gameState.rightWalkable) {
                                rotation.easeTo(yaw - 90, pitch, 1000);
                            } else {
                                rotation.easeTo(yaw + 90, pitch, 1000);
                            }
                        }
                        return;
                    case LIFT:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Lifting");
                            PlayerUtils.clickOpenContainerSlot(stoneSlot);
                            PlayerUtils.clickOpenContainerSlot(stoneSlot, 0, 6);
                            stoneDropTimer.schedule(200);
                        } else if (stoneDropTimer.passed()) {
                            stoneState = StoneThrowState.SWITCH;
                            stoneDropTimer.reset();
                        }
                        return;
                    case SWITCH:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Switching");
                            stoneDropTimer.schedule(1000);
                            PlayerUtils.clickOpenContainerSlot(35 + 7, 0, 0);
                        } else if (stoneDropTimer.passed()) {
                            stoneState = StoneThrowState.DROP;
                            stoneDropTimer.reset();
                        }
                        return;
                    case DROP:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Dropping");
                            mc.thePlayer.dropOneItem(true);
                            stoneDropTimer.schedule(1000);
                        } else if (stoneDropTimer.passed()) {
                            rotation.reset();
                            stoneState = StoneThrowState.ROTATE_BACK;
                            stoneDropTimer.reset();
                        }
                        return;
                    case ROTATE_BACK:
                        if (rotation.completed) {
                            currentState = State.SWITCH_END;
                            rotation.reset();
                            Antistuck.stuck = false;
                            Antistuck.cooldown.schedule(2000);
                            return;
                        } else if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating back");
                            mc.thePlayer.inventory.currentItem = hoeSlot;
                            rotation.easeTo(yaw, pitch, 1000);
                        }
                        return;
                }
            case NONE:
                LogUtils.debugFullLog("idk");

        }
    }


    private void updateState() {
        State lastState = currentState;

        if (currentState == State.STONE_THROW) {
            currentState = State.STONE_THROW;
        } else if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) && !tpCoolDown.isScheduled()) {
            currentState = State.TP_PAD;
            isTping = true;
            LogUtils.debugLog("On the TP Pad, tping");
//            if(!tpCoolDown.isScheduled() && lastState != currentState)
//                tpCoolDown.schedule(1500);
        } else if (currentState != State.TP_PAD && (layerY - mc.thePlayer.posY > 1 || currentState == State.DROPPING || isDropping())) {
            currentState = State.DROPPING;
        } else if (gameState.leftWalkable && gameState.rightWalkable) {
            // layerY = mc.thePlayer.posY;
            if (currentState != State.RIGHT && currentState != State.LEFT) {
                PlayerUtils.attemptSetSpawn();
                currentState = calculateDirection();
            }
        } else if (gameState.frontWalkable && !gameState.backWalkable && (FarmConfig.cropType != CropEnum.CACTUS || !BlockUtils.getRelativeBlock(0, 0, 2).equals(Blocks.cactus))) {
            if(waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                currentState = State.SWITCH_START;
                waitForChangeDirection.reset();
                System.out.println("Switching to start");
                return;
            }
            if (!waitForChangeDirection.isScheduled()) {
                long waitTime = (long) (Math.random() * 500 + 250);
                System.out.println("Waiting " + waitTime + "ms");
                waitForChangeDirection.schedule(waitTime);
            }
        } else if (gameState.frontWalkable && gameState.backWalkable) {
            currentState = State.SWITCH_MID;
        } else if (gameState.backWalkable) {
            if(waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                currentState = State.SWITCH_END;
                waitForChangeDirection.reset();
                System.out.println("Switching to end");
                return;
            }
            if (!waitForChangeDirection.isScheduled()) {
                long waitTime = (long) (Math.random() * 500 + 250);
                System.out.println("Waiting2 " + waitTime + "ms");
                waitForChangeDirection.schedule(waitTime);
            }
        } else if (gameState.leftWalkable) {
            currentState = State.LEFT;
        } else if (gameState.rightWalkable) {
            currentState = State.RIGHT;
        } else {
            currentState = State.NONE;
        }

        if (lastState == State.TP_PAD && lastState != currentState) {
            layerY = mc.thePlayer.posY;
        }

        if (lastState != currentState) {
            waitForChangeDirection.reset();
            rotation.reset();
        }
    }

    private State calculateDirection() {

        if (leftCropIsReady()) {
            return State.LEFT;
        } else if (rightCropIsReady()) {
            return State.RIGHT;
        }

        for (int i = 1; i < 180; i++) {
            if (!isWalkable(BlockUtils.getRelativeBlock(i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1)) || isWalkable(BlockUtils.getRelativeBlock(i - 1, -1, 0))) {
                    return State.RIGHT;
                } else {
                    LogUtils.debugFullLog("Failed right: " + BlockUtils.getRelativeBlock(i - 1, 0, 1));
                    return State.LEFT;
                }
            } else if (!isWalkable(BlockUtils.getRelativeBlock(-i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-i + 1, 0, 1)) || isWalkable(BlockUtils.getRelativeBlock(-i + 1, -1, 0))) {
                    return State.LEFT;
                } else {
                    LogUtils.debugFullLog("Failed left: " + isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1)));
                    return State.RIGHT;
                }
            }
        }
        LogUtils.debugLog("Cannot find direction. Length > 180");
        return State.NONE;
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

    public Runnable fixSwitchStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, false, false, true, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, true, false, false);
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

    public Runnable fixCornerStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            if (gameState.rightWalkable) {
                updateKeys(false, false, true, false, false);
            } else {
                updateKeys(false, false, false, true, false);
            }
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            antistuckActive = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public boolean findAndEquipHoe() {
        int hoeSlot = PlayerUtils.getHoeSlot();
        if (hoeSlot == -1) {
            hoeEquipFails = hoeEquipFails + 1;
            if (hoeEquipFails > 10) {
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
            mc.thePlayer.inventory.currentItem = hoeSlot;
            hoeEquipFails = 0;
            return true;
        }
    }

    private static boolean shouldWalkForwards() {
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        System.out.println(angle);
        if (angle == 0) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 90) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        } else if (angle == 180) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 270) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        }
        return false;
    }

    private boolean shouldPushBack() {
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        System.out.println(angle);
        Block blockBehind = BlockUtils.getRelativeBlock(0, 0, -1);
        if (!blockBehind.equals(Blocks.cobblestone) && !blockBehind.equals(Blocks.carpet)) return false;
        if (angle == 0) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 90) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        } else if (angle == 180) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 270) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        }
        return false;
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