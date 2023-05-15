package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class CocoaBeanMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        DROPPING,
        TP_PAD,
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
    private boolean tpFlag;
    private float yaw;
    private float pitch;
    private int axeEquipFails = 0;
    private int notmovingticks = 0;
    private double prevPlayerX;
    private double prevPlayerZ;

    @Override
    public void onEnable() {
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        antistuckActive = false;
        Antistuck.stuck = false;
        tpFlag = false;
        Antistuck.cooldown.schedule(1000);
        pitch = -90f;
        yaw = AngleUtils.getClosest();
        rotation.easeTo(yaw, pitch, 500);
    }

    @Override
    public void onDisable() {
        updateKeys(false, false, false, false, false);
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
        if (currentState == State.TP_PAD && (Math.abs(prevPlayerZ - mc.thePlayer.posZ) > 10 || Math.abs(prevPlayerX - mc.thePlayer.posX) > 10)) {
            LogUtils.debugLog("Detected teleport due to sudden large position change");
            tpFlag = true;
        }

        if (gameState.currentLocation != GameState.location.ISLAND) {
            updateKeys(false, false, false, false, false);
            enabled = false;
            return;
        }

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
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
                        //new Thread(fixRowStuck).start();
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
            case TP_PAD:
                if (Math.abs(mc.thePlayer.posY - layerY) > 0.5 || tpFlag) {
                    LogUtils.debugLog("Teleported!");
                    updateKeys(true, false, false, false, false);
                    tpFlag = false;
                    currentState = State.LEFT;
                } else {
                    LogUtils.debugLog("Waiting for teleport land");
                    updateKeys(false, false, false, false, false);
                }
                return;
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

        prevPlayerX = mc.thePlayer.posX;
        prevPlayerZ = mc.thePlayer.posZ;
    }

    private void updateState() {
        State lastState = currentState;

        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) || (currentState == State.TP_PAD && !tpFlag)) {
            currentState = State.TP_PAD;
        } else if (layerY - mc.thePlayer.posY > 1 || currentState == State.DROPPING || isDropping()) {
            currentState = State.DROPPING;
        } else if (currentState == State.SWITCH_ROW) {
            if (BlockUtils.getRelativeBlock(-1, 0, 1).equals(Blocks.cobblestone)) {
                currentState = State.LEFT;
            }
        } else if (currentState == State.SWITCH_SIDE) {
            if (BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.cobblestone)) {
                currentState = State.RIGHT;
            }
        } else if (currentState == State.KEEP_RIGHT && !BlockUtils.getRelativeBlock(0, 0, -1).equals(Blocks.air)) {
            currentState = State.SWITCH_ROW;
        } else if (currentState == State.LEFT_KEEP && BlockUtils.getRelativeBlock(-1, 0, 1).equals(Blocks.air)) {
            currentState = State.SWITCH_SIDE;
        } else if (BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.cobblestone)) {
            if ((currentState == State.NONE || (currentState == State.LEFT && lastState == State.LEFT))) {
                currentState = State.LEFT_KEEP;
            }
        } else if (BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.cobblestone)) {
            if ((currentState == State.NONE || (currentState == State.RIGHT && lastState == State.RIGHT))) {
                currentState = State.KEEP_RIGHT;
            }
        } else {
            currentState = State.NONE;
        }

        if (lastState == State.TP_PAD && lastState != currentState) {
            layerY = mc.thePlayer.posY;
        }

        if (lastState != currentState) {
            //TODO: Further test set spawn
            if(currentState == State.KEEP_RIGHT || currentState == State.LEFT_KEEP) {
                PlayerUtils.attemptSetSpawn();
            }
            rotation.reset();
            tpFlag = false;
        }
    }

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
