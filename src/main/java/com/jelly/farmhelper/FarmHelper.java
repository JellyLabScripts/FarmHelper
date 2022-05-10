package com.jelly.farmhelper;

import com.jelly.farmhelper.config.FarmHelperConfig;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.gui.MenuGUI;
import com.jelly.farmhelper.gui.utils.BlurUtils;
import com.jelly.farmhelper.macros.CropMacro;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.*;


@Mod(modid = FarmHelper.MODID, name = FarmHelper.NAME, version = FarmHelper.VERSION)
public class FarmHelper {
    public static final String MODID = "farmhelper";
    public static final String NAME = "Farm Helper";
    public static final String VERSION = "3.0-beta5.0.2";
    public static int tickCount = 0;
    public static boolean openedGUI = false;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static GameState gameState = new GameState();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // ClientCommandHandler.instance.registerCommand((ICommand)new CheetoCommand());
        Display.setTitle(FarmHelper.NAME + " v" + FarmHelper.VERSION + " | GIGACHADS ONLY");
        FarmHelperConfig.init();
        KeyBindUtils.setup();
        MinecraftForge.EVENT_BUS.register(new FarmHelper());
        MinecraftForge.EVENT_BUS.register(new MenuGUI());
        MinecraftForge.EVENT_BUS.register(new MacroHandler());

        MinecraftForge.EVENT_BUS.register(new CropMacro());

        // MinecraftForge.EVENT_BUS.register(new Resync());
        MinecraftForge.EVENT_BUS.register(new Failsafe());
        MinecraftForge.EVENT_BUS.register(new Antistuck());
        BlurUtils.registerListener();
    }

    @SubscribeEvent
    public void onRender(final RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            GL11.glPushMatrix();
            final ScaledResolution resolution = new ScaledResolution(mc);
            final float translatedWidth = width * Math.max(Math.min(module2.isToggled() ? ((250.0f - module2.toggledTime.getTimePassed()) / 250.0f) : (module2.toggledTime.getTimePassed() / 250.0f), 1.0f), 0.0f);
            GL11.glTranslated(translatedWidth, 0.0, 0.0);
            BlurUtils.renderBlurredBackground(10.0f, resolution.getScaledWidth() - translatedWidth, resolution.getScaledHeight(), resolution.getScaledWidth() - 1 - width, y, width, height);
        }
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
        if (mc.thePlayer != null && mc.theWorld != null) gameState.update();
        tickCount += 1;
        tickCount %= 20;
    }
}