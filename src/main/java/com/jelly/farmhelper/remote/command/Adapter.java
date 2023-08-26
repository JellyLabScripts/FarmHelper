package com.jelly.farmhelper.remote.command;

import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.BrigadierAdapter;
import dev.volix.lib.brigadier.command.CommandInstance;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

public class Adapter extends BrigadierAdapter<MessageEvent> {

    @Override
    public void handleRegister(final String label, final CommandInstance command) {
		/*
		  this method will be called if we register a command to brigadier
		  we can use this to register the command to _Bukkit_ or _BungeeCord_ for example
		*/
    }

    @Override
    public boolean checkPermission(final MessageEvent commandSource, final CommandInstance command) {
        return true;
    }

    @Override
    public void runAsync(final Runnable runnable) {
    }

    @Override
    public Class<MessageEvent> getCommandSourceClass() {
		/*
		  returns the class of the command source as we can't get
		  the exact source class during runtime otherwise
		*/
        return MessageEvent.class;
    }

    @Override
    public CommandContext<MessageEvent> constructCommandContext(final MessageEvent commandSource, final CommandInstance command, final ParameterSet parameter) {
		/*
		  returns the context we created earlier.
		  This way brigadier can provide us the context automatically during execution
		*/
        return new RemoteCommandContext(commandSource, command, parameter);
    }

}