package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.InventoryUtils;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    private final Clock autoCookieDelay = new Clock();

    @Override
    public String getName() {
        return "Auto Cookie";
    }

    @Override
    public boolean isEnabled() {
        return autoCookieDelay.isScheduled() && !autoCookieDelay.passed() || enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public void stop() {
        enabled = false;
        mainState = State.GET_COOKIE;
        bazaarState = BazaarState.NONE;
        moveCookieState = MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PICKUP;
        hotbarSlot = -1;
        autoCookieDelay.reset();
        timeoutClock.reset();
    }

    @Override
    public boolean isActivated() {
        return FarmHelperConfig.autoCookie;
    }

    enum State {
        GET_COOKIE,
        MOVE_COOKIE_TO_HOTBAR,
        SELECT_COOKIE,
        LEFT_CLICK_COOKIE,
        CONSUME_COOKIE
    }

    private State mainState = State.GET_COOKIE;

    private void setMainState(State state) {
        mainState = state;
        timeoutClock.schedule(7_500);
    }

    enum BazaarState {
        NONE,
        GO_CENTER,
        GO_BAZAAR,
        OPEN_BAZAAR,
        CLICK_ODDITIES,
        CLICK_COOKIE,
        CLICK_BUY_INSTANTLY,
        CLICK_BUY_ONLY_ONE,
        CLOSE_GUI
    }

    private BazaarState bazaarState = BazaarState.NONE;

    private void setBazaarState(BazaarState state) {
        bazaarState = state;
        timeoutClock.schedule(7_500);
    }

    enum MoveCookieState {
        SWAP_COOKIE_TO_HOTBAR_PICKUP,
        SWAP_COOKIE_TO_HOTBAR_PUT,
        SWAP_COOKIE_TO_HORBAR_PUT_BACK,
        PUT_ITEM_BACK_PICKUP,
        PUT_ITEM_BACK_PUT
    }

    private MoveCookieState moveCookieState = MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PICKUP;
    private int hotbarSlot = -1;

    private void setMoveCookieState(MoveCookieState state) {
        moveCookieState = state;
        timeoutClock.schedule(7_500);
    }

    private final BlockPos[] hubWaypoints = new BlockPos[]{
            new BlockPos(-3, 69, -77),
            new BlockPos(-31, 69, -77)
    };

    private final Clock timeoutClock = new Clock();

    public void enable() {
        enabled = true;
        LogUtils.sendWarning("Auto Cookie is now enabled!");
        autoCookieDelay.reset();
        timeoutClock.schedule(7_500);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroing()) return;
        if (!isActivated()) return;

        if (GameStateHandler.getInstance().isCookieBuffActive() && mainState == State.GET_COOKIE) {
            if (autoCookieDelay.isScheduled() && !autoCookieDelay.passed()) {
                // waiting
            } else if (autoCookieDelay.isScheduled() && autoCookieDelay.passed()) {
                // should resume
                stop();
            }
            return;
        }

        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.LOBBY) {
            if (!enabled && !autoCookieDelay.isScheduled()) {
                LogUtils.sendWarning("Your Cookie Buff is not active! Activating Auto Cookie in 1.5 second!");
                autoCookieDelay.schedule(1_500);
                return;
            } else if (!enabled && !autoCookieDelay.passed()) {
                return;
            } else if (!enabled && autoCookieDelay.passed()) {
                enable();
            }
        }

        if (!enabled) return;
        if (autoCookieDelay.isScheduled() && !autoCookieDelay.passed()) return;

        switch (mainState) {
            case GET_COOKIE:
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

                switch (bazaarState) {
                    case NONE:
                        mc.thePlayer.sendChatMessage("/hub");
                        setBazaarState(BazaarState.GO_CENTER);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case GO_CENTER:
                        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.HUB) break;

                        break;
                    case GO_BAZAAR:
                        break;
                    case OPEN_BAZAAR:
                        break;
                    case CLICK_ODDITIES:
                        break;
                    case CLICK_COOKIE:
                        break;
                    case CLICK_BUY_INSTANTLY:
                        break;
                    case CLICK_BUY_ONLY_ONE:
                        break;
                    case CLOSE_GUI:
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
                        int cookieSlot = InventoryUtils.getSlotOfItemInInventory("Booster Cookie");
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
                            setMoveCookieState(MoveCookieState.SWAP_COOKIE_TO_HORBAR_PUT_BACK);
                        } else {
                            setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PICKUP);
                            setMainState(State.SELECT_COOKIE);
                        }
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case SWAP_COOKIE_TO_HORBAR_PUT_BACK:
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
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setMoveCookieState(MoveCookieState.PUT_ITEM_BACK_PUT);
                        autoCookieDelay.schedule(getRandomDelay());
                        break;
                    case PUT_ITEM_BACK_PUT:
                        if (mc.currentScreen == null) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                            stop();
                            break;
                        }
                        InventoryUtils.clickSlot(this.hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setMoveCookieState(MoveCookieState.SWAP_COOKIE_TO_HOTBAR_PICKUP);
                        setMainState(State.GET_COOKIE);
                        autoCookieDelay.schedule(3_000);
                        break;
                }
                break;
            case SELECT_COOKIE:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    autoCookieDelay.schedule(getRandomDelay());
                    break;
                }
                int cookieSlot = InventoryUtils.getSlotOfItemInHotbar("Booster Cookie");
                LogUtils.sendDebug("Cookie Slot: " + cookieSlot);
                if (cookieSlot == -1 || cookieSlot > 8) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the cookie!");
                    stop();
                    break;
                }
                mc.thePlayer.inventory.currentItem = cookieSlot;
                setMainState(State.LEFT_CLICK_COOKIE);
                autoCookieDelay.schedule(getRandomDelay());
                break;
            case LEFT_CLICK_COOKIE:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    autoCookieDelay.schedule(getRandomDelay());
                    break;
                }
                KeyBindUtils.leftClick();
                setMainState(State.CONSUME_COOKIE);
                autoCookieDelay.schedule(getRandomDelay());
                break;
            case CONSUME_COOKIE:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Consume Booster Cookie?"))
                    break;

                int slotOfConsumeCookie = InventoryUtils.getContainerSlotOf("Consume Cookie");
                if (slotOfConsumeCookie == -1) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the consume cookie!");
                    stop();
                    break;
                }

                InventoryUtils.clickSlot(slotOfConsumeCookie, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setMainState(State.MOVE_COOKIE_TO_HOTBAR);
                autoCookieDelay.schedule(getRandomDelay());
                break;
        }

        if (timeoutClock.isScheduled() && timeoutClock.passed()) {
            LogUtils.sendWarning("Auto Cookie got stuck, restarting process!");
            stop();
            enabled = true;
            timeoutClock.schedule(7_500);
            if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                autoCookieDelay.schedule(7_500);
                mc.thePlayer.sendChatMessage("/skyblock");
            } else {
                autoCookieDelay.schedule(getRandomDelay());
            }
        }
    }

    private int getRandomDelay() {
        return (int) (Math.random() * 1_000 + 500);
    }
}
