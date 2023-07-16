package com.jelly.farmhelper.commands;

import com.jelly.farmhelper.features.PetSwapper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import static com.jelly.farmhelper.features.PetSwapper.StatePet.*;

public class TestCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "petswap";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fhrewarp [add|remove|removeall]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {

        PetSwapper.StatePet state = OPEN_MENU;

        switch(args[0]) {

            case "OPEN_MENU":
                state = OPEN_MENU;
                        break;
            case "FIND_PET":
                state = FIND_PET;
                break;
            case "TURN_PAGE":
                state = TURN_PAGE;
                break;
            case "CLICK_PET":
                state = CLICK_PET;
                break;
            case "CLOSE_MENU":
                state = CLOSE_MENU;
                break;




        }
        PetSwapper petSwapper = new PetSwapper();
        petSwapper.swapPets();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }

}
