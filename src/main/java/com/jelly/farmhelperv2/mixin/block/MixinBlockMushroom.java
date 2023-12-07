package com.jelly.farmhelperv2.mixin.block;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {BlockMushroom.class}, priority = 2000)
public class MixinBlockMushroom extends BlockBush {
    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
        if (block.equals(Blocks.brown_mushroom) || block.equals(Blocks.red_mushroom)) {
            farmHelperV2$setMushroomBoundings(worldIn, pos);
        }
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
        if (block.equals(Blocks.brown_mushroom) || block.equals(Blocks.red_mushroom)) {
            farmHelperV2$setMushroomBoundings(worldIn, pos);
        }
        return super.collisionRayTrace(worldIn, pos, start, end);
    }

    @Unique
    private void farmHelperV2$setMushroomBoundings(World worldIn, BlockPos pos) {
        if (FarmHelperConfig.increasedMushrooms)
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5f, 1.0F);
        else
            worldIn.getBlockState(pos).getBlock().setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.5f, 0.7F);
    }
}
