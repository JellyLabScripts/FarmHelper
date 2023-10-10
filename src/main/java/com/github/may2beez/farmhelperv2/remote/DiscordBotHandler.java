package com.github.may2beez.farmhelperv2.remote;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.remote.command.discordCommands.DiscordCommand;
import com.github.may2beez.farmhelperv2.remote.command.discordCommands.impl.*;
import com.github.may2beez.farmhelperv2.remote.event.InteractionAutoComplete;
import com.github.may2beez.farmhelperv2.remote.event.InteractionCreate;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import lombok.Getter;
import lombok.Setter;
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
import java.util.Arrays;
import java.util.stream.Collectors;

// Big thanks to Cephetir for the idea of standalone JDA Dependency

public class DiscordBotHandler extends ListenerAdapter {
    private static DiscordBotHandler instance;

    public static DiscordBotHandler getInstance() {
        if (instance == null) {
            instance = new DiscordBotHandler();
        }
        return instance;
    }

    @Getter
    @Setter
    private String connectingState;

    @Getter
    @Setter
    private JDA jdaClient;
    private Thread tryConnectThread;

    @Getter
    public final ArrayList<DiscordCommand> commands = new ArrayList<>();

    @Getter
    @Setter
    public boolean finishedLoading = false;

    public DiscordBotHandler() {
        commands.addAll(Arrays.asList(
                new Help(),
                new Toggle(),
                new Reconnect(),
                new Screenshot(),
                new SetSpeed(),
                new Info()));
        LogUtils.sendDebug("Registered " + commands.size() + " commands");
        connect();
    }

    public void connect() {
        if (!FarmHelperConfig.enableRemoteControl) return;
        if (FarmHelperConfig.discordRemoteControlToken.isEmpty()) return;

        if (WebsocketHandler.getInstance().isServerAlive()) {
            LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
            finishedLoading = true;
            tryConnectThread = null;
            return;
        }
        try {
            jdaClient = JDABuilder.createLight(FarmHelperConfig.discordRemoteControlToken)
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jdaClient.awaitReady();
            jdaClient.updateCommands()
                    .addCommands(commands.stream().map(DiscordCommand::getSlashCommand).collect(Collectors.toList()))
                    .queue();
            jdaClient.addEventListener(new InteractionAutoComplete());
            jdaClient.addEventListener(new InteractionCreate());
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.SERVER);
            Notifications.INSTANCE.send("Farm Helper", "Connected to the Discord Bot!");
            LogUtils.sendSuccess("Connected to the Discord Bot!");
        } catch (InvalidTokenException e) {
            Notifications.INSTANCE.send("Farm Helper", "Failed to connect to the Discord Bot, check your token. Disabling remote control...");
            LogUtils.sendError("Failed to connect to the Discord Bot, check your token. Disabling remote control...");
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
        } catch (IllegalStateException e) {
            Notifications.INSTANCE.send("Farm Helper", "Discord Bot is already connected, connecting as a client...");
            LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
        } catch (InterruptedException e) {
            Notifications.INSTANCE.send("Farm Helper", "Unexpected error while connecting to the Discord Bot, disabling remote control...");
            LogUtils.sendError("Unexpected error while connecting to the Discord Bot, disabling remote control...");
            FarmHelperConfig.enableRemoteControl = false;
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
        }
        tryConnectThread = null;
        finishedLoading = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        if (!FarmHelperConfig.enableRemoteControl) {
            if (jdaClient != null) {
                jdaClient.shutdownNow();
                jdaClient = null;
            }
            if (WebsocketHandler.getInstance().getWebsocketServer() != null) {
                try {
                    WebsocketHandler.getInstance().getWebsocketServer().stop();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                WebsocketHandler.getInstance().setWebsocketServer(null);
            }
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
            finishedLoading = false;
            connectingState = "Connecting to Socket...";
            return;
        }

        if (!Loader.isModLoaded("farmhelperjdadependency")) {
            Notifications.INSTANCE.send("Farm Helper", "FarmHelperJDA is not loaded, disabling remote control..");
            LogUtils.sendDebug("FarmHelperJDA is not loaded, disabling remote control..");
            FarmHelperConfig.enableRemoteControl = false;
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        if (FarmHelperConfig.discordRemoteControlToken == null || FarmHelperConfig.discordRemoteControlToken.isEmpty()) {
            if (WebsocketHandler.getInstance().getWebsocketState() != WebsocketHandler.WebsocketState.CLIENT) {
                LogUtils.sendDebug("Connecting as a client...");
                WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
            }
            return;
        } else if (FarmHelperConfig.discordRemoteControlToken.startsWith("https")) {
            LogUtils.sendError("You have put a webhook link in the Discord Remote Control Token field! Read the guide before using this feature, dummy! Disabling remote control...");
            FarmHelperConfig.enableRemoteControl = false;
            return;
        }

        if (jdaClient == null && WebsocketHandler.getInstance().getWebsocketState() == WebsocketHandler.WebsocketState.NONE) {
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

        if (WebsocketHandler.getInstance().getWebsocketState() == WebsocketHandler.WebsocketState.CLIENT) {
            if (WebsocketHandler.getInstance().getWebsocketClient() != null && WebsocketHandler.getInstance().getWebsocketClient().isOpen()) {
                connectingState = "Connected to the Discord Bot as a client";
            } else {
                connectingState = "Connecting to the Discord Bot as a client...";
            }
            return;
        }

        if (jdaClient != null && jdaClient.getStatus() == JDA.Status.CONNECTED) {
            connectingState = "Connected to Discord Bot as a server";
        } else {
            connectingState = "Connecting to the Discord Bot as a server...";
        }
    }
}
