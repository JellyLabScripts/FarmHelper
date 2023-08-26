package com.jelly.farmhelper.remote.command.commands;

import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;


@Command(label = "toggle")
public class ToggleCommand extends BaseCommand {

    @Override
    public void execute(WebsocketMessage message) {
//        JsonObject data = event.obj;
//        if (nullCheck() || MacroHandler.isMacroing) {
//            MacroHandler.toggleMacro();
//            data.addProperty("embed", toJson(embed()
//                    .setDescription("Toggled Farm Helper " + (MacroHandler.isMacroing ? "on" : "off"))));
//            data.addProperty("image", getScreenshot());
//        } else {
//            data.addProperty("embed", toJson(embed()
//                    .setDescription("I'm not in a world, therefore I can't toggle macro")));
//        }
//        send(data);
    }
}