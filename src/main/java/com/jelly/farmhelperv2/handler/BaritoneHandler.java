package com.jelly.farmhelperv2.handler;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import com.jelly.farmhelperv2.util.helper.BaritoneEventListener;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class BaritoneHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    public static boolean pathing = false;

    public static boolean isWalkingToGoalBlock() {
        if (pathing) {
            if (!mc.thePlayer.onGround) return true;
            GoalBlock goal = (GoalBlock) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
            double distance = mc.thePlayer.getDistance(goal.getGoalPos().getX() + 0.5f, mc.thePlayer.posY, goal.getGoalPos().getZ() + 0.5);
            System.out.println("Pathing result: " + BaritoneEventListener.pathEvent);
            if (distance <= 1.5 || BaritoneEventListener.pathEvent == PathEvent.AT_GOAL) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                pathing = false;
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean hasFailed() {
        if (BaritoneEventListener.pathEvent == PathEvent.CALC_FAILED
                || BaritoneEventListener.pathEvent == PathEvent.NEXT_CALC_FAILED) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            pathing = false;
            return true;
        }
        return false;
    }

    public static void walkToBlockPos(BlockPos blockPos) {
        PathingCommand pathingCommand = new PathingCommand(new GoalBlock(blockPos), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().secretInternalSetGoalAndPath(pathingCommand);
        pathing = true;
    }

    public static void stopPathing() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        pathing = false;
    }
}
