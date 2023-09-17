package com.jelly.farmhelper.remote.event;


import com.jelly.farmhelper.remote.DiscordBotHandler;
import com.jelly.farmhelper.remote.discordStruct.DiscordCommand;
import com.jelly.farmhelper.utils.LogUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class InteractionCreate extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        ArrayList<DiscordCommand> commands = DiscordBotHandler.commands;

        Optional<DiscordCommand> optionalCommand = commands.stream().filter(cmd -> cmd.name.equalsIgnoreCase(command)).findFirst();
        if (optionalCommand.isPresent()) {
            DiscordCommand cmd = optionalCommand.get();
            try {
                LogUtils.sendDebug("Executing command " + cmd.name);
                cmd.execute(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        System.out.println("Command not found");
        System.out.println(command);
        System.out.println("Commands list: " + Arrays.toString(commands.toArray()));
        LogUtils.sendDebug("Command not found");
        event.reply("Command not found").queue();
    }
}
