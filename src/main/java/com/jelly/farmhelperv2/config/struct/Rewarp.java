package com.jelly.farmhelperv2.config.struct;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import net.minecraft.util.BlockPos;

@Getter
public class Rewarp {
    @Expose
    public int x;
    @Expose
    public int y;
    @Expose
    public int z;

    public Rewarp(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Rewarp(BlockPos blockPos) {
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
    }

    public double getDistance(BlockPos to) {
        return Math.sqrt(Math.pow(to.getX() - x, 2) + Math.pow(to.getY() - y, 2) + Math.pow(to.getZ() - z, 2));
    }

    public boolean isTheSameAs(BlockPos blockPos) {
        return x == blockPos.getX() && y == blockPos.getY() && z == blockPos.getZ();
    }

    @Override
    public String toString() {
        return "Rewarp{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
