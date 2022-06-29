package com.yyonezu.remotecontrol.struct;

import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.EventWaiter;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.event.wait.WaiterAction;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.simple.JSONObject;

import java.awt.*;
import java.lang.reflect.Array;
import java.nio.channels.AcceptPendingException;

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


    public static JSONObject getBaseMessage (CommandEvent ev, Instance instance) {
        String args = String.join(" ", ev.getCommandDefinition().getLabels());
        String instanceuser = instance.getUser();
        String instanceid = instance.getId();
        JSONObject meta = new JSONObject();
        meta.put("args", args);
        meta.put("instanceuser", instanceuser);
        meta.put("instanceid", instanceid);

        JSONObject j = new JSONObject();
        j.put("metadata", meta);
        return j;
    }

}