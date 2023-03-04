package com.jelly.farmhelper.mixins.block;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BlockMushroom.class})
public class MixinBlockMushroom extends BlockBush {

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        if (FarmConfig.cropType == CropEnum.MUSHROOMS && MacroHandler.isMacroing)
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5f, 1.0F);
        super.setBlockBoundsBasedOnState(worldIn, pos);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        if (FarmConfig.cropType == CropEnum.MUSHROOMS && MacroHandler.isMacroing)
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5f, 1.0F);
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        if (FarmConfig.cropType == CropEnum.MUSHROOMS && MacroHandler.isMacroing)
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5f, 1.0F);
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}
