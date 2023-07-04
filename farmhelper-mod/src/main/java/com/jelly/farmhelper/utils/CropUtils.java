package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.VerticalMacroEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;

import com.jelly.farmhelper.macros.Macro;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.mixins.block.IBlockAccessor;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;


public class CropUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

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
            accessor.setMaxY(
                    blockState.getBlock() instanceof BlockPotato || blockState.getBlock() instanceof BlockCarrot
                            ? CARROT_POTATO_BOX[ageValue].maxY
                            : WHEAT_BOX[ageValue].maxY
            );
    }

    public static void updateWartMaxY(World world, BlockPos pos, Block block) {
        ((IBlockAccessor) block).setMaxY(NETHER_WART_BOX[world.getBlockState(pos).getValue(BlockNetherWart.AGE)].maxY);
    }

    public static void updateCocoaBeansHitbox(IBlockState blockState) {
        EnumFacing enumFacing = blockState.getValue(BlockDirectional.FACING);
        int age = blockState.getValue(BlockCocoa.AGE);
        int j = 4 + age * 2;
        int k = 5 + age * 2;

        if ((MacroHandler.currentMacro == MacroHandler.cocoaBeanMacro || MacroHandler.currentMacro == MacroHandler.cocoaBeanRGMacro) && MacroHandler.isMacroing) {
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

    public static boolean itemChangedByStaff = false;

    public static void getTool() {
        // Sometimes if staff changed your slot, you might not have the tool in your hand after the swap, so it won't be obvious that you're using a macro
        if (itemChangedByStaff) {
            return;
        }

        if (!FarmHelper.config.macroType) {
            if (FarmHelper.config.VerticalMacroType != SMacroEnum.PUMPKIN_MELON.ordinal()) {
                mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(MacroHandler.crop);
            } else {
                mc.thePlayer.inventory.currentItem = PlayerUtils.getAxeSlot();
            }
        } else {
            if (FarmHelper.config.SShapeMacroType != SMacroEnum.COCOA_BEANS.ordinal() &&
                FarmHelper.config.SShapeMacroType != SMacroEnum.COCOA_BEANS_RG.ordinal()) {
                mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(MacroHandler.crop);
            } else {
                mc.thePlayer.inventory.currentItem = PlayerUtils.getAxeSlot();
            }
        }
    }
}