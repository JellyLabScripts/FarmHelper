package com.jelly.farmhelper.remote.event;


import com.jelly.farmhelper.remote.DiscordBotHandler;
import com.jelly.farmhelper.remote.discordStruct.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Set;

public class InteractionCreate extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        ArrayList<Command> commands = DiscordBotHandler.commands;

        for (Command cmd : commands) {
            if (cmd.name.equalsIgnoreCase(command)) {
                try {
                    cmd.execute(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        event.reply("Command not found").queue();
    }
}
