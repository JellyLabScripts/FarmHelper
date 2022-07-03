package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.yyonezu.remotecontrol.utils.Utils.withSuffix;
import static com.yyonezu.remotecontrol.websocket.WebSocketServer.minecraftInstances;

@CommandController(value={"totalprofit", "profit", "tp"}, category = "Misc")
public class TotalProfit extends BaseCommand {
    @Command(name = "Info command", usage = "{prefix}totalprofit", desc = "Get total profit totalled from all instances")
    public void infoCommand(CommandEvent ev) {
        int size = minecraftInstances.size();
        if (size == 0) {
            ev.reply(embed().setDescription("No instances running."));
            return;
        }
        String overrided =  UUID.randomUUID().toString();
        ArrayList<JsonObject> objects = new ArrayList<>();
        register(new Waiter(
                condition -> overrided.equals(condition.message.getAsJsonObject("metadata").get("instanceid").getAsString()),
                action -> {
                    objects.add(action.message);

                    if (objects.size() == size) {
                        AtomicLong totalProfit = new AtomicLong();
                        AtomicLong profithr = new AtomicLong();
                        AtomicInteger inactive_instances = new AtomicInteger();
                        objects.forEach(c -> {
                            if (c.get("profit").getAsLong() != 0 && c.get("profithr").getAsLong() != 0) {
                                totalProfit.addAndGet(c.get("profit").getAsLong());
                                profithr.addAndGet(c.get("profithr").getAsLong());
                            }
                        });

                        ev.reply(embed()
                                .setDescription("**This is the data of all " + (objects.size() - inactive_instances.get()) + " instances running**")
                                .addField("Total Profit ", "$" + withSuffix(totalProfit.get()), false)
                                .addField("Hourly Profit ", "$" + withSuffix(profithr.get() / (objects.size() - inactive_instances.get())), false));
                        unregister(action);
                    }
                },
                false,
                3L,
                TimeUnit.SECONDS,

                () -> ev.reply("Error")
        ));

        for (String s : minecraftInstances.values()) {
            Instance instance = new Instance(s);
            JsonObject obj = getBaseMessage(ev, instance);
            obj.getAsJsonObject("metadata").addProperty("instanceid", overrided);
            instance.getSession().getRemote().sendStringByFuture(obj.toString());
        }
    }
}
