package com.jelly.farmhelper.remote.command.discordCommands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.WebsocketHandler;
import com.jelly.farmhelper.remote.discordStruct.DiscordCommand;
import com.jelly.farmhelper.remote.discordStruct.Option;
import com.jelly.farmhelper.remote.waiter.Waiter;
import com.jelly.farmhelper.remote.waiter.WaiterHandler;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Objects;

public class Screenshot extends DiscordCommand {
    public static final String name = "screenshot";
    public static final String description = "Take a screenshot";

    public Screenshot() {
        super(name, description);
        addOptions(
                new Option(OptionType.STRING, "ign", "The IGN of the instance", false, true),
                new Option(OptionType.BOOLEAN, "inventory", "Take a screenshot of the inventory", false, false)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WaiterHandler.register(new Waiter(
                5000,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    String uuid = action.args.get("uuid").getAsString();
                    String image = action.args.get("image").getAsString();

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, true);
                    embedBuilder.setImage("attachment://image.png");
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> FarmHelper Remote Control", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
                    String avatar = "https://crafatar.com/avatars/" + uuid;
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    try {
                        event.getHook().sendMessageEmbeds(embedBuilder.build()).addFiles(FileUpload.fromData(Base64.decode(image), "image.png")).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).addFiles(FileUpload.fromData(Base64.decode(image), "image.png")).queue();
                    }
                },
                timeoutAction -> {
                    event.getHook().sendMessage("Can't take a screenshot").queue();
                },
                event
        ));

        boolean inventory = event.getOption("inventory") != null && Objects.requireNonNull(event.getOption("inventory")).getAsBoolean();
        JsonObject args = new JsonObject();
        args.addProperty("inventory", inventory);
        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.websocketServer.minecraftInstances.containsValue(ign)) {
                WebsocketHandler.websocketServer.minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket, args);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.websocketServer.minecraftInstances.forEach((webSocket, s) -> {
                sendMessage(webSocket, args);
            });
        }
    }
}
