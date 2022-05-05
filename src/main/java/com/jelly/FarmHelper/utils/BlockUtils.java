package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.config.interfaces.FarmConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3i;

//import static com.jelly.FarmHelper.FarmHelper.cropAgeRefs;
//import static com.jelly.FarmHelper.FarmHelper.cropBlockStates;
import static me.acattoXD.WartMacro.cropBlockStates;
import static me.acattoXD.WartMacro.cropAgeRefs;

public class BlockUtils {
    private static Minecraft mc = Minecraft.getMinecraft();

    public static double getAverageAge(BlockPos pos) {
        LogUtils.debugFullLog("getAverageAge - enter");
        IBlockState current;
        double total = 0;
        double count = 0;
        do {
            current = mc.theWorld.getBlockState(new BlockPos(pos.getX() + (count * getUnitX()), pos.getY(), pos.getZ() + (count * getUnitZ())));
            LogUtils.debugFullLog(cropBlockStates.get(FarmConfig.cropType).toString() + " " + current.getBlock());
            if (current.getBlock().equals(cropBlockStates.get(FarmConfig.cropType))) {
                LogUtils.debugFullLog("getAverageAge - current: " + current.getBlock());
                LogUtils.debugFullLog("getAverageAge - age: " + current.getValue(cropAgeRefs.get(FarmConfig.cropType)));
                total += current.getValue(cropAgeRefs.get(FarmConfig.cropType));
                count += 1;
            }
        } while (current.getBlock() == cropBlockStates.get(FarmConfig.cropType));
        return total / count;
    }

    public static int bedrockCount() {
        // if (System.currentTimeMillis() % 1000 < 20) {
        int r = 4;
        int count = 0;
        BlockPos playerPos = mc.thePlayer.getPosition();
        playerPos.add(0, 1, 0);
        Vec3i vec3i = new Vec3i(r, r, r);
        Vec3i vec3i2 = new Vec3i(r, r, r);
        for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i2))) {
            IBlockState blockState = mc.theWorld.getBlockState(blockPos);
            if (blockState.getBlock() == Blocks.bedrock) {
                count++;
            }
        }
        return count;
        // }
        // return 0;
    }

    public static int countCarpet() {
        LogUtils.debugFullLog(String.valueOf(System.currentTimeMillis()));
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
        LogUtils.debugFullLog("Counted carpet: " + count);
        return count;
    }

    public static boolean isWalkable(Block block) {
        return block == Blocks.air || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.dark_oak_fence_gate || block == Blocks.acacia_fence_gate || block == Blocks.birch_fence_gate || block == Blocks.oak_fence_gate || block == Blocks.jungle_fence_gate || block == Blocks.spruce_fence_gate || block == Blocks.wall_sign;
    }

    public static Block getBlockAround(int rightOffset, int frontOffset, int upOffset) {
        double X = mc.thePlayer.posX;
        double Y = mc.thePlayer.posY;
        double Z = mc.thePlayer.posZ;

        return (mc.theWorld.getBlockState(
            new BlockPos(mc.thePlayer.getLookVec().zCoord * -1 * rightOffset + mc.thePlayer.getLookVec().xCoord * frontOffset + X, Y + upOffset,
                mc.thePlayer.getLookVec().xCoord * rightOffset + mc.thePlayer.getLookVec().zCoord * frontOffset + Z)).getBlock());

    }

    public static int getUnitX() {
        double modYaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360;
        if (modYaw < 45 || modYaw > 315) {
            return 0;
        } else if (modYaw < 135) {
            return -1;
        } else if (modYaw < 225) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int getUnitZ() {
        double modYaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360;
        if (modYaw < 45 || modYaw > 315) {
            return 1;
        } else if (modYaw < 135) {
            return 0;
        } else if (modYaw < 225) {
            return -1;
        } else {
            return 0;
        }
    }

    // Base
    public static Block getBelowBlock() {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ
            )).getBlock());
    }

    public static Block getFrontBlock() {
        return getFrontBlock(0);
    }

    public static Block getBackBlock() {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitX() * -1,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + getUnitZ() * -1
            )).getBlock());
    }

    public static Block getRightBlock() {
        return getRightBlock(0);
    }

    public static Block getLeftBlock() {
        return getLeftBlock(0);
    }

    // yOffset param
    public static Block getFrontBlock(double yOffset) {
        return getFrontBlock(yOffset, 1);
    }

    public static Block getBackBlock(double yOffset) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitX() * -1),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitZ() * -1)
            )).getBlock());
    }

    public static Block getRightBlock(double yOffset) {
        return getRightBlock(yOffset, 1);
    }

    public static Block getLeftBlock(double yOffset) {
        return getLeftBlock(yOffset, 1);
    }

    // Multiple extension
    public static Block getFrontBlock(double yOffset, int multiple) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitX() * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitZ() * multiple)
            )).getBlock());
    }

    public static Block getRightBlock(double yOffset, int multiple) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * -1 * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitX() * multiple)
            )).getBlock());
    }

    public static Block getLeftBlock(double yOffset, int multiple) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitX() * -1 * multiple)
            )).getBlock());
    }

    // Get block state of col block from right of row
    public static Block getRightColBlock(int multiple) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * -1 * multiple) + getUnitX(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + (getUnitX() * multiple) + getUnitZ()
            )).getBlock());
    }

    public static Block getLeftColBlock(int multiple) {
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * multiple) + getUnitX(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + (getUnitX() * -1 * multiple) + getUnitZ()
            )).getBlock());
    }
}
