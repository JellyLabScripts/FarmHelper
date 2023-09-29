package com.github.may2beez.farmhelperv2.handler;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.config.struct.Rewarp;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.feature.impl.Scheduler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.macro.impl.*;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.PlayerUtils;
import com.github.may2beez.farmhelperv2.util.RenderUtils;
import com.github.may2beez.farmhelperv2.feature.impl.UngrabMouse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private Optional<AbstractMacro> currentMacro = Optional.empty();
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
        S_SHAPE_MUSHROOM_MACRO(SShapeMushroomMacro.class),
        S_SHAPE_COCOA_BEAN_MACRO(SShapeCocoaBeanMacro.class),
        S_SHAPE_SUGARCANE_MACRO(SShapeSugarcaneMacro.class),
        S_SHAPE_VERTICAL_CROP_MACRO(SShapeVerticalCropMacro.class);

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

    public <T extends AbstractMacro> T getMacro(int type) {
        switch (type) {
            case 0: // crops
            case 1: // pumpkin/melon
            case 2: // melonkingdebil
            case 4: // cactus
                return Macros.S_SHAPE_VERTICAL_CROP_MACRO.getMacro();
            case 3: // sugarcane
                return Macros.S_SHAPE_SUGARCANE_MACRO.getMacro();
            case 5: // cocoa
                return Macros.S_SHAPE_COCOA_BEAN_MACRO.getMacro();
            case 6: // mushroom 45
            case 7: // mushroom 30
                return Macros.S_SHAPE_MUSHROOM_MACRO.getMacro();
            default:
                throw new IllegalArgumentException("Invalid crop type: " + type);
        }
    }

    public void setMacro(AbstractMacro macro) {
        setCurrentMacro(Optional.ofNullable(macro));
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

        setMacro(getMacro(FarmHelperConfig.macroType));
        if (!currentMacro.isPresent()) {
            LogUtils.sendError("Invalid macro type: " + FarmHelperConfig.macroType);
            return;
        }
        LogUtils.sendDebug("Selected macro: " + LogUtils.capitalize(currentMacro.get().getClass().getSimpleName()));
        mc.thePlayer.closeScreen();
        LogUtils.sendSuccess("Macro enabled!");
        LogUtils.webhookLog("Macro enabled!");
        setStartTime(System.currentTimeMillis());

        if (FarmHelperConfig.enableScheduler) {
            Scheduler.getInstance().start();
        }
        if (FarmHelperConfig.autoUngrabMouse) {
            UngrabMouse.getInstance().ungrabMouse();
        }

        setMacroing(true);
        enableCurrentMacro();
    }

    public void disableMacro() {
        setMacroing(false);
        LogUtils.sendSuccess("Macro disabled!");
        LogUtils.webhookLog("Macro disabled!");
        currentMacro.ifPresent(m -> {
            m.setSavedState(Optional.empty());
            m.getRotation().reset();
        });

        setCrop(null);

        FeatureManager.getInstance().disableAll();
        disableCurrentMacro();
    }

    public void pauseMacro() {
        currentMacro.ifPresent(cm -> {
            cm.saveState();
            cm.onDisable();
        });
        Scheduler.getInstance().pause();
    }

    public void resumeMacro() {
        currentMacro.ifPresent(AbstractMacro::onEnable);
        Scheduler.getInstance().resume();
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
            Multithreading.schedule(startCurrent, 300, TimeUnit.MILLISECONDS);
        }
    }

    public void disableCurrentMacro() {
        currentMacro.ifPresent(AbstractMacro::onDisable);
    }

    Runnable startCurrent = () -> {
        KeyBindUtils.stopMovement();
        if (isMacroing()) {
            currentMacro.ifPresent(AbstractMacro::onEnable);
        }
        startingUp = false;
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
        if (event.type == 0) {
            String message = event.message.getUnformattedText();
            if (message.equals("Your spawn location has been set!")) {
                PlayerUtils.setSpawnLocation();
            }
        }

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
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (FarmHelperConfig.highlightRewarp && FarmHelperConfig.rewarpList != null && GameStateHandler.getInstance().inGarden()) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 2000) / 2000, 1, 1);
            Color chromaLowerAlpha = new Color(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 120);

            for (Rewarp rewarp : FarmHelperConfig.rewarpList) {
                RenderUtils.drawBlockBox(new BlockPos(rewarp.x, rewarp.y, rewarp.z), chromaLowerAlpha);
            }
        }

        if (FarmHelperConfig.drawVisitorsDeskLocation && PlayerUtils.isDeskPosSet() && GameStateHandler.getInstance().inGarden()) {
            BlockPos deskPosTemp = new BlockPos(FarmHelperConfig.visitorsDeskPosX, FarmHelperConfig.visitorsDeskPosY, FarmHelperConfig.visitorsDeskPosZ);
            RenderUtils.drawBlockBox(deskPosTemp, new Color(Color.DARK_GRAY.getRed(), Color.DARK_GRAY.getGreen(), Color.DARK_GRAY.getBlue(), 80));
        }

        if (FarmHelperConfig.drawSpawnLocation && PlayerUtils.isSpawnLocationSet() && GameStateHandler.getInstance().inGarden()) {
            BlockPos spawnLocation = new BlockPos(PlayerUtils.getSpawnLocation());
            RenderUtils.drawBlockBox(spawnLocation, new Color(Color.orange.getRed(), Color.orange.getGreen(), Color.orange.getBlue(), 80));
        }

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
