package com.jelly.farmhelper.mixins.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BlockMushroom.class})
public class MixinBlockMushroom extends BlockBush {

    /*@Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        setMushroomBoundings(worldIn, pos);
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        setMushroomBoundings(worldIn, pos);
        return super.collisionRayTrace(worldIn, pos, start, end);
    }

    private void setMushroomBoundings(World worldIn, BlockPos pos) {
        if (FarmConfig.cropType == CropEnum.MUSHROOM && MacroHandler.isMacroing)
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5f, 1.0F);
        else {
            float f = 0.2F;
            this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        }
    }*/
}
