package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

import static com.jelly.farmhelper.utils.LogUtils.debugLog;

public class PetSwapper { // Credits: osamabeinglagging, ducklett

    final static Minecraft mc = Minecraft.getMinecraft();
    static boolean enabled = false;
    static State currentState = State.NONE;
    static Clock delay = new Clock();
    private static String expectedPetName = null;
    static int expectedPetSlot = -1;
    static final int nextPageSlot = 53;
    static String defaultPet = null;
    enum State {
        NONE,
        STARTING,
        FIND,
        SELECT
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.enablePetSwapper) return;
        if (!MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (Failsafe.emergency) return;
        if (!enabled) return;
        if (delay.isScheduled() && !delay.passed()) return;
        if (!MacroHandler.isMacroing) {
            if (defaultPet != null) {
                stopMacro(false);
                MacroHandler.crop = null;
            }
            return;
        }
        if (MacroHandler.crop == null) return;

        if (!enabled) {
            // change to specific pet
            if (expectedPetName != null) startMacro();
            if (VisitorsMacro.inJacobContest() && defaultPet == null) {
                debugLog("Changing pet to: " + Config.petSwapperName);
                expectedPetName = Config.petSwapperName;
                delay.schedule(Config.petSwapperDelay);
                return;
            } else if (!VisitorsMacro.inJacobContest() && defaultPet != null) {
                debugLog("Changing pet to the default one");
                expectedPetName = defaultPet;
                defaultPet = null;
                delay.schedule(Config.petSwapperDelay);
                return;
            } else {
                return;
            }
        }

        switch (currentState) {
            case STARTING:
                debugLog("Pet swapper: starting"); // ty tom
                mc.thePlayer.sendChatMessage("/pets");
                currentState = State.FIND;
                delay.schedule(Config.petSwapperDelay);
                break;
            case FIND:
                debugLog("Pet swapper: waiting for pets menu");
                if (mc.currentScreen instanceof GuiChest) {
                    debugLog("Pet swapper: in pets menu");
                    List < ItemStack > inventory = mc.thePlayer.openContainer.getInventory();

                    if (defaultPet == null) {
                        defaultPet = PlayerUtils.matchFromString("pet: (.*?) Click", "hide pets");
                        if (defaultPet == null) defaultPet = "none";
                        debugLog("Pet swapper: equipped (default) pet: " + defaultPet);
                        // stop if default pet is equipped
                        if (defaultPet != null && defaultPet.toLowerCase().contains(expectedPetName)) {
                            debugLog("Pet is already equipped!");
                            mc.thePlayer.closeScreen();
                            stopMacro(VisitorsMacro.inJacobContest());
                            return;
                        }
                    }

                    expectedPetSlot = PlayerUtils.getSlotFromGui(expectedPetName);
                    debugLog("Pet swapper: expected pet name: " + expectedPetName);
                    debugLog("Pet swapper: expected pet slot: " + expectedPetSlot);
                    if (expectedPetSlot == -1) {
                        if (inventory.get(nextPageSlot) != null && inventory.get(nextPageSlot).getDisplayName().toLowerCase().contains("page")) {
                            PlayerUtils.clickOpenContainerSlot(nextPageSlot);
                            delay.schedule(Config.petSwapperDelay);
                        } else {
                            debugLog("Pet swapper: pet not found!");
                            stopMacro(VisitorsMacro.inJacobContest());
                        }
                    } else {
                        currentState = State.SELECT;
                        delay.schedule(Config.petSwapperDelay);
                    }
                }
                break;
            case SELECT:
                PlayerUtils.clickOpenContainerSlot(expectedPetSlot);
                delay.schedule(Config.petSwapperDelay);
                break;
            case NONE:
                stopMacro(VisitorsMacro.inJacobContest());
        }
    }

    @SubscribeEvent
    void onChatMsgReceive(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (msg.toLowerCase().contains("you summoned your " + expectedPetName.toLowerCase())) {
            currentState = State.NONE;
            delay.schedule(1000);
            debugLog("Pet swapper: pet spawned");
        }
    }

    private static void startMacro() {
        debugLog("Disabling macro and enabling petswapper");
        MacroHandler.disableCurrentMacro(true);
        currentState = State.STARTING;
        enabled = true;
    }

    private static void stopMacro(boolean saveState) {
        debugLog("Disabling petswapper and enabling macro");
        MacroHandler.enableCurrentMacro();
        reset();
        if (saveState) {
            return;
        }
        defaultPet = null;
    }

    private static void reset() {
        currentState = State.NONE;
        enabled = false;
        expectedPetSlot = -1;
        expectedPetName = null;
    }
}