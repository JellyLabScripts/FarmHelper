package com.jelly.farmhelperv2.handler;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.pathing.goals.GoalNear;
import com.jelly.farmhelperv2.util.helper.BaritoneEventListener;
import lombok.Getter;
import net.minecraft.client.Minecraft;

public class BaritoneHandler {
    private static BaritoneHandler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    public boolean pathing = false;
    public static BaritoneHandler getInstance() {
        if (instance == null) {
            instance = new BaritoneHandler();
        }
        return instance;
    }

    private boolean checkForPathingFinish() {
        if (pathing) {
            if (!mc.thePlayer.onGround) return true;
            GoalNear goal = (GoalNear) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
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
}
