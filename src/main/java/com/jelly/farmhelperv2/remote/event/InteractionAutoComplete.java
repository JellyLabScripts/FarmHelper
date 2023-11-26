package com.jelly.farmhelperv2.remote.event;

import com.jelly.farmhelperv2.remote.WebsocketHandler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InteractionAutoComplete extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getFocusedOption().getName().equals("ign")) return;
        HashMap<WebSocket, String> instances = WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances;
        List<String> usernames = new ArrayList<>();
        for (WebSocket socket : instances.keySet()) {
            usernames.add(instances.get(socket));
        }
        List<Command.Choice> options = Stream.of(usernames.toArray(new String[0]))
                .filter(word -> word.toLowerCase()
                        .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                .map(word -> new Command.Choice(word, word))
                .collect(Collectors.toList());
        event.replyChoices(options).queue();
    }
}
