package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
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
    private final Pattern repellentRegex = Pattern.compile("(\\d+)m\\s(\\d+)s");

    private int savedSlot = 0;

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
        savedSlot = mc.thePlayer.inventory.currentItem;
        LogUtils.sendWarning("[Auto Repellent] Enabled!");
        delay.reset();
        if (MacroHandler.getInstance().isMacroToggled())
            MacroHandler.getInstance().pauseMacro();
    }

    @Override
    public void stop() {
        if (enabled)
            LogUtils.sendWarning("[Auto Repellent] Disabled! Resuming macro in 1.5 seconds...");
        enabled = false;
        notEnoughCopper = false;
        state = State.NONE;
        if (mc.currentScreen != null)
            mc.thePlayer.closeScreen();
        if (MacroHandler.getInstance().isMacroToggled())
            Multithreading.schedule(() -> {
                if (MacroHandler.getInstance().isMacroToggled()) {
                    MacroHandler.getInstance().resumeMacro();
                }
            }, 1_500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        enabled = false;
        notEnoughCopper = false;
        state = State.NONE;
        if (mc.currentScreen != null)
            mc.thePlayer.closeScreen();
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
    private boolean notEnoughCopper = false;
    private final Clock delay = new Clock();

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (enabled) return;
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (FarmHelperConfig.pestRepellentType && GameStateHandler.getInstance().getCopper() < 40 && !notEnoughCopper) {
            notEnoughCopper = true;
            LogUtils.sendError("[Auto Repellent] Not enough copper to buy Repellent! Will activate when enough copper is available.");
            return;
        } else if (!FarmHelperConfig.pestRepellentType && GameStateHandler.getInstance().getCopper() < 15 && !notEnoughCopper) {
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
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (delay.isScheduled() && !delay.passed()) return;

        switch (state) {
            case NONE:
                KeyBindUtils.stopMovement();
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
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
                    mc.thePlayer.closeScreen();
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
                    mc.thePlayer.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                if (InventoryUtils.hasItemInHotbar(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX")) {
                    state = State.SELECT_REPELLENT;
                } else {
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
                        int cookieSlot = InventoryUtils.getSlotIdOfItemInInventory("Pest Repellent");
                        if (cookieSlot == -1) {
                            LogUtils.sendError("Something went wrong while trying to get the slot of the Pest Repellent! Restarting...");
                            stop();
                            enabled = true;
                            break;
                        }
                        this.hotbarSlot = cookieSlot;
                        InventoryUtils.clickSlot(cookieSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PUT;
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case SWAP_REPELLENT_TO_HOTBAR_PUT:
                        if (mc.currentScreen == null) {
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        Slot newSlot = InventoryUtils.getSlotOfId(43);
                        if (newSlot != null && newSlot.getHasStack()) {
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PUT_BACK;
                        } else {
                            state = State.SELECT_REPELLENT;
                            moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                        }
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case SWAP_REPELLENT_TO_HOTBAR_PUT_BACK:
                        if (mc.currentScreen == null) {
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
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
                        InventoryUtils.clickSlot(hotbarSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PUT;
                        delay.schedule(300 + (long) (Math.random() * 300));
                        break;
                    case PUT_ITEM_BACK_PUT:
                        if (mc.currentScreen == null) {
                            moveRepellentState = MoveRepellentState.SWAP_REPELLENT_TO_HOTBAR_PICKUP;
                            delay.schedule(300 + (long) (Math.random() * 300));
                            break;
                        }
                        InventoryUtils.clickSlot(43, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        delay.schedule(3_000);
                        Multithreading.schedule(this::stop, 1_500, TimeUnit.MILLISECONDS);
                        break;
                }
                break;
            case SELECT_REPELLENT:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                int repellentSlot = InventoryUtils.getSlotIdOfItemInHotbar(!FarmHelperConfig.pestRepellentType ? "Pest Repellent" : "Pest Repellent MAX");
                LogUtils.sendDebug("Repellent slot: " + repellentSlot);
                if (repellentSlot == -1 || repellentSlot > 8) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of the Pest Repellent! Restarting...");
                    stop();
                    enabled = true;
                    break;
                }
                mc.thePlayer.inventory.currentItem = repellentSlot;
                state = State.USE_REPELLENT;
                delay.schedule(300 + (long) (Math.random() * 300));
                break;
            case USE_REPELLENT:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                state = State.WAIT_FOR_REPELLENT;
                KeyBindUtils.rightClick();
                delay.schedule(300 + (long) (Math.random() * 300));
                break;
            case OPEN_SKYMART:
            case CLICK_REPELLENT:
            case CONFIRM_BUY:
            case WAIT_FOR_REPELLENT:
            case END:
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
                if (!guiName.equals("Desk")) {
                    state = State.OPEN_DESK;
                    mc.thePlayer.closeScreen();
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                Slot skymartSlot = InventoryUtils.getSlotOfItemInContainer("SkyMart");
                if (skymartSlot == null) {
                    break;
                }
                InventoryUtils.clickContainerSlot(skymartSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                state = State.CLICK_REPELLENT;
                delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_REPELLENT:
                if (guiName.equals("Desk")) {
                    state = State.OPEN_SKYMART;
                    delay.schedule(300 + (long) (Math.random() * 300));
                    break;
                }
                if (!guiName.equals("SkyMart")) {
                    state = State.OPEN_DESK;
                    mc.thePlayer.closeScreen();
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
                    state = State.CLOSE_GUI;
                    InventoryUtils.clickContainerSlot(InventoryUtils.getSlotOfItemInContainer("Confirm").slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isRunning()) return;
        String message = event.message.getUnformattedText();
        if (state == State.CONFIRM_BUY) {
            if (message.startsWith("You bought Pest")) {
                state = State.CLOSE_GUI;
                delay.schedule(300 + (long) (Math.random() * 300));
            }
        } else if (state == State.WAIT_FOR_REPELLENT) {
            if (message.startsWith("YUM! Pests will now spawn")) {
                repellentFailsafeClock.schedule(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
                LogUtils.sendDebug("Repellent used!");
                if (this.hotbarSlot == -1) {
                    state = State.NONE;
                    LogUtils.sendWarning("[Auto Repellent] Successfully used Repellent! Resuming macro in 1.5 seconds...");
                    Multithreading.schedule(this::stop, 1_500, TimeUnit.MILLISECONDS);
                } else {
                    state = State.MOVE_REPELLENT_TO_HOTBAR;
                    moveRepellentState = MoveRepellentState.PUT_ITEM_BACK_PICKUP;
                }
                delay.schedule(2_000);
            } else if (message.startsWith("You already have this effect active!")) {
                state = State.END;
                Matcher matcher = repellentRegex.matcher(message);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));

                    long totalMilliseconds = (minutes * 60L + seconds) * 1000;
                    repellentFailsafeClock.schedule(totalMilliseconds);
                } else {
                    LogUtils.sendError("Failed to get repellent remaining time.");
                }

                LogUtils.sendWarning("[Auto Repellent] Already used Repellent! Resuming macro in 1.5 seconds...");
                Multithreading.schedule(() -> {
                    mc.thePlayer.inventory.currentItem = savedSlot;
                    savedSlot = 0;
                }, 200 + (long) (Math.random() * 200), TimeUnit.MILLISECONDS);
                Multithreading.schedule(this::stop, 1_500, TimeUnit.MILLISECONDS);
            }
        }
    }
}
