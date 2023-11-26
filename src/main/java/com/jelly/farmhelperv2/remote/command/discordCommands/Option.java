package com.jelly.farmhelperv2.remote.command.discordCommands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public class Option {
    public OptionType type;
    public String name;
    public String description;
    public boolean required;
    public boolean autocomplete;

    public Option(OptionType type, String name, String description, boolean required, boolean autocomplete) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.required = required;
        this.autocomplete = autocomplete;
    }
}
