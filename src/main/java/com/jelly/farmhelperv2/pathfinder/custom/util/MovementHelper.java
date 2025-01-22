package com.jelly.farmhelperv2.pathfinder.custom.util;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovementHelper {

    public static boolean bresenham(BlockStateAccessor bsa, Vec3 start, Vec3 end) {
        Vec3 current = start;

        int x1 = MathHelper.floor_double(end.xCoord);
        int y1 = MathHelper.floor_double(end.yCoord);
        int z1 = MathHelper.floor_double(end.zCoord);

        int x0 = MathHelper.floor_double(current.xCoord);
        int y0 = MathHelper.floor_double(current.yCoord);
        int z0 = MathHelper.floor_double(current.zCoord);

        int iterations = 200;

        while (iterations-- >= 0) {
            if (x0 == x1 && y0 == y1 && z0 == z1) {
                return true;
            }

            boolean hasNewX = true, hasNewY = true, hasNewZ = true;
            double newX = 999.0, newY = 999.0, newZ = 999.0;

            if (x1 > x0) newX = x0 + 1.0;
            else if (x1 < x0) newX = x0 + 0.0;
            else hasNewX = false;

            if (y1 > y0) newY = y0 + 1.0;
            else if (y1 < y0) newY = y0 + 0.0;
            else hasNewY = false;

            if (z1 > z0) newZ = z0 + 1.0;
            else if (z1 < z0) newZ = z0 + 0.0;
            else hasNewZ = false;

            double stepX = 999.0, stepY = 999.0, stepZ = 999.0;
            double dx = end.xCoord - current.xCoord;
            double dy = end.yCoord - current.yCoord;
            double dz = end.zCoord - current.zCoord;

            if (hasNewX) stepX = (newX - current.xCoord) / dx;
            if (hasNewY) stepY = (newY - current.yCoord) / dy;
            if (hasNewZ) stepZ = (newZ - current.zCoord) / dz;

            if (stepX == -0.0) stepX = -1.0E-4;
            if (stepY == -0.0) stepY = -1.0E-4;
            if (stepZ == -0.0) stepZ = -1.0E-4;

            EnumFacing enumFacing;
            if (stepX < stepY && stepX < stepZ) {
                enumFacing = x1 > x0 ? EnumFacing.WEST : EnumFacing.EAST;
                current = new Vec3(newX, current.yCoord + dy * stepX, current.zCoord + dz * stepX);
            } else if (stepY < stepZ) {
                enumFacing = y1 > y0 ? EnumFacing.DOWN : EnumFacing.UP;
                current = new Vec3(current.xCoord + dx * stepY, newY, current.zCoord + dz * stepY);
            } else {
                enumFacing = z1 > z0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                current = new Vec3(current.xCoord + dx * stepZ, current.yCoord + dy * stepZ, newZ);
            }

            x0 = MathHelper.floor_double(current.xCoord) - (enumFacing == EnumFacing.EAST ? 1 : 0);
            y0 = MathHelper.floor_double(current.yCoord) - (enumFacing == EnumFacing.UP ? 1 : 0);
            z0 = MathHelper.floor_double(current.zCoord) - (enumFacing == EnumFacing.SOUTH ? 1 : 0);

            IBlockState newPos = bsa.get(x0, y0, z0);
            if (!MovementHelper.isFree(newPos) || MovementHelper.isLiquid(newPos)) return false;
        }

        return false;
    }

    public static boolean isFree(IBlockState state) {
        if (isSolid(state)) return false;
        Block block = state.getBlock();
        return block.equals(Blocks.air)
                || block.equals(Blocks.water)
                || block.equals(Blocks.flowing_water)
                || block.equals(Blocks.brown_mushroom)
                || block.equals(Blocks.red_mushroom)
                || block.equals(Blocks.melon_stem)
                || block.equals(Blocks.pumpkin_stem)
                || block.equals(Blocks.reeds);
    }

    public static boolean isSolid(IBlockState state) {
        Block block = state.getBlock();
        return block.getMaterial().isSolid()
                || block instanceof BlockSlab
                || block instanceof BlockStainedGlass
                || block instanceof BlockPane
                || block instanceof BlockFence
                || block instanceof BlockPistonExtension
                || block instanceof BlockEnderChest
                || block instanceof BlockTrapDoor
                || block instanceof BlockPistonBase
                || block instanceof BlockChest
                || block instanceof BlockStairs
                || block instanceof BlockCactus
                || block instanceof BlockWall
                || block instanceof BlockGlass
                || block instanceof BlockSkull
                || block instanceof BlockSand;
    }

    public static boolean canNotMoveDiagonal(BlockPos from, BlockPos to, BlockStateAccessor bsa) {
        List<BlockPos> adjacent = Stream.of(
                from.add(1, 0, 0),
                from.add(-1, 0, 0),
                from.add(0, 0, 1),
                from.add(0, 0, -1)
        ).filter(Arrays.asList(
                to.add(1, 0, 0),
                to.add(-1, 0, 0),
                to.add(0, 0, 1),
                to.add(0, 0, -1)
        )::contains).collect(Collectors.toList());

        if (adjacent.size() < 2) return false;

        IBlockState first = bsa.get(adjacent.get(0));
        IBlockState firstUp = bsa.get(adjacent.get(0).up());
        IBlockState second = bsa.get(adjacent.get(1));
        IBlockState secondUp = bsa.get(adjacent.get(1).up());

        return (isFree(first) && isFree(second))
                || (isFree(first) && isFree(secondUp))
                || (isFree(firstUp) && isFree(second))
                || (isFree(firstUp) && isFree(secondUp));
    }

    public static boolean isLiquid(IBlockState state) {
        Block block = state.getBlock();
        return block.equals(Blocks.flowing_lava)
                || block.equals(Blocks.lava)
                || block.equals(Blocks.flowing_water)
                || block.equals(Blocks.water);
    }

    public static boolean cannotJump(IBlockState state) {
        Block block = state.getBlock();
        return !(block instanceof BlockFence)
                && !(block instanceof BlockWall)
                && !(block instanceof BlockFenceGate);
    }

}
