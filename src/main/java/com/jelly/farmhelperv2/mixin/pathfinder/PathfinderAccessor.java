package com.jelly.farmhelperv2.mixin.pathfinder;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PathFinder.class)
public interface PathfinderAccessor {
    @Invoker("createEntityPathTo")
    public abstract PathEntity createPath(IBlockAccess blockaccess, Entity entityIn, double x, double y, double z, float distance);
}
