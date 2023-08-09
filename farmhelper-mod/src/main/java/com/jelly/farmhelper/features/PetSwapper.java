package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class PetSwapper {

    final static Minecraft mc = Minecraft.getMinecraft();
    static boolean enabled = false;
    static State currentState = State.NONE;
    static Clock delay = new Clock();
    static String previousPet = null;
    private static boolean getPreviousPet = false;
    enum State {
        NONE,
        STARTING,
        FIND_PREVIOUS,
        FIND_NEW,
        WAITING_FOR_SPAWN
    }
    public static boolean hasPetChangedDuringThisContest = false;

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.enablePetSwapper) return;
        if (!MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (Failsafe.emergency) return;
        if (!enabled) return;
        if (delay.isScheduled() && !delay.passed()) return;

        switch (currentState) {
            case STARTING:
                if (mc.currentScreen != null) return;
                LogUtils.debugLog("Pet swapper: starting");
                mc.thePlayer.sendChatMessage("/pets");
                if (previousPet != null && !previousPet.isEmpty() && !getPreviousPet) {
                    currentState = State.FIND_NEW;
                } else {
                    currentState = State.FIND_PREVIOUS;
                }
                delay.schedule(FarmHelper.config.petSwapperDelay);
                break;
            case FIND_PREVIOUS:
                LogUtils.debugLog("Pet swapper: waiting for pets menu");
                if (!(mc.thePlayer.openContainer instanceof ContainerChest)) return;
                List<ItemStack> inventory = mc.thePlayer.openContainer.getInventory();
                for (ItemStack itemStack : inventory) {
                    if (itemStack == null) continue;
                    List<String> petLore = PlayerUtils.getItemLore(itemStack);
                    String petName = StringUtils.stripControlCodes(itemStack.getDisplayName());
                    if (petName.contains("]")) {
                        petName = petName.substring(petName.indexOf("]") + 2);
                    }
                    if (getPreviousPet) {
                        if (petName.toLowerCase().trim().contains(previousPet.toLowerCase())) {
                            LogUtils.debugLog("Pet swapper: found previous pet: " + petName);
                            PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui(petName));
                            currentState = State.WAITING_FOR_SPAWN;
                            delay.schedule(FarmHelper.config.petSwapperDelay);
                            return;
                        }
                        if (petName.toLowerCase().contains("next page")) {
                            PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui("next page"));
                            delay.schedule(FarmHelper.config.petSwapperDelay);
                            return;
                        }
                        continue;
                    }
                    if (petLore.stream().anyMatch(s -> s.toLowerCase().contains("click to despawn"))) {
                        if (petName.toLowerCase().trim().contains(FarmHelper.config.petSwapperName.toLowerCase())) {
                            LogUtils.scriptLog("Current pet is already the one we want");
                            stopMacro();
                            mc.thePlayer.closeScreen();
                            FarmHelper.config.enablePetSwapper = false;
                            return;
                        }
                        previousPet = petName.toLowerCase().trim();
                        LogUtils.debugLog("Pet swapper: previous pet: " + previousPet);
                        currentState = State.FIND_NEW;
                        delay.schedule(FarmHelper.config.petSwapperDelay);
                        mc.thePlayer.closeScreen();
                        mc.thePlayer.sendChatMessage("/pets");
                        break;
                    }
                    if (petName.toLowerCase().contains("next page")) {
                        PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui("next page"));
                        delay.schedule(FarmHelper.config.petSwapperDelay);
                        return;
                    }
                }
                if (previousPet == null) {
                    LogUtils.scriptLog("Pet swapper: no previous pet found");
                    stopMacro();
                    mc.thePlayer.closeScreen();
                    FarmHelper.config.enablePetSwapper = false;
                    return;
                }
                break;
            case FIND_NEW:
                LogUtils.debugLog("Pet swapper: waiting for pets menu");
                if (!(mc.thePlayer.openContainer instanceof ContainerChest)) return;
                inventory = mc.thePlayer.openContainer.getInventory();

                for (ItemStack itemStack : inventory) {
                    if (itemStack == null) continue;
                    String petName = StringUtils.stripControlCodes(itemStack.getDisplayName());
                    if (petName.contains("]")) {
                        petName = petName.substring(petName.indexOf("]") + 2);
                    }
                    if (petName.toLowerCase().trim().contains(FarmHelper.config.petSwapperName.toLowerCase())) {
                        LogUtils.debugLog("Pet swapper: found new pet: " + petName);
                        PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui(petName));
                        currentState = State.WAITING_FOR_SPAWN;
                        delay.schedule(FarmHelper.config.petSwapperDelay);
                        return;
                    }
                    if (petName.toLowerCase().contains("next page")) {
                        PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui("next page"));
                        delay.schedule(FarmHelper.config.petSwapperDelay);
                        return;
                    }
                }

                LogUtils.scriptLog("Pet swapper: no new pet found");
                stopMacro();
                mc.thePlayer.closeScreen();
                FarmHelper.config.enablePetSwapper = false;

                break;
            case WAITING_FOR_SPAWN:
                break;
            case NONE:
                stopMacro();
                break;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatMsgReceive(ClientChatReceivedEvent event) {
        if (!enabled || currentState != State.WAITING_FOR_SPAWN) return;
        if (event.type != 0) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String respawnMessage = "you summoned your " + (getPreviousPet ? previousPet : FarmHelper.config.petSwapperName).toLowerCase();
        if (msg.toLowerCase().contains(respawnMessage)) {
            currentState = State.NONE;
            LogUtils.debugLog("Pet swapper: pet spawned");
            delay.schedule(1000);
        }
    }

    public static void startMacro(boolean getPreviousPet) {
        LogUtils.debugLog("Disabling macro and enabling petswapper");
        MacroHandler.disableCurrentMacro(true);
        currentState = State.STARTING;
        enabled = true;
        PetSwapper.getPreviousPet = getPreviousPet;
    }

    public static void stopMacro() {
        LogUtils.debugLog("Disabling petswapper and enabling macro");
        MacroHandler.enableCurrentMacro();
        reset();
    }

    public static void reset() {
        currentState = State.NONE;
        enabled = false;
        hasPetChangedDuringThisContest = false;
    }

    @SubscribeEvent
    public final void onUnloadWorld(WorldEvent.Unload event) {
        reset();
    }

    @SubscribeEvent
    public void onTickAutoPetSwap(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (!FarmHelper.config.enablePetSwapper) return;
        if (enabled) return;
        if (currentState != State.NONE) return;
        if (Failsafe.emergency) return;
        if (!MacroHandler.isMacroing) return;
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;
        if (VisitorsMacro.inJacobContest() && !hasPetChangedDuringThisContest) {
            LogUtils.debugLog("Jacob's contest start detected");
            startMacro(false);
            hasPetChangedDuringThisContest = true;
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (!FarmHelper.config.enablePetSwapper) return;
        if (enabled) return;
        if (currentState != State.NONE) return;
        if (Failsafe.emergency) return;
        if (!MacroHandler.isMacroing) return;
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;
        if (VisitorsMacro.inJacobContest()) return;
        if (!hasPetChangedDuringThisContest) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (msg.contains("The Farming Contest is over!")) {
            LogUtils.debugLog("Jacob's contest end detected");
            startMacro(true);
            hasPetChangedDuringThisContest = false;
        }
    }
}