package com.jelly.farmhelper.mixins.block;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.utils.CropUtils;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockCocoa.class)
public abstract class MixinBlockCocoa extends BlockDirectional {
    protected MixinBlockCocoa() {
        super(Material.plants);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        if (FarmHelper.config.increasedCocoaBeans)
            CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        if (FarmHelper.config.increasedCocoaBeans)
            CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        if (FarmHelper.config.increasedCocoaBeans)
            CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}
