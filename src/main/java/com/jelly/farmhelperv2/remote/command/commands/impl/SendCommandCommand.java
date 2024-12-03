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

@Command(label = "sendcommand")
public class SendCommandCommand extends ClientCommand {
    private static final Clock clock = new Clock();
    private static String cmd = "";
    private static JsonObject data;
    private static boolean enabled = false;
    private static State currentState = State.NONE;
    boolean wasMacroing = false;

    @Override
    public void execute(RemoteMessage message) {
        JsonObject args = message.args;
        cmd = args.get("command").getAsString();
        data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("command", cmd);

        try {
            enabled = true;
            currentState = State.START;
            return;
        } catch (Exception e) {
            e.printStackTrace();
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
                wasMacroing = MacroHandler.getInstance().isMacroToggled();
                if (wasMacroing) {
                    MacroHandler.getInstance().pauseMacro();
                }
                currentState = State.TYPE_COMMAND;
                clock.schedule(200);
                break;
            case TYPE_COMMAND:
                mc.thePlayer.sendChatMessage("/" + cmd);
                currentState = State.END;
                clock.schedule(100);
                break;
            case END:
                if (wasMacroing) {
                    MacroHandler.getInstance().resumeMacro();
                }
                LogUtils.sendSuccess("Command sent: " + cmd);
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
        END
    }
}
