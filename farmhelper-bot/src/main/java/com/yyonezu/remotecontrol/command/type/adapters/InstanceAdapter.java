package com.yyonezu.remotecontrol.command.type.adapters;

import com.github.kaktushose.jda.commands.annotations.Component;
import com.github.kaktushose.jda.commands.dispatching.CommandContext;
import com.github.kaktushose.jda.commands.dispatching.adapter.TypeAdapter;
import com.yyonezu.remotecontrol.command.type.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Component
public class InstanceAdapter implements TypeAdapter<Instance> {

    @Override
    public Optional<Instance> parse(@NotNull String raw, @NotNull CommandContext context) {
        return Optional.of(new Instance(raw));
    }
}





