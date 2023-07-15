package com.jelly.farmhelper.features;

import cc.polyfrost.oneconfig.libs.checker.units.qual.N;
import cc.polyfrost.oneconfig.libs.checker.units.qual.Time;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringUtils;
import scala.actors.scheduler.SingleThreadedScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PetSwapper {

    private static Minecraft mc = Minecraft.getMinecraft();
    private static ScheduledExecutorService scheduler;
    public String swappedPet;
    private String originalPet;
    private String boneLore = "Selected Pet: ";
    //credit to Nirox for the clicktypes
    public enum ClickType {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE,
        THROW,
        QUICK_CRAFT,
    }

    public void swapPets(int n) {
        if (Config.swappingPet.equals("")) {
            LogUtils.debugLog("no pet selected");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        MacroHandler.currentMacro.saveLastStateBeforeDisable();
        int randomDelay = ThreadLocalRandom.current().nextInt(900,  1700);
        mc.thePlayer.sendChatMessage("/pets");

        // 1 for swapping pet, 2 and higher for og pet

        if(n == 1) {
            scheduler.schedule(this::clickPet, randomDelay, TimeUnit.MILLISECONDS);
            scheduler.schedule(MacroHandler::enableCurrentMacro,randomDelay + 600, TimeUnit.MILLISECONDS);
        } else if (n > 1) {
            scheduler.schedule(ogPet, randomDelay, TimeUnit.MILLISECONDS);
            scheduler.schedule(MacroHandler::enableCurrentMacro,randomDelay + 600, TimeUnit.MILLISECONDS);
        }
        PlayerUtils.rightClick();
        scheduler.schedule(this::clickPet, randomDelay, TimeUnit.MILLISECONDS);
    }


    private void openPets() {
        int petSlot = PlayerUtils.getSlotForItem("Pets");
        ArrayList<String> petLore = PlayerUtils.getItemLore(mc.thePlayer.openContainer.getSlot(petSlot).getStack());
    }

    private void clickPet() {
        if (mc.thePlayer.openContainer instanceof net.minecraft.inventory.ContainerChest) {
            //get the original pet
            for (ItemStack item : mc.thePlayer.openContainer.getInventory()) {
                if (PlayerUtils.getItemLore(item).contains("Click to despawn!")) {
                    originalPet = StringUtils.stripControlCodes(item.getDisplayName().toLowerCase());
                }
            }
            // swap to the 2nd pet
            if(PlayerUtils.getSlotForItem(Config.swappingPet) > 0) {
                PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotForItem(Config.swappingPet), 0, ClickType.PICKUP.ordinal());
            } else if (PlayerUtils.getSlotForItem("next page") > 0) {
                PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotForItem("next page"), 0, ClickType.PICKUP.ordinal());
                if (PlayerUtils.getSlotForItem(Config.swappingPet) > 0) {
                    PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotForItem(Config.swappingPet), 0, ClickType.PICKUP.ordinal());
                } else {
                    LogUtils.debugLog("Pet not found, make sure you entered the correct name");
                }
            } else {
                LogUtils.debugLog("Pet not found, make sure you entered the correct name");
            }
        } else {
            LogUtils.debugLog("/pets failed due to not being in GUI.");
        }
    }

    Runnable ogPet = () -> {
            if (mc.thePlayer.openContainer instanceof net.minecraft.inventory.ContainerChest) {
                PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotForItem(originalPet));
            }
    };
}
