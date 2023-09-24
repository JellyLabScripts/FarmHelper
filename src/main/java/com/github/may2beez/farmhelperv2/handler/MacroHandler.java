package com.github.may2beez.farmhelperv2.handler;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.Failsafe;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.macro.impl.*;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.PlayerUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MacroHandler {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static MacroHandler instance;
    public static MacroHandler getInstance() {
        if (instance == null) {
            instance = new MacroHandler();
        }
        return instance;
    }
    @Getter
    @Setter
    private Optional<AbstractMacro> currentMacro;
    @Getter
    @Setter
    private boolean isMacroing = false;

    @Getter
    @Setter
    private long startTime = 0;

    @Getter
    @Setter
    private boolean startingUp = false;

    @Getter
    @Setter
    private FarmHelperConfig.CropEnum crop;

    @AllArgsConstructor
    public enum Macros {
        S_SHAPE_MACRO(SShapeCropMacroNew.class),
        MUSHROOM_MACRO(MushroomMacroNew.class),
        COCOA_BEAN_MACRO(CocoaBeanMacroNew.class),
        SUGARCANE_MACRO(SugarcaneMacroNew.class),
        VERTICAL_CROP_MACRO(VerticalCropMacroNew.class);

        private static final Map<Macros, AbstractMacro> macros = new HashMap<>();
        private final Class<? extends AbstractMacro> macroClass;

        public <T extends AbstractMacro> T getMacro() {
            if (!macros.containsKey(this)) {
                try {
                    macros.put(this, macroClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LogUtils.sendError("Failed to instantiate macro: " + this.name());
                    e.printStackTrace();
                }
            }
            return (T) macros.get(this);
        }
    }

    public MacroHandler() {
        setMacro(Macros.S_SHAPE_MACRO);
    }

    public void setMacro(Macros macro) {
        setCurrentMacro(Optional.ofNullable(macro.getMacro()));
    }

    public void toggleMacro() {
        if (Failsafe.getInstance().isEmergency()) {
            Failsafe.getInstance().resetFailSafe();
            this.disableMacro();
            LogUtils.sendWarning("Farm manually and DO NOT restart the macro too soon! The staff might still be spectating you for a while!");
            return;
        }
        if (isMacroing()) {
            this.disableMacro();
        } else {
            this.enableMacro();
        }
    }

    public void enableMacro() {
        if (!GameStateHandler.getInstance().inGarden()) {
            LogUtils.sendError("You must be in the garden to start the macro!");
            return;
        }
        setMacroing(true);

        setMacro(Macros.S_SHAPE_MACRO);
        mc.thePlayer.closeScreen();
        LogUtils.sendSuccess("Macro enabled!");
//        LogUtils.webhookLog("Macro enabled!");
        setStartTime(System.currentTimeMillis());

        enableCurrentMacro();
    }

    public void disableMacro() {
        setMacroing(false);
        LogUtils.sendSuccess("Macro disabled!");
//        LogUtils.webhookLog("Macro disabled!");
        currentMacro.ifPresent(m -> m.setSavedState(Optional.empty()));
        disableCurrentMacro();
    }

    public void pauseMacro() {
        currentMacro.ifPresent(cm -> {
            cm.saveState();
            cm.onDisable();
        });
    }

    public void resumeMacro() {
        currentMacro.ifPresent(cm -> {
            cm.getSavedState().ifPresent(cs -> cm.restoreState());
            cm.onEnable();
        });
    }

    public void enableCurrentMacro() {
        if (currentMacro.isPresent() && !currentMacro.get().isEnabled() && !startingUp) {
            mc.displayGuiScreen(null);
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
            startingUp = true;
            PlayerUtils.itemChangedByStaff = false;
            PlayerUtils.changeItemEveryClock.reset();
            KeyBindUtils.updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            new Thread(startCurrent).start();
        }
    }

    public void disableCurrentMacro() {
        currentMacro.ifPresent(AbstractMacro::onDisable);
    }

    Runnable startCurrent = () -> {
        try {
            Thread.sleep(300);
            KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
            if (isMacroing()) {
                currentMacro.ifPresent(AbstractMacro::onEnable);
            }
            startingUp = false;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    };

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || !isMacroing()) {
            return;
        }
        currentMacro.ifPresent(cm -> {
            if (!cm.isEnabled()) return;
            cm.onTick();
        });
    }

    @SubscribeEvent
    public void onChatMessageReceived(ClientChatReceivedEvent event) {
        if (!isMacroing()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onChatMessageReceived(event.message.getUnformattedText());
        });
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isMacroing()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onLastRender();
        });
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if (!isMacroing()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onOverlayRender(event);
        });
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!isMacroing()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onPacketReceived(event);
        });
    }
}
