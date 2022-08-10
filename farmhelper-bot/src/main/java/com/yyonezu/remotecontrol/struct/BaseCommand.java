package com.yyonezu.remotecontrol.struct;

import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.EventWaiter;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.event.wait.WaiterAction;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

abstract public class BaseCommand {
    public static void register(Waiter waiter) {
        EventWaiter.register(waiter);
    }

    public static void unregister(Waiter waiter) {
        EventWaiter.unregister(waiter);
    }
    public static void unregister(WaiterAction action) {
        EventWaiter.unregister(action);
    }

    public static EmbedBuilder embed() {
       return new EmbedBuilder()
               .setColor(9372933)
               .setFooter("➤ FarmHelper Remote Control ↳ by yonezu#5542", "https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png");
    }
    @SneakyThrows
    public static void addImageToEmbedAndSend(String base64, MessageEmbed embed, CommandEvent ev) {
        EmbedBuilder em = new EmbedBuilder(embed);
        File file = new File(UUID.randomUUID() + ".png");
        file.createNewFile();
        byte[] data = Base64.getDecoder().decode(base64);
        try (OutputStream stream = Files.newOutputStream(file.toPath())) {
            stream.write(data);
        }
        em.setImage("attachment://filename.png");
        ev.getChannel().sendFile(file, "filename.png").setEmbeds(em.build()).queue();
        file.getAbsoluteFile().setWritable(true);
        while(!file.getAbsoluteFile().delete());
    }

    public static JsonObject getBaseMessage (CommandEvent ev, Instance instance) {
        String args = ev.getCommandDefinition().getLabels().get(0);
        String instanceuser = instance.getUser();
        String instanceid = instance.getId();
        JsonObject meta = new JsonObject();
        meta.addProperty("args", args);
        meta.addProperty("instanceuser", instanceuser);
        meta.addProperty("instanceid", instanceid);

        JsonObject j = new JsonObject();
        j.add("metadata", meta);
        return j;
    }

}