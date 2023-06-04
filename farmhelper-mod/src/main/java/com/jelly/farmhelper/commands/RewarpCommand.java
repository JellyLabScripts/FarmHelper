package com.jelly.farmhelper.commands;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RewarpCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "fhrewarp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fhrewarp [add|remove|removeall]";
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            LogUtils.scriptLog("Invalid arguments");
            LogUtils.scriptLog("Use /fhrewarp [add|remove|removeall]");
            return;
        }

        switch (args[0]) {
            case "add": {
                if (ConfigHandler.rewarpList.stream().anyMatch(rewarp -> rewarp.isTheSameAs(BlockUtils.getRelativeBlockPos(0, 0, 0)))) {
                    LogUtils.scriptLog("Rewarp location already set");
                    return;
                }
                ConfigHandler.addRewarp(new Rewarp(BlockUtils.getRelativeBlockPos(0, 0, 0)));
                break;
            }
            case "remove": {
                Rewarp closest = null;
                if (ConfigHandler.rewarpList.size() == 0) {
                    LogUtils.scriptLog("No rewarp locations set");
                    return;
                }
                double closestDistance = Double.MAX_VALUE;
                for (Rewarp rewarp : ConfigHandler.rewarpList) {
                    double distance = rewarp.getDistance(BlockUtils.getRelativeBlockPos(0, 0, 0));
                    if (distance < closestDistance) {
                        closest = rewarp;
                        closestDistance = distance;
                    }
                }
                if (closest != null) {
                    ConfigHandler.removeRewarp(closest);
                }
                break;
            }
            case "removeall": {
                ConfigHandler.removeAllRewarps();
                break;
            }
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

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }
}
