package com.jelly.farmhelper.remote;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.discordStruct.Command;
import com.jelly.farmhelper.remote.struct.BaseCommand;
import com.jelly.farmhelper.remote.event.InteractionAutoComplete;
import com.jelly.farmhelper.remote.event.InteractionCreate;
import com.jelly.farmhelper.remote.util.RemoteUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

// Big thanks to Cephetir for the idea of standalone JDA Dependency

public class DiscordBotHandler extends ListenerAdapter {
    public static JDA jdaClient;
    private Thread tryConnectThread;
    public static final ArrayList<Command> commands = new ArrayList<>();
    public static boolean finishedLoading = false;

    public DiscordBotHandler() {
        commands.addAll(RemoteUtils.registerCommands("com.jelly.farmhelper.remote.command.discordCommands", Command.class));
    }

    public void connect() {
        if (WebsocketHandler.isServerAlive()) {
            LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.CLIENT;
            finishedLoading = true;
            tryConnectThread = null;
            return;
        }
        try {
            jdaClient = JDABuilder.createLight(FarmHelper.config.discordRemoteControlToken)
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jdaClient.awaitReady();
            jdaClient.addEventListener(new InteractionAutoComplete());
            jdaClient.addEventListener(new InteractionCreate());
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.SERVER;
            Notifications.INSTANCE.send("Farm Helper", "Connected to the Discord Bot!");
            LogUtils.sendSuccess("Connected to the Discord Bot!");
        } catch (InvalidTokenException e) {
            Notifications.INSTANCE.send("Farm Helper", "Failed to connect to the Discord Bot, check your token. Disabling remote control...");
            LogUtils.sendError("Failed to connect to the Discord Bot, check your token. Disabling remote control...");
            FarmHelper.config.enableRemoteControl = false;
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.NONE;
        } catch (IllegalStateException e) {
            Notifications.INSTANCE.send("Farm Helper", "Discord Bot is already connected, connecting as a client...");
            System.out.println("Discord Bot is already connected, connecting as a client...");
            LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.CLIENT;
        } catch (InterruptedException e) {
            Notifications.INSTANCE.send("Farm Helper", "Unexpected error while connecting to the Discord Bot, disabling remote control...");
            LogUtils.sendError("Unexpected error while connecting to the Discord Bot, disabling remote control...");
            FarmHelper.config.enableRemoteControl = false;
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.NONE;
        }
        tryConnectThread = null;
        finishedLoading = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (Minecraft.getMinecraft().theWorld == null || Minecraft.getMinecraft().thePlayer == null) return;

        if (!FarmHelper.config.enableRemoteControl) {
            if (jdaClient != null) {
                jdaClient.shutdownNow();
                jdaClient = null;
            }
            WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.NONE;
            finishedLoading = false;
            connecting = "Connecting to Socket...";
            return;
        }

        if (!Loader.isModLoaded("farmhelperjdadependency")) {
            Notifications.INSTANCE.send("Farm Helper", "FarmHelperJDA is not loaded, disabling remote control..");
            System.out.println("FarmHelperJDA is not loaded, disabling remote control..");
            FarmHelper.config.enableRemoteControl = false;
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        if (FarmHelper.config.discordRemoteControlToken == null || FarmHelper.config.discordRemoteControlToken.isEmpty()) {
            if (WebsocketHandler.websocketState != WebsocketHandler.WebsocketState.CLIENT) {
                LogUtils.sendDebug("Connecting as a client...");
                WebsocketHandler.websocketState = WebsocketHandler.WebsocketState.CLIENT;
            }
            return;
        }

        if (jdaClient == null && WebsocketHandler.websocketState == WebsocketHandler.WebsocketState.NONE) {
            if (tryConnectThread == null) {
                tryConnectThread = new Thread(() -> {
                    try {
                        connect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                tryConnectThread.start();
            }
            return;
        }

        if (WebsocketHandler.websocketState == WebsocketHandler.WebsocketState.CLIENT) {
            if (WebsocketHandler.websocketClient != null && WebsocketHandler.websocketClient.isOpen()) {
                connecting = "Connected to the Discord Bot as a client";
            } else {
                connecting = "Connecting to the Discord Bot as a client...";
            }
            return;
        }

        if (jdaClient != null && jdaClient.getStatus() == JDA.Status.CONNECTED) {
            connecting = "Connected to Discord Bot as a server";
        } else {
            connecting = "Connecting to the Discord Bot as a server...";
        }
    }
}
