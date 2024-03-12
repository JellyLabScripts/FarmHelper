package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.event.SpawnObjectEvent;
import com.jelly.farmhelperv2.event.SpawnParticleEvent;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PestsDestroyer implements IFeature {
    private static PestsDestroyer instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<Tuple<String, String>> pests = Arrays.asList(
            new Tuple<>("Beetle", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQzNTQwNCwKICAicHJvZmlsZUlkIiA6ICJiMDU4MTFjYTdmNDk0YTM5OTZiNDU4ZjcwMmQ2MzJiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVeWlsIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM1NTkwZDUzMjZhNjVkNTViMmJjNjBjNWNkMTk0YzEzZDYxMjU2NThkM2Q0YzYwZWNlMWQ5YmVjZmFjZWE5M2MiCiAgICB9CiAgfQp9"),
            new Tuple<>("Cricket", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQ1NDYxMywKICAicHJvZmlsZUlkIiA6ICI5MThhMDI5NTU5ZGQ0Y2U2YjE2ZjdhNWQ1M2VmYjQxMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZWV2ZWxvcGVyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdiNTBkNmU2YmY5MDdmYTRlM2M0NGY0NjVjZDJjNGY3OTEyNGI1NzAzYTJkZjIyZmFjNjM3NmIxYjkxNzAzY2YiCiAgICB9CiAgfQp9"),
            new Tuple<>("Earthworm", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ1OTc0NywKICAicHJvZmlsZUlkIiA6ICIyNTBlNzc5MjZkNDM0ZDIyYWM2MTQ4N2EyY2M3YzAwNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJMdW5hMTIxMDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQwM2JhNDAyN2EzMzNkOGQyZmQzMmFiNTlkMWNmZGJhYTdkOTA4ZDgwZDIzODFkYjJhNjljYmU2NTQ1MGFkOCIKICAgIH0KICB9Cn0"),
            new Tuple<>("Fly", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTA2MzI4MSwKICAicHJvZmlsZUlkIiA6ICJjN2FmMWNkNjNiNTE0Y2YzOGY4NWQ2ZDUxNzhjYThlNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtb25zdGVyZ2FtZXIzMTUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5MGU3Nzc4MjZhNTI0NjEzNjhlMjZkMWIyZTE5YmZhMWJhNTgyZDYwMjQ4M2U1NDVmNDEyNGQwZjczMTg0MiIKICAgIH0KICB9Cn0"),
            new Tuple<>("Locust", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzU1NzA3NzAzNywKICAicHJvZmlsZUlkIiA6ICI0YjJlMGM1ODliZjU0ZTk1OWM1ZmJlMzg5MjQ1MzQzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTmVvdHJvbl8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGIyNGE0ODJhMzJkYjFlYTc4ZmI5ODA2MGIwYzJmYTRhMzczY2JkMThhNjhlZGRkZWI3NDE5NDU1YTU5Y2RhOSIKICAgIH0KICB9Cn0"),
            new Tuple<>("Mite", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQxOTcyNSwKICAicHJvZmlsZUlkIiA6ICJkYjYzNWE3MWI4N2U0MzQ5YThhYTgwOTMwOWFhODA3NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbmdlbHMxNzQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU2YmFmNjQzMWE5ZGFhMmNhNjA0ZDVhM2MyNmU5YTc2MWQ1OTUyZjA4MTcxNzRhNGZlMGI3NjQ2MTZlMjFmZiIKICAgIH0KICB9Cn0"),
            new Tuple<>("Mosquito", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTAyOTQ2MSwKICAicHJvZmlsZUlkIiA6ICI3NTE0NDQ4MTkxZTY0NTQ2OGM5NzM5YTZlMzk1N2JlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGFua3NNb2phbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJhOWZlMDViYzY2M2VmY2QxMmU1NmEzY2NjNWVjMDM1YmY1NzdiNzg3MDg1NDhiNmY0ZmZjZjFkMzBlY2NmZSIKICAgIH0KICB9Cn0"),
            new Tuple<>("Moth", "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQwNTk1NCwKICAicHJvZmlsZUlkIiA6ICJiMTUyZDlhZTE1MTM0OWNmOWM2NmI0Y2RjMTA5NTZjOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaXNxdW90aCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82NTQ4NWM0YjM0ZTViNTQ3MGJlOTRkZTEwMGU2MWY3ODE2ZjgxYmM1YTExZGZkZjBlY2NmODkwMTcyZGE1ZDBhIgogICAgfQogIH0KfQ"),
            new Tuple<>("Rat", "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
            new Tuple<>("Slug", "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ0MzA4MiwKICAicHJvZmlsZUlkIiA6ICJkOGNkMTNjZGRmNGU0Y2IzODJmYWZiYWIwOGIyNzQ4OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJaYWNoeVphY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E3OWQwZmQ2NzdiNTQ1MzA5NjExMTdlZjg0YWRjMjA2ZTJjYzUwNDVjMTM0NGQ2MWQ3NzZiZjhhYzJmZTFiYSIKICAgIH0KICB9Cn0")
    );
    private Optional<PlotUtils.Plot> closestPlot = Optional.empty();
    @Getter
    private final ArrayList<Entity> pestsLocations = new ArrayList<>();
    private final List<Entity> killedEntities = new ArrayList<>();
    @Getter
    private final Clock stuckClock = new Clock();
    @Getter
    private final Clock delayClock = new Clock();
    private final Clock delayBetweenBackTaps = new Clock();
    private final Clock delayBetweenFireworks = new Clock();
    @Getter
    private Optional<Entity> currentEntityTarget = Optional.empty();
    private boolean enabled = false;
    private boolean preparing = false;
    @Setter
    @Getter
    public int cantReachPest = 0;
    @Getter
    @Setter
    private States state = States.IDLE;
    @Getter
    private EscapeState escapeState = EscapeState.NONE;
    private Optional<BlockPos> preTpBlockPos = Optional.empty();
    private Optional<Vec3> lastFireworkLocation = Optional.empty();
    private long lastFireworkTime = 0;
    private int getLocationTries = 0;
    private int flyPathfinderTries = 0;
    private RotationState rotationState = RotationState.NONE;
    private boolean needToUpdatePlots = false;
    private final HashMap<String, Float> vacuumRange = new HashMap<String, Float>() {{
        put("Skymart Vacuum", 5F);
        put("Turbo Vacuum", 7.5F);
        put("Hyper Vacuum", 10F);
        put("InfiniVacuum Hooverius", 15F);
        put("InfiniVacuum", 12.5F);
    }};
    private float currentVacuumRange = 5F;

    public static PestsDestroyer getInstance() {
        if (instance == null) {
            instance = new PestsDestroyer();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Pests Destroyer";
    }

    @Override
    public boolean isRunning() {
        return enabled || preparing;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) return;
        preparing = true;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(am -> am.setSavedState(Optional.empty()));
            KeyBindUtils.stopMovement();
        }
        escapeState = EscapeState.NONE;
        rotationState = RotationState.NONE;
        state = States.IDLE;
        killedEntities.clear();
        Multithreading.schedule(() -> {
            if (!preparing) return;
            enabled = true;
            preparing = false;
            LogUtils.sendWarning("[Pests Destroyer] Starting killing shitters!");
            LogUtils.webhookLog("[Pests Destroyer]\\nStarting killing shitters!");
        }, MacroHandler.getInstance().isMacroToggled() ? (800 + (long) (Math.random() * 500)) : 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (enabled || preparing) {
            LogUtils.sendWarning("[Pests Destroyer] Stopping!");
            LogUtils.webhookLog("[Pests Destroyer]\\nStopping!");
            if (GameStateHandler.getInstance().getPestsCount() == 0) {
                pestsLocations.clear();
            }
        }
        PlayerUtils.closeScreen();
        currentEntityTarget = Optional.empty();
        lastFireworkLocation = Optional.empty();
        preTpBlockPos = Optional.empty();
        delayBetweenBackTaps.reset();
        delayBetweenFireworks.reset();
        delayClock.reset();
        stuckClock.reset();
        preparing = false;
        enabled = false;
        lastFireworkTime = 0;
        getLocationTries = 0;
        flyPathfinderTries = 0;
        FlyPathfinder.getInstance().stuckCounterWithMotion = 0;
        FlyPathfinder.getInstance().stuckCounterWithoutMotion = 0;
        state = States.IDLE;
        FlyPathFinderExecutor.getInstance().stop();
        KeyBindUtils.stopMovement();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        stop();
        if (!FarmHelperConfig.pestsDestroyerAfkInfiniteMode) return;
        FarmHelperConfig.pestsDestroyerAfkInfiniteMode = false;
        LogUtils.sendWarning("[Pests Destroyer] AFK Mode has been disabled");
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enablePestsDestroyer;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return escapeState == EscapeState.NONE &&
                state != States.TELEPORT_TO_PLOT &&
                state != States.WAIT_FOR_TP &&
                state != States.CHECKING_PLOT &&
                state != States.CHECKING_SPAWN &&
                state != States.GET_LOCATION;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!FarmHelperConfig.pestsDestroyerAfkInfiniteMode) return;
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            LogUtils.sendWarning("[Pests Destroyer] Disabling Pests Destroyer AFK Infinite Mode!");
            stop();
            FarmHelperConfig.pestsDestroyerAfkInfiniteMode = false;
        }
    }

    @SubscribeEvent
    public void onTickAFKMode(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!FarmHelperConfig.pestsDestroyerAfkInfiniteMode) return;
        if (MacroHandler.getInstance().isMacroToggled()) return;
        if (!isToggled()) return;
        if (isRunning()) return;

        if (canEnableMacro(true)) {
            start();
        }
    }

    public boolean canEnableMacro() {
        return canEnableMacro(false);
    }

    public boolean canEnableMacro(boolean manually) {
        if (!isToggled()) return false;
        if (isRunning()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (!MacroHandler.getInstance().isMacroToggled() && !manually) return false;
        if (enabled || preparing) return false;
        if (GameStateHandler.getInstance().getPestsCount() < FarmHelperConfig.startKillingPestsAt || (manually && GameStateHandler.getInstance().getPestsCount() == 0))
            return false;
        if (!manually && FarmHelperConfig.pausePestsDestroyerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendError("[Pests Destroyer] Pests Destroyer won't activate during Jacob's Contest!");
            return false;
        }
        if (InventoryUtils.hasItemInHotbar("SkyMart Vacuum")) {
            LogUtils.sendError("[Pests Destroyer] You need higher tier (at least second) of Vacuum to use Pests Destroyer!");
            if (FarmHelperConfig.pestsDestroyerAfkInfiniteMode) {
                LogUtils.sendWarning("[Pests Destroyer] Disabling Pests Destroyer AFK Infinite Mode!");
                FarmHelperConfig.pestsDestroyerAfkInfiniteMode = false;
            }
            return false;
        }
        if (!mc.thePlayer.capabilities.allowFlying) {
            LogUtils.sendError("[Pests Destroyer] You need to be able to fly!");
            if (FarmHelperConfig.pestsDestroyerAfkInfiniteMode) {
                LogUtils.sendWarning("[Pests Destroyer] Disabling Pests Destroyer AFK Infinite Mode!");
                FarmHelperConfig.pestsDestroyerAfkInfiniteMode = false;
            }
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public void onTickExecute(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen == null && checkedThisGui) {
            checkedThisGui = false;
        }
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden() && escapeState == EscapeState.NONE) return;
        if (!enabled) return;


        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendWarning("[Pests Destroyer] The player is struggling killing pest for 5 minutes, will do a quick Garden -> Hub -> Garden teleport.");
            LogUtils.sendFailsafeMessage("[Pests Destroyer] Couldn't kill pest for 5 minutes, will do a quick Garden -> Hub -> Garden teleport.", true);
            escapeState = EscapeState.GO_TO_HUB;
            KeyBindUtils.stopMovement();
            delayClock.schedule(300);
            stuckClock.reset();
            return;
        }

        if (delayClock.isScheduled() && !delayClock.passed()) return;

        LogUtils.sendDebug("[Pests Destroyer] State: " + state);

        if (escapeState != EscapeState.NONE) {
            if (stuckClock.isScheduled()) {
                stuckClock.reset();
            }
            if (RotationHandler.getInstance().isRotating()) {
                RotationHandler.getInstance().reset();
            }
            KeyBindUtils.stopMovement();
            switch (escapeState) {
                case GO_TO_HUB:
                    if (isInventoryOpen()) break;
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.GO_TO_GARDEN;
                        delayClock.schedule((long) (5_500 + Math.random() * 3_500));
                        break;
                    }
                    if (GameStateHandler.getInstance().inGarden()) {
                        mc.thePlayer.sendChatMessage("/hub");
                        escapeState = EscapeState.GO_TO_GARDEN;
                        delayClock.schedule((long) (1_800 + Math.random() * 1_000));
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                        escapeState = EscapeState.GO_TO_HUB;
                        mc.thePlayer.sendChatMessage("/skyblock");
                        delayClock.schedule((long) (5_000 + Math.random() * 1_500));
                        break;
                    }
                    break;
                case GO_TO_GARDEN:
                    if (isInventoryOpen()) break;
                    if (GameStateHandler.getInstance().inGarden()) {
                        escapeState = EscapeState.GO_TO_HUB;
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.RESUME_MACRO;
                        MacroHandler.getInstance().triggerWarpGarden(true, false);
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                        escapeState = EscapeState.GO_TO_HUB;
                        mc.thePlayer.sendChatMessage("/skyblock");
                        delayClock.schedule((long) (5_000 + Math.random() * 1_500));
                        break;
                    }
                    break;
                case RESUME_MACRO:
                    if (isInventoryOpen()) break;
                    if (GameStateHandler.getInstance().inGarden()) {
                        escapeState = EscapeState.NONE;
                        state = States.IDLE;
                        cantReachPest = 0;
                        delayClock.schedule((long) (1_000 + Math.random() * 500));
                        LogUtils.sendDebug("[Pests Destroyer] Came back to Garden!");
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.RESUME_MACRO;
                        MacroHandler.getInstance().triggerWarpGarden(true, false);
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                        escapeState = EscapeState.GO_TO_HUB;
                        mc.thePlayer.sendChatMessage("/skyblock");
                        delayClock.schedule((long) (5_000 + Math.random() * 1_500));
                        break;
                    }
                    break;
            }
            return;
        }

        switch (state) {
            case IDLE:
                ItemStack currentItem = mc.thePlayer.getHeldItem();
                if (GameStateHandler.getInstance().getPestsCount() == 0) {
                    if (isInventoryOpen()) return;
                    finishMacro();
                    return;
                }
                if (getVacuum(currentItem)) return;
                if (needToUpdatePlots || PlotUtils.needToUpdatePlots()) {
                    state = States.OPEN_DESK;
                } else {
                    state = States.TELEPORT_TO_PLOT;
                }
                delayClock.schedule((long) (200 + Math.random() * 200));
                break;
            case OPEN_DESK:
                if (isInventoryOpen()) break;
                mc.thePlayer.sendChatMessage("/desk");
                state = States.OPEN_PLOTS;
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 500 + Math.random() * 500));
                break;
            case OPEN_PLOTS:
                String chestName = InventoryUtils.getInventoryName();
                if (mc.currentScreen == null) {
                    state = States.OPEN_DESK;
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    break;
                }
                if (chestName != null && !chestName.equals("Desk")) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    state = States.OPEN_DESK;
                    break;
                }
                if (chestName != null) {
                    Slot configurePlots = InventoryUtils.getSlotOfItemInContainer("Configure Plots");
                    if (configurePlots == null) {
                        return;
                    }
                    state = States.WAIT_FOR_INFO;
                    InventoryUtils.clickContainerSlot(configurePlots.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 500 + Math.random() * 500));
                    break;
                }
                break;
            case WAIT_FOR_INFO:
                String chestName2 = InventoryUtils.getInventoryName();
                if (chestName2 != null && !chestName2.equals("Configure Plots")) {
                    LogUtils.sendDebug("Wrong GUI: " + chestName2);
                    PlayerUtils.closeScreen();
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 500 + Math.random() * 500));
                    state = States.OPEN_DESK;
                    break;
                }
                break;
            case TELEPORT_TO_PLOT:
                PlotUtils.Plot plot = getClosestPlot();
                if (plot == null) {
                    state = States.GO_BACK;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    return;
                }
                if (GameStateHandler.getInstance().getCurrentPlot() == plot.number && BlockUtils.canFlyHigher(8)) {
                    state = States.GET_LOCATION;
                    break;
                }
                String plotNumber = plot.name;
                preTpBlockPos = Optional.of(mc.thePlayer.getPosition());
                mc.thePlayer.sendChatMessage("/tptoplot " + plotNumber);
                state = States.WAIT_FOR_TP;
                delayClock.schedule((long) (900 + Math.random() * 500));
                break;
            case WAIT_FOR_TP:
                if (!preTpBlockPos.isPresent()) {
                    state = States.IDLE;
                    break;
                }
                if (!mc.thePlayer.getPosition().equals(preTpBlockPos.get())) {
                    Block northWest = BlockUtils.getRelativeBlock(-0.3f, 0, -0.3f);
                    Block northWestTop = BlockUtils.getRelativeBlock(-0.3f, 1, -0.3f);

                    Block northEast = BlockUtils.getRelativeBlock(0.3f, 0, -0.3f);
                    Block northEastTop = BlockUtils.getRelativeBlock(0.3f, 1, -0.3f);

                    Block southWest = BlockUtils.getRelativeBlock(-0.3f, 0, 0.3f);
                    Block southWestTop = BlockUtils.getRelativeBlock(-0.3f, 1, 0.3f);

                    Block southEast = BlockUtils.getRelativeBlock(0.3f, 0, 0.3f);
                    Block southEastTop = BlockUtils.getRelativeBlock(0.3f, 1, 0.3f);

                    if (!northWest.isCollidable() && !northWestTop.isCollidable()
                            && !northEast.isCollidable() && !northEastTop.isCollidable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
                    } else if (!northWest.isCollidable() && !northWestTop.isCollidable()
                            && !southWest.isCollidable() && !southWestTop.isCollidable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                    } else if (!northEast.isCollidable() && !northEastTop.isCollidable()
                            && !southEast.isCollidable() && !southEastTop.isCollidable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
                    } else if (!southWest.isCollidable() && !southWestTop.isCollidable()
                            && !southEast.isCollidable() && !southEastTop.isCollidable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                    } else if (BlockUtils.hasCollision(BlockUtils.getRelativeBlockPos(0, 0, 0)) && !BlockUtils.hasCollision(BlockUtils.getRelativeBlockPos(0, 1, 0))) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        } else {
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                            Multithreading.schedule(KeyBindUtils::stopMovement, (long) (80 + Math.random() * 50), TimeUnit.MILLISECONDS);
                        }
                    }
                    state = States.CHECKING_PLOT;
                    delayClock.schedule((long) (200 + Math.random() * 200));
                }
                break;
            case CHECKING_PLOT:
                if (isInventoryOpenDelayed()) break;
                KeyBindUtils.stopMovement();

                if (PlayerUtils.isPlayerSuffocating() || !BlockUtils.canFlyHigher(5)) {
                    LogUtils.sendWarning("[Pests Destroyer] The player is suffocating and/or it can't fly higher. Going back to spawnpoint.");
                    delayClock.schedule(1_000 + Math.random() * 500);
                    MacroHandler.getInstance().triggerWarpGarden(true, false);
                    state = States.CHECKING_SPAWN;
                    return;
                }
                state = States.GET_LOCATION;
                break;
            case CHECKING_SPAWN:
                if (MacroHandler.getInstance().isTeleporting()) return;

                if (!BlockUtils.canFlyHigher(4)) {
                    LogUtils.sendError("[Pests Destroyer] Your spawnpoint is obstructed! Make sure there is no block above your spawnpoint! Disabling Pests Destroyer!");
                    stop();
                    FarmHelperConfig.enablePestsDestroyer = false;
                    finishMacro();
                } else {
                    state = States.GET_CLOSEST_PLOT;
                    LogUtils.sendDebug("[Pests Destroyer] Spawnpoint is not obstructed");
                }
                break;
            case GET_CLOSEST_PLOT:
                if (isInventoryOpenDelayed()) break;

                PlotUtils.Plot closestPlot = getClosestPlot();

                if (closestPlot == null) {
                    LogUtils.sendError("[Pests Destroyer] Couldn't find closest plot!");
                    state = States.GET_LOCATION;
                    return;
                }

                this.closestPlot = Optional.of(closestPlot);
                state = States.FLY_TO_THE_CLOSEST_PLOT;
                delayClock.schedule((long) (500 + Math.random() * 500));
                break;
            case FLY_TO_THE_CLOSEST_PLOT:
                if (isInventoryOpenDelayed()) break;

                if (!this.closestPlot.isPresent()) {
                    LogUtils.sendError("[Pests Destroyer] Couldn't find closest plot!");
                    state = States.GET_LOCATION;
                    return;
                }

                BlockPos plotCenter = PlotUtils.getPlotCenter(this.closestPlot.get().number);

                if (getClosestPest() != null) {
                    KeyBindUtils.stopMovement();
                    state = States.FLY_TO_PEST;
                    break;
                }

                if ((mc.thePlayer.onGround || !flyDelay.passed()) && mc.thePlayer.capabilities.allowFlying && !mc.thePlayer.capabilities.isFlying) {
                    fly();
                    break;
                }

                if (mc.thePlayer.getDistance(plotCenter.getX(), mc.thePlayer.posY, plotCenter.getZ()) < 15) {
                    state = States.GET_LOCATION;
                    KeyBindUtils.stopMovement();
                    FlyPathFinderExecutor.getInstance().stop();
                    break;
                }

                if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                    FlyPathFinderExecutor.getInstance().findPath(new Vec3(plotCenter.getX(), 80, plotCenter.getZ()), true, true);
                }
                break;
            case GET_LOCATION:
                if (GameStateHandler.getInstance().getPestsCount() == 0) {
                    state = States.GO_BACK;
                    return;
                }
                if (isInventoryOpenDelayed()) break;
                ItemStack currentItem2 = mc.thePlayer.getHeldItem();
                if (getVacuum(currentItem2)) return;

                if (getClosestPest() != null) {
                    FlyPathFinderExecutor.getInstance().stop();
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    return;
                }

                if (!mc.thePlayer.capabilities.isFlying) {
                    fly();
                    break;
                }
                if (hasBlocksAround()) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                    break;
                } else {
                    if (mc.gameSettings.keyBindJump.isKeyDown()) {
                        KeyBindUtils.stopMovement();
                    }
                }

                lastFireworkLocation = Optional.empty();
                lastFireworkTime = System.currentTimeMillis();
                MovingObjectPosition mop = mc.objectMouseOver;
                if (RotationHandler.getInstance().isRotating()) break;
                float yaw = -1;
                Vec3 playerPos = mc.thePlayer.getPositionEyes(1);
                for (float i = 0; i < 360; i += 10) {
                    Vec3 testRotation = AngleUtils.getVectorForRotation(0, i);
                    Vec3 lookVector = playerPos.addVector(testRotation.xCoord * 5, testRotation.yCoord * 5, testRotation.zCoord * 5);
                    MovingObjectPosition mop2 = mc.theWorld.rayTraceBlocks(playerPos, lookVector, false, true, false);
                    if (mop2 == null || mop2.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                        yaw = i;
                        break;
                    }
                }
                Rotation upRotation = new Rotation((float) (yaw + (Math.random() * 5 - 2.5)), (float) (-20 + (Math.random() * 6 - 4)));
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && RotationHandler.getInstance().shouldRotate(upRotation, 10)) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            upRotation,
                            FarmHelperConfig.getRandomRotationTime(),
                            null
                    ).easeOutBack(true));
                    delayClock.schedule(300);
                    break;
                }
                state = States.WAIT_FOR_LOCATION;
                if (getLocationTries > 4) {
                    LogUtils.sendWarning("[Pests Destroyer] Couldn't find any firework location. Trying to fix it by sending /pq low.");
                    mc.thePlayer.sendChatMessage("/pq low");
                    getLocationTries = 0;
                }
                KeyBindUtils.leftClick();
                getLocationTries++;
                if (!stuckClock.isScheduled())
                    stuckClock.schedule(1_000 * 60 * FarmHelperConfig.pestsKillerStuckTime);
                delayClock.schedule(300);
                break;
            case WAIT_FOR_LOCATION:
                if (isInventoryOpenDelayed()) break;

                if (RotationHandler.getInstance().isRotating()) return;

                if (getClosestPest() != null) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (lastFireworkLocation.isPresent()) {
                    if (lastFireworkTime + 250 < System.currentTimeMillis()) {
                        RotationHandler.getInstance().reset();
                        state = States.FLY_TO_PEST;
                        delayBetweenFireworks.schedule(3_000);
                        delayClock.schedule(300);
                    }
                }
                if (System.currentTimeMillis() - lastFireworkTime > 6_000) {
                    state = States.GET_LOCATION;
                    break;
                }
                break;
            case FLY_TO_PEST:
                if (isInventoryOpenDelayed()) break;
                getLocationTries = 0;
                if (GameStateHandler.getInstance().getPestsCount() == 0) {
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }

                Entity closestPest = getClosestPest();

                if (closestPest == null) {
                    if (!lastFireworkLocation.isPresent()) {
                        LogUtils.sendDebug("[Pests Destroyer] No firework location found. Looking for a firework.");
                        state = States.GET_LOCATION;
                        break;
                    }

                    if (System.currentTimeMillis() - lastFireworkTime > 1_500) {
                        state = States.GET_LOCATION;
                        break;
                    }

                    if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                        Vec3 firework = new Vec3(lastFireworkLocation.get().xCoord, Math.min(mc.thePlayer.posY + 1, 80), lastFireworkLocation.get().zCoord);
                        state = States.GET_LOCATION;
                        if (mc.thePlayer.getDistance(firework.xCoord, firework.yCoord, firework.zCoord) < 2) {
                            int y = 130;
                            Block block = mc.theWorld.getBlockState(new BlockPos(firework.xCoord, y, firework.zCoord)).getBlock();
                            while (y > 90 && block.equals(Blocks.air)) {
                                y--;
                            }
                            y += 3;
                            FlyPathFinderExecutor.getInstance().findPath(new Vec3(firework.xCoord, y, firework.zCoord), true, true);
                            LogUtils.sendWarning("[Pests Destroyer] Firework is too close to player. Flying to x: " + firework.xCoord + " y: " + y + " z: " + firework.zCoord);
                            break;
                        }
                        FlyPathFinderExecutor.getInstance().findPath(firework, true, true);
                    }
                    break;
                }
                if (closestPest instanceof EntityArmorStand) {
                    Entity realEntity = PlayerUtils.getEntityCuttingOtherEntity(closestPest, (e) -> e instanceof EntityBat || e instanceof EntitySilverfish);
                    if (realEntity != null) {
                        closestPest = realEntity;
                    }
                }
                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    FlyPathFinderExecutor.getInstance().stop();
                }


                currentEntityTarget = Optional.of(closestPest);

                state = States.KILL_PEST;
                cantReachPest = 0;
                KeyBindUtils.stopMovement();
                if (!stuckClock.isScheduled())
                    stuckClock.schedule(1_000 * 60 * FarmHelperConfig.pestsKillerStuckTime);
                delayClock.schedule(300);
                break;
            case KILL_PEST:
                ItemStack currentItem3 = mc.thePlayer.getHeldItem();
                if (getVacuum(currentItem3)) return;
                if (isInventoryOpenDelayed()) break;
                if (mc.thePlayer.posY < 67 && FlyPathFinderExecutor.getInstance().isRunning()) {
                    FlyPathFinderExecutor.getInstance().stop();
                    RotationHandler.getInstance().reset();
                    state = States.GET_LOCATION;
                    return;
                }
                if (!currentEntityTarget.isPresent()) {
                    FlyPathFinderExecutor.getInstance().stop();
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }
                Entity entity = currentEntityTarget.get();
                if (entity.isDead || killedEntities.contains(entity) || !mc.theWorld.loadedEntityList.contains(entity)) {
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    FlyPathFinderExecutor.getInstance().stop();
                    return;
                }

                double distance = mc.thePlayer.getDistance(entity.posX, entity.posY + entity.getEyeHeight() + 1, entity.posZ);
                double distanceXZ = mc.thePlayer.getDistance(entity.posX, mc.thePlayer.posY, entity.posZ);

                float vacuumMinRange = currentVacuumRange - 2;
                if (FarmHelperConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking > 0
                        && (distanceXZ < 1.5 || distance <= Math.max(vacuumMinRange - 2, 10))
                        && Math.abs(mc.thePlayer.motionX) < 0.1
                        && Math.abs(mc.thePlayer.motionZ) < 0.1
                        && !canEntityBeSeenIgnoreNonCollidable(entity)) {
                    cantReachPest++;
                }

                if (cantReachPest >= FarmHelperConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking) {
                    LogUtils.sendWarning("[Pests Destroyer] Can't reach the pest, will do a quick Garden -> Hub -> Garden teleport.");
                    escapeState = EscapeState.GO_TO_HUB;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300);
                    return;
                }


                if (distance < vacuumMinRange) {
                    float targetVelocity = (float) (Math.abs(entity.motionX) + Math.abs(entity.motionZ));
                    if (distanceXZ < vacuumMinRange && targetVelocity < 0.15) {
                        if (FlyPathFinderExecutor.getInstance().isRunning()) {
                            if (distance < vacuumMinRange - 1) {
                                FlyPathFinderExecutor.getInstance().stop();
                                LogUtils.sendDebug("[Pests Destroyer] Stopping pathfinder because the pest is close enough");
                            }
                        } else {
                            float playerVelocity = (float) (Math.abs(mc.thePlayer.motionX) + Math.abs(mc.thePlayer.motionZ));
                            if (playerVelocity > 0.15)
                                KeyBindUtils.onTick(mc.gameSettings.keyBindBack);
                        }
                    }
                    if (rotationState != RotationState.CLOSE) {
                        rotationState = RotationState.CLOSE;
                        RotationHandler.getInstance().reset();
                    }
                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity).additionalY(-0.5f),
                                (long) (400 + Math.random() * 200),
                                null
                        ).followTarget(true));
                    }
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, true);
                } else {
                    if (rotationState != RotationState.FAR) {
                        FlyPathFinderExecutor.getInstance().stop();
                        rotationState = RotationState.FAR;
                        RotationHandler.getInstance().reset();
                    }
                    if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                        LogUtils.sendDebug("Should pathfind to: " + entity.posX + " " + (entity.posY + 2.75) + " " + entity.posZ);
                        FlyPathFinderExecutor.getInstance().findPath(entity, true, true, 2.75f, true);
                    }
                    if (FlyPathFinderExecutor.getInstance().getState() == FlyPathFinderExecutor.State.FAILED
                            && mc.thePlayer.motionX == 0 && mc.thePlayer.motionZ == 0) {
                        flyPathfinderTries++;
                    } else {
                        flyPathfinderTries = 0;
                    }
                    if (flyPathfinderTries > 5) {
                        LogUtils.sendWarning("[Pests Destroyer] Couldn't pathfind to the pest. Flying from the spawnpoint.");
                        flyPathfinderTries = 0;
                        KeyBindUtils.stopMovement();
                        delayClock.schedule(1_000 + Math.random() * 500);
                        MacroHandler.getInstance().triggerWarpGarden(true, false);
                        state = States.CHECKING_SPAWN;
                        return;
                    }
                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity).additionalY(-0.3f),
                                (long) (400 + Math.random() * 200),
                                null
                        ));
                    }
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, distance < 4.5);
                }
                break;
            case CHECK_ANOTHER_PEST:
                LogUtils.sendDebug(GameStateHandler.getInstance().getPestsCount() + " pest" + (GameStateHandler.getInstance().getPestsCount() == 1 ? "" : "s") + " left");
                if (GameStateHandler.getInstance().getPestsCount() == 0) {
                    state = States.GO_BACK;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    break;
                }
                Entity closestPest2 = getClosestPest();
                KeyBindUtils.stopMovement();
                if (closestPest2 != null) {
                    LogUtils.sendDebug("Found another pest");
                    state = States.KILL_PEST;
                    currentEntityTarget = Optional.of(closestPest2);
                    delayClock.schedule(50 + (long) (Math.random() * 100));
                } else {
                    PlotUtils.Plot plotOpt = getClosestPlot();
                    if (plotOpt != null && Math.sqrt(mc.thePlayer.getDistanceSqToCenter(PlotUtils.getPlotCenter(plotOpt.number))) < 60) {
                        LogUtils.sendDebug("Going manually to another plot");
                        state = States.GET_CLOSEST_PLOT;
                        delayClock.schedule(300 + (long) (Math.random() * 250));
                    } else {
                        LogUtils.sendDebug("Teleporting to plot");
                        state = States.TELEPORT_TO_PLOT;
                        delayClock.schedule(600 + (long) (Math.random() * 500));
                    }
                }
                break;
            case GO_BACK:
                finishMacro();
                break;
        }
    }

    @Nullable
    private Entity getClosestPest() {
        Entity closestPest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : pestsLocations) {
            if (killedEntities.contains(entity)) continue;
            Entity realEntity = PlayerUtils.getEntityCuttingOtherEntity(entity, (e) -> e instanceof EntityBat || e instanceof EntitySilverfish);
            if (realEntity != null && (killedEntities.contains(realEntity) || realEntity.isDead)) continue;
            if (mc.thePlayer.getDistanceToEntity(entity) < 5 && killedEntities.stream().anyMatch(ke -> ke.getDistanceToEntity(entity) < 1.5))
                continue;
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPest = entity;
            }
        }
        return closestPest;
    }

    private boolean getVacuum(ItemStack currentItem2) {
        if (currentItem2 == null || !currentItem2.getDisplayName().contains("Vacuum")) {
            int vacuum = InventoryUtils.getSlotIdOfItemInHotbar("Vacuum");
            if (vacuum == -1) {
                LogUtils.sendError("[Pests Destroyer] Failed to find vacuum in hotbar!");
                state = States.GO_BACK;
                FarmHelperConfig.enablePestsDestroyer = false;
                finishMacro();
                return true;
            }
            mc.thePlayer.inventory.currentItem = vacuum;
            ItemStack vacuumItem = mc.thePlayer.inventory.getStackInSlot(vacuum);
            for (Map.Entry<String, Float> vacuumRange : this.vacuumRange.entrySet()) {
                if (vacuumItem.getDisplayName().contains(vacuumRange.getKey())) {
                    currentVacuumRange = vacuumRange.getValue();
                    System.out.println("Current vacuum range: " + currentVacuumRange);
                    break;
                }
            }
            delayClock.schedule((long) (200 + Math.random() * 200));
            return true;
        }
        return false;
    }

    private boolean isInventoryOpenDelayed() {
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            delayClock.schedule(300 + (long) (Math.random() * 300));
            Multithreading.schedule(() -> {
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(100 + (long) (Math.random() * 200));
                }
            }, (long) (200 + Math.random() * 100), TimeUnit.MILLISECONDS);
            return true;
        }
        return false;
    }

    private boolean isInventoryOpen() {
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
            delayClock.schedule(500 + (long) (Math.random() * 500));
            return true;
        }
        return false;
    }


    private void finishMacro() {
        if (isInventoryOpen()) return;
        if (FlyPathFinderExecutor.getInstance().isPathing()) {
            FlyPathFinderExecutor.getInstance().stop();
        }
        if (MacroHandler.getInstance().isMacroToggled()) {
            stop();
            MacroHandler.getInstance().triggerWarpGarden(true, true, false);
            delayClock.schedule(2_000 + Math.random() * 500);
        } else {
            stop();
        }
    }

    private boolean hasBlocksAround() {
        Vec3 angle0 = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw);
        Vec3 angle90 = AngleUtils.getVectorForRotation(90, mc.thePlayer.rotationYaw);
        Vec3 angle180 = AngleUtils.getVectorForRotation(180, mc.thePlayer.rotationYaw);
        Vec3 angle270 = AngleUtils.getVectorForRotation(270, mc.thePlayer.rotationYaw);
        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);
        if (checkIfBlockExists(angle0, playerPos)) return true;
        if (checkIfBlockExists(angle90, playerPos)) return true;
        if (checkIfBlockExists(angle180, playerPos)) return true;
        return checkIfBlockExists(angle270, playerPos);
    }

    private boolean checkIfBlockExists(Vec3 angle0, Vec3 playerPos) {
        MovingObjectPosition mop0 = mc.theWorld.rayTraceBlocks(playerPos, playerPos.addVector(angle0.xCoord * 1.5, angle0.yCoord * 1.5, angle0.zCoord * 1.5), false, true, false);
        if (mop0 != null && mop0.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return true;
        }
        return false;
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0 || event.message == null) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText().trim());
        if (message.startsWith("You can't fast travel while in combat!") && enabled) {
            LogUtils.sendWarning("[Pests Destroyer] Can't fast travel while in combat, will try again to teleport.");
            Multithreading.schedule(this::finishMacro, 1_000 + (long) (Math.random() * 1_000), TimeUnit.MILLISECONDS);
            return;
        }
        if (message.toLowerCase().startsWith("there are not any pests on your garden right now") && enabled && state != States.GO_BACK) {
            LogUtils.sendDebug("[Pests Destroyer] There are not any Pests on your Garden right now! Keep farming!");
            state = States.GO_BACK;
            delayClock.schedule((long) (500 + Math.random() * 500));
            return;
        }
        if (message.contains("The worm seems to have burrowed")) {
            cantReachPest = 0;
            return;
        }
        if (message.contains("Couldn't find Plot")) {
            needToUpdatePlots = true;
            delayClock.schedule((long) (500 + Math.random() * 500));
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (FarmHelperConfig.streamerMode) return;

        List<Entity> pests = mc.theWorld.loadedEntityList.stream().filter(entity -> {
            if (entity.isDead) return false;
            if (entity.posY < 50) return false;
            if (entity instanceof EntityArmorStand) {
                if (killedEntities.contains(entity)) return false;
                ItemStack itemStack = ((EntityArmorStand) entity).getEquipmentInSlot(4);
                if (itemStack != null && itemStack.hasTagCompound()) {
                    String displayName = itemStack.getTagCompound().toString();
                    if (!displayName.contains("display:")) {
                        return this.pests.stream().anyMatch(pest -> displayName.contains(pest.getSecond()));
                    }
                }
            }
            return false;
        }).collect(Collectors.toList());

        pestsLocations.clear();
        pestsLocations.addAll(pests);

        for (Entity entity : pests) {
            AxisAlignedBB boundingBox = new AxisAlignedBB(entity.posX - 0.5, entity.posY + entity.getEyeHeight() - 0.35, entity.posZ - 0.5, entity.posX + 0.5, entity.posY + entity.getEyeHeight() + 0.65, entity.posZ + 0.5);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            if (FarmHelperConfig.pestsESP) {
                Color color = FarmHelperConfig.pestsESPColor.toJavaColor();
                if (canEntityBeSeenIgnoreNonCollidable(entity)) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(50, color.getAlpha()));
                }
                float distance = mc.thePlayer.getDistanceToEntity(entity);
                if (distance > 5) {
                    try {
                        ItemStack itemStack = ((EntityArmorStand) entity).getEquipmentInSlot(4);
                        String pestName = this.pests.stream().filter(pest -> itemStack.getTagCompound().toString().contains(pest.getSecond())).findFirst().get().getFirst();
                        RenderUtils.drawText(pestName, entity.posX, entity.posY + entity.getEyeHeight() + 0.65 + 0.5, entity.posZ, 1 + Math.min((distance / 20), 3));
                    } catch (Exception ignored) {
                    }
                }
                RenderUtils.drawBox(boundingBox, color);

            }
            if (FarmHelperConfig.pestsTracers) {
                RenderUtils.drawTracer(new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), FarmHelperConfig.pestsTracersColor.toJavaColor());
            }
        }

        if (!FarmHelperConfig.highlightPlotWithPests) return;

        for (int plotNumber : GameStateHandler.getInstance().getInfestedPlots()) {
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotChunksBasedOnNumber(plotNumber);
            if (chunks.isEmpty()) continue;
            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            float centerX = (float) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
            float centerZ = (float) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
            RenderUtils.drawText("Plot " + plotNumber, centerX, 80, centerZ, 1);
        }
    }

    private boolean canEntityBeSeenIgnoreNonCollidable(Entity entity) {
        Vec3 vec3 = new Vec3(entity.posX, entity.posY + entity.getEyeHeight() + 0.5, entity.posZ);
        Vec3 vec31 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vec31, vec3, false, true, false);
        return mop == null || mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.cactus) || !BlockUtils.hasCollision(mop.getBlockPos()));
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onEntityDeath(LivingDeathEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        Entity entity = event.entity;
        LogUtils.sendDebug("[Pests Destroyer] Entity died: " + entity.getName() + "(" + entity.getEntityId() + ")" + " at: " + entity.getPosition());
        killedEntities.add(entity);
        if (entity instanceof EntityArmorStand) {
            Entity realEntity = PlayerUtils.getEntityCuttingOtherEntity(entity, (e) -> e instanceof EntityBat || e instanceof EntitySilverfish);
            if (realEntity != null) {
                LogUtils.sendDebug("[Pests Destroyer] Found real entity: " + realEntity.getName() + "(" + realEntity.getEntityId() + ")" + " at: " + realEntity.getPosition());
                killedEntities.add(realEntity);
            }
        }
        if (entity instanceof EntityBat || entity instanceof EntitySilverfish) {
            Entity armorStand = PlayerUtils.getEntityCuttingOtherEntity(entity, (e) -> e instanceof EntityArmorStand);
            if (armorStand != null) {
                LogUtils.sendDebug("[Pests Destroyer] Found armor stand: " + armorStand.getName() + "(" + armorStand.getEntityId() + ")" + " at: " + armorStand.getPosition());
                killedEntities.add(armorStand);
            }
        }
        RotationHandler.getInstance().reset();
        FlyPathFinderExecutor.getInstance().stop();
        lastFireworkLocation = Optional.empty();
        lastFireworkTime = 0;
        currentEntityTarget.ifPresent(e -> {
            if (!e.equals(event.entity)) {
                return;
            }
            KeyBindUtils.stopMovement();
            currentEntityTarget = Optional.empty();
            stuckClock.reset();
            state = States.CHECK_ANOTHER_PEST;
            delayClock.schedule(150);
        });
        PlotUtils.Plot plot = PlotUtils.getPlotNumberBasedOnLocation(entity.getPosition());
        if (plot == null) {
            if (isRunning())
                LogUtils.sendDebug("[Pests Destroyer] Failed to get plot for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        LogUtils.sendDebug("[Pests Destroyer] Removed 1 pest from plot number: " + plot.number);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onFirework(SpawnParticleEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!enabled) return;
        if (state != States.WAIT_FOR_LOCATION) return;

        EnumParticleTypes type = event.getParticleTypes();
        if (type != EnumParticleTypes.REDSTONE) return;

        if (lastFireworkLocation.isPresent()) {
            if (lastFireworkLocation.get().distanceTo(event.getPos()) > 5) {
                return;
            }
        } else {
            if (mc.thePlayer.getPositionVector().distanceTo(event.getPos()) > 5) {
                return;
            }
        }

        lastFireworkTime = System.currentTimeMillis();
        lastFireworkLocation = Optional.of(event.getPos());
    }

    @SubscribeEvent
    public void onSpawnObject(SpawnObjectEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (event.type != 76) return;
        if (!enabled) return;
        if (state != States.WAIT_FOR_LOCATION) return;

        double distance = mc.thePlayer.getDistance(event.pos.xCoord, event.pos.yCoord, event.pos.zCoord);
        if (distance < 3) {
            int y = 130;
            Block block = mc.theWorld.getBlockState(new BlockPos(event.pos.xCoord, y, event.pos.zCoord)).getBlock();
            while (y > 90 && block.equals(Blocks.air)) {
                y--;
            }
            y += 3;
            FlyPathFinderExecutor.getInstance().findPath(new Vec3(event.pos.xCoord, y, event.pos.zCoord), true, true);
            LogUtils.sendWarning("[Pests Destroyer] Firework is too close to player. Flying to x: " + event.pos.xCoord + " y: " + y + " z: " + event.pos.zCoord);
            state = States.GET_LOCATION;
        }
    }

    private boolean checkedThisGui = false;

    @SubscribeEvent
    public void onGuiOpen(DrawScreenAfterEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (checkedThisGui) return;
        if (!(event.guiScreen instanceof GuiChest)) return;
        String guiName = InventoryUtils.getInventoryName();
        if (guiName == null) return;
        if (!delayClock.passed()) return;
        ContainerChest guiChest = (ContainerChest) ((GuiChest) event.guiScreen).inventorySlots;

        Slot lastSlot = guiChest.inventorySlots.get(42); // last plot in gui
        if (lastSlot == null || !lastSlot.getHasStack() || !lastSlot.getStack().getDisplayName().contains("Plot")) {
            return;
        }

        int plotCounter = 0;
        if (StringUtils.stripControlCodes(guiName).equals("Configure Plots")) {
            for (int i = 0; i < guiChest.inventorySlots.size(); i++) {
                Slot slot = guiChest.inventorySlots.get(i);
                if (slot == null || !slot.getHasStack()) continue;
                if (slot.getStack().getDisplayName().contains("Plot")) {
                    String displayName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                    try {
                        String plotName = displayName.replace("Plot - ", "").trim();
                        int plotNumber = PlotUtils.getPLOT_NUMBERS().get(plotCounter);
                        PlotUtils.setPlot(plotNumber, plotName);
                    } catch (Exception e) {
                        LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + displayName);
                    }
                    plotCounter++;
                } else if (StringUtils.stripControlCodes(slot.getStack().getDisplayName()).equals("The Barn"))
                    plotCounter++;
            }

        } else {
            return;
        }
        needToUpdatePlots = false;
        checkedThisGui = true;
        PlotUtils.savePlots();
        LogUtils.sendDebug("[Pests Destroyer] Updated plots");
        if (state == States.WAIT_FOR_INFO) {
            PlayerUtils.closeScreen();
            state = States.TELEPORTING_TO_PLOT;
            delayClock.schedule(300 + (long) (Math.random() * 300));
        }
    }

    private final Clock flyDelay = new Clock();

    private void fly() {
        if (mc.thePlayer.capabilities.isFlying) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
            return;
        }
        if (mc.thePlayer.motionY < -0.0784000015258789 || BlockUtils.getRelativeBlock(0, 0, 0).getMaterial().isLiquid())
            if (flyDelay.passed()) {
                if (!mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = true;
                    mc.thePlayer.sendPlayerAbilities();
                }
                flyDelay.reset();
            } else if (flyDelay.isScheduled()) {
                return;
            }
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            flyDelay.schedule(80 + (long) (Math.random() * 80));
        } else if (!mc.thePlayer.capabilities.isFlying && !flyDelay.isScheduled()) {
            flyDelay.schedule(80 + (long) (Math.random() * 80));
        }
    }

    private PlotUtils.Plot getClosestPlot() {
        List<Integer> infestedPlots = GameStateHandler.getInstance().getInfestedPlots();
        PlotUtils.Plot closestPlot = null;
        double closestDistance = Double.MAX_VALUE;
        for (int plot : infestedPlots) {
            double distance = mc.thePlayer.getDistanceSqToCenter(PlotUtils.getPlotCenter(plot));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlot = PlotUtils.getPlotBasedOnNumber(plot);
            }
        }
        if (closestPlot == null) {
            LogUtils.sendError("[Pests Destroyer] Failed to get closest plot");
            return null;
        }
        return closestPlot;
    }

    public enum States {
        IDLE,
        OPEN_DESK,
        OPEN_PLOTS,
        WAIT_FOR_INFO,
        TELEPORTING_TO_PLOT,
        TELEPORT_TO_PLOT,
        WAIT_FOR_TP,
        CHECKING_PLOT,
        CHECKING_SPAWN,
        GET_CLOSEST_PLOT,
        FLY_TO_THE_CLOSEST_PLOT,
        GET_LOCATION,
        WAIT_FOR_LOCATION,
        FLY_TO_PEST,
        KILL_PEST,
        CHECK_ANOTHER_PEST,
        GO_BACK
    }

    enum RotationState {
        NONE,
        CLOSE,
        MEDIUM,
        FAR
    }

    public enum EscapeState {
        NONE,
        GO_TO_HUB,
        GO_TO_GARDEN,
        RESUME_MACRO
    }
}
