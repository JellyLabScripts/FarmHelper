package com.jelly.farmhelperv2.util;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.helper.Rotation;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;


public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] initialWalkables = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.waterlily, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem};

    public static float getUnitX() {
        return getUnitX((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static float getUnitZ() {
        return getUnitZ((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static float getUnitX(float modYaw) {
        float yaw = AngleUtils.get360RotationYaw(modYaw);
        if (yaw < 30) {
            return 0;
        } else if (yaw < 150) {
            return -1f;
        } else if (yaw < 210) {
            return 0;
        } else if (yaw < 330) {
            return 1f;
        } else {
            return 0;
        }
    }

    public static float getUnitZ(float modYaw) {
        float yaw = AngleUtils.get360RotationYaw(modYaw);
        if (yaw < 60) {
            return 1f;
        } else if (yaw < 120) {
            return 0;
        } else if (yaw < 240) {
            return -1f;
        } else if (yaw < 300) {
            return 0;
        } else {
            return 1;
        }
    }

    public static Vec3 getBlockPosCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static double getHorizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.xCoord - b.xCoord;
        double dz = a.zCoord - b.zCoord;
        return dx * dx + dz * dz;
    }

    public static Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static Block getRelativeBlock(float x, float y, float z) {
        return getBlock(getRelativeBlockPos(x, y, z));
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
        );
    }

    public static Block getRelativeBlock45Deg(float x, float y, float z, float yaw) {
        return getBlock(getRelativeBlockPos45Deg(x, y, z, yaw));
    }

    public static BlockPos getRelativeBlockPos45Deg(float x, float y, float z, float yaw) {
        float roundedYaw = AngleUtils.getClosest45(yaw);
        double radians = Math.toRadians(roundedYaw);

        double unitX = -Math.sin(radians);
        double unitZ = Math.cos(radians);

        return new BlockPos(
                mc.thePlayer.posX + unitX * z - unitZ * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + unitZ * z + unitX * x
        );
    }

    public static Block getRelativeFullBlock(float x, float y, float z) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                        mc.thePlayer.posY + y,
                        mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
                )).getBlock();
    }

    public static BlockPos getRelativeFullBlockPos(float x, float y, float z) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                mc.thePlayer.posY + y,
                mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
        );
    }

    public static Block getRelativeBlock(float x, float y, float z, float yaw) {
        return getBlock(getRelativeBlockPos(x, y, z, yaw));
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    public static Vec3 getRelativeVec(float x, float y, float z, float yaw) {
        return new Vec3(
                mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
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

    public boolean canFlyThrough() {
        return BlockUtils.getRelativeFullBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 0, 1))
                && BlockUtils.getRelativeFullBlock(0, 1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 1, 1));
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        return canWalkThrough(blockPos, null);
    }

    public static boolean canWalkThrough(BlockPos blockPos, Direction direction) {
        return canWalkThroughBottom(blockPos, direction) && canWalkThroughAbove(blockPos.add(0, 1, 0), direction);
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos, Direction direction) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        // if no blocks down to 65, then return false
        boolean allAir = true;
        for (int y = blockPos.getY(); y >= 65; y--) {
            if (mc.theWorld.getBlockState(new BlockPos(blockPos.getX(), y, blockPos.getZ())).getBlock() != Blocks.air) {
                allAir = false;
                break;
            }
        }

        if (allAir) return false;

        if (mc.thePlayer.posY % 1 >= 0.5 && mc.thePlayer.posY % 1 <= 0.75)
            return true;

        if (Arrays.asList(initialWalkables).contains(block))
            return true;

        if (block instanceof BlockDoor && direction != null) {
            return canWalkThroughDoor(blockPos, direction);
        }

        if (block instanceof BlockFence)
            return false;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor) {
            return state.getValue(BlockTrapDoor.OPEN) || state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.BOTTOM;
        }

        if (block instanceof BlockSnow)
            return state.getValue(BlockSnow.LAYERS) <= 5;

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

    private static boolean canWalkThroughAbove(BlockPos blockPos, Direction direction) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof BlockCarpet)
            return false;

        if (block instanceof BlockDoor && direction != null) {
            return canWalkThroughDoor(blockPos.subtract(new Vec3i(0, 1, 0)), direction);
        }

        if (block instanceof BlockFence)
            return false;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor) {
            EnumFacing playerFacing = EnumFacing.fromAngle(mc.thePlayer.rotationYaw);
            EnumFacing doorFacing = mc.theWorld.getBlockState(blockPos).getValue(BlockTrapDoor.FACING);
            boolean standingOnDoor = getRelativeBlockPos(0, 1, 0).equals(blockPos);

            if (state.getValue(BlockTrapDoor.OPEN) && direction != null) {
                return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
            } else {
                return state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP;
            }
        }

        return block.isPassable(mc.theWorld, blockPos);
    }

    private static boolean canWalkThroughDoorWithDirection(Direction direction, EnumFacing playerFacing, EnumFacing doorFacing, boolean standingOnDoor) {
        switch (direction) {
            case FORWARD:
                if (doorFacing.equals(playerFacing.getOpposite()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing)) {
                    return false;
                }
                break;
            case BACKWARD:
                if (doorFacing.equals(playerFacing) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.getOpposite())) {
                    return false;
                }
                break;
            case LEFT:
                if (doorFacing.equals(playerFacing.rotateY()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.rotateYCCW())) {
                    return false;
                }
                break;
            case RIGHT:
                if (doorFacing.equals(playerFacing.rotateYCCW()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.rotateY())) {
                    return false;
                }
                break;
        }
        return true;
    }

    public static boolean isAboveHeadClear() {
        BlockPos blockPosStart = getRelativeBlockPos(0, 1, 0);
        for (int y = blockPosStart.getY(); y < 100; y++) {
            BlockPos blockPos = new BlockPos(blockPosStart.getX(), y, blockPosStart.getZ());
            if (blockHasCollision(blockPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean blockHasCollision(BlockPos blockPos) {
        AxisAlignedBB axisAlignedBB = mc.theWorld.getBlockState(blockPos).getBlock().getCollisionBoundingBox(mc.theWorld, blockPos, mc.theWorld.getBlockState(blockPos));
        return !mc.theWorld.getBlockState(blockPos).getBlock().isPassable(mc.theWorld, blockPos) || axisAlignedBB != null;
    }

    public static boolean blockHasCollision(BlockPos blockPos, IBlockState blockState, Block block, IBlockAccess blockAccess) {
        if (block.equals(Blocks.air) || block.equals(Blocks.water) || block.equals(Blocks.flowing_water)) {
            return false;
        }

        if (block.equals(Blocks.brown_mushroom) || block.equals(Blocks.red_mushroom) || block.equals(Blocks.melon_stem) || block.equals(Blocks.pumpkin_stem) || block.equals(Blocks.reeds)) {
            return false;
        }

        if (block.equals(Blocks.ladder)) {
            return false;
        }

        try {
            return !block.isPassable(blockAccess, blockPos) || block.getCollisionBoundingBox((World) blockAccess, blockPos, blockState) != null;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canWalkThroughDoor(Direction direction) {
        return canWalkThroughDoor(getRelativeBlockPos(0, 0, 0), direction);
    }

    public static boolean canWalkThroughDoor(BlockPos blockPos, Direction direction) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        if (!(block instanceof BlockDoor)) return true;

        EnumFacing playerFacing = EnumFacing.fromAngle(mc.thePlayer.rotationYaw);
        EnumFacing doorFacing = mc.theWorld.getBlockState(blockPos).getValue(BlockDoor.FACING);
        boolean standingOnDoor = getRelativeBlockPos(0, 0, 0).equals(blockPos);

        return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(-0.25, 0.1, -0.25),
            new Vec3(-0.25, 0.1, 0.25),
            new Vec3(0.25, 0.1, -0.25),
            new Vec3(0.25, 0.1, 0.25)
    };

    public static boolean canFlyHigher(int distance) {
        BlockPos blockPos = getRelativeBlockPos(0, 1, 0);
        for (Vec3 vec3 : BLOCK_SIDE_MULTIPLIERS) {
            Vec3 vec = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vec.add(vec3), vec.addVector(0, distance, 0).add(vec3), false, true, false);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return false;
            }
        }
        return true;
    }

    public static BlockPos getBlockPosLookingAt() {
        MovingObjectPosition mop = mc.thePlayer.rayTrace(5, 1);
        if (mop == null)
            return null;
        return mop.getBlockPos();
    }

    public static boolean isCropReady(int xOffset) {
        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().isPresent()) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().get();
        } else {
            yaw = AngleUtils.get360RotationYaw();
        }
        yaw = (float) wrapAngleTo180(yaw);
        List<BlockPos> crops;
        if (MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.CACTUS || MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.SUGAR_CANE) {
            crops = Arrays.asList(
                    getRelativeBlockPos(xOffset, 1, 1, yaw),
                    getRelativeBlockPos(xOffset, 1, 2, yaw),
                    getRelativeBlockPos(xOffset * 2, 1, 1, yaw),
                    getRelativeBlockPos(xOffset * 2, 1, 2, yaw)
            );
        } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_COCOA_BEANS_LEFT_RIGHT) {
            crops = Arrays.asList(
                    getRelativeBlockPos(xOffset, 2, 0, yaw),
                    getRelativeBlockPos(xOffset, 3, 0, yaw)
            );
        } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_CACTUS_SUNTZU || MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.COCOA_BEANS) {
            crops = Arrays.asList(
                    getRelativeBlockPos(xOffset, 2, 1, yaw),
                    getRelativeBlockPos(xOffset, 3, 1, yaw)
            );
        } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_MUSHROOM || FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_MUSHROOM_ROTATE) {
            crops = Arrays.asList(
                    getRelativeBlockPos(xOffset, 1, 1, yaw),
                    getRelativeBlockPos(xOffset, 2, 1, yaw),
                    getRelativeBlockPos(xOffset, 3, 1, yaw)
            );
        } else {
            crops = Arrays.asList(
                    getRelativeBlockPos(xOffset, 0, 1, yaw),
                    getRelativeBlockPos(xOffset, 1, 1, yaw)
            );
        }

        List<BlockPos> cropList = crops.stream().filter(c -> {
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
        Optional<BlockPos> optionalBlockPos = Optional.empty();

        for (BlockPos crop : cropList) {
            if (optionalBlockPos.isPresent()) {
                double distance1 = mc.thePlayer.getPositionEyes(1).distanceTo(new Vec3(crop.getX() + 0.5, crop.getY() + 0.5, crop.getZ() + 0.5));
                double distance2 = mc.thePlayer.getPositionEyes(1).distanceTo(new Vec3(optionalBlockPos.get().getX() + 0.5, optionalBlockPos.get().getY() + 0.5, optionalBlockPos.get().getZ() + 0.5));
                if (distance1 < distance2) {
                    optionalBlockPos = Optional.of(crop);
                }
            } else {
                optionalBlockPos = Optional.of(crop);
            }
        }

        if (!optionalBlockPos.isPresent()) {
            return false;
        }

        LogUtils.sendDebug("Closest crop: " + optionalBlockPos.get());

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

    public static boolean isWater(Block block) {
        return block instanceof BlockLiquid && block.getMaterial() == Material.water;
    }

    public static BlockPos getEasiestBlock(ArrayList<BlockPos> list, Predicate<? super BlockPos> predicate) {
        EntityPlayerSP player = mc.thePlayer;
        BlockPos easiest = null;

        Rotation serverSideRotation = new Rotation(RotationHandler.getInstance().getServerSideYaw(), RotationHandler.getInstance().getServerSidePitch());

        for (BlockPos blockPos : list) {
            if (predicate.test(blockPos) && canBlockBeSeen(blockPos, 8, new Vec3(0, 0, 0), x -> false)) {
                if (easiest == null || RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(blockPos)).getValue() < RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(easiest)).getValue()) {
                    easiest = blockPos;
                }
            }
        }

        if (easiest != null) return easiest;

        for (BlockPos blockPos : list) {
            if (predicate.test(blockPos)) {
                if (easiest == null || RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(blockPos)).getValue() < RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(easiest)).getValue()) {
                    easiest = blockPos;
                }
            }
        }

        return easiest;
    }

    public static boolean canBlockBeSeen(BlockPos blockPos, double dist, Vec3 offset, Predicate<? super BlockPos> predicate) {
        Vec3 vec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).add(offset);
        MovingObjectPosition mop = rayTraceBlocks(mc.thePlayer.getPositionEyes(1.0f), vec, false, true, false, predicate);
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return mop.getBlockPos().equals(blockPos) && vec.distanceTo(mc.thePlayer.getPositionEyes(1.0f)) < dist;
        }

        return false;
    }

    public static MovingObjectPosition rayTraceBlocks(Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Predicate<? super BlockPos> predicate) {
        return rayTraceBlocks(vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, predicate, false);
    }

    public static MovingObjectPosition rayTraceBlocks(Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Predicate<? super BlockPos> predicate, boolean fullBlocks) {
        if (!(Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord))) {
            if (!(Double.isNaN(vec32.xCoord) || Double.isNaN(vec32.yCoord) || Double.isNaN(vec32.zCoord))) {
                MovingObjectPosition movingobjectposition;
                int i = MathHelper.floor_double(vec32.xCoord);
                int j = MathHelper.floor_double(vec32.yCoord);
                int k = MathHelper.floor_double(vec32.zCoord);
                int l = MathHelper.floor_double(vec31.xCoord);
                int i1 = MathHelper.floor_double(vec31.yCoord);
                int j1 = MathHelper.floor_double(vec31.zCoord);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                if (!predicate.test(blockpos) && (!ignoreBlockWithoutBoundingBox || block.getCollisionBoundingBox(mc.theWorld, blockpos, iblockstate) != null) && block.canCollideCheck(iblockstate, stopOnLiquid) && (movingobjectposition = collisionRayTrace(block, blockpos, vec31, vec32, fullBlocks)) != null) {
                    return movingobjectposition;
                }
                MovingObjectPosition movingobjectposition2 = null;
                int k1 = 200;
                while (k1-- >= 0) {
                    EnumFacing enumfacing;
                    if (Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord)) {
                        return null;
                    }
                    if (l == i && i1 == j && j1 == k) {
                        return returnLastUncollidableBlock ? movingobjectposition2 : null;
                    }
                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0;
                    double d1 = 999.0;
                    double d2 = 999.0;
                    if (i > l) {
                        d0 = (double) l + 1.0;
                    } else if (i < l) {
                        d0 = (double) l + 0.0;
                    } else {
                        flag2 = false;
                    }
                    if (j > i1) {
                        d1 = (double) i1 + 1.0;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0;
                    } else {
                        flag = false;
                    }
                    if (k > j1) {
                        d2 = (double) j1 + 1.0;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0;
                    } else {
                        flag1 = false;
                    }
                    double d3 = 999.0;
                    double d4 = 999.0;
                    double d5 = 999.0;
                    double d6 = vec32.xCoord - vec31.xCoord;
                    double d7 = vec32.yCoord - vec31.yCoord;
                    double d8 = vec32.zCoord - vec31.zCoord;
                    if (flag2) {
                        d3 = (d0 - vec31.xCoord) / d6;
                    }
                    if (flag) {
                        d4 = (d1 - vec31.yCoord) / d7;
                    }
                    if (flag1) {
                        d5 = (d2 - vec31.zCoord) / d8;
                    }
                    if (d3 == -0.0) {
                        d3 = -1.0E-4;
                    }
                    if (d4 == -0.0) {
                        d4 = -1.0E-4;
                    }
                    if (d5 == -0.0) {
                        d5 = -1.0E-4;
                    }
                    if (d3 < d4 && d3 < d5) {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3(d0, vec31.yCoord + d7 * d3, vec31.zCoord + d8 * d3);
                    } else if (d4 < d5) {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3(vec31.xCoord + d6 * d4, d1, vec31.zCoord + d8 * d4);
                    } else {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3(vec31.xCoord + d6 * d5, vec31.yCoord + d7 * d5, d2);
                    }
                    l = MathHelper.floor_double(vec31.xCoord) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor_double(vec31.yCoord) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor_double(vec31.zCoord) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();
                    if (ignoreBlockWithoutBoundingBox && block1.getCollisionBoundingBox(mc.theWorld, blockpos, iblockstate1) == null)
                        continue;
                    if (predicate.test(blockpos)) continue;
                    if (block1.canCollideCheck(iblockstate1, stopOnLiquid)) {
                        MovingObjectPosition movingobjectposition1 = collisionRayTrace(block1, blockpos, vec31, vec32, fullBlocks);
                        if (movingobjectposition1 == null) continue;
                        return movingobjectposition1;
                    }
                    movingobjectposition2 = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec31, enumfacing, blockpos);
                }
                return returnLastUncollidableBlock ? movingobjectposition2 : null;
            }
            return null;
        }
        return null;
    }

    public static MovingObjectPosition collisionRayTrace(Block block, BlockPos pos, Vec3 start, Vec3 end, boolean fullBlocks) {
        start = start.addVector(-pos.getX(), -pos.getY(), -pos.getZ());
        end = end.addVector(-pos.getX(), -pos.getY(), -pos.getZ());

        Vec3 vec3 = start.getIntermediateWithXValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinX());
        Vec3 vec31 = start.getIntermediateWithXValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxX());
        Vec3 vec32 = start.getIntermediateWithYValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinY());
        Vec3 vec33 = start.getIntermediateWithYValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxY());
        Vec3 vec34 = start.getIntermediateWithZValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinZ());
        Vec3 vec35 = start.getIntermediateWithZValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());

        if (!isVecInsideYZBounds(block, vec3, fullBlocks)) {
            vec3 = null;
        }
        if (!isVecInsideYZBounds(block, vec31, fullBlocks)) {
            vec31 = null;
        }
        if (!isVecInsideXZBounds(block, vec32, fullBlocks)) {
            vec32 = null;
        }
        if (!isVecInsideXZBounds(block, vec33, fullBlocks)) {
            vec33 = null;
        }
        if (!isVecInsideXYBounds(block, vec34, fullBlocks)) {
            vec34 = null;
        }
        if (!isVecInsideXYBounds(block, vec35, fullBlocks)) {
            vec35 = null;
        }

        Vec3 vec36 = null;

        if (vec3 != null) {
            vec36 = vec3;
        }
        if (vec31 != null && (vec36 == null || start.squareDistanceTo(vec31) < start.squareDistanceTo(vec36))) {
            vec36 = vec31;
        }
        if (vec32 != null && (vec36 == null || start.squareDistanceTo(vec32) < start.squareDistanceTo(vec36))) {
            vec36 = vec32;
        }
        if (vec33 != null && (vec36 == null || start.squareDistanceTo(vec33) < start.squareDistanceTo(vec36))) {
            vec36 = vec33;
        }
        if (vec34 != null && (vec36 == null || start.squareDistanceTo(vec34) < start.squareDistanceTo(vec36))) {
            vec36 = vec34;
        }
        if (vec35 != null && (vec36 == null || start.squareDistanceTo(vec35) < start.squareDistanceTo(vec36))) {
            vec36 = vec35;
        }
        if (vec36 == null) {
            return null;
        }
        EnumFacing enumfacing = null;
        if (vec36 == vec3) {
            enumfacing = EnumFacing.WEST;
        }
        if (vec36 == vec31) {
            enumfacing = EnumFacing.EAST;
        }
        if (vec36 == vec32) {
            enumfacing = EnumFacing.DOWN;
        }
        if (vec36 == vec33) {
            enumfacing = EnumFacing.UP;
        }
        if (vec36 == vec34) {
            enumfacing = EnumFacing.NORTH;
        }
        if (vec36 == vec35) {
            enumfacing = EnumFacing.SOUTH;
        }
        return new MovingObjectPosition(vec36.addVector(pos.getX(), pos.getY(), pos.getZ()), enumfacing, pos);
    }

    private static boolean isVecInsideYZBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.yCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinY()) && point.yCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxY()) && point.zCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinZ()) && point.zCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());
    }

    private static boolean isVecInsideXZBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.xCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinX()) && point.xCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxX()) && point.zCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinZ()) && point.zCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());
    }

    private static boolean isVecInsideXYBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.xCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinX()) && point.xCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxX()) && point.yCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinY()) && point.yCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxY());
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        if (mc.theWorld == null) return null;
        return mc.theWorld.getBlockState(blockPos);
    }

    public static EnumFacing calculateEnumfacing(Vec3 vec) {
        int x = MathHelper.floor_double(vec.xCoord);
        int y = MathHelper.floor_double(vec.yCoord);
        int z = MathHelper.floor_double(vec.zCoord);
        MovingObjectPosition position = calculateIntercept(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1), vec, 50.0f);
        return (position != null) ? position.sideHit : null;
    }

    public static MovingObjectPosition calculateIntercept(AxisAlignedBB aabb, Vec3 vec, float range) {
        Vec3 playerPositionEyes = mc.thePlayer.getPositionEyes(1f);
        Vec3 blockVector = getLook(vec);
        return aabb.calculateIntercept(playerPositionEyes, playerPositionEyes.addVector(blockVector.xCoord * range, blockVector.yCoord * range, blockVector.zCoord * range));
    }

    public static Vec3 getLook(final Vec3 vec) {
        final double diffX = vec.xCoord - mc.thePlayer.posX;
        final double diffY = vec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double diffZ = vec.zCoord - mc.thePlayer.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        return getVectorForRotation((float) (-(MathHelper.atan2(diffY, dist) * 180.0 / 3.141592653589793)), (float) (MathHelper.atan2(diffZ, diffX) * 180.0 / 3.141592653589793 - 90.0));
    }

    public static Vec3 getVectorForRotation(final float pitch, final float yaw) {
        final float f2 = -MathHelper.cos(-pitch * 0.017453292f);
        return new Vec3(MathHelper.sin(-yaw * 0.017453292f - 3.1415927f) * f2, MathHelper.sin(-pitch * 0.017453292f), MathHelper.cos(-yaw * 0.017453292f - 3.1415927f) * f2);
    }

    public static AxisAlignedBB getBlocksAround(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        return new AxisAlignedBB(x - 2, y - 2, z - 1, x + 2, y + 1, z + 2);
    }

    public static List<BlockPos> getBlocksAroundEntity(Entity entity) {
        List<BlockPos> blocks = new ArrayList<>();
        int x = (int) Math.floor(entity.posX);
        int y = (int) Math.floor(entity.posY);
        int z = (int) Math.floor(entity.posZ);
        blocks.add(new BlockPos(x + 1, y, z));
        blocks.add(new BlockPos(x - 1, y, z));
        blocks.add(new BlockPos(x, y, z + 1));
        blocks.add(new BlockPos(x, y, z - 1));
        return blocks;
    }

    public static boolean hasCollision(BlockPos blockPos) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block != null && block.getCollisionBoundingBox(mc.theWorld, blockPos, mc.theWorld.getBlockState(blockPos)) != null;
    }

    public static int cropAroundAmount(BlockPos blockPos) {
        AxisAlignedBB axisAlignedBB = getBlocksAround(blockPos);
        int count = 0;
        for (int x = (int) axisAlignedBB.minX; x < axisAlignedBB.maxX; x++) {
            for (int y = (int) axisAlignedBB.minY; y < axisAlignedBB.maxY; y++) {
                for (int z = (int) axisAlignedBB.minZ; z < axisAlignedBB.maxZ; z++) {
                    BlockPos blockPos1 = new BlockPos(x, y, z);
                    Block block = mc.theWorld.getBlockState(blockPos1).getBlock();
                    if (CropUtils.isCropReady(block, blockPos1)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static boolean isFree(float x, float y, float z, IBlockAccess blockaccess) {
        GameStateHandler.Location location = GameStateHandler.getInstance().getLocation();
        if (location.equals(GameStateHandler.Location.GARDEN)) {
            if (y < 65 || x < -300 || x > 300 || z < -300 || z > 300) return false;
        }
        BlockPos blockpos = new BlockPos(x, y, z);
        IBlockState blockState = blockaccess.getBlockState(blockpos);
        Block block = blockState.getBlock();

        return !blockHasCollision(blockpos, blockState, block, blockaccess);
    }

    public static List<BlockPos> getBlocksInBB(AxisAlignedBB bb) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = (int) bb.minX; x < bb.maxX; x++) {
            for (int y = (int) bb.minY; y < bb.maxY; y++) {
                for (int z = (int) bb.minZ; z < bb.maxZ; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks;
    }

    public enum PathNodeType {
        OPEN,
        BLOCKED
    }

    public enum Direction {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT
    }
}
