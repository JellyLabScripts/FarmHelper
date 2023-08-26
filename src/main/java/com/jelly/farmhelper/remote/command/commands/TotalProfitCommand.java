package com.jelly.farmhelper.remote.command.commands;

import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;


@Command(label = "totalprofit")
public class TotalProfitCommand extends BaseCommand {

    @Override
    public void execute(WebsocketMessage event) {
//        JsonObject data = event.obj;
//        data.addProperty("profit", MacroHandler.isMacroing ? Math.round(ProfitCalculator.realProfit) : 0);
//        data.addProperty("profithr", MacroHandler.isMacroing ? Math.round(ProfitCalculator.getHourProfit(ProfitCalculator.realProfit)) : 0);
//        send(data);
    }
}
