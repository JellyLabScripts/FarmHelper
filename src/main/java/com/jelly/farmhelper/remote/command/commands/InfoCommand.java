package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.struct.BaseCommand;
import com.jelly.farmhelper.remote.struct.Command;
import com.jelly.farmhelper.remote.struct.RemoteMessage;

import static com.jelly.farmhelper.utils.LogUtils.getRuntimeFormat;

@Command(label = "info")
public class InfoCommand extends BaseCommand {
    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();

        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("runtime", getRuntimeFormat());
        data.addProperty("totalProfit", ProfitCalculator.profit);
        data.addProperty("profitPerHour", ProfitCalculator.profitHr);
        data.addProperty("cropType", String.valueOf(MacroHandler.crop == null ? "None" : MacroHandler.crop));
        data.addProperty("currentState", String.valueOf(MacroHandler.currentMacro == null ? "Macro is not running" : MacroHandler.currentMacro.currentState));
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("image", getScreenshot());

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}
