package com.github.may2beez.farmhelperv2.command;

import com.github.may2beez.farmhelperv2.FarmHelper;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FarmHelperCommand implements ICommand {
    public static ArrayList<String> aliases = new ArrayList<>(Arrays.asList("fh", "farmhelper"));

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
