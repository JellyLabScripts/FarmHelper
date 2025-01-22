package com.jelly.farmhelperv2.pathfinder.custom.calculate;

import com.jelly.farmhelperv2.pathfinder.custom.util.BlockStateAccessor;
import com.jelly.farmhelperv2.pathfinder.custom.util.MovementHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.*;

public class Pathfinder {

    public List<BlockPos> calculatePath(BlockPos end, boolean smooth) {
        return calculatePath(new BlockPos(
                Minecraft.getMinecraft().thePlayer.posX,
                Minecraft.getMinecraft().thePlayer.posY,
                Minecraft.getMinecraft().thePlayer.posZ
        ), end, smooth);
    }

    public List<BlockPos> calculatePath(BlockPos start, BlockPos end, boolean smooth) {
        BlockStateAccessor bsa = new BlockStateAccessor(Minecraft.getMinecraft().theWorld);

        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(PathNode::getTotalCost));
        Set<PathNode> closed = new HashSet<>();

        PathNode best = new PathNode(start, end, null);
        open.add(best);

        while (!open.isEmpty()) {
            PathNode curr = open.poll();
            assert curr != null;
            closed.add(curr);

            if (curr.getTotalCost() < best.getTotalCost())
                best = curr;
            if (end.equals(curr.pos))
                return smooth ? smoothPath(reconstruct(curr), bsa) : reconstruct(curr);

            for (PathNode neighbor : curr.getNeighbors()) {
                if (closed.contains(neighbor)) continue;

                if (neighbor.isTraversable(bsa)) {
                    if (!open.contains(neighbor)) {
                        open.add(neighbor);
                    } else if (neighbor.getTotalCost() < curr.getTotalCost()) {
                        open.remove(neighbor);
                        open.add(neighbor);
                    }
                }
            }
        }

        return smooth ? smoothPath(reconstruct(best), bsa) : reconstruct(best);
    }

    private List<BlockPos> smoothPath(List<BlockPos> path, BlockStateAccessor bsa) {
        List<BlockPos> smooth = new ArrayList<>();

        smooth.add(path.get(0));
        int currPoint = 0;

        while (currPoint + 1 < path.size()) {
            int nextPos = currPoint + 1;

            for (int i = path.size() - 1; i >= nextPos; i--) {
                if (MovementHelper.bresenham(
                    bsa,
                    new Vec3(path.get(currPoint)).add(new Vec3(0.5, 0.5, 0.5)),
                    new Vec3(path.get(i)).add(new Vec3(0.5, 0.5, 0.5))
                )) {
                    nextPos = i;
                    break;
                }
            }

            smooth.add(path.get(nextPos));
            currPoint = nextPos;
        }

        return smooth;
    }

    private List<BlockPos> reconstruct(PathNode node) {
        PathNode curr = node;
        List<BlockPos> path = new ArrayList<>();

        while(curr.parent != null) {
            path.add(0, curr.pos);
            curr = curr.parent;
        }

        return path;
    }

}
