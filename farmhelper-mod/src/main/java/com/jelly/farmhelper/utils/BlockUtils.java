package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.VerticalMacroEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;

import com.jelly.farmhelper.macros.MushroomMacro;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

import java.util.Arrays;

import static com.jelly.farmhelper.utils.AngleUtils.get360RotationYaw;

public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] walkables = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.trapdoor };
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
                LogUtils.debugFullLog("Carpet color: " + blockState.getValue(BlockCarpet.COLOR));
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

    public static boolean leftCropIsReady(){
        Block crop = getRelativeBlock(-1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? 2 : 1, 1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType ==  SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? MushroomMacro.closest90Yaw : mc.thePlayer.rotationYaw);
        System.out.println(crop);
        if (crop.equals(Blocks.nether_wart)) {
            return mc.theWorld.getBlockState(getRelativeBlockPos(-1, 1, 1)).getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return mc.theWorld.getBlockState(getRelativeBlockPos(-1, 1, 1)).getValue(BlockCrops.AGE) == 7;
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
        Block crop = getRelativeBlock(1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? 2 : 1, 1, (FarmHelper.config.macroType && (FarmHelper.config.SShapeMacroType ==  SMacroEnum.MUSHROOM.ordinal() || FarmHelper.config.SShapeMacroType == SMacroEnum.MUSHROOM_ROTATE.ordinal())) ? MushroomMacro.closest90Yaw : mc.thePlayer.rotationYaw);
        System.out.println(crop);
        if (crop.equals(Blocks.nether_wart)) {
            return mc.theWorld.getBlockState(getRelativeBlockPos(1, 1, 1)).getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return mc.theWorld.getBlockState(getRelativeBlockPos(1, 1, 1)).getValue(BlockCrops.AGE) == 7;
        else if (crop.equals(Blocks.brown_mushroom) || crop.equals(Blocks.red_mushroom)) {
            return true;
        } else if (crop.equals(Blocks.cactus)) {
            return true;
        } else {
            crop = getRelativeBlock(1, 0, 1);
            return crop.equals(Blocks.melon_block) || crop.equals(Blocks.pumpkin);
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
        if (currentPositionBlock.equals(Blocks.dirt) && !mc.theWorld.getBlockState(currentPosition.down()).getBlock().isPassable(mc.theWorld, currentPosition.down())) {
            return true;
        }
        return false;
    }

    public static boolean isBlockVisible(BlockPos pos) {
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return mop == null || (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 2) || mop.getBlockPos().equals(pos);
    }
}
