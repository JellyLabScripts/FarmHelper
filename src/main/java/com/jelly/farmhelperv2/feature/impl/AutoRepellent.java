package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoRepellent implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoRepellent instance;

    public final static Clock repellentFailsafeClock = new Clock();
    private final Pattern repellentRegex = Pattern.compile("(\\d+m\\s)?(\\d+s)");

    public static AutoRepellent getInstance() {
        if (instance == null) {
            instance = new AutoRepellent();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Auto Repellent";
    }

    @Override
    public boolean isRunning() {
        return enabled;
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
        notEnoughCopper = false;
        state = State.NONE;
        LogUtils.sendWarning("[Auto Repellent] Enabled!");
        delay.reset();
        hotbarSlot = -1;
        MacroHandler.getInstance().pauseMacro();
        IFeature.super.start();
    }

    @Override
    public void stop() {
        if (enabled)
            LogUtils.sendWarning("[Auto Repellent] Disabled! Resuming macro...");
        PlayerUtils.closeScreen();
        notEnoughCopper = false;
        state = State.NONE;
        enabled = false;
        Multithreading.schedule(() -> {
            MacroHandler.getInstance().resumeMacro();
            IFeature.super.stop();
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        enabled = false;
        notEnoughCopper = false;
        state = State.NONE;
        if (mc.currentScreen != null)
            PlayerUtils.closeScreen();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoPestRepellent;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    enum State {
        NONE,
        OPEN_DESK,
        OPEN_SKYMART,
        CLICK_REPELLENT,
        CONFIRM_BUY,
        CLOSE_GUI,
        MOVE_REPELLENT_TO_HOTBAR,
        SELECT_REPELLENT,
        USE_REPELLENT,
        WAIT_FOR_REPELLENT,
        END
    }

    @Getter
    private State state = State.NONE;

    enum MoveRepellentState {
        SWAP_REPELLENT_TO_HOTBAR_PICKUP,
        SWAP_REPELLENT_TO_HOTBAR_PUT,
        SWAP_REPELLENT_TO_HOTBAR_PUT_BACK,
        PUT_ITEM_BACK_PICKUP,
        PUT_ITEM_BACK_PUT
    }

    private MoveRepellentState moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
    private int hotbarSlot = -1;

    private boolean enabled = false;
    @Getter
    private boolean notEnoughCopper = false;
    @Getter
    private final Clock delay = new Clock();

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (enabled) return;
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (RotationHandler.getInstance().isRotating()) return;
        if (FarmHelperConfig.pauseAutoPestRepellentDuringJacobsContest && GameStateHandler.getInstance().inJacobContest())
            return;

        if (FarmHelperConfig.pestRepellentType && !InventoryUtils.hasItemInInventory("Pest Repellent MAX") && GameStateHandler.getInstance().getCopper() < 40 && !notEnoughCopper) {
            notEnoughCopper = true;
            LogUtils.sendError("[Auto Repellent] Not enough copper to buy Repellent! Will activate when enough copper is available.");
            return;
        } else if (!FarmHelperConfig.pestRepellentType && !InventoryUtils.hasItemInInventory("Pest Repellent") && GameStateHandler.getInstance().getCopper() < 15 && !notEnoughCopper) {
            notEnoughCopper = true;
            LogUtils.sendError("[Auto Repellent] Not enough copper to buy Repellent! Will activate when enough copper is available.");
            return;
        } else if (notEnoughCopper) {
            if (FarmHelperConfig.pestRepellentType && GameStateHandler.getInstance().getCopper() >= 40) {
                notEnoughCopper = false;
            } else if (!FarmHelperConfig.pestRepellentType && GameStateHandler.getInstance().getCopper() >= 15) {
                notEnoughCopper = false;
            } else {
                return;
            }
        }

        if (GameStateHandler.getInstance().getPestRepellentState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            LogUtils.sendWarning("[Auto Repellent] Activating!");
            KeyBindUtils.stopMovement();
            start();
        }
    }

    @SubscribeEvent
    public void onTickExecution(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!enabled) return;
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("Server closing, stopping Auto Repellent");
            stop();
            return;
        }
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (delay.isScheduled() && !delay.passed()) return;

        switch (state) {
            case NONE:
                KeyBindUtils.stopMovement();
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                if (InventoryUtils.hasItemInInventory(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX")) {
                    state = State.CLOSE_GUI;
                } else {
                    state = State.OPEN_DESK;
                }
                delay.schedule(300 + (long) (Math.random() * 300));
                break;
            case OPEN_DESK:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                KeyBindUtils.stopMovement();
                mc.thePlayer.sendChatMessage("/desk");
                state = State.OPEN_SKYMART;
                delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLOSE_GUI:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                if (InventoryUtils.hasItemInHotbar(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX")) {
                    LogUtils.sendDebug("Repellent in hotbar, selecting repellent");
                    state = State.SELECT_REPELLENT;
                } else {
                    LogUtils.sendDebug("Repellent not in hotbar, moving to hotbar");
                    state = State.MOVE_REPELLENT_TO_HOTBAR;
                }
                delay.schedule(300 + (long) (Math.random() * 300));
                break;
            case MOVE_REPELLENT_TO_HOTBAR:
                switch (moveRepellentState) {
                    case SWAP_REPELLENT_TO_HOTBAR_PICKUP:
                        if (mc.currentScreen == null) {
                            InventoryUtils.openInventory();
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        int repellentSlot = InventoryUtils.getSlotOfItemByHypixelIdInInventory("PEST_REPELLENT", true);
                        if (repellentSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the Pest Repellent! Disabling Auto Repellent until manual check");
                            FarmHelperConfig.autoPestRepellent = false;
                            stop();
                            break;
                        }
                        this.hotbarSlot = repellentSlot;
                        LogUtils.sendDebug("Repellent slot: " + repellentSlot);
                        InventoryUtils.clickSlot(repellentSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PUT;
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case SWAP_REPELLENT_TO_HOTBAR_PUT:
                        if (mc.currentScreen == null) {
                            LogUtils.sendDebug("Inventory is closed! Changing state to: SWAP_REPELLENT_TO_HOTBAR_PICKUP");
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        Slot newSlot = InventoryUtils.getSlotOfId(43);
                        if (newSlot != null && newSlot.getHasStack()) {
                            LogUtils.sendDebug("Slot 43 has item, swapping");
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PUT_BACK;
                        } else {
                            LogUtils.sendDebug("Slot 43 is empty, putting item back");
                            state = State.SELECT_REPELLENT;
                            moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                        }
                        LogUtils.sendDebug("Clicking slot 43");
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case SWAP_REPELLENT_TO_HOTBAR_PUT_BACK:
                        if (mc.currentScreen == null) {
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
                            LogUtils.sendDebug("Inventory is closed! Changing state to: SWAP_REPELLENT_TO_HOTBAR_PICKUP");
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        LogUtils.sendDebug("Clicking hotbarSlot " + hotbarSlot);
                        InventoryUtils.clickSlot(hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                        state = State.SELECT_REPELLENT;
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case PUT_ITEM_BACK_PICKUP:
                        if (mc.currentScreen == null) {
                            InventoryUtils.openInventory();
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        LogUtils.sendDebug("Clicking hotbarSlot " + hotbarSlot);
                        InventoryUtils.clickSlot(hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PUT;
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case PUT_ITEM_BACK_PUT:
                        if (mc.currentScreen == null) {
                            moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                            LogUtils.sendDebug("Inventory is closed! Changing state to: PUT_ITEM_BACK_PICKUP");
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        LogUtils.sendDebug("Clicking slot 43");
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        delay.schedule(3_000);
                        Multithreading.schedule(this::stop, 1_500, TimeUnit.MILLISECONDS);
                        break;
                }
                break;
            case SELECT_REPELLENT:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                int repellentSlot = InventoryUtils.getSlotIdOfItemInHotbar(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX");
                LogUtils.sendDebug("Repellent slot: " + repellentSlot);
                if (repellentSlot == -1 || repellentSlot > 8) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the Pest Repellent! Disabling Auto Repellent until manual check");
                    FarmHelperConfig.autoPestRepellent = false;
                    stop();
                    break;
                }
                mc.thePlayer.inventory.currentItem = repellentSlot;
                state = State.USE_REPELLENT;
                delay.schedule(300 + (long) (Math.random() * 300));
                break;
            case USE_REPELLENT:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                state = State.WAIT_FOR_REPELLENT;
                LogUtils.sendDebug("Using repellent");
                KeyBindUtils.rightClick();
                delay.schedule(2_500 + (long) (Math.random() * 1_000));
                break;
            case OPEN_SKYMART:
            case CLICK_REPELLENT:
            case CONFIRM_BUY:
            case END:
                break;
            case WAIT_FOR_REPELLENT:
                if (!delay.passed()) break;
                LogUtils.sendError("[Auto Repellent] Repellent hasn't been used. Trying to use again.");
                state = State.USE_REPELLENT;
                break;
        }
    }

    @SubscribeEvent
    public void onDrawGui(DrawScreenAfterEvent event) {
        if (!isRunning()) return;
        String guiName = InventoryUtils.getInventoryName();
        if (guiName == null) return;
        if (delay.isScheduled() && !delay.passed()) return;

        switch (state) {
            case OPEN_SKYMART:
                if (guiName.equals("Desk")) {
                    Slot skymartSlot = InventoryUtils.getSlotOfItemInContainer("SkyMart");
                    if (skymartSlot == null) {
                        break;
                    }
                    InventoryUtils.clickContainerSlot(skymartSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                } else if (guiName.equals("SkyMart")) {
                    Slot pestsSlot = InventoryUtils.getSlotOfItemInContainer("Pests");
                    if (pestsSlot == null) {
                        break;
                    }
                    InventoryUtils.clickContainerSlot(pestsSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    state = State.CLICK_REPELLENT;
                } else {
                    state = State.OPEN_DESK;
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_REPELLENT:
                if (guiName.equals("Desk")) {
                    state = State.OPEN_SKYMART;
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                if (!guiName.equals("SkyMart") && !guiName.equals("SkyMart Pests")) {
                    state = State.OPEN_DESK;
                    PlayerUtils.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                Slot repellentSlot = InventoryUtils.getSlotOfItemInContainer(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX");
                if (repellentSlot == null) {
                    break;
                }
                state = State.CONFIRM_BUY;
                InventoryUtils.clickContainerSlot(repellentSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
            case CONFIRM_BUY:
                if (guiName.equals("Confirm")) {
                    Slot confirmSlot = InventoryUtils.getSlotOfItemInContainer("Confirm");
                    if (confirmSlot == null) {
                        break;
                    }
                    state = State.CLOSE_GUI;
                    InventoryUtils.clickContainerSlot(confirmSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                break;
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onChatReceived(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText()); // just to be sure lol
        if (isRunning() && state == State.CONFIRM_BUY) {
            if (message.startsWith("You bought Pest")) {
                state = State.CLOSE_GUI;
                delay.schedule(300 + (long) (Math.random() * 300));
            }
        }
        if (message.startsWith("YUM! Pests will now spawn")) {
            repellentFailsafeClock.schedule(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS) + 5_000);
            GameStateHandler.getInstance().setPestRepellentState(GameStateHandler.BuffState.ACTIVE);
            LogUtils.sendDebug("Repellent used!");
            if (isRunning()) {
                if (this.hotbarSlot == -1) {
                    state = State.NONE;
                    LogUtils.sendWarning("[Auto Repellent] Successfully used Repellent! Resuming macro...");
                    stop();
                } else {
                    state = State.MOVE_REPELLENT_TO_HOTBAR;
                    moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                }
                delay.schedule(2_000);
            }
        } else if (message.startsWith("You already have this effect active!")) {
            Matcher matcher = repellentRegex.matcher(message);
            if (matcher.find()) {
                try {
                    int minutes;
                    int seconds;
                    if (matcher.group(1).contains("m")) {
                        minutes = Integer.parseInt(matcher.group(1).replace("m", "").trim());
                        seconds = Integer.parseInt(matcher.group(2).replace("s", "").trim());
                    } else {
                        minutes = 0;
                        seconds = Integer.parseInt(matcher.group(1).replace("s", "").trim());
                    }

                    long totalMilliseconds = (minutes * 60L + seconds) * 1000 + 5_000;
                    repellentFailsafeClock.schedule(totalMilliseconds);
                    GameStateHandler.getInstance().setPestRepellentState(GameStateHandler.BuffState.FAILSAFE);
                } catch (NumberFormatException e) {
                    LogUtils.sendError("Failed to parse repellent remaining time.");
                    e.printStackTrace();
                }
            } else {
                LogUtils.sendError("Failed to get repellent remaining time.");
            }

            if (isRunning()) {
                state = State.END;
                LogUtils.sendWarning("[Auto Repellent] Already used Repellent! Resuming macro...");
                stop();
            }
        }
    }
}
