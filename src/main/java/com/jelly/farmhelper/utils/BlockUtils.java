package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.macros.MushroomMacroNew;
import net.minecraft.block.*;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.*;

import java.util.Arrays;

import static com.jelly.farmhelper.utils.AngleUtils.get360RotationYaw;
import static com.jelly.farmhelper.utils.AngleUtils.getClosest;
import static net.minecraft.block.BlockSlab.HALF;

public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] walkables = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.trapdoor };
    private static final Block[] initialWalkables = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.waterlily, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem };
    private static final Block[] walkablesMushroom = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.trapdoor, Blocks.red_mushroom, Blocks.brown_mushroom };
    private static final Block[] walkablesCactus = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.snow_layer, Blocks.trapdoor };

    public static int getUnitX(){
        return getUnitX((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static int getUnitZ(){
        return getUnitZ((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static int getUnitX(float modYaw) {
        if (get360RotationYaw(modYaw) < 45 || get360RotationYaw(modYaw) > 315) {
            return 0;
        } else if (get360RotationYaw(modYaw) < 135) {
            return -1;
        } else if (get360RotationYaw(modYaw) < 225) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int getUnitZ(float modYaw) {
        if (get360RotationYaw(modYaw) < 45 || get360RotationYaw(modYaw) > 315) {
            return 1;
        } else if (get360RotationYaw(modYaw) < 135) {
            return 0;
        } else if (get360RotationYaw(modYaw) < 225) {
            return -1;
        } else {
            return 0;
        }
    }

    public static Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static boolean isRelativeBlockPassable(float x, float y, float z) {
        return BlockUtils.getRelativeBlock(x, y, z).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(x, y, z));
    }

    public static Block getRelativeBlock(float x, float y, float z) {
        return (mc.theWorld.getBlockState(
          new BlockPos(
            mc.thePlayer.posX + (getUnitX() * z) + (getUnitZ() * -1 * x),
                  ((mc.thePlayer.posY % 1) > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
            mc.thePlayer.posZ + (getUnitZ() * z) + (getUnitX() * x)
          )).getBlock());
    }
    public static BlockPos getRelativeBlockPos(float x, float y, float z) {
           return new BlockPos(
                    mc.thePlayer.posX + (getUnitX() * z) + (getUnitZ() * -1 * x),
                   ((mc.thePlayer.posY % 1) > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                    mc.thePlayer.posZ + (getUnitZ() * z) + (getUnitX() * x)
            );
    }

    public static Block getRelativeBlock(float x, float y, float z, float yaw) {
        return (mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + (getUnitX(yaw) * z) + (getUnitZ(yaw) * -1 * x),
                        ((mc.thePlayer.posY % 1) > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                        mc.thePlayer.posZ + (getUnitZ(yaw) * z) + (getUnitX(yaw) * x)
                )).getBlock());
    }
    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        return new BlockPos(
                mc.thePlayer.posX + (getUnitX(yaw) * z) + (getUnitZ(yaw) * -1 * x),
                ((mc.thePlayer.posY % 1) > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + (getUnitZ(yaw) * z) + (getUnitX(yaw) * x)
        );
    }

    public static int countCarpet() {
        int r = 2;
        int count = 0;
        BlockPos playerPos = mc.thePlayer.getPosition();
        playerPos.add(0, 1, 0);
        Vec3i vec3i = new Vec3i(r, r, r);
        Vec3i vec3i2 = new Vec3i(r, r, r);
        for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i2))) {
            IBlockState blockState = mc.theWorld.getBlockState(blockPos);
            if (blockState.getBlock() == Blocks.carpet && blockState.getValue(BlockCarpet.COLOR) == EnumDyeColor.BROWN) {
                count++;
            }
        }
        return count;
    }

    public static int bedrockCount() {
        int count = 0;
        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                if(getBlock(mc.thePlayer.getPosition().add(i, 1, j)).equals(Blocks.bedrock))
                    count ++;
            }
        }
        return count;
    }
    public static boolean isWater(Block b){
        return b.equals(Blocks.water) || b.equals(Blocks.flowing_water);
    }

    public static Block getLeftBlock(){
        return getRelativeBlock(-1, 0, 0, mc.thePlayer.rotationYaw);
    }
    public static Block getRightBlock(){
        return getRelativeBlock(1, 0, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getLeftTopBlock() {
        return getRelativeBlock(-1, 1, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getRightTopBlock() {
        return getRelativeBlock(1, 1, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getBackBlock(){
        return getRelativeBlock(0, 0, -1);
    }
    public static Block getFrontBlock(){
        return getRelativeBlock(0, 0, 1);
    }

    public static boolean isWalkable(Block block) {
        if (!FarmHelper.config.macroType) {
            return Arrays.asList(walkables).contains(block);
        } else {
            if (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal() ||
                FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal()) {
                return Arrays.asList(walkablesMushroom).contains(block);
            } else
            if (FarmHelper.config.SShapeMacroType == SMacroEnum.CACTUS.ordinal()) {
                return Arrays.asList(walkablesCactus).contains(block);
            } else return Arrays.asList(walkables).contains(block);
        }
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        // blocks that are always walkable, no matter what the position is

        if (Arrays.asList(initialWalkables).contains(block))
            return true;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor)
            return state.getValue(BlockTrapDoor.OPEN);

        if (block instanceof BlockSnow)
            return state.getValue(BlockSnow.LAYERS) <= 5;

        return canWalkThroughBottom(blockPos) && canWalkThroughAbove(blockPos.add(0, 1, 0));
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();
        if (mc.thePlayer.posY % 1 >= 0.5)
            return true;

        if (block instanceof BlockSlab) {
            // if the player is on the bottom half of the slab, all slabs are walkable (top, bottom and double)
            if (mc.thePlayer.posY % 1 < 0.5) {
                if (((BlockSlab) block).isDouble())
                    return false;
                return state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
            }
        }

        if (block instanceof BlockCarpet)
            return true;

        if (block instanceof BlockStairs) {
            // check if the stairs are rotated in the direction the player is coming from
            EnumFacing facing = state.getValue(BlockStairs.FACING);
            BlockPos posDiff = blockPos.subtract(mc.thePlayer.getPosition());
            if (state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP)
                return false;
            if (facing == EnumFacing.NORTH && posDiff.getZ() < 0)
                return true;
            if (facing == EnumFacing.SOUTH && posDiff.getZ() > 0)
                return true;
            if (facing == EnumFacing.WEST && posDiff.getX() < 0)
                return true;
            return facing == EnumFacing.EAST && posDiff.getX() > 0;
        }

        return block.isPassable(mc.theWorld, blockPos);
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof BlockCarpet)
            return false;

        return block.isPassable(mc.theWorld, blockPos);
    }

    public static BlockPos getBlockPosLookingAt() {
        MovingObjectPosition mop = mc.thePlayer.rayTrace(5, 1);
        if (mop == null)
            return null;
        return mop.getBlockPos();
    }

    public static boolean leftCropIsReady(){
        if (!FarmHelper.gameState.leftWalkable) return false;
        Block crop = getRelativeBlock(-1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? 2 : 1, 1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType ==  SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? MushroomMacroNew.closest90Yaw : mc.thePlayer.rotationYaw);
        if (crop.equals(Blocks.nether_wart)) {
            return crop.getBlockState().getBaseState().getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return crop.getBlockState().getBaseState().getValue(BlockCrops.AGE) == 7;
        else if (crop.equals(Blocks.brown_mushroom) || crop.equals(Blocks.red_mushroom)) {
            return true;
        } else if (crop.equals(Blocks.cactus)) {
            return true;
        } else {
            crop = getRelativeBlock(-1, 0, 1);
            return crop.equals(Blocks.melon_block) || crop.equals(Blocks.pumpkin);
        }
    }
    public static boolean rightCropIsReady(){
        if (!FarmHelper.gameState.rightWalkable) return false;
        float yaw;
        if (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) {
            yaw = MushroomMacroNew.closest90Yaw;
        } else {
            if (FarmHelper.config.customYaw) {
                yaw = getClosest(FarmHelper.config.customYawLevel);
            } else {
                yaw = MacroHandler.currentMacro.yaw;
            }
        }

        Block crop = getRelativeBlock(1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? 2 : 1, 1, yaw);
        if (crop.equals(Blocks.nether_wart)) {
            return crop.getBlockState().getBaseState().getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return crop.getBlockState().getBaseState().getValue(BlockCrops.AGE) == 7;
        else if (crop.equals(Blocks.brown_mushroom) || crop.equals(Blocks.red_mushroom)) {
            return true;
        } else if (crop.equals(Blocks.cactus)) {
            return true;
        } else {
            crop = getRelativeBlock(1, 0, 1);
            Block crop2 = getRelativeBlock(1, 1, 1); // melonkingdebil farm
            Block crop3 = getRelativeBlock(1, 2, 1); // melonkingdebil farm
            return crop.equals(Blocks.melon_block) || crop.equals(Blocks.pumpkin) || crop2.equals(Blocks.melon_block) || crop2.equals(Blocks.pumpkin) || crop3.equals(Blocks.melon_block) || crop3.equals(Blocks.pumpkin);
        }
    }

    public static boolean canSetSpawn(BlockPos currentPosition) {
        Block currentPositionBlock = mc.theWorld.getBlockState(currentPosition).getBlock();
        if ((currentPositionBlock.equals(Blocks.carpet) || currentPositionBlock.equals(Blocks.stone_slab) || currentPositionBlock.equals(Blocks.stone_slab2) || currentPositionBlock.equals(Blocks.wooden_slab)) && !mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.air)) {
            return true;
        }
        if (currentPositionBlock.equals(Blocks.water) && (mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.trapdoor) || mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.iron_trapdoor))) {
            return true;
        }
        if (currentPositionBlock.equals(Blocks.air) && !mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.air)) {
            return true;
        }
        if (currentPositionBlock.equals(Blocks.reeds) && (mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.dirt) || mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.grass) || mc.theWorld.getBlockState(currentPosition.down()).getBlock().equals(Blocks.sand))) {
            return true;
        }
        if (currentPositionBlock.equals(Blocks.melon_stem) || currentPositionBlock.equals(Blocks.pumpkin_stem)) {
            return true;
        }
        return currentPositionBlock.equals(Blocks.dirt) && !mc.theWorld.getBlockState(currentPosition.down()).getBlock().isPassable(mc.theWorld, currentPosition.down());
    }

    public static boolean isBlockVisible(BlockPos pos) {
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return mop == null || (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 2) || mop.getBlockPos().equals(pos);
    }
}
