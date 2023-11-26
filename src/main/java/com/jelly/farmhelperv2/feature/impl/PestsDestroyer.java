package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.event.SpawnParticleEvent;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.settings.KeyBinding;
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
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
    private final HashMap<Integer, Integer> pestsPlotMap = new HashMap<>();
    @Getter
    private final ArrayList<Entity> pestsLocations = new ArrayList<>();
    @Getter
    private final Clock stuckClock = new Clock();
    private final Pattern pestPattern = Pattern.compile("GROSS! A Pest has appeared in Plot - (\\d+)!");
    @Getter
    private final Clock delayClock = new Clock();
    private final Clock delayBetweenBackTaps = new Clock();
    private final Pattern pestPatternDeskGui = Pattern.compile(".*?(\\d+).*?");
    @Getter
    private Optional<Entity> currentEntityTarget = Optional.empty();
    @Getter
    private int totalPests = 0;
    private boolean enabled = false;
    private boolean preparing = false;
    @Setter
    private int cantReachPest = 0;
    @Getter
    private States state = States.IDLE;
    @Getter
    private EscapeState escapeState = EscapeState.NONE;
    private Optional<BlockPos> preTpBlockPos = Optional.empty();
    private RotationType rotationType = RotationType.NONE;
    private Optional<Vec3> lastFireworkLocation = Optional.empty();
    private long lastFireworkTime = 0;

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
        if (!mc.thePlayer.capabilities.allowFlying) {
            LogUtils.sendError("[Pests Destroyer] You need to be able to fly!");
            FarmHelperConfig.enablePestsDestroyer = false;
            return;
        }
        preparing = true;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::clearSavedState);
            KeyBindUtils.stopMovement();
        }
        escapeState = EscapeState.NONE;
        state = States.IDLE;
        Multithreading.schedule(() -> {
            if (!preparing) return;
            enabled = true;
            preparing = false;
            LogUtils.sendWarning("[Pests Destroyer] Starting killing shitters!");
            LogUtils.webhookLog("[Pests Destroyer]\\nStarting killing shitters!");
        }, 800 + (long) (Math.random() * 500), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (enabled || preparing) {
            LogUtils.sendWarning("[Pests Destroyer] Stopping!");
            LogUtils.webhookLog("[Pests Destroyer]\\nStopping!");
        }
        resetStatesAfterMacroDisabled();
        KeyBindUtils.stopMovement();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        currentEntityTarget = Optional.empty();
        lastFireworkLocation = Optional.empty();
        preTpBlockPos = Optional.empty();
        delayBetweenBackTaps.reset();
        delayClock.reset();
        stuckClock.reset();
        pestsLocations.clear();
        pestsPlotMap.clear();
        preparing = false;
        enabled = false;
        totalPests = 0;
        lastFireworkTime = 0;
        rotationType = RotationType.NONE;
        state = States.IDLE;
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enablePestsDestroyer;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return state != States.TELEPORT_TO_PLOT && state != States.WAIT_FOR_TP && escapeState == EscapeState.NONE;
    }

    public boolean canEnableMacro() {
        if (!isToggled()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (!MacroHandler.getInstance().isMacroToggled()) return false;
        if (enabled || preparing) return false;
        if (totalPests < FarmHelperConfig.startKillingPestsAt) return false;

        return PlayerUtils.isStandingOnSpawnPoint() || PlayerUtils.isStandingOnRewarpLocation();
    }

    @SubscribeEvent
    public void onTickExecute(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden() && escapeState == EscapeState.NONE) return;
        if (!enabled) return;


        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendWarning("[Pests Destroyer] The player is struggling killing pest for 5 minutes, stopping fully!");
            FarmHelperConfig.enablePestsDestroyer = false;
            LogUtils.sendFailsafeMessage("[Pests Destroyer] Couldn't kill pest for 5 minutes, stopping fully!", true);
            finishMacro();
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
            if (rotationType != RotationType.NONE) {
                rotationType = RotationType.NONE;
            }
            KeyBindUtils.stopMovement();
            switch (escapeState) {
                case GO_TO_HUB:
                    if (mc.currentScreen != null) {
                        mc.thePlayer.closeScreen();
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.GO_TO_GARDEN;
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
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
                    if (mc.currentScreen != null) {
                        mc.thePlayer.closeScreen();
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().inGarden()) {
                        escapeState = EscapeState.GO_TO_HUB;
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.RESUME_MACRO;
                        mc.thePlayer.sendChatMessage("/warp garden");
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
                    if (mc.currentScreen != null) {
                        mc.thePlayer.closeScreen();
                        delayClock.schedule((long) (500 + Math.random() * 500));
                        break;
                    }
                    if (GameStateHandler.getInstance().inGarden()) {
                        escapeState = EscapeState.NONE;
                        state = States.IDLE;
                        cantReachPest = 0;
                        delayClock.schedule((long) (2_500 + Math.random() * 1_500));
                        LogUtils.sendDebug("[Pests Destroyer] Came back to Garden!");
                        break;
                    }
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        escapeState = EscapeState.RESUME_MACRO;
                        mc.thePlayer.sendChatMessage("/warp garden");
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
                if (currentItem == null || !currentItem.getDisplayName().contains("Vacuum")) {
                    int vacuum = InventoryUtils.getSlotIdOfItemInHotbar("Vacuum");
                    if (vacuum == -1) {
                        LogUtils.sendError("[Pests Destroyer] Failed to find vacuum in hotbar!");
                        state = States.GO_BACK;
                        FarmHelperConfig.enablePestsDestroyer = false;
                        finishMacro();
                        return;
                    }
                    mc.thePlayer.inventory.currentItem = vacuum;
                    delayClock.schedule((long) (200 + Math.random() * 200));
                    return;
                }
                int pestsInMap = pestsPlotMap.values().stream().mapToInt(i -> i).sum();
                if (pestsInMap == 0) {
                    state = States.OPEN_DESK;
                } else {
                    state = States.TELEPORT_TO_PLOT;
                }
                delayClock.schedule((long) (200 + Math.random() * 200));
                break;
            case OPEN_DESK:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    break;
                }
                mc.thePlayer.sendChatMessage("/desk");
                state = States.OPEN_PLOTS;
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 500 + Math.random() * 500));
                break;
            case OPEN_PLOTS:
                String chestName = InventoryUtils.getInventoryName();
                if (chestName != null && !chestName.equals("Desk")) {
                    mc.thePlayer.closeScreen();
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
                    LogUtils.sendDebug("Wrong " + chestName2);
                    mc.thePlayer.closeScreen();
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 500 + Math.random() * 500));
                    state = States.OPEN_DESK;
                    break;
                }
                break;
            case TELEPORT_TO_PLOT:
                Optional<Integer> plotNumberOpt = pestsPlotMap.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).findFirst();
                if (!plotNumberOpt.isPresent()) {
                    state = States.GO_BACK;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    return;
                }
                int plotNumber = plotNumberOpt.get();
                preTpBlockPos = Optional.of(mc.thePlayer.getPosition());
                mc.thePlayer.sendChatMessage("/tptoplot " + plotNumber);
                state = States.WAIT_FOR_TP;
                delayClock.schedule((long) (200 + Math.random() * 100));
                break;
            case WAIT_FOR_TP:
                if (!preTpBlockPos.isPresent()) {
                    state = States.IDLE;
                    break;
                }
                if (!mc.thePlayer.getPosition().equals(preTpBlockPos.get())) {
                    if (!mc.thePlayer.capabilities.isFlying) {
                        fly();
                        delayClock.schedule(350);
                        break;
                    }
                    state = States.GET_LOCATION;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                }
                break;
            case GET_LOCATION:
                if (totalPests == 0) {
                    state = States.GO_BACK;
                    return;
                }
                if (mc.currentScreen != null) {
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300 + (long) (Math.random() * 300));
                    Multithreading.schedule(() -> {
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                            delayClock.schedule(100 + (long) (Math.random() * 200));
                        }
                    }, (long) (200 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    break;
                }
                if (!mc.thePlayer.capabilities.isFlying) {
                    fly();
                    delayClock.schedule(350);
                    break;
                }

                flyAwayFromStructures();

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                state = States.WAIT_FOR_LOCATION;
                lastFireworkLocation = Optional.empty();
                KeyBindUtils.leftClick();
                delayClock.schedule(300);
                break;
            case WAIT_FOR_LOCATION:
                if (mc.currentScreen != null) {
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300 + (long) (Math.random() * 300));
                    Multithreading.schedule(() -> {
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                            delayClock.schedule(100 + (long) (Math.random() * 200));
                        }
                    }, (long) (200 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    break;
                }
                flyAwayFromStructures();

                if (RotationHandler.getInstance().isRotating()) return;

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (lastFireworkLocation.isPresent()) {
                    if (lastFireworkTime + 150 < System.currentTimeMillis()) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(new Vec3(lastFireworkLocation.get().xCoord, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), lastFireworkLocation.get().zCoord)),
                                FarmHelperConfig.getRandomRotationTime(),
                                () -> {
                                    if (state != States.WAIT_FOR_LOCATION) {
                                        RotationHandler.getInstance().reset();
                                        return;
                                    }
                                    LogUtils.sendDebug("[Pests Destroyer] Finished rotating to firework location!");
                                    state = States.FLY_TO_PEST;
                                    RotationHandler.getInstance().reset();
                                }
                        ));
                        delayClock.schedule(300);
                    }
                }
                break;
            case FLY_TO_PEST:
                if (mc.currentScreen != null) {
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300 + (long) (Math.random() * 300));
                    Multithreading.schedule(() -> {
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                            delayClock.schedule(100 + (long) (Math.random() * 200));
                        }
                    }, (long) (200 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    break;
                }
                if (!mc.thePlayer.capabilities.isFlying) {
                    fly();
                    break;
                }

                if (pestsLocations.isEmpty()) {
                    if (!lastFireworkLocation.isPresent()) {
                        state = States.GET_LOCATION;
                        break;
                    }
                    if (mc.thePlayer.getDistance(lastFireworkLocation.get().xCoord, mc.thePlayer.posY, lastFireworkLocation.get().zCoord) < 1.5) {
                        state = States.GET_LOCATION;
                        return;
                    }
                    boolean objects = objectsInFrontOfPlayer();
                    KeyBindUtils.holdThese(
                            !objects ? mc.gameSettings.keyBindForward : null,
                            mc.gameSettings.keyBindSprint,
                            objects ? mc.gameSettings.keyBindJump : null
                    );
                    break;
                }

                Entity closestPest = null;
                double closestDistance = Double.MAX_VALUE;
                for (Entity entity : pestsLocations) {
                    double distance = mc.thePlayer.getDistanceToEntity(entity);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestPest = entity;
                    }
                }

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
                if (mc.currentScreen != null) {
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300 + (long) (Math.random() * 300));
                    Multithreading.schedule(() -> {
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                            delayClock.schedule(100 + (long) (Math.random() * 200));
                        }
                    }, (long) (200 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    break;
                }
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
                double distanceWithoutY = mc.thePlayer.getDistance(entity.posX, mc.thePlayer.posY, entity.posZ);

                Rotation rotationEntity = RotationHandler.getInstance().getRotation(entity);
                float yawDifference = Math.abs(AngleUtils.normalizeAngle(rotationEntity.getYaw() - AngleUtils.get360RotationYaw()));

                if (FarmHelperConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking > 0 && (distanceWithoutY < 1.5 || distance <= 10) && GameStateHandler.getInstance().getDx() < 0.1 && GameStateHandler.getInstance().getDz() < 0.1 && !canEntityBeSeenIgnoreNonCollidable(entity)) {
                    cantReachPest++;
                    LogUtils.sendDebug("[Pests Destroyer] Probably can't reach that pest: " + cantReachPest);
                }

                if (cantReachPest >= FarmHelperConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking) {
                    LogUtils.sendWarning("[Pests Destroyer] Can't reach pest, will do a quick Garden -> Hub -> Garden teleport.");
                    escapeState = EscapeState.GO_TO_HUB;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(300);
                    return;
                }


                if (distance <= 3) {
                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().reset();
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity),
                                FarmHelperConfig.getRandomPestsKillerRotationTime(),
                                null
                        ));
                        rotationType = RotationType.CLOSE;
                    }
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem);
                } else if (distance <= 10 || distanceWithoutY <= 1) {
                    if (!mc.thePlayer.capabilities.isFlying) {
                        fly();
                        delayClock.schedule(350);
                        break;
                    }
                    if (distanceWithoutY <= 3 && (mc.thePlayer.motionX > 0.05 || mc.thePlayer.motionZ > 0.05)) {
                        KeyBindUtils.holdThese(distance < 6 ? mc.gameSettings.keyBindUseItem : null);
                        if (delayBetweenBackTaps.passed()) {
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                            delayBetweenBackTaps.schedule(100 + (long) (Math.random() * 200));
                        }
                        break;
                    }
                    if (objectsInFrontOfPlayer() || entity.posY + entity.getEyeHeight() + 1 - mc.thePlayer.posY >= 2) {
                        KeyBindUtils.holdThese(distance < 6 ? mc.gameSettings.keyBindUseItem : null, mc.gameSettings.keyBindJump, distanceWithoutY > 3 && yawDifference < 45 ? mc.gameSettings.keyBindForward : null, distanceWithoutY < 4 && (GameStateHandler.getInstance().getDx() > 0.04 || GameStateHandler.getInstance().getDz() > 0.04) ? mc.gameSettings.keyBindBack : null);
                    } else if (entity.posY + entity.getEyeHeight() + 1 - mc.thePlayer.posY <= -2) {
                        if (hasBlockUnderThePlayer()) {
                            LogUtils.sendDebug("Has block under the player");
                            KeyBindUtils.holdThese(distance < 6 ? mc.gameSettings.keyBindUseItem : null, getMovementToEvadeBottomBlock(), distanceWithoutY < 4 && (GameStateHandler.getInstance().getDx() > 0.04 || GameStateHandler.getInstance().getDz() > 0.04) ? mc.gameSettings.keyBindBack : null);
                        } else {
                            LogUtils.sendDebug("Doesn't have block under the player");
                            KeyBindUtils.holdThese(distance < 6 ? mc.gameSettings.keyBindUseItem : null, mc.gameSettings.keyBindSneak, distanceWithoutY > 3 && yawDifference < 45 ? mc.gameSettings.keyBindForward : null, distanceWithoutY < 4 && (GameStateHandler.getInstance().getDx() > 0.04 || GameStateHandler.getInstance().getDz() > 0.04) ? mc.gameSettings.keyBindBack : null);
                        }
                    } else {
                        KeyBindUtils.holdThese(distance < 6 ? mc.gameSettings.keyBindUseItem : null, distanceWithoutY > 3 && yawDifference < 45 ? mc.gameSettings.keyBindForward : null, distanceWithoutY < 4 && (GameStateHandler.getInstance().getDx() > 0.04 || GameStateHandler.getInstance().getDz() > 0.04) ? mc.gameSettings.keyBindBack : null);
                    }
                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().reset();
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ));
                        rotationType = RotationType.MEDIUM;
                    }
                } else {
                    if (!mc.thePlayer.capabilities.isFlying) {
                        fly();
                        delayClock.schedule(350);
                        break;
                    }

                    if (!GameStateHandler.getInstance().isLeftWalkable() && GameStateHandler.getInstance().isRightWalkable()) {
                        KeyBindUtils.holdThese(objectsInFrontOfPlayer() ? mc.gameSettings.keyBindJump : null, distanceWithoutY > 2 && yawDifference < 90 ? mc.gameSettings.keyBindForward : null, mc.gameSettings.keyBindRight);
                    } else if (GameStateHandler.getInstance().isLeftWalkable() && !GameStateHandler.getInstance().isRightWalkable()) {
                        KeyBindUtils.holdThese(objectsInFrontOfPlayer() ? mc.gameSettings.keyBindJump : null, distanceWithoutY > 2 && yawDifference < 90 ? mc.gameSettings.keyBindForward : null, mc.gameSettings.keyBindLeft);
                    } else {
                        KeyBindUtils.holdThese(objectsInFrontOfPlayer() ? mc.gameSettings.keyBindJump : null, distanceWithoutY > 2 && yawDifference < 90 ? mc.gameSettings.keyBindForward : null);
                    }

                    if (!RotationHandler.getInstance().isRotating()) {
                        RotationHandler.getInstance().reset();
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(entity),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ));
                        rotationType = RotationType.FAR;
                    }
                }
                break;
            case CHECK_ANOTHER_PEST:
                // remove 1 from count because we send message before scoreboard update
                LogUtils.sendDebug((totalPests-1) + " pest" + (totalPests == 2 ? "" : "s") + " left");
                if (totalPests-1 == 0) {
                    state = States.GO_BACK;
                } else {
                    if (pestsPlotMap.isEmpty())
                    {
                        LogUtils.sendDebug("Manually searching for pest");
                        state = States.GET_LOCATION;
                    } else {
                        state = States.TELEPORT_TO_PLOT;
                    }
                }
                KeyBindUtils.stopMovement();
                delayClock.schedule(1_500 + (long) (Math.random() * 1_000));
                break;
            case GO_BACK:
                finishMacro();
                break;
        }
    }

    private void flyAwayFromStructures() {
        if (mc.thePlayer.posY < 75 && !hasBlockAboveThePlayer()) {
            LogUtils.sendDebug("Has block above the player");
            if (!GameStateHandler.getInstance().isRightWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft, mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
            } else if (!GameStateHandler.getInstance().isLeftWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight, mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
            } else if (!GameStateHandler.getInstance().isFrontWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
            } else if (!GameStateHandler.getInstance().isFrontWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
            } else {
                KeyBindUtils.holdThese(mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
            }
        } else {
            KeyBindUtils.stopMovement();
        }
    }

    private void finishMacro() {
        if (mc.currentScreen != null) {
            mc.thePlayer.closeScreen();
            delayClock.schedule(500 + (long) (Math.random() * 500));
            return;
        }
        stop();
        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
        Multithreading.schedule(() -> {
            if (MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendDebug("Enabling macro after teleportation");
                MacroHandler.getInstance().resumeMacro();
            }
        }, 1_500 + (long) (Math.random() * 1_500), TimeUnit.MILLISECONDS);
    }

    private void fly() {
        if (!mc.thePlayer.capabilities.allowFlying) {
            LogUtils.sendError("[Pests Destroyer] You need to be able to fly!");
            FarmHelperConfig.enablePestsDestroyer = false;
            stop();
            return;
        }
        if (mc.thePlayer.onGround)
            mc.thePlayer.jump();
        Multithreading.schedule(() -> {
            if (!mc.thePlayer.capabilities.isFlying) {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }
        }, 300, TimeUnit.MILLISECONDS);
        delayClock.schedule(400);
    }

    private boolean objectsInFrontOfPlayer() {
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 leftPos = BlockUtils.getRelativeVec(-0.66f, 0, 0.66f, mc.thePlayer.rotationYaw);
        Vec3 rightPos = BlockUtils.getRelativeVec(0.66f, 0, 0.66f, mc.thePlayer.rotationYaw);
        Vec3 playerLook = AngleUtils.getVectorForRotation(0, mc.thePlayer.rotationYaw);
        Vec3 lookForwardFeet = playerPos.addVector(playerLook.xCoord * 6, 0, playerLook.zCoord * 6);
        Vec3 lookForwardHead = playerPos.addVector(playerLook.xCoord * 6, mc.thePlayer.eyeHeight, playerLook.zCoord * 6);
        Vec3 lookForwardFeetLeft = leftPos.addVector(playerLook.xCoord * 5, 0, playerLook.zCoord * 5);
        Vec3 lookForwardHeadLeft = leftPos.addVector(playerLook.xCoord * 5, mc.thePlayer.eyeHeight, playerLook.zCoord * 5);
        Vec3 lookForwardFeetRight = rightPos.addVector(playerLook.xCoord * 5, 0, playerLook.zCoord * 5);
        Vec3 lookForwardHeadRight = rightPos.addVector(playerLook.xCoord * 5, mc.thePlayer.eyeHeight, playerLook.zCoord * 5);
        MovingObjectPosition mopFeet = mc.theWorld.rayTraceBlocks(playerPos, lookForwardFeet, false, true, false);
        if (unpassableBlock(mopFeet)) return true;
        MovingObjectPosition mopHead = mc.theWorld.rayTraceBlocks(playerPos.addVector(0, mc.thePlayer.eyeHeight, 0), lookForwardHead, false, true, false);
        if (unpassableBlock(mopHead)) return true;
        MovingObjectPosition mopFeetLeft = mc.theWorld.rayTraceBlocks(leftPos, lookForwardFeetLeft, false, true, false);
        if (unpassableBlock(mopFeetLeft)) return true;
        MovingObjectPosition mopHeadLeft = mc.theWorld.rayTraceBlocks(leftPos.addVector(0, mc.thePlayer.eyeHeight, 0), lookForwardHeadLeft, false, true, false);
        if (unpassableBlock(mopHeadLeft)) return true;
        MovingObjectPosition mopFeetRight = mc.theWorld.rayTraceBlocks(rightPos, lookForwardFeetRight, false, true, false);
        if (unpassableBlock(mopFeetRight)) return true;
        MovingObjectPosition mopHeadRight = mc.theWorld.rayTraceBlocks(rightPos.addVector(0, mc.thePlayer.eyeHeight, 0), lookForwardHeadRight, false, true, false);
        return unpassableBlock(mopHeadRight);
    }

    private boolean hasBlockUnderThePlayer() {
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 lookDown = AngleUtils.getVectorForRotation(90, mc.thePlayer.rotationYaw);
        Vec3 lookDownFeet = playerPos.addVector(lookDown.xCoord * 2, lookDown.yCoord * 2, lookDown.zCoord * 2);
        MovingObjectPosition mopFeet = mc.theWorld.rayTraceBlocks(playerPos, lookDownFeet, false, true, false);
        return unpassableBlock(mopFeet);
    }

    private boolean hasBlockAboveThePlayer() {
        Vec3 playerPos = mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.getEyeHeight(), 0);
        Vec3 lookUp = AngleUtils.getVectorForRotation(-90, mc.thePlayer.rotationYaw);
        Vec3 lookUpFeet = playerPos.addVector(lookUp.xCoord, lookUp.yCoord, lookUp.zCoord);
        MovingObjectPosition mopFeet = mc.theWorld.rayTraceBlocks(playerPos, lookUpFeet, false, true, false);
        return unpassableBlock(mopFeet);
    }

    private KeyBinding getMovementToEvadeBottomBlock() {
        Vec3 playerPosForward = BlockUtils.getRelativeVec(0, 0, 1, mc.thePlayer.rotationYaw);
        Vec3 playerPosBackward = BlockUtils.getRelativeVec(0, 0, -1, mc.thePlayer.rotationYaw);
        Vec3 playerPosLeft = BlockUtils.getRelativeVec(-1, 0, 0, mc.thePlayer.rotationYaw);
        Vec3 playerPosRight = BlockUtils.getRelativeVec(1, 0, 0, mc.thePlayer.rotationYaw);
        MovingObjectPosition mopForward = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), playerPosForward, false, true, false);
        if (!unpassableBlock(mopForward) && GameStateHandler.getInstance().isFrontWalkable())
            return mc.gameSettings.keyBindForward;
        MovingObjectPosition mopBackward = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), playerPosBackward, false, true, false);
        if (!unpassableBlock(mopBackward) && GameStateHandler.getInstance().isBackWalkable())
            return mc.gameSettings.keyBindBack;
        MovingObjectPosition mopLeft = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), playerPosLeft, false, true, false);
        if (!unpassableBlock(mopLeft) && GameStateHandler.getInstance().isLeftWalkable())
            return mc.gameSettings.keyBindLeft;
        MovingObjectPosition mopRight = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), playerPosRight, false, true, false);
        if (!unpassableBlock(mopRight) && GameStateHandler.getInstance().isRightWalkable())
            return mc.gameSettings.keyBindRight;
        LogUtils.sendError("[Pests Destroyer] Couldn't find a way to evade bottom block!");
        return null;
    }

    private boolean unpassableBlock(MovingObjectPosition mop) {
        return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !isBlockPassable(mop.getBlockPos());
    }

    private boolean isBlockPassable(BlockPos blockPos) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block.isPassable(mc.theWorld, blockPos);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0 || event.message == null) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText().trim());
        Matcher matcher = pestPattern.matcher(message);
        if (!matcher.matches()) {
            return;
        }
        String plot = matcher.group(1);
        int plotNumber;
        try {
            plotNumber = Integer.parseInt(plot);
        } catch (Exception e) {
            LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + plot);
            return;
        }
        int finalPlotNumber = plotNumber;
        if (pestsPlotMap.containsKey(plotNumber)) {
            pestsPlotMap.put(plotNumber, pestsPlotMap.get(plotNumber) + 1);
        } else {
            pestsPlotMap.put(plotNumber, 1);
        }
        LogUtils.sendDebug("New pest at plot number: " + finalPlotNumber + ", total number on this plot is: " + pestsPlotMap.get(finalPlotNumber));
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        List<Entity> pests = mc.theWorld.loadedEntityList.stream().filter(entity -> {
            if (entity instanceof EntityArmorStand) {
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

        for (Map.Entry<Integer, Integer> plotNumber : pestsPlotMap.entrySet()) {
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotBasedOnNumber(plotNumber.getKey());
            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            float centerX = (float) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
            float centerZ = (float) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
            int numberOfPests = plotNumber.getValue();
            RenderUtils.drawText("Plot: " + EnumChatFormatting.AQUA + plotNumber.getKey() + EnumChatFormatting.RESET + " has " + EnumChatFormatting.GOLD + numberOfPests + EnumChatFormatting.RESET + " pests", centerX, 80, centerZ, 1);
        }
    }

    private boolean canEntityBeSeenIgnoreNonCollidable(Entity entity) {
        Vec3 vec3 = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Vec3 vec31 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vec31, vec3, false, true, false);
        return mop == null || mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.cactus);
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        Entity entity = event.entity;
        LogUtils.sendDebug("[Pests Destroyer] Entity died: " + entity.getName() + " at: " + entity.getPosition());
        int plotNumber = PlotUtils.getPlotNumberBasedOnLocation(entity.getPosition());
        if (plotNumber == -1) {
            LogUtils.sendError("[Pests Destroyer] Failed to get plot number for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        currentEntityTarget.ifPresent(ent -> {
            if (ent.equals(entity) || PlayerUtils.getEntityCuttingOtherEntity(ent).equals(entity)) {
                if (pestsPlotMap.get(plotNumber) > 1) {
                    pestsPlotMap.put(plotNumber, pestsPlotMap.get(plotNumber) - 1);
                    LogUtils.sendDebug("[Pests Destroyer] Removed 1 pest from plot number: " + plotNumber);
                } else {
                    pestsPlotMap.remove(plotNumber);
                    LogUtils.sendDebug("[Pests Destroyer] Removed all pests from plot number: " + plotNumber);
                }
                currentEntityTarget = Optional.empty();
                lastFireworkLocation = Optional.empty();
                lastFireworkTime = 0;
                rotationType = RotationType.NONE;
                KeyBindUtils.stopMovement();
                stuckClock.reset();
                RotationHandler.getInstance().reset();
            }
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
                totalPests = 0;
            }
        }
        if (totalPests == 0) {
            pestsPlotMap.clear();
        }
    }

    @SubscribeEvent
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
    public void onGuiOpen(DrawScreenAfterEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!enabled) return;
        if (!(event.guiScreen instanceof GuiChest)) return;
        String guiName = InventoryUtils.getInventoryName();
        if (guiName == null) return;
        if (!delayClock.passed()) return;
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
                        int plotNumber = PlotUtils.getPLOT_NUMBERS().get(plotCounter);
                        List<String> lore = InventoryUtils.getItemLore(slot.getStack());
                        for (String line : lore) {
                            if (line.contains("This plot has")) {
                                Matcher matcher = pestPatternDeskGui.matcher(line);
                                if (matcher.matches()) {
                                    int pests = Integer.parseInt(matcher.group(1));
                                    pestsPlotMap.put(plotNumber, pests);
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
                    LogUtils.sendError("[Pests Destroyer] Attempting to find pest with tracker");
                    state = States.GET_LOCATION;
                    Multithreading.schedule(() -> {
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                        }
                    }, delay, TimeUnit.MILLISECONDS);
                    return;
                }
            }
        }
        int foundPests = pestsPlotMap.values().stream().mapToInt(i -> i).sum();
        if (foundPests == 0) return;
        for (Map.Entry<Integer, Integer> plot : pestsPlotMap.entrySet()) {
            if (plot.getValue() > 0) {
                LogUtils.sendDebug("Found plot with pests: " + plot.getKey() + " with " + plot.getValue() + " pests");
            }
        }
        if (state == States.WAIT_FOR_INFO) {
            Multithreading.schedule(() -> {
                if (mc.currentScreen != null) {
                    state = States.TELEPORT_TO_PLOT;
                    delayClock.schedule((long) (300 + Math.random() * 300));
                    mc.thePlayer.closeScreen();
                }
            }, 500 + (long) (Math.random() * 500), TimeUnit.MILLISECONDS);
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
        TELEPORT_TO_PLOT,
        WAIT_FOR_TP,
        GET_LOCATION,
        WAIT_FOR_LOCATION,
        FLY_TO_PEST,
        KILL_PEST,
        CHECK_ANOTHER_PEST,
        GO_BACK
    }

    public enum EscapeState {
        NONE,
        GO_TO_HUB,
        GO_TO_GARDEN,
        RESUME_MACRO
    }

    enum RotationType {
        NONE,
        CLOSE,
        MEDIUM,
        FAR
    }
}
