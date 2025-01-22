package com.jelly.farmhelperv2.pathfinder.custom.calculate;

import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.pathfinder.custom.util.BlockStateAccessor;
import com.jelly.farmhelperv2.pathfinder.custom.util.MovementHelper;
import com.jelly.farmhelperv2.util.helper.Rotation;
import net.minecraft.block.BlockCactus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PathNode {

    public final BlockPos pos, end;
    public double gCost, hCost;
    public final PathNode parent;
    public Rotation rotation = new Rotation(0, 0);

    public PathNode(BlockPos pos, BlockPos end, PathNode parent) {
        this.pos = pos;
        this.end = end;
        this.gCost = 0;
        this.parent = parent;
        this.hCost = pos.distanceSq(end);

        if (parent != null) {
            this.gCost = parent.gCost + parent.pos.distanceSq(pos);
        }
    }

    public double getTotalCost() {
        return gCost + hCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PathNode)) return false;
        return pos == ((PathNode) obj).pos;
    }

    public boolean isTraversable(BlockStateAccessor bsa) {
        if (pos.getX() != parent.pos.getX() && pos.getZ() != parent.pos.getZ() && MovementHelper.canNotMoveDiagonal(parent.pos, pos, bsa)) return false;
        if (!MovementHelper.isFree(bsa.get(pos.up())) || !MovementHelper.isFree(bsa.get(pos))) return false;
        if (pos.getY() > parent.pos.getY() && !MovementHelper.cannotJump(bsa.get(pos.down()))) return false;

        gCost += calculateCost(bsa);
        return true;
    }
    
    private double calculateCost(BlockStateAccessor bsa) {
        double cost = 0;

        if (MovementHelper.isLiquid(bsa.get(pos.up(2))) || !MovementHelper.isFree(bsa.get(pos.up(2)))) cost += 2;
        if (MovementHelper.isLiquid(bsa.get(pos.down())) || !MovementHelper.isFree(bsa.get(pos.down()))) cost += 2;
        if (bsa.get(pos.down()) instanceof BlockCactus) cost += 3;

        rotation = RotationHandler.getInstance().getRotation(new Vec3(parent.pos), new Vec3(pos));
        if (parent.rotation.getYaw() != rotation.getYaw()) cost *= 2;
        
        return cost;
    }

    public List<PathNode> getNeighbors() {
        List<BlockPos> neighbors = new ArrayList<>();

        neighbors.add(pos.add(1, 0, 0));
        neighbors.add(pos.add(-1, 0, 0));
        neighbors.add(pos.add(0, 0, 1));
        neighbors.add(pos.add(0, 0, -1));

        neighbors.add(pos.add(1, 0, -1));
        neighbors.add(pos.add(-1, 0, -1));
        neighbors.add(pos.add(1, 0, 1));
        neighbors.add(pos.add(-1, 0, 1));

        neighbors.add(pos.add(0, 1, 0));
        neighbors.add(pos.add(0, -1, 0));

        return neighbors.stream().map(item -> new PathNode(item, end, this)).collect(Collectors.toList());
    }

}
