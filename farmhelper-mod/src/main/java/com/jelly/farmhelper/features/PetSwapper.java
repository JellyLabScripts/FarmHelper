package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PetSwapper {

    private static Minecraft mc = Minecraft.getMinecraft();
    public static String swappedPet;
    private static String originalPet;
    private static Clock delay = new Clock();

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
        OPEN_MENU,
        FIND_PET,
        FIND_ORIGINAL_PET,
        TURN_PAGE,
        CLICK_PET,
        CLOSE_MENU

    }

    private static StatePet state = StatePet.OPEN_MENU;
    private static ContainerChest chest;
    private static int petSlot;
    private static int pageSlot;
    private static boolean shouldSwap = false;

    public static void swapPets(String petName) {
try {
        shouldSwap = true;
        switch (state) {
            case OPEN_MENU:
               MacroHandler.disableCurrentMacro(true);
                mc.thePlayer.sendChatMessage("/pets");
                state = StatePet.FIND_ORIGINAL_PET;
                delay.schedule(2000);
                break;
            case FIND_PET:
                if(mc.currentScreen instanceof GuiChest) {
                    if (getSlotForItem(petName.toLowerCase()) > 0) {
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

                break;
            case FIND_ORIGINAL_PET:
                    if (mc.currentScreen instanceof GuiChest) {
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
                PlayerUtils.clickOpenContainerSlot(getSlotForItem(petName.toLowerCase()), 0, ClickType.PICKUP.ordinal());
                state = StatePet.CLOSE_MENU;
                delay.schedule(1000);
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



        if (VisitorsMacro.InJacobContest() && Config.swappingPet != null && shouldSwap) {
            if(delay.isScheduled() && !delay.passed()) {
                LogUtils.debugLog("Waiting on delay " + delay.getRemainingTime() + " ms");
            } else if(delay.isScheduled() && delay.passed()) {
                swapPets(Config.swappingPet.toLowerCase());
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

        if (mc.currentScreen instanceof GuiChest) {
            for (final ItemStack item : mc.thePlayer.openContainer.getInventory()) {
                if (item != null) {
                    if (StringUtils.stripControlCodes(item.getDisplayName()).equalsIgnoreCase(itemName) && mc.thePlayer.openContainer.getInventory().indexOf(item) <= 28) {
                        return mc.thePlayer.openContainer.getInventory().indexOf(item);
                    }
                }
            }
        }
        return -1;
    }



}