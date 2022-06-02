package com.jelly.farmhelper;

import com.jelly.farmhelper.config.FarmHelperConfig;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Autosell;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.gui.MenuGUI;
import com.jelly.farmhelper.gui.Render;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;


@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper {
    public static final String MODID = "farmhelper";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "4.0";
    public static int tickCount = 0;
    public static boolean openedGUI = false;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static GameState gameState;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Display.setTitle(FarmHelper.NAME + " v" + FarmHelper.VERSION + " | GIGACHADS ONLY");
        FarmHelperConfig.init();
        KeyBindUtils.setup();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Render());
        MinecraftForge.EVENT_BUS.register(new MenuGUI());
        MinecraftForge.EVENT_BUS.register(new MacroHandler());
        MinecraftForge.EVENT_BUS.register(new Failsafe());
        MinecraftForge.EVENT_BUS.register(new Antistuck());
        MinecraftForge.EVENT_BUS.register(new Autosell());
        gameState = new GameState();

    }
    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[0].isPressed()) {
            openedGUI = true;
            mc.displayGuiScreen(new MenuGUI());
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer != null && mc.theWorld != null)
            gameState.update();
        tickCount += 1;
        tickCount %= 20;
    }
}