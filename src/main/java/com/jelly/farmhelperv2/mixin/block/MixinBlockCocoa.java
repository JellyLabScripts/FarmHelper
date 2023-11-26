package com.jelly.farmhelperv2.mixin.block;

import com.jelly.farmhelperv2.util.CropUtils;
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

@Mixin(value = BlockCocoa.class, priority = 2000)
public abstract class MixinBlockCocoa extends BlockDirectional {
    protected MixinBlockCocoa() {
        super(Material.plants);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        CropUtils.updateCocoaBeansHitbox(worldIn.getBlockState(pos));
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}
