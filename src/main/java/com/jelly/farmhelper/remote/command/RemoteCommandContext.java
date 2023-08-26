package com.jelly.farmhelper.remote.command;

import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.command.CommandInstance;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

public class RemoteCommandContext extends CommandContext<MessageEvent> {

    public RemoteCommandContext(MessageEvent commandSource, CommandInstance command, ParameterSet parameter) {
        super(commandSource, command, parameter);
    }
}
