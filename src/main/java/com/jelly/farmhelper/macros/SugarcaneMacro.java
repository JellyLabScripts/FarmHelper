package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import java.util.ArrayList;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.BlockUtils.isWalkable;
import static com.jelly.farmhelper.utils.KeyBindUtils.*;
import static com.jelly.farmhelper.FarmHelper.gameState;

public class SugarcaneMacro extends Macro{
    public static State lastLaneDirection;
    public static State currentState;
    public static boolean pushedOff;
    volatile static boolean stuck = false;
    public int layerY = 0;

    static boolean setspawnLag;
    State lastState;


    private static final Rotation rotation = new Rotation();

    enum State{
        DROPPING,
        TPPAD,
        RIGHT, 
        LEFT,
        FORWARD,
        SWITCH,
        NONE
    }

    public static BlockPos targetBlockPos = new BlockPos(1000, 1000, 1000);

    private static float playerYaw = 0;


    @Override
    public void onEnable(){
        initializeVariables();
        mc.thePlayer.closeScreen();
        enabled = true;
        playerYaw = AngleUtils.get360RotationYaw(AngleUtils.getClosest());
        layerY = (int)mc.thePlayer.posY;
        rotation.easeTo(playerYaw, 0, 500);
        currentState = State.NONE;
        lastState = State.NONE;
        targetBlockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    @Override
    public void onDisable(){
        stopMovement();
    }

    @Override
    public void onRender(){
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @Override
    public void onChatMessageReceived(String msg){
        if(msg.contains("spawn location has been set"))
            setspawnLag = false;
    }


    @Override
    public void onTick(){
        if (gameState.currentLocation != GameState.location.ISLAND) {
            updateKeys(false, false, false, false, false);
            return;
        }

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }
        if(stuck)
            return;

        mc.thePlayer.inventory.currentItem = InventoryUtils.getSCHoeSlot();
        mc.thePlayer.rotationPitch = 0;

        if(Antistuck.stuck) {
            new Thread(fixStuck).start();
            stuck = true;
        }

        updateState();
        lastState = currentState;
        switch (currentState){
            case TPPAD:
                if (BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) {
                    if (mc.thePlayer.posY % 1 == 0.8125) {
                        if (isWalkable(getRelativeBlock(1, 0.1875f, 0))) {
                            LogUtils.debugFullLog("On top of pad, go right");
                            updateKeys(false, false, true, false, true);
                        } else if (isWalkable(getRelativeBlock(-1, 0.1875f, 0))) {
                            LogUtils.debugFullLog("On top of pad, go left");
                            updateKeys(false, false, false, true, true);
                        } else {
                            LogUtils.debugFullLog("On top of pad, cant detect where to go");
                            updateKeys(false, false, false, false, false);
                        }
                    } else if (mc.thePlayer.posY % 1 < 0.8125) {
                        LogUtils.debugFullLog("Stuck in teleport pad, holding space");
                        updateKeys(false, false, false, false, false, false, true);
                    } else {
                        LogUtils.debugFullLog("Waiting for teleport land (close)");
                        updateKeys(false, false, false, false, false);
                    }
                }
                return;
            case LEFT:
                updateKeys(false, false, false, !setspawnLag, true, false, false);
                LogUtils.debugFullLog("Going left");
                return;
            case RIGHT:
                updateKeys(false, false, !setspawnLag, false, true, false, false);
                LogUtils.debugFullLog("Going right");
                return;
            case SWITCH: {
                updateKeys(false, false, false, false, false, true, false);
                if (!pushedOff) {
                    updateKeys(false, false, !gameState.leftWalkable, gameState.leftWalkable, false, true, false);
                }
                pushedOff = true;
                return;
            }
            case FORWARD: {
                updateKeys(true, false, false, false, true, !BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, -1, 1)) && !BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, -1, 0)), false);
                LogUtils.debugFullLog("Going Forward");
                pushedOff = false;
                return;
            }
            case DROPPING: {
                LogUtils.debugFullLog("Rotating");
                if (!rotation.completed) {
                    if (!rotation.rotating) {
                        if(mc.thePlayer.posY - mc.thePlayer.lastTickPosY == 0) {
                            LogUtils.debugFullLog("Rotating 180");
                            rotation.reset();
                            playerYaw = AngleUtils.get360RotationYaw(playerYaw + getRotateAmount());
                            rotation.easeTo(playerYaw, 0, 2000);
                        }
                    }
                    LogUtils.debugFullLog("Waiting Rotating");
                    updateKeys(false, false, false, false, false);
                } else {
                    if (mc.thePlayer.posY % 1 == 0) {
                        LogUtils.debugFullLog("Dropped, resuming");

                        //rotation.reset();
                        layerY = (int)mc.thePlayer.posY;
                        targetBlockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                        currentState = State.NONE;
                        lastState = State.NONE;
                    } else {
                        LogUtils.debugFullLog("Falling");
                    }
                    updateKeys(false, false, false, false, false);
                }
                return;
            }
            case NONE:
                updateKeys(false, false, false, false, false, false, false);
        }



    }
    private void updateState() {
        BlockPos blockInPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) {
            currentState = State.TPPAD;
            return;
        }
        if(currentState == State.DROPPING || (mc.thePlayer.posY - mc.thePlayer.lastTickPosY != 0 && Math.abs(mc.thePlayer.posY - layerY) > 1 && currentState != State.TPPAD && BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.air) && gameState.currentLocation == GameState.location.ISLAND)) {
            LogUtils.debugLog("dropping" + " " + (currentState == State.DROPPING));
            currentState = State.DROPPING;
            return;
        }
        if(lastState == State.SWITCH){
            currentState = State.FORWARD;
            return;
        }
        if(lastState == State.FORWARD){
            if(blockInPos.getX() != targetBlockPos.getX() || blockInPos.getZ() != targetBlockPos.getZ() || !isInCenterOfBlock())
                return;
            mc.thePlayer.sendChatMessage("/setspawn");
            setspawnLag = true;
            currentState = BlockUtils.isWalkable(BlockUtils.getLeftBlock()) ? State.LEFT : State.RIGHT;
            return;
        }
        if((!gameState.leftWalkable || !gameState.rightWalkable) &&
                (blockInPos.getX() != targetBlockPos.getX() || blockInPos.getZ() != targetBlockPos.getZ()) &&
               gameState.dx == 0 && gameState.dz == 0){

            LogUtils.debugFullLog("switch");
            currentState = State.SWITCH;
            targetBlockPos = calculateTargetBlockPos();
            return;
        }
        if (lastState != currentState || currentState == State.NONE) {
            LogUtils.debugFullLog("Idk lol");
            currentState = calculateDirection();
            rotation.reset();
        }
    }
    //threads
    Runnable fixStuck = () -> {
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
            stuck = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        }catch(Exception e){
            e.printStackTrace();
        }
    };
    BlockPos calculateTargetBlockPos(){
        if(!BlockUtils.isWalkable(BlockUtils.getRightBlock()) || !BlockUtils.isWalkable(BlockUtils.getLeftBlock())){
            if(!BlockUtils.isWalkable(BlockUtils.getRightBlock()) && !BlockUtils.isWalkable(BlockUtils.getLeftBlock())){
                return BlockUtils.getRelativeBlockPos(0, 0, 1);
            } else {
                if(!BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 1, 0)) && !BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 1, 0))) {
                    LogUtils.scriptLog("Detected one block off");
                    return BlockUtils.getRelativeBlockPos(0, 0, 5);
                }
                else {
                    return BlockUtils.getRelativeBlockPos(0, 0, 6);
                }

            }
        }

        LogUtils.scriptLog("can't calculate target block!");
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);


    }

    State calculateDirection() {
        ArrayList<Integer> unwalkableBlocks = new ArrayList<>();
        if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock().equals(Blocks.end_portal_frame)) {
            for (int i = -3; i < 3; i++) {
                if (!BlockUtils.isWalkable(BlockUtils.getRelativeBlock(i, 0, 1))) {
                    unwalkableBlocks.add(i);
                }
            }
        } else {
            for (int i = -3; i < 3; i++) {
                if (!BlockUtils.isWalkable(BlockUtils.getRelativeBlock(i, 0, 0))) {
                    unwalkableBlocks.add(i);
                }
            }
        }

        if (unwalkableBlocks.size() == 0)
            return State.RIGHT;
        else if (unwalkableBlocks.get(0) > 0)
            return State.LEFT;
        else
            return State.RIGHT;
    }
    int getRotateAmount(){
        if(BlockUtils.getRelativeBlock(1, 0, 2).equals(Blocks.dirt) || BlockUtils.getRelativeBlock(-1, 0, 2).equals(Blocks.dirt)
        || BlockUtils.getRelativeBlock(1, 0, -2).equals(Blocks.dirt) || BlockUtils.getRelativeBlock(-1, 0, -2).equals(Blocks.dirt))
            return 180;

        System.out.println(BlockUtils.getRelativeBlock(2, 0, 2));
        if(BlockUtils.getRelativeBlock(2, 0, 2).equals(Blocks.dirt) || BlockUtils.getRelativeBlock(2, 0, -2).equals(Blocks.dirt))
            return 90;


        if(BlockUtils.getRelativeBlock(-2, 0, -2).equals(Blocks.dirt) || BlockUtils.getRelativeBlock(-2, 0, 2).equals(Blocks.dirt))
            return -90;

        LogUtils.scriptLog("Unknown rotation case");
        return 0;
    }


    void initializeVariables() {
        pushedOff = false;
        lastLaneDirection = calculateDirection();
        currentState = calculateDirection();
        stuck = false;
        setspawnLag = false;
    }
    public static boolean isInCenterOfBlock(){
        return (Math.round(AngleUtils.get360RotationYaw()) == 180 || Math.round(AngleUtils.get360RotationYaw()) == 0) ?Math.abs(Minecraft.getMinecraft().thePlayer.posZ) % 1 > 0.3f && Math.abs(Minecraft.getMinecraft().thePlayer.posZ) % 1 < 0.7f :
                Math.abs(Minecraft.getMinecraft().thePlayer.posX) % 1 > 0.3f && Math.abs(Minecraft.getMinecraft().thePlayer.posX) % 1 < 0.7f;

    }



}
