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
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PetSwapper {

    private static Minecraft mc = Minecraft.getMinecraft();
    public String swappedPet;
    private String originalPet;
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
        TURN_PAGE,
        CLICK_PET,
        CLOSE_MENU

    }

    private static StatePet state = StatePet.OPEN_MENU;
    private static ContainerChest chest;
    private static int petSlot;
    private int pageSlot;
    private boolean shouldSwap = false;

    public void swapPets() {
try {
        shouldSwap = true;
        switch (state) {
            case OPEN_MENU:
               MacroHandler.disableCurrentMacro(true);
                mc.thePlayer.sendChatMessage("/pets");
                PlayerUtils.rightClick();
                state = StatePet.FIND_PET;
                delay.schedule(2000);
                break;
            case FIND_PET:
                System.out.println(" i dont");

              /*  int i;
                for (ItemStack item : mc.thePlayer.openContainer.getInventory()) {
                 //  System.out.println(mc.thePlayer.openContainer.getSlot(i).getStack().getDisplayName());
                    if (item != null) {
                        if (item.getDisplayName().contains("elephant")) {
                            System.out.println("fuck ye");
                            petSlot = mc.thePlayer.openContainer.getInventory().indexOf(item);
                            state = StatePet.CLICK_PET;
                            delay.schedule(1200);
                            break;
                        }
                    } */
                if(mc.currentScreen instanceof GuiChest) {
                    if (getSlotForItem(Config.swappingPet.toLowerCase()) > 0) {
                        petSlot = getSlotForItem(Config.swappingPet.toLowerCase());
                        delay.schedule(1000);
                        state = StatePet.CLICK_PET;
                        //  delay.schedule(1000);
                    } else {
                        state = StatePet.TURN_PAGE;
                        delay.schedule(1000);
                    }
                } else {
                   LogUtils.debugLog("");
                }

                break;
            case TURN_PAGE:
                System.out.println(" i may");
                if (getSlotForItem("next page") > 0) {
                    pageSlot = getSlotForItem("next page");
                    PlayerUtils.clickOpenContainerSlot(pageSlot, ClickType.PICKUP.ordinal());
                    delay.schedule(1000);
                    state = StatePet.FIND_PET;
                } else {
                    delay.schedule(1000);
                    state = StatePet.CLOSE_MENU;
                    MacroHandler.toggleMacro();
                    LogUtils.debugLog("Couldn't find next page or the pet, make sure you have the pet and there are no spelling errors.");
                }
                break;
            case CLICK_PET:

                PlayerUtils.clickOpenContainerSlot(getSlotForItem(Config.swappingPet.toLowerCase()), 0, ClickType.PICKUP.ordinal());
                state = StatePet.CLOSE_MENU;
                delay.schedule(1000);
                break;
            case CLOSE_MENU:
                if (mc.thePlayer.openContainer != null) mc.thePlayer.closeScreen();
                shouldSwap = false;
                state = StatePet.OPEN_MENU;
                // MacroHandler.toggle();
                LogUtils.debugLog("Closed the Pet GUI");
                break;

        } } catch (Exception q) {
    q.printStackTrace();
        }
    }

   /* private void clickPet() {
        if (mc.thePlayer.openContainer instanceof net.minecraft.inventory.ContainerChest) {
            //get the original pet
           /* for (ItemStack item : mc.thePlayer.openContainer.getInventory()) {
                if (PlayerUtils.getItemLore(item).contains("Click to despawn!")) {
                    originalPet = StringUtils.stripControlCodes(item.getDisplayName().toLowerCase());
                }
            }
            // swap to the 2nd pet
            for (int i = 0; i < mc.thePlayer.openContainer.getInventory().size(); i++) {
                if (StringUtils.stripControlCodes(mc.thePlayer.openContainer.getSlot(i).getStack().getDisplayName()).contains(Config.swappingPet)) {
                    PlayerUtils.clickOpenContainerSlot(i, 0, ClickType.PICKUP.ordinal());
                }

            }
            if (PlayerUtils.getSlotForItem(Config.swappingPet) > 0) {
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
        }
    }


    Runnable ogPet = () -> {
        if (mc.thePlayer.openContainer instanceof net.minecraft.inventory.ContainerChest) {
            PlayerUtils.clickOpenContainerSlot(getSlotForItem("mooshroom cow"), 0, ClickType.PICKUP.ordinal());
            //for some reason PlayerUtils getSlotForItem doesnt work
        }
    }; */

    //^ old code / dont work


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
                swapPets();
            }

        } else if (!VisitorsMacro.InJacobContest() && !shouldSwap) {
            shouldSwap = true;
        }

        /*
        if (delay.isScheduled() && !delay.passed() && shouldSwap) {
                System.out.println(getSlotForItem("next page"));
        } else if (delay.isScheduled() && delay.passed() &&shouldSwap) {
            System.out.println(state);
            swapPets();
        } */
            //^ testing code


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