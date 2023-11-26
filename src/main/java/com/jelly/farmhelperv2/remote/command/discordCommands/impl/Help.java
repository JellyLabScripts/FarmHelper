package com.jelly.farmhelperv2.remote.command.discordCommands.impl;

import com.jelly.farmhelperv2.remote.DiscordBotHandler;
import com.jelly.farmhelperv2.remote.command.discordCommands.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Help extends DiscordCommand {
    public static final String name = "help";
    public static final String description = "Get information about commands";

    public Help() {
        super(Help.name, Help.description);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("FarmHelper Remote Control");
        builder.setDescription("Commands list");
        for (DiscordCommand command : DiscordBotHandler.getInstance().getCommands()) {
            builder.addField(command.name, command.description, false);
        }
        int random = (int) (Math.random() * 0xFFFFFF);
        builder.setColor(random);
        builder.setFooter("-> FarmHelper Remote Control", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
        MessageEmbed embed = builder.build();
        try {
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }
    }
}
