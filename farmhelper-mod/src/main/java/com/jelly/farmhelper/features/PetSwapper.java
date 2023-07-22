package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import jdk.internal.net.http.common.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import scala.util.control.TailCalls;

import java.util.ArrayList;

public class PetSwapper {

    private static Minecraft mc = Minecraft.getMinecraft();
    public static String swappedPet;
    private static String originalPet;
    private static Clock delay = new Clock();
    private static int ogPetPage = -1;

    //credit to Nirox for the clicktypes
    public enum ClickType {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE,
        THROW,
        QUICK_CRAFT
    }

    public enum StatePet {
        STOP_MACRO,
        OPEN_MENU,
        FIND_PET,
        FIND_ORIGINAL_PET,
        TURN_PAGE,
        CLICK_PET,
        CLOSE_MENU

    }

    private static StatePet state = StatePet.STOP_MACRO;
    private static ContainerChest gui;
    private static IInventory guiInv;
    private static ContainerChest chest;
    private static ArrayList<String> ogPetArray;
    private static int petSlot;
    private static int pageSlot;
    private static boolean shouldSwap = true;

    public static void swapPets(String petName) {
try {
        shouldSwap = true;
        switch (state) {
            case STOP_MACRO:
                if (MacroHandler.isMacroing) {
                    MacroHandler.disableCurrentMacro(true);
                    LogUtils.debugLog("Stopped macro.");
                    state = StatePet.OPEN_MENU;
                    delay.schedule(700);
                } else {
                    state = StatePet.CLOSE_MENU;
                    LogUtils.debugLog("How did u get here :sus:");
                    delay.schedule(800);
                }
                break;
            case OPEN_MENU:
               MacroHandler.disableCurrentMacro(true);
                mc.thePlayer.sendChatMessage("/pets");
                state = StatePet.FIND_ORIGINAL_PET;
                delay.schedule(2000);
                break;
            case FIND_PET:
                if(mc.currentScreen instanceof GuiChest) {
                    if (ogPetPage == -1) {
                        if (getSlotForItem(petName.toLowerCase()) <= 28) {
                            petSlot = getSlotForItem(petName.toLowerCase());
                            delay.schedule(1000);
                            state = StatePet.CLICK_PET;
                            delay.schedule(1000);
                        } else {
                            state = StatePet.TURN_PAGE;
                            delay.schedule(1000);
                        }
                    } else {
                        LogUtils.debugLog("PetSwapper: not in Pets Gui, disabling...");
                        state = StatePet.CLOSE_MENU;
                        delay.schedule(1500);
                    }
                } else if (ogPetPage > 0) {


                }
                break;
            case FIND_ORIGINAL_PET:
                    if (mc.currentScreen instanceof GuiChest) {
                        if (mc.thePlayer.openContainer instanceof ContainerChest) {
                            gui = (ContainerChest) mc.thePlayer.openContainer;
                            guiInv = gui.getLowerChestInventory();
                            if (guiInv.getDisplayName().getUnformattedText().equalsIgnoreCase("Pets")) {
                                ogPetPage = 1;
                            } else {
                               ogPetArray = PlayerUtils.getItemLore(mc.thePlayer.openContainer.inventoryItemStacks.get(getSlotForItem("next page")));
                               for (String line : ogPetArray) {
                                   if (line.contains("Page")) {
                                       ogPetPage = line.indexOf("page" + 1);
                                   } else {
                                       LogUtils.debugLog("Didnt find the next page for original pet?");
                                       state = StatePet.FIND_PET;
                                       delay.schedule(1200);
                                   }
                               }
                            }
                        }

                        for (ItemStack ogP : mc.thePlayer.openContainer.getInventory()) {
                            if (ogP != null && mc.thePlayer.openContainer.inventoryItemStacks.indexOf(ogP) <= 28) {
                                if (PlayerUtils.getItemLore(ogP).contains(EnumChatFormatting.getTextWithoutFormattingCodes("Click to despawn!"))) {
                                        originalPet = ogP.getDisplayName();
                                        state = StatePet.FIND_PET;
                                        delay.schedule(1300);
                                        break;
                                }
                            }

                        }
                    } else {
                        state = StatePet.CLOSE_MENU;
                        delay.schedule(1300);
                        LogUtils.debugLog("StateFindOriginalPet was not in pets menu.");
                    }
                break;
            case TURN_PAGE:
                if (getSlotForItem("next page") > 0) {
                    pageSlot = getSlotForItem("next page");
                    PlayerUtils.clickOpenContainerSlot(pageSlot, ClickType.PICKUP.ordinal());
                    delay.schedule(1000);
                    state = StatePet.FIND_PET;
                } else {
                    delay.schedule(1000);
                    state = StatePet.CLOSE_MENU;
                    LogUtils.debugLog("Couldn't find next page or the pet, make sure you have the pet and there are no spelling errors.");
                }
                break;
            case CLICK_PET:
                PlayerUtils.clickOpenContainerSlot(getSlotForItem(petName.toLowerCase()));
                LogUtils.debugLog(getSlotForItem(petName.toLowerCase()) + " is the slot");
                delay.schedule(1000);
                state = StatePet.CLOSE_MENU;
                break;
            case CLOSE_MENU:
                if (mc.thePlayer.openContainer != null) mc.thePlayer.closeScreen();
                shouldSwap = false;
                state = StatePet.OPEN_MENU;
                MacroHandler.toggleMacro();
                LogUtils.debugLog("Closed the Pet GUI");
                break;

        } } catch (Exception q) {
    q.printStackTrace();
        }
    }

