package com.jelly.farmhelper.remote.waiter;

import com.jelly.farmhelper.remote.struct.RemoteMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.function.Consumer;

public class Waiter {
    @Getter
    private Integer timeout;
    @Getter
    private String command;
    @Getter
    private Consumer<RemoteMessage> action;
    @Getter
    private Consumer<String> timeoutAction;
    @Getter
    public final SlashCommandInteractionEvent event;
    @Getter
    @Setter
    private boolean answeredAtleastOnce = false;

    public Waiter(Integer timeout, String command, Consumer<RemoteMessage> action, Consumer<String> timeoutAction, SlashCommandInteractionEvent event) {
        this.timeout = timeout;
        this.command = command;
        this.action = action;
        this.timeoutAction = timeoutAction;
        this.event = event;
    }
}
