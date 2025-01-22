package com.jelly.farmhelperv2.command;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.jelly.farmhelperv2.feature.impl.AutoWardrobe;
import com.jelly.farmhelperv2.feature.impl.PestFarmer;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

@Command("test")
public class TestCommand {

    public BlockPos position;

    @SubCommand
    public void debug() {
        LogUtils.sendSuccess("Cooldown Over: " + PestFarmer.getInstance().isCooldownOver());
        LogUtils.sendSuccess("Current Armor Slot: " + AutoWardrobe.activeSlot);
    }

    @SubCommand
    public void fly() {
        FlyPathFinderExecutor.getInstance().findPath(new Vec3(position.getX() + 0.5, position.getY(), position.getZ() + 0.5), false, true);
        LogUtils.sendSuccess("Flying to " + position);
    }

    @SubCommand()
    public void sp() {
        position = new BlockPos(
                Minecraft.getMinecraft().thePlayer.posX,
                Minecraft.getMinecraft().thePlayer.posY,
                Minecraft.getMinecraft().thePlayer.posZ
        );

        LogUtils.sendSuccess("Saving position as " + position);
    }

}
