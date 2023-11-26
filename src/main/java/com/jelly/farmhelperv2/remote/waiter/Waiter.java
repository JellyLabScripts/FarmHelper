package com.jelly.farmhelperv2.remote.waiter;


import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.function.Consumer;

@Getter
public class Waiter {
    private final Integer timeout;
    private final String command;
    private final Consumer<RemoteMessage> action;
    private final Consumer<String> timeoutAction;
    public final SlashCommandInteractionEvent event;
    @Setter
    private boolean answeredAtLeastOnce = false;

    public Waiter(Integer timeout, String command, Consumer<RemoteMessage> action, Consumer<String> timeoutAction, SlashCommandInteractionEvent event) {
        this.timeout = timeout;
        this.command = command;
        this.action = action;
        this.timeoutAction = timeoutAction;
        this.event = event;
    }
}
