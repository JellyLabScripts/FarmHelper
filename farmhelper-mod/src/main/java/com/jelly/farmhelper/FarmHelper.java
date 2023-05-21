package com.jelly.farmhelper;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.features.*;
import com.jelly.farmhelper.gui.MenuGUI;
import com.jelly.farmhelper.gui.Render;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.remote.RemoteControlHandler;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper {
    public static final String MODID = "farmhelper";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "4.3.5-PRE";
    public static String analyticUrl;

    // the actual mod version from gradle properties, should match with VERSION
    public static String MODVERSION = "-1";
    public static String BOTVERSION = "-1";
    public static int tickCount = 0;
    public static boolean openedGUI = false;
    public static TickTask ticktask;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static GameState gameState;



    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        setVersions();
        ConfigHandler.init();
        KeyBindUtils.setup();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Render());
        MinecraftForge.EVENT_BUS.register(new MenuGUI());
        MinecraftForge.EVENT_BUS.register(new MacroHandler());
        MinecraftForge.EVENT_BUS.register(new Failsafe());
        MinecraftForge.EVENT_BUS.register(new Antistuck());
        MinecraftForge.EVENT_BUS.register(new Autosell());
        MinecraftForge.EVENT_BUS.register(new Scheduler());
        MinecraftForge.EVENT_BUS.register(new AutoReconnect());
        MinecraftForge.EVENT_BUS.register(new AutoCookie());
        MinecraftForge.EVENT_BUS.register(new AutoPot());
        MinecraftForge.EVENT_BUS.register(new BanwaveChecker());
        MinecraftForge.EVENT_BUS.register(new RemoteControlHandler());
        MinecraftForge.EVENT_BUS.register(new ProfitCalculator());
        MinecraftForge.EVENT_BUS.register(new Utils());
        MinecraftForge.EVENT_BUS.register(new VisitorsMacro());
        gameState = new GameState();
        try {
            analyticUrl = (String) APIHelper.readJsonFromUrl("NONE","User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                    .get("url");
        } catch (Exception ignored) {}

    }


    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[0].isPressed()) {
            openedGUI = true;
            mc.displayGuiScreen(new MenuGUI());
        }
        if (KeyBindUtils.customKeyBinds[2].isPressed()) {
            BlockPos pos = BlockUtils.getRelativeBlockPos(0, -1, 0);
            ConfigHandler.set("visitorsDeskPosX", pos.getX());
            ConfigHandler.set("visitorsDeskPosY", pos.getY());
            ConfigHandler.set("visitorsDeskPosZ", pos.getZ());
            LogUtils.scriptLog("Visitors Desk Position Set. BlockPos: " + pos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) throws IOException {
        if (ticktask != null ) {
            ticktask.onTick();
        }
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer != null && mc.theWorld != null)
            gameState.update();
        tickCount += 1;
        tickCount %= 20;
    }


    @SneakyThrows
    public static void setVersions() {
        Class<FarmHelper> clazz = FarmHelper.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = Objects.requireNonNull(clazz.getResource(className)).toString();
        if (!classPath.startsWith("jar")) return;

        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
                "/META-INF/MANIFEST.MF";
        Manifest manifest = new Manifest(new URL(manifestPath).openStream());
        Attributes attr = manifest.getMainAttributes();
        MODVERSION = attr.getValue("modversion");
        BOTVERSION = attr.getValue("botversion");
        Display.setTitle(FarmHelper.NAME + " " + MODVERSION + " | Bing Chilling");
    }


}
