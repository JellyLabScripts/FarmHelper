package com.jelly.farmhelper.mixins.block;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.utils.CropUtils;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockCrops.class)
public abstract class MixinBlockCrops extends BlockBush {

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        if (FarmHelper.config.increasedCrops)
            CropUtils.updateCropsMaxY(worldIn, pos, worldIn.getBlockState(pos).getBlock());
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        if (FarmHelper.config.increasedCrops)
            CropUtils.updateCropsMaxY(worldIn, pos, worldIn.getBlockState(pos).getBlock());
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}