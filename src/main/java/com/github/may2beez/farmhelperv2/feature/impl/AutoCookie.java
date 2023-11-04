package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
public class AutoCookie implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoCookie instance;

    public static AutoCookie getInstance() {
        if (instance == null) {
            instance = new AutoCookie();
        }
        return instance;
    }

    @Setter
    private boolean enabled;
    @Setter
    private boolean activating;

    private final Clock autoCookieDelay = new Clock();
    private final Clock dontEnableClock = new Clock();

    @Override
    public String getName() {
        return "Auto Cookie";
    }

    @Override
    public boolean isRunning() {
        return enabled || activating;
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
        enabled = true;
        LogUtils.sendWarning("[Auto Cookie] Enabled!");
        autoCookieDelay.reset();
        setMainState(State.GET_COOKIE);
        timeoutClock.schedule(7_500);
    }

    @Override
    public void stop() {
        if (enabled)
            LogUtils.sendWarning("[Auto Cookie] Disabled!");
        enabled = false;
        mainState = State.NONE;
        bazaarState = BazaarState.NONE;
        moveCookieState = MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PICKUP;
        hotbarSlot = -1;
        activating = false;
        autoCookieDelay.reset();
        timeoutClock.reset();
        mc.thePlayer.closeScreen();
        KeyBindUtils.stopMovement();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        dontEnableClock.reset();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoCookie;
    }

    enum State {
        NONE,
        GET_COOKIE,
        MOVE_COOKIE_TO_HOTBAR,
        SELECT_COOKIE,
        RIGHT_CLICK_COOKIE,
        CONSUME_COOKIE,
        WAIT_FOR_CONSUME
    }

    private State mainState = State.NONE;

    private void setMainState(State state) {
        mainState = state;
        timeoutClock.schedule(7_500);
    }

    enum BazaarState {
        NONE,
        GO_BAZAAR,
        OPEN_BAZAAR,
        CLICK_ODDITIES,
        CLICK_COOKIE,
        CLICK_BUY_INSTANTLY,
        CLICK_BUY_ONLY_ONE,
        WAIT_FOR_COOKIE_BUY,
        CLOSE_GUI,
        TELEPORT_TO_GARDEN
    }

    private BazaarState bazaarState = BazaarState.NONE;

    private void setBazaarState(BazaarState state) {
        bazaarState = state;
        timeoutClock.schedule(30_000);
    }

    enum MoveCookieState {
        SWAP_COOKIE_TO_HOTBAR_PICKUP,
        SWAP_COOKIE_TO_HOTBAR_PUT,
        SWAP_COOKIE_TO_HOTBAR_PUT_BACK,
        PUT_ITEM_BACK_PICKUP,
        PUT_ITEM_BACK_PUT
    }

    private MoveCookieState moveCookieState = MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PICKUP;
    private int hotbarSlot = -1;

    private void setMoveCookieState(MoveCookieState state) {
        moveCookieState = state;
        timeoutClock.schedule(7_500);
    }

    private final BlockPos hubWaypoint = new BlockPos(-31, 69, -77);

    private final Clock timeoutClock = new Clock();

    private final RotationUtils rotation = new RotationUtils();

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (isRunning()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.LOBBY && GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            if (!enabled && !activating && (!dontEnableClock.isScheduled() || dontEnableClock.passed())) {
                LogUtils.sendWarning("[Auto Cookie] Your Cookie Buff is not active! Activating Auto Cookie in 1.5 second!");
                activating = true;
                KeyBindUtils.stopMovement();
                Multithreading.schedule(() -> {
                    if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
                        start();
                        activating = false;
                    }
                }, 1_500, TimeUnit.MILLISECONDS);
                dontEnableClock.reset();
            }
        }
    }

    @SubscribeEvent
    public void onTickUpdate(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (!isEnabled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.ACTIVE && mainState == State.NONE || (dontEnableClock.isScheduled() && !dontEnableClock.passed())) {
            return;
        }

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
            timeoutClock.schedule(bazaarState != BazaarState.NONE ? 30_000 : 7_500);
            return;
        }

        if (autoCookieDelay.isScheduled() && !autoCookieDelay.passed()) return;

        switch (mainState) {
            case NONE:
                break;
            case GET_COOKIE:
                switch (bazaarState) {
                    case NONE:
                        if (InventoryUtils.hasItemInHotbar("Booster Cookie")) {
                            setMainState(State.SELECT_COOKIE);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        if (InventoryUtils.hasItemInInventory("Booster Cookie")) {
                            setMainState(State.MOVE_COOKIE_TO_HOTBAR);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        mc.thePlayer.sendChatMessage("/hub");
                        setBazaarState(BazaarState.GO_BAZAAR);
                        autoCookieDelay.schedule(3_000);
                        break;
                    case GO_BAZAAR:
                        if (mc.currentScreen != null) {
                            KeyBindUtils.stopMovement();
                            mc.thePlayer.closeScreen();
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }

                        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.HUB) break;

                        if (Math.sqrt(mc.thePlayer.getDistanceSqToCenter(hubWaypoint)) < 1) {
                            KeyBindUtils.stopMovement();
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }

                        if (rotation.rotating) break;

                        RotationUtils.Rotation rotationNeeded2 = AngleUtils.getRotation(new Vec3(hubWaypoint.up().up()).add(new Vec3(0.5, 0.5, 0.5)), true);
                        if (AngleUtils.smallestAngleDifference(mc.thePlayer.rotationYaw, rotationNeeded2.getYaw()) > 4 ||
                                AngleUtils.smallestAngleDifference(mc.thePlayer.rotationPitch, rotationNeeded2.getPitch()) > 4) {
                            rotation.easeTo(rotationNeeded2.getYaw(), rotationNeeded2.getPitch(), FarmHelperConfig.getRandomRotationTime());
                            break;
                        }

                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindSprint);
                        break;
                    case OPEN_BAZAAR:
                        if (mc.currentScreen != null) {
                            mc.thePlayer.closeScreen();
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }

                        if (rotation.rotating) break;

                        Optional<Entity> bazaarNpc = mc.theWorld.loadedEntityList.stream().filter(entity -> {
                            double distance = Math.sqrt(entity.getDistanceSqToCenter(mc.thePlayer.getPosition()));
                            String name = StringUtils.stripControlCodes(entity.getCustomNameTag());
                            return distance < 4.5 && name != null && name.equals("Bazaar");
                        }).findFirst();

                        if (!bazaarNpc.isPresent()) {
                            LogUtils.sendDebug("Can't find Bazaar npc, waiting for it");
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        RotationUtils.Rotation rotationNeeded3 = AngleUtils.getRotation(bazaarNpc.get(), true);

                        if (AngleUtils.smallestAngleDifference(mc.thePlayer.rotationYaw, rotationNeeded3.getYaw()) > 5 ||
                                AngleUtils.smallestAngleDifference(mc.thePlayer.rotationPitch, rotationNeeded3.getPitch()) > 5) {
                            KeyBindUtils.stopMovement();
                            rotation.easeTo(rotationNeeded3.getYaw(), rotationNeeded3.getPitch(), FarmHelperConfig.getRandomRotationTime());
                            break;
                        }

                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, bazaarNpc.get());
                        setBazaarState(BazaarState.CLICK_ODDITIES);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case CLICK_ODDITIES:
                        if (mc.currentScreen == null) {
                            autoCookieDelay.schedule(getRandomDelay());
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            break;
                        }
                        if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().startsWith("Bazaar ➜ Oddities")) {
                            setBazaarState(BazaarState.CLICK_COOKIE);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().startsWith("Bazaar")) {
                            LogUtils.sendError("Something went wrong while opening bazaar, trying to open again!");
                            autoCookieDelay.schedule(getRandomDelay() * 2L);
                            mc.thePlayer.closeScreen();
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            break;
                        }
                        int odditiesSlot = InventoryUtils.getSlotIdOfItemInContainer("Oddities");
                        if (odditiesSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the oddities!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickContainerSlot(odditiesSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setBazaarState(BazaarState.CLICK_COOKIE);
                        autoCookieDelay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    case CLICK_COOKIE:
                        if (mc.currentScreen == null) {
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }

                        if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().startsWith("Bazaar ➜") && !InventoryUtils.getInventoryName().startsWith("Bazaar ➜ Oddities")) {
                            setBazaarState(BazaarState.CLICK_ODDITIES);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }

                        if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().startsWith("Bazaar ➜ Oddities")) {
                            LogUtils.sendError("Something went wrong while opening bazaar, trying to open again!");
                            autoCookieDelay.schedule(getRandomDelay() * 2L);
                            mc.thePlayer.closeScreen();
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            break;
                        }

                        int cookieSlot = InventoryUtils.getSlotIdOfItemInContainer("Booster Cookie");
                        if (cookieSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickContainerSlot(cookieSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setBazaarState(BazaarState.CLICK_BUY_INSTANTLY);
                        autoCookieDelay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    case CLICK_BUY_INSTANTLY:
                        if (mc.currentScreen == null) {
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().startsWith("Oddities ➜") && !InventoryUtils.getInventoryName().startsWith("Oddities ➜ Booster Cookie")) {
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            autoCookieDelay.schedule(getRandomDelay());
                            mc.thePlayer.closeScreen();
                            LogUtils.sendError("Something went wrong while trying to buy the cookie, trying to buy again!");
                            break;
                        }

                        int buyInstantlySlot = InventoryUtils.getSlotIdOfItemInContainer("Buy Instantly");
                        if (buyInstantlySlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the buy instantly!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickContainerSlot(buyInstantlySlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setBazaarState(BazaarState.CLICK_BUY_ONLY_ONE);
                        autoCookieDelay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    case CLICK_BUY_ONLY_ONE:
                        if (mc.currentScreen == null) {
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        if (InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().startsWith("Booster Cookie ➜") && !InventoryUtils.getInventoryName().startsWith("Booster Cookie ➜ Instant Buy")) {
                            setBazaarState(BazaarState.OPEN_BAZAAR);
                            autoCookieDelay.schedule(getRandomDelay());
                            mc.thePlayer.closeScreen();
                            LogUtils.sendError("Something went wrong while trying to buy the cookie, trying to buy again!");
                            break;
                        }

                        int buyOnlyOneSlot = InventoryUtils.getSlotIdOfItemInContainer("Buy only one!");
                        if (buyOnlyOneSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the buy only one!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickContainerSlot(buyOnlyOneSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setBazaarState(BazaarState.WAIT_FOR_COOKIE_BUY);
                        autoCookieDelay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    case WAIT_FOR_COOKIE_BUY:
                        // waiting for buy
                        break;
                    case CLOSE_GUI:
                        if (mc.currentScreen == null) {
                            mc.thePlayer.sendChatMessage("/warp garden");
                            setBazaarState(BazaarState.TELEPORT_TO_GARDEN);
                            autoCookieDelay.schedule(3_000);
                            break;
                        }
                        mc.thePlayer.closeScreen();
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case TELEPORT_TO_GARDEN:
                        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) {
                            setBazaarState(BazaarState.CLOSE_GUI);
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        if (dontEnableClock.isScheduled()) {
                            Multithreading.schedule(this::stop, 3_000, TimeUnit.MILLISECONDS);
                            return;
                        } else if (InventoryUtils.hasItemInHotbar("Booster Cookie")) {
                            setMainState(State.SELECT_COOKIE);
                        } else if (InventoryUtils.hasItemInInventory("Booster Cookie")) {
                            setMainState(State.MOVE_COOKIE_TO_HOTBAR);
                        } else {
                            LogUtils.sendError("Something went wrong while trying to get the cookie!");
                            stop();
                            break;
                        }
                        setBazaarState(BazaarState.NONE);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                }

                break;
            case MOVE_COOKIE_TO_HOTBAR:
                switch (moveCookieState) {
                    case SWAP_COOKIE_TO_HOTBAR_PICKUP:
                        if (mc.currentScreen == null) {
                            InventoryUtils.openInventory();
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        int cookieSlot = InventoryUtils.getSlotIdOfItemInInventory("Booster Cookie");
                        if (cookieSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        this.hotbarSlot = cookieSlot;
                        InventoryUtils.clickSlot(cookieSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setMoveCookieState(MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PUT);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case SWAP_COOKIE_TO_HOTBAR_PUT:
                        if (mc.currentScreen == null) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        Slot newSlot = InventoryUtils.getSlotOfId(43);
                        if (newSlot != null && newSlot.getHasStack()) {
                            setMoveCookieState(MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PUT_BACK);
                        } else {
                            setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PICKUP);
                            setMainState(State.SELECT_COOKIE);
                        }
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case SWAP_COOKIE_TO_HOTBAR_PUT_BACK:
                        if (mc.currentScreen == null) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickSlot(this.hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PICKUP);
                        setMainState(State.SELECT_COOKIE);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case PUT_ITEM_BACK_PICKUP:
                        if (mc.currentScreen == null) {
                            InventoryUtils.openInventory();
                            autoCookieDelay.schedule(getRandomDelay());
                            break;
                        }
                        InventoryUtils.clickSlot(this.hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PUT);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case PUT_ITEM_BACK_PUT:
                        if (mc.currentScreen == null) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        autoCookieDelay.schedule(3_000);
                        Multithreading.schedule(this::stop, 1_500, TimeUnit.MILLISECONDS);
                        break;
                }
                break;
            case SELECT_COOKIE:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    autoCookieDelay.schedule(getRandomDelay());
                    break;
                }
                int cookieSlot = InventoryUtils.getSlotIdOfItemInHotbar("Booster Cookie");
                LogUtils.sendDebug("Cookie Slot: " + cookieSlot);
                if (cookieSlot == -1 || cookieSlot > 8) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                    stop();
                    break;
                }
                mc.thePlayer.inventory.currentItem = cookieSlot;
                setMainState(State.RIGHT_CLICK_COOKIE);
                autoCookieDelay.schedule(getRandomDelay());
                break;
            case RIGHT_CLICK_COOKIE:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    autoCookieDelay.schedule(getRandomDelay());
                    break;
                }
                KeyBindUtils.rightClick();
                setMainState(State.CONSUME_COOKIE);
                autoCookieDelay.schedule(3_000);
                break;
            case CONSUME_COOKIE:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Consume Booster Cookie?"))
                    break;

                int slotOfConsumeCookie = InventoryUtils.getSlotIdOfItemInContainer("Consume Cookie");
                if (slotOfConsumeCookie == -1) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the consume cookie!");
                    stop();
                    break;
                }

                InventoryUtils.clickContainerSlot(slotOfConsumeCookie, InventoryUtils.ClickType.RIGHT, InventoryUtils.ClickMode.PICKUP);
                setMainState(State.WAIT_FOR_CONSUME);
                autoCookieDelay.schedule(getRandomDelay());
                break;
            case WAIT_FOR_CONSUME:
                // waiting for consume
                break;
        }

        if (timeoutClock.isScheduled() && timeoutClock.passed()) {
            LogUtils.sendWarning("Auto Cookie got stuck, restarting process!");
            stop();
            enabled = true;
            timeoutClock.schedule(bazaarState != BazaarState.NONE ? 15_000 : 7_500);
            if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                autoCookieDelay.schedule(7_500);
                mc.thePlayer.sendChatMessage("/skyblock");
            } else {
                autoCookieDelay.schedule(getRandomDelay());
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMessageReceived(ClientChatReceivedEvent event) {
        if (!isRunning()) return;
        if (event.type != 0) return;

        String message = event.message.getUnformattedText().trim();
        if (message.contains(":")) return;

        if (mainState == State.WAIT_FOR_CONSUME) {
            if (message.startsWith("You consumed a Booster Cookie!")) {
                LogUtils.sendDebug("Successfully consumed a cookie!");
                if (this.hotbarSlot == -1) {
                    setMainState(State.NONE);
                } else {
                    setMainState(State.MOVE_COOKIE_TO_HOTBAR);
                    setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PICKUP);
                }
                autoCookieDelay.schedule(getRandomDelay());
//                Multithreading.schedule(this::stop, 3_000, TimeUnit.MILLISECONDS);
            }
        }

        if (mainState == State.GET_COOKIE && bazaarState == BazaarState.WAIT_FOR_COOKIE_BUY) {
            if (message.startsWith("[Bazaar] Bought 1x Booster Cookie for")) {
                LogUtils.sendWarning("Successfully bought a cookie!");
                setBazaarState(BazaarState.CLOSE_GUI);
                autoCookieDelay.schedule(getRandomDelay());
            } else if (message.startsWith("[Bazaar] You cannot afford this!")) {
                LogUtils.sendError("You cannot afford a cookie! Disabling this feature for next 30 minutes");
                dontEnableClock.schedule(30_000 * 60);
                setBazaarState(BazaarState.CLOSE_GUI);
            }
        }
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating && mc.currentScreen == null) {
            rotation.update();
        }
    }

    private int getRandomDelay() {
        return (int) (Math.random() * 1_000 + 500);
    }
}
