package com.jelly.farmhelper.remote;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.reflections.Reflections;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class RemoteControlHandler {
    public static int tick = 1;
    public static Client client;
    public static Client analytic;
    static Minecraft mc = Minecraft.getMinecraft();
    public static final ArrayList<BaseCommand> commands = new ArrayList<>();

    public RemoteControlHandler() {
        registerCommands();
    }

    public void connect() {
        try {
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("password", FarmHelper.config.webSocketPassword);
            requestJson.addProperty("name", mc.getSession().getUsername());
            requestJson.addProperty("modversion", FarmHelper.MODVERSION);
            requestJson.addProperty("botversion", FarmHelper.BOTVERSION);
            String data = Base64.getEncoder().encodeToString(requestJson.toString().getBytes(StandardCharsets.UTF_8));

            client = new Client(new URI("ws://" + FarmHelper.config.webSocketIP.split(":")[0] + ":58637/farmhelperws"));
            client.addHeader("auth", data);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void registerCommands() {
        Set<Class<? extends BaseCommand>> classes = new Reflections("com.jelly.farmhelper.remote.command.commands").getSubTypesOf(BaseCommand.class);

        for (Class<?> clazz : classes) {
            try {
                commands.add((BaseCommand) clazz.newInstance());
                System.out.println("Registered command " + clazz.getName());
                System.out.println("Annotation: " + clazz.getAnnotation(Command.class).label());
            } catch (Exception e) {
                System.out.println("Failed to register command " + clazz.getName());
                e.printStackTrace();
            }
        }
    }

    public static Optional<BaseCommand> getCommand(String command) {
        return commands.stream().filter(clazz -> clazz.getClass().getAnnotation(Command.class).label().equalsIgnoreCase(command)).findFirst();
    }

    @SneakyThrows
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        if (!FarmHelper.config.enableRemoteControl) {
            if (client != null && client.isOpen()) {
                client.closeConnection(69, "connecting to remote control");
            }
            return;
        }

        if (client != null && client.isOpen()) {
            connecting = "Connected to Socket";
            return;
        }

        if (tick % 100 == 0) {
            connect();
            tick = 1;
        } else {
            tick++;
        }
    }
}
