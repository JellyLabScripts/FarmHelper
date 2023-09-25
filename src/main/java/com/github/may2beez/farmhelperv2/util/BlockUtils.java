package com.github.may2beez.farmhelperv2.util;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;


public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] walkables = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.trapdoor};
    private static final Block[] initialWalkables = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.waterlily, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem};
    private static final Block[] walkablesMushroom = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.carpet, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.trapdoor, Blocks.red_mushroom, Blocks.brown_mushroom};
    private static final Block[] walkablesCactus = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.dark_oak_fence_gate, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem, Blocks.iron_trapdoor, Blocks.stone_stairs, Blocks.snow_layer, Blocks.trapdoor};

    public static int getUnitX() {
        return getUnitX((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static int getUnitZ() {
        return getUnitZ((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static int getUnitX(float modYaw) {
        if (AngleUtils.get360RotationYaw(modYaw) < 45 || AngleUtils.get360RotationYaw(modYaw) > 315) {
            return 0;
        } else if (AngleUtils.get360RotationYaw(modYaw) < 135) {
            return -1;
        } else if (AngleUtils.get360RotationYaw(modYaw) < 225) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int getUnitZ(float modYaw) {
        if (AngleUtils.get360RotationYaw(modYaw) < 45 || AngleUtils.get360RotationYaw(modYaw) > 315) {
            return 1;
        } else if (AngleUtils.get360RotationYaw(modYaw) < 135) {
            return 0;
        } else if (AngleUtils.get360RotationYaw(modYaw) < 225) {
            return -1;
        } else {
            return 0;
        }
    }

    public static Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static boolean isRelativeBlockPassable(float x, float y, float z) {
        return BlockUtils.getRelativeBlock(x, y, z).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(x, y, z));
    }

    public static Block getRelativeBlock(float x, float y, float z) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                        (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                        mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
                )).getBlock();
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
        );
    }

    public static Block getRelativeBlock(float x, float y, float z, float yaw) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                        (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                        mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
                )).getBlock();
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    public static int countCarpet() {
        int r = 2;
        int count = 0;
        BlockPos playerPos = mc.thePlayer.getPosition();
        playerPos.add(0, 1, 0);
        Vec3i vec3i = new Vec3i(r, r, r);
        Vec3i vec3i2 = new Vec3i(r, r, r);
        for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.add(vec3i), playerPos.subtract(vec3i2))) {
            IBlockState blockState = mc.theWorld.getBlockState(blockPos);
            if (blockState.getBlock() == Blocks.carpet && blockState.getValue(BlockCarpet.COLOR) == EnumDyeColor.BROWN) {
                count++;
            }
        }
        return count;
    }

    public static int bedrockCount() {
        int count = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (getBlock(mc.thePlayer.getPosition().add(i, 1, j)).equals(Blocks.bedrock))
                    count++;
            }
        }
        return count;
    }

    public static boolean isWater(Block b) {
        return b.equals(Blocks.water) || b.equals(Blocks.flowing_water);
    }

    public static Block getLeftBlock() {
        return getRelativeBlock(-1, 0, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getRightBlock() {
        return getRelativeBlock(1, 0, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getLeftTopBlock() {
        return getRelativeBlock(-1, 1, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getRightTopBlock() {
        return getRelativeBlock(1, 1, 0, mc.thePlayer.rotationYaw);
    }

    public static Block getBackBlock() {
        return getRelativeBlock(0, 0, -1);
    }

    public static Block getFrontBlock() {
        return getRelativeBlock(0, 0, 1);
    }

    public static boolean isWalkable(Block block) {
        if (FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_MUSHROOM_ROTATE.ordinal() ||
                FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_MUSHROOM.ordinal()) {
            return Arrays.asList(walkablesMushroom).contains(block);
        } else if (FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_CACTUS.ordinal()) {
            return Arrays.asList(walkablesCactus).contains(block);
        } else return Arrays.asList(walkables).contains(block);
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        // blocks that are always walkable, no matter what the position is

        if (Arrays.asList(initialWalkables).contains(block))
            return true;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor)
            return state.getValue(BlockTrapDoor.OPEN);

        if (block instanceof BlockSnow)
            return state.getValue(BlockSnow.LAYERS) <= 5;

        return canWalkThroughBottom(blockPos) && canWalkThroughAbove(blockPos.add(0, 1, 0));
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();
        if (mc.thePlayer.posY % 1 >= 0.5)
            return true;

        if (block instanceof BlockSlab) {
            // if the player is on the bottom half of the slab, all slabs are walkable (top, bottom and double)
            if (mc.thePlayer.posY % 1 < 0.5) {
                if (((BlockSlab) block).isDouble())
                    return false;
                return state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
            }
        }

        if (block instanceof BlockCarpet)
            return true;

        if (block instanceof BlockStairs) {
            // check if the stairs are rotated in the direction the player is coming from
            EnumFacing facing = state.getValue(BlockStairs.FACING);
            BlockPos posDiff = blockPos.subtract(mc.thePlayer.getPosition());
            if (state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP)
                return false;
            if (facing == EnumFacing.NORTH && posDiff.getZ() < 0)
                return true;
            if (facing == EnumFacing.SOUTH && posDiff.getZ() > 0)
                return true;
            if (facing == EnumFacing.WEST && posDiff.getX() < 0)
                return true;
            return facing == EnumFacing.EAST && posDiff.getX() > 0;
        }

        return block.isPassable(mc.theWorld, blockPos);
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof BlockCarpet)
            return false;

        return block.isPassable(mc.theWorld, blockPos);
    }

    public static BlockPos getBlockPosLookingAt() {
        MovingObjectPosition mop = mc.thePlayer.rayTrace(5, 1);
        if (mop == null)
            return null;
        return mop.getBlockPos();
    }

    public static boolean isCropReady(int xOffset) {
        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg() != -1337) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg();
        } else {
            yaw = mc.thePlayer.rotationYaw;
        }
        yaw = (float) wrapAngleTo180(yaw);
        BlockPos[] crops = Arrays.asList(
                getRelativeBlockPos(xOffset, 0, 1, yaw),
                getRelativeBlockPos(xOffset, 1, 1, yaw),
                getRelativeBlockPos(xOffset, 2, 1, yaw),
                getRelativeBlockPos(xOffset, 3, 1, yaw)

        ).toArray(new BlockPos[0]);

        List<BlockPos> cropList = Arrays.stream(crops).filter(c -> {
            IBlockState blockState = mc.theWorld.getBlockState(c);
            Block block = blockState.getBlock();
            return block instanceof BlockCrops && blockState.getValue(BlockCrops.AGE) == 7 ||
                    block instanceof BlockNetherWart && blockState.getValue(BlockNetherWart.AGE) == 3 ||
                    block instanceof BlockCocoa && blockState.getValue(BlockCocoa.AGE) == 2 ||
                    block instanceof BlockMelon ||
                    block instanceof BlockPumpkin ||
                    block instanceof BlockCactus ||
                    block instanceof BlockMushroom;
        }).collect(Collectors.toList());
        Optional<BlockPos> optionalBlockPos = cropList.stream().min((c1, c2) -> {
            double d1 = mc.thePlayer.getDistance(c1.getX(), c1.getY(), c1.getZ());
            double d2 = mc.thePlayer.getDistance(c2.getX(), c2.getY(), c2.getZ());
            return Double.compare(d1, d2);
        });

        if (!optionalBlockPos.isPresent()) {
            return false;
        }

        Block crop = mc.theWorld.getBlockState(optionalBlockPos.get()).getBlock();

        if (crop == null) return false;

        switch (MacroHandler.getInstance().getCrop()) {
            case WHEAT:
                return crop.equals(Blocks.wheat);
            case CARROT:
                return crop.equals(Blocks.carrots);
            case POTATO:
                return crop.equals(Blocks.potatoes);
            case MELON:
                return crop instanceof BlockMelon;
            case PUMPKIN:
                return crop instanceof BlockPumpkin;
            case CACTUS:
                return crop instanceof BlockCactus;
            case MUSHROOM:
                return crop instanceof BlockMushroom;
            case NETHER_WART:
                return crop instanceof BlockNetherWart;
            case COCOA_BEANS:
                return crop instanceof BlockCocoa;
            case SUGAR_CANE:
                return crop instanceof BlockReed;
            default:
                return false;
        }
    }

    public static boolean leftCropIsReady() {
        if (!GameStateHandler.getInstance().isLeftWalkable()) return false;

        return isCropReady(-1);
    }

    public static boolean rightCropIsReady() {
        if (!GameStateHandler.getInstance().isRightWalkable()) return false;

        return isCropReady(1);
    }

    public static boolean isBlockVisible(BlockPos pos) {
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return mop == null || mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 2 || mop.getBlockPos().equals(pos);
    }
}
