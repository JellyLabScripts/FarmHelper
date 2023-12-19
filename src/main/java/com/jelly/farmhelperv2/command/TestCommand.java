package com.jelly.farmhelperv2.command;


import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.jelly.farmhelperv2.feature.impl.NewAutoSprayonator;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.util.LogUtils;

@Command(value = "set")
public class TestCommand {
    @Main
    private void main(){
//        LogUtils.sendDebug("CurrentPlot: " + GameStateHandler.getInstance().getCurrentPlot());
        NewAutoSprayonator.getInstance().start();
    }

    @SubCommand(aliases = "sf")
    private void stop(){
        NewAutoSprayonator.getInstance().stop();
    }
}
