package com.jelly.farmhelper.commands;

import com.jelly.farmhelper.FarmHelper;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FarmHelperCommand implements ICommand {
    public static ArrayList<String> aliases = new ArrayList<>(Arrays.asList("fh", "farmhelper", "farmhelpermod", "farmhelper-mod"));

    @Override
    public String getCommandName() {
        return "fh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fh";
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        FarmHelper.config.openGui();
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
