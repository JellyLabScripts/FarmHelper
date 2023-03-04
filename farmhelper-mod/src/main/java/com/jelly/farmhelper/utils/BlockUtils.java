package com.jelly.farmhelper.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3i;

import java.util.Arrays;

import static com.jelly.farmhelper.utils.AngleUtils.get360RotationYaw;

public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] walkables = { Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet };

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

    public static Block getRelativeBlock(float x, float y, float z) {
        return (mc.theWorld.getBlockState(
          new BlockPos(
            mc.thePlayer.posX + (getUnitX() * z) + (getUnitZ() * -1 * x),
            mc.thePlayer.posY + y,
            mc.thePlayer.posZ + (getUnitZ() * z) + (getUnitX() * x)
          )).getBlock());
    }
    public static BlockPos getRelativeBlockPos(float x, float y, float z) {
           return new BlockPos(
                    mc.thePlayer.posX + (getUnitX() * z) + (getUnitZ() * -1 * x),
                    mc.thePlayer.posY + y,
                    mc.thePlayer.posZ + (getUnitZ() * z) + (getUnitX() * x)
            );
    }

    public static Block getRelativeBlock(float x, float y, float z, float yaw) {
        return (mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + (getUnitX(yaw) * z) + (getUnitZ(yaw) * -1 * x),
                        mc.thePlayer.posY + y,
                        mc.thePlayer.posZ + (getUnitZ(yaw) * z) + (getUnitX(yaw) * x)
                )).getBlock());
    }
    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        return new BlockPos(
                mc.thePlayer.posX + (getUnitX(yaw) * z) + (getUnitZ(yaw) * -1 * x),
                mc.thePlayer.posY + y,
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
        return getRelativeBlock(-1, 0, 0);
    }
    public static Block getRightBlock(){
        return getRelativeBlock(1, 0, 0);
    }

    public static Block getLeftTopBlock() {
        return getRelativeBlock(-1, 1, 0);
    }

    public static Block getRightTopBlock() {
        return getRelativeBlock(1, 1, 0);
    }

    public static Block getBackBlock(){
        return getRelativeBlock(0, 0, -1);
    }
    public static Block getFrontBlock(){
        return getRelativeBlock(0, 0, 1);
    }

    public static boolean isWalkable(Block block) {
        return Arrays.asList(walkables).contains(block);
    }

    public static boolean leftCropIsReady(){
        Block crop = getRelativeBlock(-1, 1, 1);
        if (crop.equals(Blocks.nether_wart)) {
            return mc.theWorld.getBlockState(getRelativeBlockPos(-1, 1, 1)).getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return mc.theWorld.getBlockState(getRelativeBlockPos(-1, 1, 1)).getValue(BlockCrops.AGE) == 7;
        else {
            crop = getRelativeBlock(-1, 0, 1);
            return crop.equals(Blocks.melon_block) || crop.equals(Blocks.pumpkin);
        }
    }
    public static boolean rightCropIsReady(){
        Block crop = getRelativeBlock(1, 1, 1);
        if (crop.equals(Blocks.nether_wart)) {
            return mc.theWorld.getBlockState(getRelativeBlockPos(1, 1, 1)).getValue(BlockNetherWart.AGE) == 3;
        } else if (crop.equals(Blocks.wheat) || crop.equals(Blocks.carrots) || crop.equals(Blocks.potatoes))
            return mc.theWorld.getBlockState(getRelativeBlockPos(1, 1, 1)).getValue(BlockCrops.AGE) == 7;
        else {
            crop = getRelativeBlock(1, 0, 1);
            return crop.equals(Blocks.melon_block) || crop.equals(Blocks.pumpkin);
        }
    }
}
