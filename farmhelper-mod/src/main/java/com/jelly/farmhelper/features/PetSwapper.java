package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

import static com.jelly.farmhelper.utils.LogUtils.debugLog;

public class PetSwapper {

    final static Minecraft mc = Minecraft.getMinecraft();
    public static boolean toggleSwapper = false;
    static State currentState = State.NONE;
    static Clock delay = new Clock();
    public static String expectedPet = null;
    static int expectedPetSlot = -1;
    static final int nextPageSlot = 53;
    static String defaultPet = null;
    enum State{
        NONE,
        STARTING,
        FIND,
        SELECT
    }

    public static boolean jacob = false;

    @SubscribeEvent(priority =  EventPriority.LOWEST)
    void onTick(TickEvent.ClientTickEvent event){
        if(mc.thePlayer == null || mc.theWorld == null) return;
        if(delay.isScheduled() && !delay.passed()) {return;}
        if(!MacroHandler.isMacroing) {if(defaultPet!=null){stop(false);jacob=false;
            MacroHandler.crop=null;
        } return;}
        if(MacroHandler.crop == null) return;
        if(MacroHandler.rotation.rotating) return;
        jacob = VisitorsMacro.inJacobContest();
        if(!toggleSwapper){
            // change to specific pet
            if(expectedPet!=null) start();
            if(jacob && defaultPet==null){
                debugLog("change pet to wanted pet and stop macro");
                expectedPet = Config.petName;
                delay.schedule(2000);
                return;
            }
            else if(!jacob && defaultPet!=null){
                debugLog("change wanted pet to default pet and stop macro");
                expectedPet = defaultPet;
                defaultPet = null;
                delay.schedule(2000);
                return;
            }
            else{
                return;
            }
        }

        switch(currentState){
            case STARTING:
                debugLog("starting");
                mc.thePlayer.sendChatMessage("/pets");
                currentState = State.FIND;
                delay.schedule(1000);
                break;
            case FIND:
                debugLog("inFind");
                if(mc.currentScreen instanceof GuiChest){
                    debugLog("inChestGui");
                    List<ItemStack> inventory = mc.thePlayer.openContainer.getInventory();

                    if(defaultPet==null){
                        defaultPet = PlayerUtils.matchFromString("pet: (.*?) Click", "hide pets");
                        if(defaultPet==null) defaultPet="none";
                        debugLog("defaultPet: " + defaultPet);
                        // if default pet equipped stop
                        if(defaultPet != null && defaultPet.toLowerCase().contains(expectedPet)){
                            debugLog("pet already equipped");
                            mc.thePlayer.closeScreen();
                            stop(jacob);
                            return;
                        }
                    }

                    expectedPetSlot = PlayerUtils.getSlotFromGui(expectedPet);
                    debugLog("pet: " + expectedPet);
                    debugLog("petSlot: " + expectedPetSlot);
                    if (expectedPetSlot == -1) {
                        if(inventory.get(nextPageSlot) != null && inventory.get(nextPageSlot).getDisplayName().toLowerCase().contains("page")){
                            PlayerUtils.clickOpenContainerSlot(nextPageSlot);
                            delay.schedule(500);
                        }
                        else {
                            debugLog("pet not found");
                            stop(jacob);
                        }
                    } else {
                        currentState=State.SELECT;
                        delay.schedule(300);
                    }
                }
                break;
            case SELECT:
                PlayerUtils.clickOpenContainerSlot(expectedPetSlot);
                delay.schedule(3000);
                break;
            case NONE:
                stop(jacob);
        }
    }

    @SubscribeEvent
    void onChatMsgReceive(ClientChatReceivedEvent event){
        if(event.type!=0) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if(msg.toLowerCase().contains("you summoned your " + expectedPet.toLowerCase())){
            currentState=State.NONE;
            delay.schedule(1000);
            debugLog("pet spawned");
        }
    }

    public static void start(){
        debugLog("osamaClient: disabling macro");
        MacroHandler.disableCurrentMacro(true);
        currentState=State.STARTING;
        debugLog("starting");
        toggleSwapper=true;
    }

    public static void stop(boolean saveState){
        debugLog("osamaclient: enablingMacro");
        MacroHandler.enableCurrentMacro();
        reset();
        if(saveState){
            return;
        }
        debugLog("stopping");
        defaultPet = null;
    }

    public static void reset(){
        currentState=State.NONE;
        toggleSwapper=false;
        expectedPetSlot=-1;
        expectedPet=null;
    }
}
