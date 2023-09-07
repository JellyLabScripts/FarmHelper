package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Command(label = "setspeed")
public class SetSpeedCommand extends BaseCommand {
    private static int speed = -1;
    private static JsonObject data;

    @Override
    public void execute(WebsocketMessage message) {
        JsonObject args = message.args;
        speed = args.get("speed").getAsInt();
        data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("speed", speed);

        if (PlayerUtils.getRancherBootSpeed() == -1) {
            data.addProperty("error", "You need to have Rancher's boots equipped for this command to work.");
        } else if (PlayerUtils.getRancherBootSpeed() == speed) {
            data.addProperty("error", "Your Rancher's boots are already at " + speed + " speed.");
        } else {
            try {
                enabled = true;
                currentState = State.START;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }

    private static boolean enabled = false;
    private static State currentState = State.NONE;
    private static final Clock clock = new Clock();
    int freeSlot = -1;
    boolean wasMacroing = false;
    public enum State {
        NONE,
        START,
        OPEN_INVENTORY,
        TAKE_OFF_BOOTS,
        CLOSE_INVENTORY,
        HOLD_RANCHER_BOOTS,
        CLICK_RANCHER_BOOTS,
        TYPE_IN_SPEED,
        OPEN_INVENTORY_AGAIN,
        PUT_ON_BOOTS,
        CLOSE_INVENTORY_AGAIN,
        END
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (clock.isScheduled() && !clock.passed()) return;

        switch (currentState) {
            case NONE:
                clock.reset();
                enabled = false;
                break;
            case START:
                if (mc.thePlayer.inventory.getStackInSlot(8) == null || !mc.thePlayer.inventory.getStackInSlot(8).getDisplayName().equals("Rancher boots")) {
                    disableWithError("You don't wear Rancher's boots! Disabling...");
                    break;
                }
                wasMacroing = MacroHandler.isMacroing;
                if (wasMacroing) {
                    MacroHandler.disableCurrentMacro();
                    MacroHandler.isMacroing = false;
                }
                currentState = State.OPEN_INVENTORY;
                clock.schedule(500);
                break;
            case OPEN_INVENTORY:
                PlayerUtils.openInventory();
                freeSlot = -1;
                currentState = State.TAKE_OFF_BOOTS;
                clock.schedule(500);
                break;
            case TAKE_OFF_BOOTS:
                if (mc.thePlayer.openContainer == null) {
                    currentState = State.OPEN_INVENTORY;
                    clock.schedule(500);
                    break;
                }
                for (int i = 36; i < 44; i++) {
                    if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                        LogUtils.sendDebug("Found free slot: " + i);
                        freeSlot = i;
                        break;
                    }
                }
                if (freeSlot == -1) {
                    disableWithError("You don't have any free slot in your hotbar! Disabling...");
                    break;
                }
                PlayerUtils.clickOpenContainerSlot(8);
                PlayerUtils.clickOpenContainerSlot(freeSlot);
                currentState = State.CLOSE_INVENTORY;
                clock.schedule(500);
                break;
            case CLOSE_INVENTORY:
                mc.currentScreen = null;
                currentState = State.HOLD_RANCHER_BOOTS;
                clock.schedule(500);
                break;
            case HOLD_RANCHER_BOOTS:
                if (mc.currentScreen != null) {
                    currentState = State.CLOSE_INVENTORY;
                    clock.schedule(500);
                    break;
                }
                mc.thePlayer.inventory.currentItem = freeSlot;
                currentState = State.CLICK_RANCHER_BOOTS;
                clock.schedule(500);
                break;
            case CLICK_RANCHER_BOOTS:
                if (mc.thePlayer.inventory.currentItem != freeSlot) {
                    currentState = State.HOLD_RANCHER_BOOTS;
                    clock.schedule(500);
                    break;
                }
                if (mc.thePlayer.inventory.getStackInSlot(freeSlot) == null) {
                    currentState = State.HOLD_RANCHER_BOOTS;
                    clock.schedule(500);
                    break;
                }
                KeyBindUtils.leftClick();
                currentState = State.TYPE_IN_SPEED;
                clock.schedule(500);
                break;
            case TYPE_IN_SPEED:
                if (mc.currentScreen == null) {
                    currentState = State.CLICK_RANCHER_BOOTS;
                    clock.schedule(500);
                    break;
                }
                Utils.signText = String.valueOf(speed);
                mc.currentScreen = null;
                currentState = State.OPEN_INVENTORY_AGAIN;
                clock.schedule(500);
                break;
            case OPEN_INVENTORY_AGAIN:
                if (mc.thePlayer.openContainer != null) {
                    currentState = State.TYPE_IN_SPEED;
                    clock.schedule(500);
                    break;
                }
                PlayerUtils.openInventory();
                currentState = State.PUT_ON_BOOTS;
                clock.schedule(500);
                break;
            case PUT_ON_BOOTS:
                if (mc.thePlayer.openContainer == null) {
                    currentState = State.OPEN_INVENTORY_AGAIN;
                    clock.schedule(500);
                    break;
                }
                PlayerUtils.clickOpenContainerSlot(freeSlot);
                PlayerUtils.clickOpenContainerSlot(8);
                currentState = State.CLOSE_INVENTORY_AGAIN;
                clock.schedule(500);
                break;
            case CLOSE_INVENTORY_AGAIN:
                mc.currentScreen = null;
                currentState = State.END;
                clock.schedule(500);
                break;
            case END:
                PlayerUtils.getFarmingTool(MacroHandler.crop, false, false);
                if (wasMacroing) {
                    MacroHandler.isMacroing = true;
                    MacroHandler.enableCurrentMacro();
                }
                LogUtils.sendSuccess("Rancher's boots speed has been set to " + speed + ".");
                currentState = State.NONE;
                clock.reset();
                enabled = false;
                break;
        }
    }

    private void disableWithError(String message) {
        currentState = State.NONE;
        clock.reset();
        enabled = false;
        LogUtils.sendError(message);
        data.addProperty("error", message);
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }
}
