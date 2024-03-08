package com.jelly.farmhelperv2.util.helper;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class PlayerSimulation {

    public final World worldObj;

    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    @Getter
    @Setter
    private AxisAlignedBB entityBoundingBox;

    public boolean onGround;

    public float jumpMovementFactor;

    public float landMovementFactor;

    public float fallDistance;

    public boolean isCollided;

    public boolean isCollidedHorizontally;

    public boolean isCollidedVertically;

    public float rotationYaw;

    public boolean noClip;

//    public Entity ridingEntity;

    public boolean isSneaking;

    public boolean isSprinting;

    public boolean isFlying;

    public boolean isOnLadder;

    public float stepHeight;

    public float moveStrafing;

    public float moveForward;

    public int depthStriderModifier;

    public PlayerSimulation(World world) {
        this.worldObj = world;
    }

    public void copy(EntityLivingBase entity) {
        posX = entity.posX;
        posY = entity.posY;
        posZ = entity.posZ;
        motionX = entity.motionX;
        motionY = entity.motionY;
        motionZ = entity.motionZ;
        entityBoundingBox = entity.getEntityBoundingBox();
        onGround = entity.onGround;
        jumpMovementFactor = entity.jumpMovementFactor;
        landMovementFactor = entity.getAIMoveSpeed();
        fallDistance = entity.fallDistance;
        isCollided = entity.isCollided;
        isCollidedHorizontally = entity.isCollidedHorizontally;
        isCollidedVertically = entity.isCollidedVertically;
        rotationYaw = entity.rotationYaw;
        noClip = entity.noClip;
        isSneaking = entity.isSneaking();
        isSprinting = entity.isSprinting();
        isFlying = false;
        stepHeight = entity.stepHeight;
        moveStrafing = 0;
        moveForward = 0;
        depthStriderModifier = EnchantmentHelper.getDepthStriderModifier(entity);
    }

    public float getAIMoveSpeed() {
        return this.landMovementFactor;
    }

    public boolean isInLava() {
        return this.worldObj.isMaterialInBB(this.getEntityBoundingBox().expand(-0.1f, -0.4f, -0.1f), Material.lava);
    }

    public boolean isInWater() {
        return this.worldObj.isMaterialInBB(this.getEntityBoundingBox().expand(-0.1f, -0.4f, -0.1f), Material.water);
    }

    public boolean isOffsetPositionInLiquid(double x, double y, double z) {
        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox().offset(x, y, z);
        return this.worldObj.isMaterialInBB(axisalignedbb.expand(-0.1f, -0.4f, -0.1f), Material.water) ||
                this.worldObj.isMaterialInBB(axisalignedbb.expand(-0.1f, -0.4f, -0.1f), Material.lava);
    }

    public void onLivingUpdate() {
        if (Math.abs(this.motionX) < 0.005) {
            this.motionX = 0.0;
        }
        if (Math.abs(this.motionY) < 0.005) {
            this.motionY = 0.0;
        }
        if (Math.abs(this.motionZ) < 0.005) {
            this.motionZ = 0.0;
        }
        this.moveStrafing *= 0.98f;
        this.moveForward *= 0.98f;
        moveEntityWithHeading_EntityPlayer(moveStrafing, moveForward);
    }

    public void moveEntityWithHeading_EntityPlayer(float strafe, float forward) {
//        if (this.isFlying && this.ridingEntity == null)
        if (this.isFlying) {
            double d3 = this.motionY;
            float f = this.jumpMovementFactor;
            this.jumpMovementFactor = 0.05F * (float) (this.isSprinting ? 2 : 1);
            this.moveEntityWithHeading(strafe, forward);
            this.motionY = d3 * 0.6D;
            this.jumpMovementFactor = f;
        } else {
            this.moveEntityWithHeading(strafe, forward);
        }
    }

    public void moveEntityWithHeading(float strafe, float forward) {
        if (!this.isInWater() || this.isFlying) {
            if (!this.isInLava() || this.isFlying) {
                float f4 = 0.91f;
                if (this.onGround) {
                    f4 = this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(this.posZ))).getBlock().slipperiness * 0.91f;
                }
                float f = 0.16277136f / (f4 * f4 * f4);
                float f5 = this.onGround ? this.getAIMoveSpeed() * f : this.jumpMovementFactor;
                this.moveFlying(strafe, forward, f5);
                f4 = 0.91f;
                if (this.onGround) {
                    f4 = this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(this.posZ))).getBlock().slipperiness * 0.91f;
                }

                this.moveEntity(this.motionX, this.motionY, this.motionZ);

                if (this.isCollidedHorizontally && this.isOnLadder) {
                    this.motionY = 0.2D;
                }

                if (this.worldObj.isRemote && (!this.worldObj.isBlockLoaded(new BlockPos((int) this.posX, 0, (int) this.posZ)) || !this.worldObj.getChunkFromBlockCoords(new BlockPos((int) this.posX, 0, (int) this.posZ)).isLoaded())) {
                    if (this.posY > 0.0D) {
                        this.motionY = -0.1D;
                    } else {
                        this.motionY = 0.0D;
                    }
                } else {
                    this.motionY -= 0.08D;
                }

                this.motionY *= 0.98F;
                this.motionX *= f4;
                this.motionZ *= f4;
            } else {
                double d1 = this.posY;
                this.moveFlying(strafe, forward, 0.02f);
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                this.motionX *= 0.5;
                this.motionY *= 0.5;
                this.motionZ *= 0.5;
                this.motionY -= 0.02;
                if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + (double) 0.6f - this.posY + d1, this.motionZ)) {
                    this.motionY = 0.3f;
                }
            }
        } else {
            double d0 = this.posY;
            float f1 = 0.8f;
            float f2 = 0.02f;
            float f3 = this.depthStriderModifier;
            if (!this.onGround) {
                f3 *= 0.5f;
            }
            if (f3 > 0.0f) {
                f1 += (0.54600006f - f1) * f3 / 3.0f;
                f2 += (this.getAIMoveSpeed() - f2) * f3 / 3.0f;
            }
            this.moveFlying(strafe, forward, f2);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= f1;
            this.motionY *= 0.8f;
            this.motionZ *= f1;
            this.motionY -= 0.02;
            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + (double) 0.6f - this.posY + d0, this.motionZ)) {
                this.motionY = 0.3f;
            }
        }
    }

    public void moveFlying(float strafe, float forward, float friction) {
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4f) {
            if ((f = MathHelper.sqrt_float(f)) < 1.0f) {
                f = 1.0f;
            }
            f = friction / f;
            float f1 = MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0f);
            float f2 = MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0f);
            this.motionX += ((strafe *= f) * f2 - (forward *= f) * f1);
            this.motionZ += (forward * f2 + strafe * f1);
        }
    }

    public void moveEntity(double x, double y, double z) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
            this.resetPositionToBB();
        } else {
            boolean flag;
            double d3 = x;
            double d4 = y;
            double d5 = z;
            flag = this.onGround && this.isSneaking;
            if (flag) {
                double d6 = 0.05;
                while (x != 0.0 && this.worldObj.getCollidingBoundingBoxes(player, this.getEntityBoundingBox().offset(x, -1.0, 0.0)).isEmpty()) {
                    x = x < d6 && x >= -d6 ? 0.0 : (x > 0.0 ? x - d6 : x + d6);
                    d3 = x;
                }
                while (z != 0.0 && this.worldObj.getCollidingBoundingBoxes(player, this.getEntityBoundingBox().offset(0.0, -1.0, z)).isEmpty()) {
                    z = z < d6 && z >= -d6 ? 0.0 : (z > 0.0 ? z - d6 : z + d6);
                    d5 = z;
                }
                while (x != 0.0 && z != 0.0 && this.worldObj.getCollidingBoundingBoxes(player, this.getEntityBoundingBox().offset(x, -1.0, z)).isEmpty()) {
                    x = x < d6 && x >= -d6 ? 0.0 : (x > 0.0 ? x - d6 : x + d6);
                    d3 = x;
                    z = z < d6 && z >= -d6 ? 0.0 : (z > 0.0 ? z - d6 : z + d6);
                    d5 = z;
                }
            }
            List<AxisAlignedBB> list1 = this.worldObj.getCollidingBoundingBoxes(player, this.getEntityBoundingBox().addCoord(x, y, z));
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            for (AxisAlignedBB axisAlignedBB : list1) {
                y = axisAlignedBB.calculateYOffset(this.getEntityBoundingBox(), y);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
            boolean flag1 = this.onGround || d4 != y && d4 < 0.0;
            for (AxisAlignedBB axisalignedbb2 : list1) {
                x = axisalignedbb2.calculateXOffset(this.getEntityBoundingBox(), x);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0, 0.0));
            for (AxisAlignedBB axisalignedbb13 : list1) {
                z = axisalignedbb13.calculateZOffset(this.getEntityBoundingBox(), z);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, z));
            if (this.stepHeight > 0.0f && flag1 && (d3 != x || d5 != z)) {
                double d = x;
                double d7 = y;
                double d8 = z;
                AxisAlignedBB axisalignedbb3 = this.getEntityBoundingBox();
                this.setEntityBoundingBox(axisalignedbb);
                y = this.stepHeight;
                List<AxisAlignedBB> list = this.worldObj.getCollidingBoundingBoxes(player, this.getEntityBoundingBox().addCoord(d3, y, d5));
                AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
                AxisAlignedBB axisalignedbb5 = axisalignedbb4.addCoord(d3, 0.0, d5);
                double d9 = y;
                for (AxisAlignedBB axisalignedbb6 : list) {
                    d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);
                }
                axisalignedbb4 = axisalignedbb4.offset(0.0, d9, 0.0);
                double d15 = d3;
                for (AxisAlignedBB axisalignedbb7 : list) {
                    d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15);
                }
                axisalignedbb4 = axisalignedbb4.offset(d15, 0.0, 0.0);
                double d16 = d5;
                for (AxisAlignedBB axisalignedbb8 : list) {
                    d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16);
                }
                axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, d16);
                AxisAlignedBB axisalignedbb14 = this.getEntityBoundingBox();
                double d17 = y;
                for (AxisAlignedBB axisalignedbb9 : list) {
                    d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17);
                }
                axisalignedbb14 = axisalignedbb14.offset(0.0, d17, 0.0);
                double d18 = d3;
                for (AxisAlignedBB axisalignedbb10 : list) {
                    d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18);
                }
                axisalignedbb14 = axisalignedbb14.offset(d18, 0.0, 0.0);
                double d19 = d5;
                for (AxisAlignedBB axisalignedbb11 : list) {
                    d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19);
                }
                axisalignedbb14 = axisalignedbb14.offset(0.0, 0.0, d19);
                double d20 = d15 * d15 + d16 * d16;
                double d10 = d18 * d18 + d19 * d19;
                if (d20 > d10) {
                    x = d15;
                    z = d16;
                    y = -d9;
                    this.setEntityBoundingBox(axisalignedbb4);
                } else {
                    x = d18;
                    z = d19;
                    y = -d17;
                    this.setEntityBoundingBox(axisalignedbb14);
                }
                for (AxisAlignedBB axisalignedbb12 : list) {
                    y = axisalignedbb12.calculateYOffset(this.getEntityBoundingBox(), y);
                }
                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));
                if (d * d + d8 * d8 >= x * x + z * z) {
                    x = d;
                    y = d7;
                    z = d8;
                    this.setEntityBoundingBox(axisalignedbb3);
                }
            }
            this.resetPositionToBB();
            this.isCollidedHorizontally = d3 != x || d5 != z;
            this.isCollidedVertically = d4 != y;
            this.onGround = this.isCollidedVertically && d4 < 0.0;
            this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
            this.updateFallState(y, this.onGround);
            if (d3 != x) {
                this.motionX = 0.0;
            }
            if (d5 != z) {
                this.motionZ = 0.0;
            }
            if (d4 != y) {
                motionY = 0;
            }
        }
    }

    private void resetPositionToBB() {
        this.posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0;
        this.posY = this.getEntityBoundingBox().minY;
        this.posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0;
    }


    protected void updateFallState(double y, boolean onGroundIn) {
        if (onGroundIn) {
            if (this.fallDistance > 0.0f) {
                this.fallDistance = 0.0f;
            }
        } else if (y < 0.0) {
            this.fallDistance = (float) ((double) this.fallDistance - y);
        }
    }
}