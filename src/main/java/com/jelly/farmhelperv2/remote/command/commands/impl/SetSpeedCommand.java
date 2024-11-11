package com.jelly.farmhelperv2.remote.command.commands.impl;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.command.commands.Command;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/*
    Credits to mostly Yuro with few changes by May2Bee for this superb class
*/
@Command(label = "setspeed")
public class SetSpeedCommand extends ClientCommand {
    private static final Clock clock = new Clock();
    private static int speed = -1;
    private static JsonObject data;
    private static boolean enabled = false;
    private static State currentState = State.NONE;
    boolean wasMacroing = false;

    @Override
    public void execute(RemoteMessage message) {
        JsonObject args = message.args;
        speed = args.get("speed").getAsInt();
        data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("speed", speed);

        if (InventoryUtils.getRancherBootSpeed() == -1) {
            data.addProperty("error", "You need to have Rancher's boots equipped for this command to work.");
        } else if (InventoryUtils.getRancherBootSpeed() == speed) {
            data.addProperty("error", "Your Rancher's boots are already at " + speed + " speed.");
        } else {
            for (int i = 36; i < 44; i++) {
                if (ClientCommand.mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
                    LogUtils.sendDebug("Found free slot: " + i);
                    break;
                }
            }
            try {
                enabled = true;
                currentState = State.START;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (ClientCommand.mc.thePlayer == null || ClientCommand.mc.theWorld == null) return;
        if (clock.isScheduled() && !clock.passed()) return;

        switch (currentState) {
            case NONE:
                clock.reset();
                enabled = false;
                break;
            case START:
                if (ClientCommand.mc.thePlayer.inventoryContainer.getSlot(8) == null || !ClientCommand.mc.thePlayer.inventoryContainer.getSlot(8).getStack().getDisplayName().contains("Rancher's Boots")) {
                    disableWithError("You don't wear Rancher's Boots! Disabling...");
                    break;
                }
                wasMacroing = MacroHandler.getInstance().isMacroToggled();
                if (wasMacroing) {
                    MacroHandler.getInstance().pauseMacro();
                }
                currentState = State.TYPE_COMMAND;
                clock.schedule(500);
                break;
            case TYPE_COMMAND:
                mc.thePlayer.sendChatMessage("/setmaxspeed");
                currentState = State.TYPE_IN_SPEED;
                clock.schedule(500);
                break;
            case TYPE_IN_SPEED:
                if (ClientCommand.mc.currentScreen == null) {
                    currentState = State.TYPE_COMMAND;
                    clock.schedule(500);
                    break;
                }
                SignUtils.setTextToWriteOnString(String.valueOf(speed));
                currentState = State.CLOSE_SIGN;
                clock.schedule(500);
                break;
            case CLOSE_SIGN:
                if (ClientCommand.mc.currentScreen == null) {
                    currentState = State.TYPE_IN_SPEED;
                    clock.schedule(500);
                    break;
                }
                SignUtils.confirmSign();
                currentState = State.END;
                clock.schedule(500);
                break;
            case END:
                PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), false, false);
                if (wasMacroing) {
                    MacroHandler.getInstance().resumeMacro();
                }
                LogUtils.sendSuccess("Rancher's Boots speed has been set to " + speed + ".");
                currentState = State.NONE;
                clock.reset();
                enabled = false;
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
                break;
        }
    }

    private void disableWithError(String message) {
        currentState = State.NONE;
        clock.reset();
        enabled = false;
        LogUtils.sendError(message);
        data.addProperty("error", message);
        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (enabled) {
            disableWithError("World change detected! Disabling...");
        }
    }

    public enum State {
        NONE,
        START,
        TYPE_COMMAND,
        TYPE_IN_SPEED,
        CLOSE_SIGN,
        END
    }
}
