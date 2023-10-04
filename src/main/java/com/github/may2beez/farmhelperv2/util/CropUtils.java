package com.github.may2beez.farmhelperv2.util;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.mixin.block.IBlockAccessor;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class CropUtils {

    public static final AxisAlignedBB[] CARROT_POTATO_BOX = {
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.3125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.375D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.4375D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5625D, 1.0D)
    };

    public static final AxisAlignedBB[] WHEAT_BOX = {
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.375D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.625D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.875D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)
    };

    public static final AxisAlignedBB[] NETHER_WART_BOX = {
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.3125D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.6875D, 1.0D),
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.875D, 1.0D)
    };

    public static void updateCropsMaxY(World world, BlockPos pos, Block block) {
        final IBlockState blockState = world.getBlockState(pos);
        final Integer ageValue = blockState.getValue(BlockCrops.AGE);
        IBlockAccessor accessor = (IBlockAccessor) block;
        if (FarmHelperConfig.increasedCrops)
            accessor.setMaxY(
                    blockState.getBlock() instanceof BlockPotato || blockState.getBlock() instanceof BlockCarrot
                            ? CARROT_POTATO_BOX[ageValue].maxY
                            : WHEAT_BOX[ageValue].maxY
            ); // mc 1.12
        else
            accessor.setMaxY(0.25D); // mc 1.8.9
    }

    public static void updateWartMaxY(World world, BlockPos pos, Block block) {
        if (FarmHelperConfig.increasedNetherWarts)
            ((IBlockAccessor) block).setMaxY(NETHER_WART_BOX[world.getBlockState(pos).getValue(BlockNetherWart.AGE)].maxY); // mc 1.12
        else
            ((IBlockAccessor) block).setMaxY(0.25D); // mc 1.8.9
    }

    public static void updateCocoaBeansHitbox(IBlockState blockState) {
        EnumFacing enumFacing = blockState.getValue(BlockDirectional.FACING);
        int age = blockState.getValue(BlockCocoa.AGE);
        int j = 4 + age * 2;
        int k = 5 + age * 2;

        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().equals(MacroHandler.Macros.S_SHAPE_COCOA_BEAN_MACRO.getMacro()) && MacroHandler.getInstance().isMacroToggled() && FarmHelperConfig.increasedCocoaBeans) {
            switch (enumFacing) {
                case SOUTH: {
                    blockState.getBlock().setBlockBounds(0, (12.0f - (float) k) / 16.0f, (15.0f - (float) j) / 16.0f, 1, 0.75f, 0.9375f);
                    break;
                }
                case NORTH: {
                    blockState.getBlock().setBlockBounds(0, (12.0f - (float) k) / 16.0f, 0.0625f, 1, 0.75f, (1.0f + (float) j) / 16.0f);
                    break;
                }
                case WEST: {
                    blockState.getBlock().setBlockBounds(0.0625f, (12.0f - (float) k) / 16.0f, 0, (1.0f + (float) j) / 16.0f, 0.75f, 1);
                    break;
                }
                case EAST: {
                    blockState.getBlock().setBlockBounds((15.0f - (float) j) / 16.0f, (12.0f - (float) k) / 16.0f, 0, 0.9375f, 0.75f, 1);
                }
            }
        } else {
            float f = (float)j / 2.0f;
            switch (enumFacing) {
                case SOUTH: {
                    blockState.getBlock().setBlockBounds((8.0f - f) / 16.0f, (12.0f - (float)k) / 16.0f, (15.0f - (float)j) / 16.0f, (8.0f + f) / 16.0f, 0.75f, 0.9375f);
                    break;
                }
                case NORTH: {
                    blockState.getBlock().setBlockBounds((8.0f - f) / 16.0f, (12.0f - (float)k) / 16.0f, 0.0625f, (8.0f + f) / 16.0f, 0.75f, (1.0f + (float)j) / 16.0f);
                    break;
                }
                case WEST: {
                    blockState.getBlock().setBlockBounds(0.0625f, (12.0f - (float)k) / 16.0f, (8.0f - f) / 16.0f, (1.0f + (float)j) / 16.0f, 0.75f, (8.0f + f) / 16.0f);
                    break;
                }
                case EAST: {
                    blockState.getBlock().setBlockBounds((15.0f - (float)j) / 16.0f, (12.0f - (float)k) / 16.0f, (8.0f - f) / 16.0f, 0.9375f, 0.75f, (8.0f + f) / 16.0f);
                }
            }
        }
    }
}
