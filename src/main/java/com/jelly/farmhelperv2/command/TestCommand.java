package com.jelly.farmhelperv2.command;


import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.jelly.farmhelperv2.feature.impl.AutoBazaar;

@Command(value = "set")
public class TestCommand {
    @Main
    private void main(){
//        LogUtils.sendDebug("CurrentPlot: " + GameStateHandler.getInstance().getCurrentPlot());
//        AutoBazaar.getInstance().buy("Sugar Cane", 2, false);
        AutoBazaar.getInstance().sell(AutoBazaar.SELL_INVENTORY);
    }

    @SubCommand(aliases = "sf")
    private void stop(){
        AutoBazaar.getInstance().stop();
    }

    @SubCommand(aliases = "one")
    private void one(){
        AutoBazaar.getInstance().buy("Sugar Cane", 1, false);
    }

    @SubCommand(aliases = "two")
    private void two(){
        AutoBazaar.getInstance().buy("Sugar Cane", 16, false);
    }

    @SubCommand(aliases = "three")
    private void three(){
        AutoBazaar.getInstance().sell(AutoBazaar.SELL_INVENTORY);
    }

    @SubCommand(aliases = "four")
    private void four(){
        AutoBazaar.getInstance().sell(AutoBazaar.SELL_INVENTORY, AutoBazaar.SELL_SACK);
    }
}
