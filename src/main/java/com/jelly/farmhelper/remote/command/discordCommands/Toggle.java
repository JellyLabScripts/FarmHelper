package com.jelly.farmhelper.remote.command.discordCommands;

import com.jelly.farmhelper.remote.WebsocketHandler;
import com.jelly.farmhelper.remote.discordStruct.DiscordCommand;
import com.jelly.farmhelper.remote.waiter.Waiter;
import com.jelly.farmhelper.remote.waiter.WaiterHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Objects;

public class Toggle extends DiscordCommand {
    public static final Toggle INSTANCE = new Toggle();
    public static final String name = "toggle";
    public static final String description = "Toggle the bot";

    public Toggle() {
        super(Toggle.name, Toggle.description);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WaiterHandler.register(new Waiter(
                5000,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    boolean toggled = action.args.get("toggled").getAsBoolean();
                    String uuid = action.args.get("uuid").getAsString();
                    boolean error = action.args.has("error");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, false);
                    if (error) {
                        embedBuilder.addField("Error", action.args.get("error").getAsString(), false);
                    }
                    embedBuilder.addField("Turned macro", (toggled && !error) ? "On" : "Off", false);
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> FarmHelper Remote Control", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
                    String avatar = "https://crafatar.com/avatars/" + uuid;
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    try {
                        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                },
                timeoutAction -> {
                    event.getHook().sendMessage("Can't toggle the bot").queue();
                },
                event
        ));

        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.websocketServer.minecraftInstances.containsValue(ign)) {
                WebsocketHandler.websocketServer.minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.websocketServer.minecraftInstances.forEach((webSocket, s) -> {
                sendMessage(webSocket);
            });
        }
    }
}
