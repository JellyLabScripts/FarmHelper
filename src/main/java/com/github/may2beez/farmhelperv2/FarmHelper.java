package com.github.may2beez.farmhelperv2;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.command.FarmHelperCommand;
import com.github.may2beez.farmhelperv2.command.RewarpCommand;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.impl.AntiStuck;
import com.github.may2beez.farmhelperv2.feature.impl.AutoCookie;
import com.github.may2beez.farmhelperv2.feature.impl.Scheduler;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.ReflectionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

@Mod(modid = "farmhelper", useMetadata = true)
public class FarmHelper {
    private final Minecraft mc = Minecraft.getMinecraft();
    public static FarmHelperConfig config;
    public static final String VERSION = "%%VERSION%%";
    public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    public static boolean sentInfoAboutShittyClient = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        initializeFields();
        initializeListeners();
        initializeCommands();
        FeatureManager.getInstance().fillFeatures();

        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.gammaSetting = 1000;
        Display.setTitle("Farm Helper [v" + VERSION + "] Bing Chilling");
    }

    @SubscribeEvent
    public void onTickSendInfoAboutShittyClient(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (sentInfoAboutShittyClient) return;

        if (ReflectionUtils.hasFeatherClient()) {
            Notifications.INSTANCE.send("FarmHelper", "You've got Feather Client! Be aware, you might have a lot of bugs because of this shitty client!", 15000);
            LogUtils.sendError("You've got Feather Client! Be aware, you might have a lot of bugs because of this shitty client!");
        }
        sentInfoAboutShittyClient = true;
    }

    private void initializeListeners() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(MacroHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(Scheduler.getInstance());
        MinecraftForge.EVENT_BUS.register(AutoCookie.getInstance());
        MinecraftForge.EVENT_BUS.register(AntiStuck.getInstance());
    }

    private void initializeFields() {
        config = new FarmHelperConfig();
    }

    private void initializeCommands() {
        ClientCommandHandler.instance.registerCommand(new RewarpCommand());
        ClientCommandHandler.instance.registerCommand(new FarmHelperCommand());
    }
}
