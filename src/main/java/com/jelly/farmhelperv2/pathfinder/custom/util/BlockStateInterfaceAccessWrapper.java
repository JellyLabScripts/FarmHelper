package com.jelly.farmhelperv2.pathfinder.custom.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;

public class BlockStateInterfaceAccessWrapper implements IBlockAccess {

    private final BlockStateAccessor bsi;
    private final IBlockAccess world;

    public BlockStateInterfaceAccessWrapper(BlockStateAccessor bsi, IBlockAccess world) {
        this.bsi = bsi;
        this.world = world;
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 0;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return bsi.get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return bsi.get(pos.getX(), pos.getY(), pos.getZ()).getBlock() == Blocks.air;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
        return null;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    // Uncomment and implement if needed
    // @Override
    // public Biome getBiome(BlockPos pos) {
    //     return Biomes.FOREST;
    // }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return world.getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return false;
    }
}
