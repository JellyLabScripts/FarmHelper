package com.jelly.farmhelper.commands;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RewarpCommand implements ICommand {
    @Override
    public String getCommandName() {
        return "fhrewarp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fhrewarp [reset|set]";
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            LogUtils.scriptLog("Invalid arguments");
            LogUtils.scriptLog("Use /fhrewarp [reset|set]");
            return;
        }

        if (args[0].equals("reset")) {
            LogUtils.scriptLog("Resetting re-warp location");
            MiscConfig.rewarpPosX = 0;
            MiscConfig.rewarpPosY = 0;
            MiscConfig.rewarpPosZ = 0;
            ConfigHandler.set("rewarpPosX", 0);
            ConfigHandler.set("rewarpPosY", 0);
            ConfigHandler.set("rewarpPosZ", 0);
            return;
        }

        if (args[0].equals("set")) {
            LogUtils.scriptLog("Setting re-warp location to: X: " + sender.getPosition().getX() + ", Y: " + sender.getPosition().getY() + ", Z: " + sender.getPosition().getZ());
            MiscConfig.rewarpPosX = (int) sender.getPosition().getX();
            MiscConfig.rewarpPosY = (int) sender.getPosition().getY();
            MiscConfig.rewarpPosZ = (int) sender.getPosition().getZ();
            ConfigHandler.set("rewarpPosX", sender.getPosition().getX());
            ConfigHandler.set("rewarpPosY", sender.getPosition().getY());
            ConfigHandler.set("rewarpPosZ", sender.getPosition().getZ());
        }


    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(@NotNull ICommand o) {
        return 0;
    }
}
