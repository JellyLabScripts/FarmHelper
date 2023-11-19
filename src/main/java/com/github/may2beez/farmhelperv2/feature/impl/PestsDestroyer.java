package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.SpawnParticleEvent;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.handler.RotationHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import com.github.may2beez.farmhelperv2.util.helper.Target;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PestsDestroyer implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static PestsDestroyer instance;

    public static PestsDestroyer getInstance() {
        if (instance == null) {
            instance = new PestsDestroyer();
        }
        return instance;
    }

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
    @Setter
    public static class Pest {
        int amount;
        int plotNumber;

        public Pest(int amount, int plotNumber) {
            this.amount = amount;
            this.plotNumber = plotNumber;
        }

        public void addPest() {
            amount++;
        }

        @Override
        public String toString() {
            return "Pest{" +
                    "amount=" + amount +
                    ", plotNumber=" + plotNumber +
                    '}';
        }
    }

    @Getter
    private final ArrayList<Pest> pestsMap = new ArrayList<>();

    @Getter
    private final ArrayList<Entity> pestsLocations = new ArrayList<>();

    @Getter
    private Optional<Pest> currentTarget = Optional.empty();

    @Getter
    private Optional<Entity> currentEntityTarget = Optional.empty();

    @Getter
    private int amountOfPests = 0;

    private boolean enabled = false;

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

    private boolean preparing = false;

    @Override
    public void start() {
        if (enabled) return;
        if (!mc.thePlayer.capabilities.allowFlying) {
            LogUtils.sendError("[Pests Destroyer] You need to be able to fly!");
            FarmHelperConfig.enablePestsDestroyer = false;
            return;
        }
        preparing = true;
        currentTarget = Optional.empty();
        lastFireworkLocation = Optional.empty();
        lastFireworkTime = 0;
        state = States.IDLE;
        delayClock.reset();
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::clearSavedState);
            KeyBindUtils.stopMovement();
        }
        LogUtils.sendWarning("[Pests Destroyer] Starting killing shitters!");
        Multithreading.schedule(() -> {
            enabled = true;
            preparing = false;
        }, 800 + (long) (Math.random() * 500), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (enabled || preparing)
            LogUtils.sendWarning("[Pests Destroyer] Stopping!");
        enabled = false;
        preparing = false;
        KeyBindUtils.stopMovement();
    }

    public void toggle() {
        if (enabled) {
            stop();
        } else {
            start();
        }
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enablePestsDestroyer;
    }

    private final Pattern pestPattern = Pattern.compile("GROSS! A Pest has appeared in Plot - (\\d+)!");

    enum States {
        IDLE,
        TELEPORT_TO_PLOT,
        WAIT_FOR_TP,
        GET_LOCATION,
        WAIT_FOR_LOCATION,
        FLY_TO_PEST,
        KILL_PEST,
        CHECK_ANOTHER_PEST,
        GO_BACK
    }

    @Getter
    private States state = States.IDLE;

    private Optional<BlockPos> preTpBlockPos = Optional.empty();

    @Getter
    private final Clock delayClock = new Clock();

    public boolean canEnableMacro() {
        if (!isToggled()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (!MacroHandler.getInstance().isMacroToggled()) return false;
        if (enabled || preparing) return false;
        if (amountOfPests < FarmHelperConfig.startKillingPestsAt) return false;

        if (PlayerUtils.isStandingOnRewarpLocation()) {
            System.out.println(BlockUtils.isAboveHeadClear());
            if ((BlockUtils.isAboveHeadClear() && !pestsLocations.isEmpty()) || !pestsMap.isEmpty()) {
                start();
            }
        }

        return PlayerUtils.isStandingOnSpawnPoint();
    }

    @SubscribeEvent
    public void onTickExecute(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!enabled) return;

        System.out.println("State: " + state + " delay: " + delayClock.getRemainingTime());

        if (delayClock.isScheduled() && !delayClock.passed()) return;

        switch (state) {
            case IDLE:
                if (amountOfPests == 0) {
                    if (preparing) return;
                    preparing = true;
                    Multithreading.schedule(() -> {
                        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                        Multithreading.schedule(() -> {
                            stop();
                            MacroHandler.getInstance().resumeMacro();
                        }, 1_000, TimeUnit.MILLISECONDS);
                    }, 500, TimeUnit.MILLISECONDS);
                    return;
                }
                ItemStack currentItem = mc.thePlayer.getHeldItem();
                if (currentItem == null || !currentItem.getDisplayName().contains("Vacuum")) {
                    int vacuum = InventoryUtils.getSlotIdOfItemInHotbar("Vacuum");
                    if (vacuum == -1) {
                        LogUtils.sendError("[Pests Destroyer] Failed to find vacuum in hotbar!");
                        state = States.IDLE;
                        return;
                    }
                    mc.thePlayer.inventory.currentItem = vacuum;
                    delayClock.schedule((long) (500 + Math.random() * 500));
                    return;
                }

                if (mc.thePlayer.onGround) {
                    fly();
                    delayClock.schedule(350);
                    return;
                }

                if (pestsMap.isEmpty()) {
                    currentTarget = Optional.empty();
                    state = States.GET_LOCATION;
                } else {
                    KeyBindUtils.stopMovement();
                    Pest pest = pestsMap.get(0);
                    currentTarget = Optional.of(pest);
                    state = States.TELEPORT_TO_PLOT;
                    delayClock.schedule((long) (1_500 + Math.random() * 1_000));
                }
                break;
            case TELEPORT_TO_PLOT:
                if (!currentTarget.isPresent()) {
                    state = States.IDLE;
                    return;
                }
                Pest pest = currentTarget.get();
                int plotNumber = pest.getPlotNumber();
                preTpBlockPos = Optional.of(mc.thePlayer.getPosition());
                mc.thePlayer.sendChatMessage("/tptoplot " + plotNumber);
                state = States.WAIT_FOR_TP;
                delayClock.schedule((long) (1_500 + Math.random() * 1_000));
                break;
            case WAIT_FOR_TP:
                if (!preTpBlockPos.isPresent()) {
                    state = States.IDLE;
                    return;
                }
                if (!mc.thePlayer.getPosition().equals(preTpBlockPos.get())) {
                    state = States.GET_LOCATION;
                    delayClock.schedule((long) (500 + Math.random() * 1_000));
                    return;
                } else {
                    delayClock.schedule((long) (500 + Math.random() * 1_000));
                }
                break;
            case GET_LOCATION:
                KeyBindUtils.stopMovement();

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                state = States.WAIT_FOR_LOCATION;
                lastFireworkLocation = Optional.empty();
                KeyBindUtils.leftClick();
                break;
            case WAIT_FOR_LOCATION:
                if (RotationHandler.getInstance().isRotating()) return;

                if (!pestsLocations.isEmpty()) {
                    state = States.FLY_TO_PEST;
                    break;
                }

                if (lastFireworkLocation.isPresent()) {
                    if (lastFireworkTime + 350 < System.currentTimeMillis()) {
                        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                                new Target(new Vec3(lastFireworkLocation.get().xCoord, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), lastFireworkLocation.get().zCoord)),
                                FarmHelperConfig.getRandomRotationTime(),
                                () -> state = States.FLY_TO_PEST
                        ));
                        delayClock.schedule(300);
                    }
                }
                break;
            case FLY_TO_PEST:
                if (mc.thePlayer.onGround) {
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

                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(closestPest.getPositionEyes(1)),
                        FarmHelperConfig.getRandomRotationTime(),
                        null
                ));
                state = States.KILL_PEST;
                delayClock.schedule(300);
                break;
            case KILL_PEST:
                if (!currentEntityTarget.isPresent()) {
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }
                Entity entity = currentEntityTarget.get();
                if (entity.isDead) {
                    state = States.CHECK_ANOTHER_PEST;
                    return;
                }
                double distance = mc.thePlayer.getDistance(entity.posX, mc.thePlayer.posY, entity.posZ);
                if (distance > 2 || !canEntityBeSeenIgnoreNonCollidable(entity)) {
                    if (!mc.thePlayer.capabilities.isFlying) {
                        fly();
                        delayClock.schedule(350);
                        return;
                    }
                    if (distance < 8) {
                        System.out.println(entity.posY + entity.getEyeHeight() + 3 - mc.thePlayer.posY);
                        if ((entity.posY + entity.getEyeHeight() + 3 - mc.thePlayer.posY > 3 || objectsInFrontOfPlayer()) && mc.thePlayer.capabilities.isFlying) {
                            LogUtils.sendDebug("Holding jump");
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem, mc.gameSettings.keyBindJump);
                            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false), (long) (150 + Math.random() * 100), TimeUnit.MILLISECONDS);
                        } else if (entity.posY + entity.getEyeHeight() + 3 - mc.thePlayer.posY < 3 && mc.thePlayer.capabilities.isFlying) {
                            LogUtils.sendDebug("Holding sneak");
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem, mc.gameSettings.keyBindSneak);
                            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false), (long) (150 + Math.random() * 100), TimeUnit.MILLISECONDS);
                        } else {
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem);
                        }
                    }
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            new Target(entity.getPositionEyes(1)),
                            entityTeleported(entity) ? getRandomRotationTime() * 4 : getRandomRotationTime(),
                            null
                    ));
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, objectsInFrontOfPlayer() && mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindJump : null);
                    return;
                } else {
                    System.out.println(entity.posY + entity.getEyeHeight() + 3 - mc.thePlayer.posY);
                    if ((entity.posY + 3 - mc.thePlayer.posY > 3 || objectsInFrontOfPlayer()) && mc.thePlayer.capabilities.isFlying) {
                        LogUtils.sendDebug("Holding jump");
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem, mc.gameSettings.keyBindJump);
                        Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false), (long) (150 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    } else if (entity.posY + entity.getEyeHeight() + 3 - mc.thePlayer.posY < 3 && mc.thePlayer.capabilities.isFlying) {
                        LogUtils.sendDebug("Holding sneak");
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem, mc.gameSettings.keyBindSneak);
                        Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false), (long) (150 + Math.random() * 100), TimeUnit.MILLISECONDS);
                    } else {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindUseItem);
                    }

                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            new Target(entity.getPositionEyes(1)),
                            entityTeleported(entity) ? FarmHelperConfig.getRandomRotationTime() * 2 : FarmHelperConfig.getRandomRotationTime(),
                            null
                    ));
                }
                break;
            case CHECK_ANOTHER_PEST:
                if (pestsLocations.isEmpty()) {
                    state = States.GO_BACK;
                } else {
                    state = States.IDLE;
                }
                KeyBindUtils.stopMovement();
                delayClock.schedule(1_500 + (long) (Math.random() * 1_000));
                break;
            case GO_BACK:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delayClock.schedule(500 + (long) (Math.random() * 500));
                    return;
                }
                MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                Multithreading.schedule(() -> {
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                    Multithreading.schedule(() -> {
                        stop();
                        MacroHandler.getInstance().resumeMacro();
                    }, 1_000, TimeUnit.MILLISECONDS);
                }, 500, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private boolean entityTeleported(Entity entity) {
        Vec3 currentPos = entity.getPositionVector();
        Vec3 prevPos = new Vec3(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
        return currentPos.distanceTo(prevPos) > 1;
    }

    private void fly() {
        mc.thePlayer.jump();
        Multithreading.schedule(() -> {
            if (!mc.thePlayer.onGround && !mc.thePlayer.capabilities.isFlying && mc.thePlayer.capabilities.allowFlying) {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }
        }, 300, TimeUnit.MILLISECONDS);
        delayClock.schedule(400);
    }

    private boolean objectsInFrontOfPlayer() {
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 playerLook = mc.thePlayer.getLookVec();
        // look 5 blocks in front of the player's feet and head level
        Vec3 lookAtFeet = playerPos.addVector(playerLook.xCoord * 8, 0, playerLook.zCoord * 8);
        Vec3 lookAtHead = playerPos.addVector(playerLook.xCoord * 8, 1.5, playerLook.zCoord * 8);
        // check if there is a block in the way
        MovingObjectPosition feetRayTrace = mc.theWorld.rayTraceBlocks(new Vec3(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord), lookAtFeet, false, true, false);
        MovingObjectPosition headRayTrace = mc.theWorld.rayTraceBlocks(new Vec3(playerPos.xCoord, playerPos.yCoord + 1.5, playerPos.zCoord), lookAtHead, false, true, false);
        return (feetRayTrace != null && feetRayTrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) || (headRayTrace != null && headRayTrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK);
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
        int plotNumber = -1;
        try {
            plotNumber = Integer.parseInt(plot);
        } catch (Exception e) {
            LogUtils.sendError("[Pests Destroyer] Failed to parse plot number: " + plot);
            return;
        }
        int finalPlotNumber = plotNumber;
        Optional<Pest> pestOptional = pestsMap.stream().filter(p -> p.plotNumber == finalPlotNumber).findFirst();
        if (pestOptional.isPresent()) {
            Pest pest1 = pestOptional.get();
            pest1.addPest();
            pestsMap.set(pestsMap.indexOf(pest1), pest1);
            LogUtils.sendDebug("PlotNumber: " + finalPlotNumber + " count: " + pest1.amount);
            return;
        }
        Pest newPest = new Pest(1, finalPlotNumber);
        pestsMap.add(newPest);
        LogUtils.sendDebug("PlotNumber: " + finalPlotNumber + " count: " + newPest.amount);
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
                        RenderUtils.drawText(pestName, entity.posX, entity.posY + entity.getEyeHeight() + 0.65 + 0.5, entity.posZ, 1 + (float) Math.min((distance / 20), 3));
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

        for (Pest pest : pestsMap) {
            int plotNumber = pest.getPlotNumber();
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotBasedOnNumber(plotNumber);
            AxisAlignedBB boundingBox = new AxisAlignedBB(chunks.get(0).getFirst() * 16, 66, chunks.get(0).getSecond() * 16, chunks.get(chunks.size() - 1).getFirst() * 16 + 16, 80, chunks.get(chunks.size() - 1).getSecond() * 16 + 16);
            double d0 = Minecraft.getMinecraft().getRenderManager().viewerPosX;
            double d1 = Minecraft.getMinecraft().getRenderManager().viewerPosY;
            double d2 = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
            float centerX = (float) (boundingBox.minX + (boundingBox.maxX - boundingBox.minX) / 2);
            float centerZ = (float) (boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) / 2);
            boundingBox = boundingBox.offset(-d0, -d1, -d2);
            RenderUtils.drawBox(boundingBox, FarmHelperConfig.plotHighlightColor.toJavaColor());
            int numberOfPests = pest.getAmount();
            RenderUtils.drawText("Pests in plot: " + numberOfPests, centerX, 80, centerZ, 1);
        }
    }

    private long getRandomRotationTime() {
        return (long) (150 + Math.random() * 150);
    }

    private boolean canEntityBeSeenIgnoreNonCollidable(Entity entity) {
        Vec3 vec3 = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Vec3 vec31 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        return mc.theWorld.rayTraceBlocks(vec31, vec3, false, true, false) == null;
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        Entity entity = event.entity;
        LogUtils.sendDebug("[Pests Destroyer] Entity died: " + entity.getName() + " at: " + entity.getPosition());
        currentEntityTarget.ifPresent(ent -> {
            if (ent.equals(entity) || PlayerUtils.getEntityCuttingOtherEntity(ent).equals(entity)) {
                currentEntityTarget = Optional.empty();
                KeyBindUtils.stopMovement();
            }
        });
        int plotNumber = PlotUtils.getPlotNumberBasedOnLocation(entity.getPosition());
        if (plotNumber == -1) {
            LogUtils.sendError("[Pests Destroyer] Failed to get plot number for entity: " + entity.getName() + " at: " + entity.getPosition());
            return;
        }
        Optional<Pest> pestOptional = pestsMap.stream().filter(p -> p.plotNumber == plotNumber).findFirst();
        if (pestOptional.isPresent()) {
            Pest pest = pestOptional.get();
            pest.amount--;
            if (pest.amount <= 0) {
                pestsMap.remove(pest);
            }
        }
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
                    if (pestsAmount != amountOfPests) {
                        amountOfPests = pestsAmount;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (line.contains("Garden") || line.contains("Plot")) {
                amountOfPests = 0;
            }
        }
        if (amountOfPests == 0) {
            pestsMap.clear();
        }
    }

    private Optional<Vec3> lastFireworkLocation = Optional.empty();
    private long lastFireworkTime = 0;

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

        lastFireworkLocation = Optional.of(event.getPos());
        lastFireworkTime = System.currentTimeMillis();
        LogUtils.sendDebug("Firework at: " + event.getPos());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        pestsMap.clear();
    }
}
