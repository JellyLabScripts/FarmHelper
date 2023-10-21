package com.github.may2beez.farmhelperv2.util;

import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.config.struct.Rewarp;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlayerUtils {
    
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isInventoryEmpty(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    public static FarmHelperConfig.CropEnum getFarmingCrop() {
        Pair<Block, BlockPos> closestCrop = null;
        boolean foundCropUnderMouse = false;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block instanceof BlockCrops || block instanceof BlockReed || block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockMelon || block instanceof  BlockPumpkin || block instanceof BlockMushroom || block instanceof BlockCactus) {
                closestCrop = Pair.of(block, pos);
                foundCropUnderMouse = true;
            }
        }

        if (!foundCropUnderMouse) {
            for (int x = -3; x < 3; x++) {
                for (int y = -1; y < 5; y++) {
                    for (int z = -1; z < 3; z++) {
                        BlockPos pos = BlockUtils.getRelativeBlockPos(x, y, z,
                                (FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_MUSHROOM.ordinal() ||
                                        FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_SUGAR_CANE.ordinal() ?
                                        AngleUtils.getClosestDiagonal() - 45 :
                                        AngleUtils.getClosest()));
                        Block block = mc.theWorld.getBlockState(pos).getBlock();
                        if (!(block instanceof BlockCrops || block instanceof BlockReed || block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockMelon || block instanceof BlockPumpkin || block instanceof BlockMushroom || block instanceof BlockCactus))
                            continue;

                        if (closestCrop == null || mc.thePlayer.getPosition().distanceSq(pos.getX(), pos.getY(), pos.getZ()) < mc.thePlayer.getPosition().distanceSq(closestCrop.getRight().getX(), closestCrop.getRight().getY(), closestCrop.getRight().getZ())) {
                            closestCrop = Pair.of(block, pos);
                        }
                    }
                }
            }
        }

        if (closestCrop != null) {
            Block left = closestCrop.getLeft();
            if (left.equals(Blocks.wheat)) {
                return FarmHelperConfig.CropEnum.WHEAT;
            } else if (left.equals(Blocks.carrots)) {
                return FarmHelperConfig.CropEnum.CARROT;
            } else if (left.equals(Blocks.potatoes)) {
                return FarmHelperConfig.CropEnum.POTATO;
            } else if (left.equals(Blocks.nether_wart)) {
                return FarmHelperConfig.CropEnum.NETHER_WART;
            } else if (left.equals(Blocks.reeds)) {
                return FarmHelperConfig.CropEnum.SUGAR_CANE;
            } else if (left.equals(Blocks.cocoa)) {
                return FarmHelperConfig.CropEnum.COCOA_BEANS;
            } else if (left.equals(Blocks.melon_block)) {
                return FarmHelperConfig.CropEnum.MELON;
            } else if (left.equals(Blocks.pumpkin)) {
                return FarmHelperConfig.CropEnum.PUMPKIN;
            } else if (left.equals(Blocks.red_mushroom)) {
                return FarmHelperConfig.CropEnum.MUSHROOM;
            } else if (left.equals(Blocks.brown_mushroom)) {
                return FarmHelperConfig.CropEnum.MUSHROOM;
            } else if (left.equals(Blocks.cactus)) {
                return FarmHelperConfig.CropEnum.CACTUS;
            }
        }
        LogUtils.sendError("Can't detect crop type! Defaulting to wheat.");
        return FarmHelperConfig.CropEnum.WHEAT;
    }

    public static boolean itemChangedByStaff = false;
    public static final Clock changeItemEveryClock = new Clock();

    public static void getTool() {
        // Sometimes if staff changed your slot, you might not have the tool in your hand after the swap, so it won't be obvious that you're using a macro
        if (itemChangedByStaff) {
            return;
        }

        if (changeItemEveryClock.isScheduled() && !changeItemEveryClock.passed()) {
            return;
        }

        changeItemEveryClock.schedule(5_000L);
        mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop());
    }

    public static int getFarmingTool(FarmHelperConfig.CropEnum crop) {
        return getFarmingTool(crop, false, false);
    }

    public static int getFarmingTool(FarmHelperConfig.CropEnum crop, boolean withError, boolean fullInventory) {
        if (crop == null) return withError ? -1 : 0;
        for (int i = fullInventory ? 0 : 36; i < 44; i++) {
            if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {
                switch (crop) {
                    case NETHER_WART:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Newton")) {
                            return i - 36;
                        }
                        continue;
                    case CARROT:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Gauss")) {
                            return i - 36;
                        }
                        continue;
                    case WHEAT:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Euclid")) {
                            return i - 36;
                        }
                        continue;
                    case POTATO:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Pythagorean")) {
                            return i - 36;
                        }
                        continue;
                    case SUGAR_CANE:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Turing")) {
                            return i - 36;
                        }
                        continue;
                    case CACTUS:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Knife")) {
                            return i - 36;
                        }
                        continue;
                    case MUSHROOM:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Fungi")) {
                            return i - 36;
                        }
                        continue;
                    case MELON:
                    case PUMPKIN:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Dicer")) {
                            return i - 36;
                        }
                        continue;
                    case COCOA_BEANS:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Chopper")) {
                            return i - 36;
                        }
                }
            }
        }
        return withError ? -1 : 0;
    }
    public static boolean isRewarpLocationSet() {
        return !FarmHelperConfig.rewarpList.isEmpty();
    }

    public static boolean isStandingOnRewarpLocation() {
        if (FarmHelperConfig.rewarpList.isEmpty()) return false;
        Rewarp closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Rewarp rewarp : FarmHelperConfig.rewarpList) {
            double distance = rewarp.getDistance(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
            if (distance < closestDistance) {
                closest = rewarp;
                closestDistance = distance;
            }
        }
        if (closest == null) return false;
        BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        return playerPos.getX() == closest.getX() && playerPos.getY() == closest.getY() && playerPos.getZ() == closest.getZ();
    }

    public static boolean shouldPushBack() {
        if (Failsafe.getInstance().isEmergency()) return false;
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        Block blockBehind = BlockUtils.getRelativeBlock(0, 0, -1);
        if (!(blockBehind.getMaterial().isSolid() || (blockBehind instanceof BlockSlab) || blockBehind.equals(Blocks.carpet) || (blockBehind instanceof BlockDoor)) || blockBehind.getMaterial().isLiquid())
            return false;
        if (angle == 0) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 90) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        } else if (angle == 180) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 270) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        }
        return false;
    }

    public static boolean shouldWalkForwards() {
        if (Failsafe.getInstance().isEmergency()) return false;
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg() != -1337) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg();
        } else {
            yaw = mc.thePlayer.rotationYaw;
        }
        if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, 1, yaw))) {
            return false;
        }
        if (angle == 0) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 90) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        } else if (angle == 180) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 270) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        }
        return false;
    }

    public static boolean isSpawnLocationSet() {
        return FarmHelperConfig.spawnPosX != 0 || FarmHelperConfig.spawnPosY != 0 || FarmHelperConfig.spawnPosZ != 0;
    }

    public static boolean isStandingOnSpawnPoint() {
        BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        return pos.getX() == FarmHelperConfig.spawnPosX && pos.getY() == FarmHelperConfig.spawnPosY && pos.getZ() == FarmHelperConfig.spawnPosZ;
    }

    public static Vec3 getSpawnLocation() {
        return new Vec3(FarmHelperConfig.spawnPosX + 0.5, FarmHelperConfig.spawnPosY + 0.5, FarmHelperConfig.spawnPosZ + 0.5);
    }

    public static void setSpawnLocation() {
        if (mc.thePlayer == null) return;
        BlockPos pos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        FarmHelperConfig.spawnPosX = pos.getX();
        FarmHelperConfig.spawnPosY = pos.getY();
        FarmHelperConfig.spawnPosZ = pos.getZ();
        FarmHelper.config.save();
    }

    public static boolean isDeskPosSet() {
        return FarmHelperConfig.visitorsDeskPosX != 0 || FarmHelperConfig.visitorsDeskPosY != 0 || FarmHelperConfig.visitorsDeskPosZ != 0;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e) {
        return getEntityCuttingOtherEntity(e, false);
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, boolean armorStand) {
        List<Entity> possible = mc.theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(mc.thePlayer));
            boolean flag2 = armorStand == (a instanceof EntityArmorStand);
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag4 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            return flag1 && flag2 && flag3 && flag4;
        });
        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
        return null;
    }
}