    private int myFinder(String name) {

        for (ItemStack item : mc.thePlayer.openContainer.inventoryItemStacks) {
            if (item != null) {
                if (StringUtils.stripControlCodes(item.getDisplayName().toLowerCase()).contains(name.toLowerCase())) {
                    return mc.thePlayer.openContainer.inventoryItemStacks.indexOf(item);
                }
            }

        }
        return -1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (mc.thePlayer.openContainer != null && FarmHelper.config.switchPet ) {
                if (mc.thePlayer.openContainer instanceof ContainerChest) {
                    ContainerChest gui = (ContainerChest) mc.thePlayer.openContainer;
                    IInventory guiInv = gui.getLowerChestInventory();
                    if (guiInv.getDisplayName() != null) {
                        mc.thePlayer.addChatMessage(guiInv.getDisplayName());
                    }
                }

        }


        if (VisitorsMacro.InJacobContest() && Config.swappingPet != null && shouldSwap) {
                    if(!delay.isScheduled()) {
                        swapPets(Config.swappingPet);
                        LogUtils.debugLog("1st clause, current state is" + state.name());
                    } else if (delay.isScheduled() && !delay.passed()) {
                        LogUtils.debugLog("Waiting on delay + " + delay.getRemainingTime() + " ms");
                        LogUtils.debugLog("2nd clause, current state is" + state.name());
                    } else if (delay.isScheduled() && delay.passed()) {
                        swapPets(Config.swappingPet);
                        LogUtils.debugLog("3rd clause, current state is" + state.name());

                    }

        } else if (!VisitorsMacro.InJacobContest() && !shouldSwap) {
            if (originalPet != null) {
                swapPets(originalPet);
            } else {
                LogUtils.debugLog("Original Pet not found. sorry uwu");
            }
            shouldSwap = true;
        }

    }

    private static int getSlotForItem(final String itemName) {

   /*     if (mc.thePlayer.openContainer instanceof net.minecraft.inventory.ContainerChest) {
            for (final ItemStack item : mc.thePlayer.openContainer.getInventory()) {
                if (item != null) {
                    if (StringUtils.stripControlCodes(item.getDisplayName()).equalsIgnoreCase(itemName) && mc.thePlayer.openContainer.getInventory().indexOf(item) > 0) {
                        return mc.thePlayer.openContainer.getInventory().indexOf(item);
                    }
                }
            }
        }
        return -1; */


        //thanks osama :pray:

        for (ItemStack itemStack : mc.thePlayer.openContainer.getInventory()) {
            if (itemStack == null) continue;
            if (itemStack.getDisplayName().toLowerCase().contains(itemName) ) {
                return  mc.thePlayer.openContainer.getInventory().indexOf(itemStack);
            }
        }
        return -1;
    }

    private void switchPage(int pageNum) {
        int pageSlot;
        ArrayList<String> pageArray;
        if (mc.thePlayer.openContainer instanceof ContainerChest) {
            pageSlot = getSlotForItem("next page");
           pageArray = PlayerUtils.getItemLore(mc.thePlayer.openContainer.inventoryItemStacks.get(getSlotForItem("next page")));
           for (String line : pageArray) {
               if (line.contains("Page")) {
                   if (pageNum == (Integer) line.indexOf("Page" + 1) - 1) {
                    // do nothing
                   } else if (pageNum != (Integer) line.indexOf("Page" + 1) - 1) {
                       PlayerUtils.clickOpenContainerSlot(pageSlot);
                   }
               }
           }

        }
    }
}