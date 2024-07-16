package com.jelly.farmhelperv2.remote.command.discordCommands.impl;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.remote.WebsocketHandler;
import com.jelly.farmhelperv2.remote.command.discordCommands.DiscordCommand;
import com.jelly.farmhelperv2.remote.command.discordCommands.Option;
import com.jelly.farmhelperv2.remote.waiter.Waiter;
import com.jelly.farmhelperv2.remote.waiter.WaiterHandler;
import com.jelly.farmhelperv2.util.AvatarUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Base64;
import java.util.Objects;

public class Reconnect extends DiscordCommand {
    public static final String name = "reconnect";
    public static final String description = "Reconnect to the server";

    public Reconnect() {
        super(name, description);
        addOptions(new Option(OptionType.STRING, "ign", "The IGN of the instance", false, true),
                new Option(OptionType.INTEGER, "delay", "The delay before reconnecting in ms", false, false));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        int timeout = 5000;
        WaiterHandler.register(new Waiter(
                timeout,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    String image = action.args.get("image").getAsString();
                    int delay = action.args.get("delay").getAsInt();
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, true);
                    embedBuilder.addField("Reconnecting in", delay + "ms", true);
                    embedBuilder.setImage("attachment://image.png");
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> FarmHelper Remote Control", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
                    String avatar = AvatarUtils.getAvatarUrl(action.args.get("uuid").getAsString());
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    MessageEmbed em = embedBuilder.build();
                    try {
                        event.getHook().sendMessageEmbeds(em).addFiles(FileUpload.fromData(Base64.getDecoder().decode(image), "image.png")).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(em).addFiles(FileUpload.fromData(Base64.getDecoder().decode(image), "image.png")).queue();
                    }
                },
                timeoutAction -> {
                    try {
                        event.getHook().sendMessage("Can't invoke the reconnect").queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessage("Can't invoke the reconnect").queue();
                    }
                },
                event
        ));
        int delay = event.getOption("delay") == null ? 5_000 : Objects.requireNonNull(event.getOption("delay")).getAsInt();
        JsonObject args = new JsonObject();
        args.addProperty("delay", delay);
        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.containsValue(ign)) {
                WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket, args);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> sendMessage(webSocket, args));
        }
    }
}
