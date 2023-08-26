package com.jelly.farmhelper.remote.command.commands;

import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.Clock;

@Command(label = "reconnect")
public class ReconnectCommand extends BaseCommand {
    public static boolean isEnabled = false;

    public static Clock reconnectClock = new Clock();

    @Override
    public void execute(WebsocketMessage event) {
//        JsonObject obj = event.obj;
//        if (nullCheck()) {
//            long ms = obj.get("reconnectTime").getAsLong();
//            reconnectClock.schedule(ms);
//            mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Reconnecting in " + formatTime(ms)));
//            isEnabled = true;
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("I disconnected, reconnecting in " + formatTime(ms))));
//        } else {
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("Can't reconnect since I was already disconnected")));
//            obj.addProperty("image", getScreenshot());
//
//        }
//        send(obj);
    }

    public void cancel(WebsocketMessage event) {
//        JsonObject obj = event.obj;
//        if (nullCheck()) {
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("I'm already connected, can't cancel reconnect.")));
//        } else if (reconnectClock.isScheduled()) {
//            reconnectClock.reset();
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("Reconnecting...")));
//        } else {
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("Can't reconnect, I was not disconnected by the bot. It's probably because " +
//                            (BanwaveChecker.banwaveOn ?
//                            "there's a banwave going on" :
//                        FailsafeNew.isJacobFailsafeExceeded ?
//                                "there's a Jacob Contest":
//                                "of a disconnection error?"))));
//            obj.addProperty("image", getScreenshot());
//        }
//        send(obj);
    }
}
