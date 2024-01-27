package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
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
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    @Getter
    private final HashMap<Plot, Integer> pestsPlotMap = new HashMap<>();
    private Optional<Plot> closestPlot = Optional.empty();
    @Getter
    private final ArrayList<Entity> pestsLocations = new ArrayList<>();
    private Entity lastKilledEntity = null;
    @Getter
    private final Clock stuckClock = new Clock();
    private final Pattern pestPattern = Pattern.compile(".* (\\d+|A) .* (?:appeared|spawned) in Plot - (\\d+)!");
    @Getter
    private final Clock delayClock = new Clock();
    private final Clock delayBetweenBackTaps = new Clock();
    private final Clock delayBetweenFireworks = new Clock();
    private final Pattern pestPatternDeskGui = Pattern.compile(".*?(\\d+).*?");
    @Getter
    private Optional<Entity> currentEntityTarget = Optional.empty();
    private Optional<Entity> previousEntityTarget = Optional.empty();
    @Getter
    private int totalPests = 0;
    private boolean enabled = false;
    private boolean preparing = false;
    @Setter
    @Getter
    public int cantReachPest = 0;
    @Getter
    private States state = States.IDLE;
    @Getter
    private EscapeState escapeState = EscapeState.NONE;
    private Optional<BlockPos> preTpBlockPos = Optional.empty();
    private Optional<Vec3> lastFireworkLocation = Optional.empty();
    private long lastFireworkTime = 0;
    private int getLocationTries = 0;
    private RotationState rotationState = RotationState.NONE;

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
        lastKilledEntity = null;
        preparing = true;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(am -> am.setSavedState(Optional.empty()));
            KeyBindUtils.stopMovement();
        }
        escapeState = EscapeState.NONE;
        rotationState = RotationState.NONE;
        state = States.IDLE;
        pestsPlotMap.clear();
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
            if (totalPests == 0) {
                pestsLocations.clear();
                pestsPlotMap.clear();
            }
        }
        lastKilledEntity = null;
        PlayerUtils.closeScreen();
        previousEntityTarget = Optional.empty();
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
        finishTries = 0;
        FlyPathfinder.getInstance().stuckCounterWithMotion = 0;
        FlyPathfinder.getInstance().stuckCounterWithoutMotion = 0;
        state = States.IDLE;
        KeyBindUtils.stopMovement();
        FlyPathFinderExecutor.getInstance().stop();
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
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (!MacroHandler.getInstance().isMacroToggled() && !manually) return false;
        if (enabled || preparing) return false;
        if (totalPests < FarmHelperConfig.startKillingPestsAt || (manually && totalPests == 0)) return false;
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
                if (totalPests == 0) {
                    if (isInventoryOpen()) return;
                    finishMacro();
                    return;
                }
                if (getVacuum(currentItem)) return;
                state = States.OPEN_DESK;
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
                Optional<Plot> plotNumberOpt = pestsPlotMap.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).findFirst();
                if (!plotNumberOpt.isPresent()) {
                    state = States.GO_BACK;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    return;
                }
                String plotNumber = plotNumberOpt.get().name;
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
                    LogUtils.sendDebug("[Pests Destroyer] The player is suffocating and/or it can't fly higher. Going back to spawnpoint.");
                    delayClock.schedule(1_000 + Math.random() * 500);
                    MacroHandler.getInstance().triggerWarpGarden(true, false);
                    state = States.CHECKING_SPAWN;
                    return;
                }
                state = States.GET_LOCATION;
                break;
            case CHECKING_SPAWN:
                if (MacroHandler.getInstance().isTeleporting()) return;

                if (!BlockUtils.canFlyHigher(5)) {
                    LogUtils.sendError("[Pests Destroyer] Your spawnpoint is obstructed! Make sure there is no block above your spawnpoint!");
                    stop();
                } else {
                    state = States.GET_CLOSEST_PLOT;
                    LogUtils.sendDebug("[Pests Destroyer] Spawnpoint is not obstructed");
                }
                break;
            case GET_CLOSEST_PLOT:
                if (isInventoryOpenDelayed()) break;

                Plot closestPlot = null;

                for (Plot plot : pestsPlotMap.keySet()) {
                    BlockPos plotCenter = PlotUtils.getPlotCenter(plot.plotNumber);
                    if (closestPlot == null || mc.thePlayer.getDistanceSqToCenter(plotCenter) < mc.thePlayer.getDistanceSqToCenter(PlotUtils.getPlotCenter(closestPlot.plotNumber))) {
                        closestPlot = plot;
                    }
                }

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

                BlockPos plotCenter = PlotUtils.getPlotCenter(this.closestPlot.get().plotNumber);

                if (getClosestPest() != null) {
                    KeyBindUtils.stopMovement();
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (mc.thePlayer.getDistance(plotCenter.getX(), mc.thePlayer.posY, plotCenter.getZ()) < 15) {
                    state = States.GET_LOCATION;
                    KeyBindUtils.stopMovement();
                    FlyPathFinderExecutor.getInstance().stop();
                    break;
                }

                if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                    FlyPathFinderExecutor.getInstance().findPath(new Vec3(plotCenter.getX(), 80, plotCenter.getZ()), false, true);
                }
                break;
            case GET_LOCATION:
                if (totalPests == 0) {
                    state = States.GO_BACK;
                    return;
                }
                if (isInventoryOpenDelayed()) break;
                ItemStack currentItem2 = mc.thePlayer.getHeldItem();
                if (getVacuum(currentItem2)) return;

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                lastFireworkLocation = Optional.empty();
                lastFireworkTime = System.currentTimeMillis();
                MovingObjectPosition mop = mc.objectMouseOver;
                if (RotationHandler.getInstance().isRotating()) break;
                Rotation upRotation = new Rotation((float) (mc.thePlayer.rotationYaw + (Math.random() * 5 - 2.5)), (float) (-84 + (Math.random() * 6 - 4)));
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && RotationHandler.getInstance().shouldRotate(upRotation, 10)) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            upRotation,
                            FarmHelperConfig.getRandomRotationTime(),
                            null
                    ).easeOutBack(true));
                    delayClock.schedule(300);
                    break;
                } else if (mc.thePlayer.rotationYaw < -82 && BlockUtils.getRelativeBlock(0, 2, 0).equals(Blocks.air)) {
                    Rotation middleRotation = new Rotation((float) (mc.thePlayer.rotationYaw + (Math.random() * 5 - 2.5)), (float) (-10 + (Math.random() * 6 - 4)));
                    if (RotationHandler.getInstance().shouldRotate(middleRotation, 10)) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                middleRotation,
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ).easeOutBack(true));
                        delayClock.schedule(300);
                        break;
                    }
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

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (lastFireworkLocation.isPresent()) {
                    if (lastFireworkTime + 250 < System.currentTimeMillis()) {
                        if (state != States.WAIT_FOR_LOCATION) {
                            RotationHandler.getInstance().reset();
                            return;
                        }
                        state = States.FLY_TO_PEST;
                        RotationHandler.getInstance().reset();
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
                if (totalPests == 0) {
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }

                if (getClosestPest() == null) {
                    if (!lastFireworkLocation.isPresent()) {
                        LogUtils.sendDebug("[Pests Destroyer] No firework location found. Looking for a firework.");
                        state = States.GET_LOCATION;
                        break;
                    }

                    if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                        Vec3 firework = new Vec3(lastFireworkLocation.get().xCoord, mc.thePlayer.posY, lastFireworkLocation.get().zCoord);
                        Vec3 player = mc.thePlayer.getPositionVector();
                        Vec3 direction = firework.subtract(player).normalize();
                        Vec3 target = firework.addVector(direction.xCoord * 15, Math.min(direction.yCoord * 15, 10), direction.zCoord * 15);
                        FlyPathFinderExecutor.getInstance().findPath(target, false, true);
                    }
                    break;
                }

                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    FlyPathFinderExecutor.getInstance().stop();
                }

                Entity closestPest = getClosestPest();

                if (closestPest == null) {
                    state = States.GET_LOCATION;
                    return;
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
                if (isInventoryOpenDelayed()) break;
                if (!currentEntityTarget.isPresent()) {
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }
                Entity entity = currentEntityTarget.get();
                if (entity.isDead) {
                    RotationHandler.getInstance().reset();
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }

                double distance = mc.thePlayer.getDistance(entity.posX, entity.posY + entity.getEyeHeight() + 1, entity.posZ);
                double distanceXZ = mc.thePlayer.getDistance(entity.posX, mc.thePlayer.posY, entity.posZ);

                if (FarmHelperConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking > 0
                        && (distanceXZ < 1.5 || distance <= 10)
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

                if (!FlyPathFinderExecutor.getInstance().isDontRotate() && distance < 5.5) {
                    FlyPathFinderExecutor.getInstance().setDontRotate(true);
                    RotationHandler.getInstance().reset();
                } else if (FlyPathFinderExecutor.getInstance().isDontRotate() && distance > 5.5) {
                    FlyPathFinderExecutor.getInstance().setDontRotate(false);
                    RotationHandler.getInstance().reset();
                }
                System.out.println(distance);
                if (distance < 3) {
                    if (rotationState != RotationState.CLOSE) {
                        rotationState = RotationState.CLOSE;
                    }
                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity).additionalY(0.9f),
                                (long) (600 + Math.random() * 200),
                                null
                        ).followTarget(true));
                    }
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, true);
                } else {
                    if (rotationState != RotationState.FAR) {
                        FlyPathFinderExecutor.getInstance().stop();
                        rotationState = RotationState.FAR;
                    }
                    if (FlyPathFinderExecutor.getInstance().isDontRotate()) {
                        RotationHandler.getInstance().reset();
                        FlyPathFinderExecutor.getInstance().setDontRotate(false);
                    }
                    if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                        FlyPathFinderExecutor.getInstance().findPath(entity, true, true, 1.75f, false);
                    }
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindUseItem, distance < 4.5);
                }
                break;
            case CHECK_ANOTHER_PEST:
                LogUtils.sendDebug(totalPests + " pest" + (totalPests == 1 ? "" : "s") + " left");
                if (totalPests == 0) {
                    state = States.GO_BACK;
                    delayClock.schedule((long) (1_000 + Math.random() * 500));
                } else {
                    if (!pestsLocations.isEmpty() && getClosestPest() != null) {
                        LogUtils.sendDebug("Found another pest");
                        state = States.FLY_TO_PEST;
                        delayClock.schedule(50 + (long) (Math.random() * 100));
                    } else if (pestsPlotMap.isEmpty()) {
                        LogUtils.sendDebug("Manually searching for pest");
                        state = States.GET_LOCATION;
                        delayClock.schedule(300 + (long) (Math.random() * 250));
                    } else {
                        LogUtils.sendDebug("Teleporting to plot");
                        state = States.TELEPORT_TO_PLOT;
                        delayClock.schedule(400 + (long) (Math.random() * 400));
                    }
                }
                KeyBindUtils.stopMovement();
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
            if (previousEntityTarget.isPresent() && entity.equals(previousEntityTarget.get())) continue;
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

    private int finishTries = 0;

    private void finishMacro() {
        if (isInventoryOpen()) return;
        if (MacroHandler.getInstance().isMacroToggled()) {
            if (PlayerUtils.isStandingOnSpawnPoint()) {
                stop();
                MacroHandler.getInstance().resumeMacro();
                return;
            }
            if (finishTries == 7) {
                LogUtils.sendError("[Pests Destroyer] Couldn't enable macro after teleportation! Check if your spawn point is set in mod (Yellow rectangle). If not, re-do command /setspawn");
                LogUtils.webhookLog("[Pests Destroyer] Couldn't enable macro after teleportation!");
                MacroHandler.getInstance().disableMacro();
                return;
            }
            MacroHandler.getInstance().triggerWarpGarden(true, true);
            finishTries++;
            delayClock.schedule(1_000 + Math.random() * 500);
        } else {
            stop();
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0 || event.message == null) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText().trim());
        if (message.startsWith("You can't fast travel while in combat!")) {
            LogUtils.sendWarning("[Pests Destroyer] Can't fast travel while in combat, will try again to teleport.");
            enabled = true;
            Multithreading.schedule(this::finishMacro, 1_000 + (long) (Math.random() * 1_000), TimeUnit.MILLISECONDS);
            return;
        }
        if (message.contains("The worm seems to have burrowed")) {
            cantReachPest = 0;
            return;
        }
        Matcher matcher = pestPattern.matcher(message);
        if (!matcher.matches()) {
            return;
        }
        String numberOfPests = matcher.group(1);
        String plot = matcher.group(2);
        int plotNumber;
        int pests;
        try {
            plotNumber = Integer.parseInt(plot);
            pests = Objects.equals(numberOfPests, "A") ? 1 : Integer.parseInt(numberOfPests);
        } catch (Exception e) {
            LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + plot);
            return;
        }
        int finalPlotNumber = plotNumber;
        boolean found = false;
        Plot plotObj = null;
        for (Map.Entry<Plot, Integer> entry : pestsPlotMap.entrySet()) {
            if (entry.getKey().plotNumber == finalPlotNumber) {
                pestsPlotMap.put(entry.getKey(), entry.getValue() + pests);
                found = true;
                plotObj = entry.getKey();
                break;
            }
        }
        if (!found) {
            plotObj = new Plot(String.valueOf(plotNumber), plotNumber);
            pestsPlotMap.put(plotObj, pests);
        }
        LogUtils.sendDebug("New pest at plot number: " + finalPlotNumber + ", total number on this plot is: " + pestsPlotMap.get(plotObj));
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        List<Entity> pests = mc.theWorld.loadedEntityList.stream().filter(entity -> {
            if (entity.isDead) return false;
            if (entity.posY < 50) return false;
            if (entity instanceof EntityArmorStand) {
                if (entity.equals(lastKilledEntity)) return false;
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

        for (Map.Entry<Plot, Integer> plotNumber : pestsPlotMap.entrySet()) {
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotBasedOnNumber(plotNumber.getKey().plotNumber);
            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            float centerX = (float) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
            float centerZ = (float) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
            int numberOfPests = plotNumber.getValue();
            RenderUtils.drawText("Plot: " + EnumChatFormatting.AQUA + plotNumber.getKey().plotNumber + EnumChatFormatting.RESET + " has " + EnumChatFormatting.GOLD + numberOfPests + EnumChatFormatting.RESET + " pests", centerX, 80, centerZ, 1);
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
        LogUtils.sendDebug("[Pests Destroyer] Entity died: " + entity.getName() + " at: " + entity.getPosition());
        int plotNumber = PlotUtils.getPlotNumberBasedOnLocation(entity.getPosition());
        if (plotNumber == -1) {
            if (isRunning())
                LogUtils.sendError("[Pests Destroyer] Failed to get plot number for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        Plot plot;
        try {
            plot = pestsPlotMap.entrySet().stream().filter(entry -> entry.getKey().plotNumber == plotNumber).findFirst().get().getKey();
        } catch (Exception e) {
            LogUtils.sendError("[Pests Destroyer] Failed to get plot for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        if (pestsPlotMap.get(plot) > 1) {
            pestsPlotMap.replace(plot, pestsPlotMap.get(plot) - 1);
            LogUtils.sendDebug("[Pests Destroyer] Removed 1 pest from plot number: " + plotNumber);
        } else {
            pestsPlotMap.remove(plot);
            LogUtils.sendDebug("[Pests Destroyer] Removed all pests from plot number: " + plotNumber);
        }
        lastKilledEntity = entity;
        lastFireworkLocation = Optional.empty();
        lastFireworkTime = 0;
        currentEntityTarget.ifPresent(e -> {
            if (!e.equals(event.entity)) {
                return;
            }
            KeyBindUtils.stopMovement();
            previousEntityTarget = currentEntityTarget;
            currentEntityTarget = Optional.empty();
            stuckClock.reset();
            RotationHandler.getInstance().reset();
            delayClock.schedule(150);
        });
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        List<String> scoreBoard = ScoreboardUtils.getScoreboardLines();

        for (String line : scoreBoard) {
            String withoutColors = StringUtils.stripControlCodes(line);
            String clean = ScoreboardUtils.cleanSB(withoutColors);
            if (line.contains("ൠ")) {
                String[] split = clean.split(" ");
                try {
                    String pests = split[split.length - 1].replace("x", "").trim();
                    int pestsAmount = Integer.parseInt(pests);
                    if (pestsAmount != totalPests) {
                        totalPests = pestsAmount;
                        if (!isRunning() && totalPests >= FarmHelperConfig.startKillingPestsAt) {
                            if (FarmHelperConfig.sendWebhookLogIfPestsDetectionNumberExceeded) {
                                LogUtils.webhookLog("[Pests Destroyer]\\nThere " + (totalPests > 1 ? "are" : "is") + " currently **" + totalPests + "** " + (totalPests > 1 ? "pests" : "pest") + " in the garden!", FarmHelperConfig.pingEveryoneOnPestsDetectionNumberExceeded);
                            }
                            if (FarmHelperConfig.sendNotificationIfPestsDetectionNumberExceeded) {
                                FailsafeUtils.getInstance().sendNotification("There " + (totalPests > 1 ? "are" : "is") + " currently " + totalPests + " " + (totalPests > 1 ? "pests" : "pest") + " in the garden!", TrayIcon.MessageType.WARNING);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (line.contains("Garden") || line.contains("Plot")) {
                if (totalPests > 0) {
                    totalPests = 0;
                }
            }
        }
        if (totalPests == 0) {
            pestsPlotMap.clear();
            pestsLocations.clear();
        }
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

    private int getAmountOfPestsInPlots() {
        int pests = 0;
        for (Map.Entry<Plot, Integer> plot : pestsPlotMap.entrySet()) {
            pests += plot.getValue();
        }
        return pests;
    }

    @SubscribeEvent
    public void onGuiOpen(DrawScreenAfterEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!(event.guiScreen instanceof GuiChest)) return;
        String guiName = InventoryUtils.getInventoryName();
        if (guiName == null) return;
        if (!delayClock.passed()) return;
        if (getAmountOfPestsInPlots() == totalPests) {
            if (enabled && state == States.WAIT_FOR_INFO) {
                state = States.TELEPORTING_TO_PLOT;
                Multithreading.schedule(() -> {
                    if (mc.currentScreen != null) {
                        state = States.TELEPORT_TO_PLOT;
                        delayClock.schedule((long) (300 + Math.random() * 300));
                        PlayerUtils.closeScreen();
                    }
                }, 500 + (long) (Math.random() * 500), TimeUnit.MILLISECONDS);
            }
            return;
        }
        ContainerChest guiChest = (ContainerChest) ((GuiChest) event.guiScreen).inventorySlots;

        int plotCounter = 0;
        if (StringUtils.stripControlCodes(guiName).equals("Configure Plots")) {
            pestsPlotMap.clear();
            for (int i = 0; i < guiChest.inventorySlots.size(); i++) {
                Slot slot = guiChest.inventorySlots.get(i);
                if (slot == null || !slot.getHasStack()) continue;
                if (slot.getStack().getDisplayName().contains("Plot")) {
                    String displayName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                    try {
                        String plotName = displayName.replace("Plot - ", "").trim();
                        int plotNumber = PlotUtils.getPLOT_NUMBERS().get(plotCounter);
                        List<String> lore = InventoryUtils.getItemLore(slot.getStack());
                        for (String line : lore) {
                            if (line.contains("This plot has")) {
                                Matcher matcher = pestPatternDeskGui.matcher(line);
                                if (matcher.matches()) {
                                    int pests = Integer.parseInt(matcher.group(1));
                                    pestsPlotMap.put(new Plot(plotName, plotNumber), pests);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + displayName);
                    }
                    plotCounter++;
                } else if (StringUtils.stripControlCodes(slot.getStack().getDisplayName()).equals("The Barn"))
                    plotCounter++;
            }
            Slot slot = guiChest.inventorySlots.get(42); // last plot in gui
            if (slot != null && slot.getHasStack() && slot.getStack().getDisplayName().contains("Plot")) {
                if (pestsPlotMap.isEmpty()) {
                    long delay = 500 + (long) (Math.random() * 500);
                    delayClock.schedule(delay + (long) (Math.random() * 200));
                    LogUtils.sendError("[Pests Destroyer] Failed to get locations of pests. (Uncleaned plots?)");
                    if (state == States.WAIT_FOR_INFO && isRunning()) {
                        LogUtils.sendError("[Pests Destroyer] Attempting to find pest with tracker");
                        state = States.GET_LOCATION;
                        Multithreading.schedule(() -> {
                            if (mc.currentScreen != null) {
                                PlayerUtils.closeScreen();
                            }
                        }, delay, TimeUnit.MILLISECONDS);
                    }
                    return;
                }
            }
        } else {
            return;
        }
        int foundPests = pestsPlotMap.values().stream().mapToInt(i -> i).sum();
        if (foundPests == 0) return;
        for (Map.Entry<Plot, Integer> plot : pestsPlotMap.entrySet()) {
            if (plot.getValue() > 0) {
                LogUtils.sendDebug("Found plot with pests: " + plot.getKey().plotNumber + " with " + plot.getValue() + " pests");
            }
        }
        if (state == States.WAIT_FOR_INFO) {
            if (!pestsLocations.isEmpty() && BlockUtils.canFlyHigher(3)) {
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                }
                state = States.FLY_TO_PEST;
                delayClock.schedule((long) (300 + Math.random() * 300));
            } else {
                state = States.TELEPORTING_TO_PLOT;
                Multithreading.schedule(() -> {
                    if (mc.currentScreen != null) {
                        state = States.TELEPORT_TO_PLOT;
                        delayClock.schedule((long) (300 + Math.random() * 300));
                        PlayerUtils.closeScreen();
                    }
                }, 500 + (long) (Math.random() * 500), TimeUnit.MILLISECONDS);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        pestsPlotMap.clear();
        totalPests = 0;
    }

    enum States {
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

    public static class Plot {
        public String name;
        public int plotNumber;

        Plot(String name, int plotNumber) {
            this.name = name;
            this.plotNumber = plotNumber;
        }
    }
}
