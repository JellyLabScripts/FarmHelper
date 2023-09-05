package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LocationUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.world.JacobsContestHandler;
import lombok.Getter;
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
    @Getter
    static boolean enabled = false;
    public static State currentState = State.NONE;
    static Clock delay = new Clock();
    static String previousPet = null;
    private static boolean getPreviousPet = false;
    public enum State {
        NONE,
        STARTING,
        FIND_PREVIOUS,
        FIND_NEW,
        WAITING_FOR_SPAWN
    }
    public static boolean hasPetChangedDuringThisContest = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.enablePetSwapper) return;
        if (!MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
        if (FailsafeNew.emergency) return;
        if (VisitorsMacro.isEnabled()) return;
        if (!enabled) return;
        if (delay.isScheduled() && !delay.passed()) return;

        switch (currentState) {
            case STARTING:
                if (mc.currentScreen != null) return;
                LogUtils.sendDebug("[PetSwapper] starting");
                mc.thePlayer.sendChatMessage("/pets");
                if (previousPet != null && !previousPet.isEmpty() && !getPreviousPet) {
                    currentState = State.FIND_NEW;
                } else {
                    currentState = State.FIND_PREVIOUS;
                }
                delay.schedule(FarmHelper.config.petSwapperDelay);
                break;
            case FIND_PREVIOUS:
                LogUtils.sendDebug("[PetSwapper] waiting for pets menu");
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
                            LogUtils.sendDebug("[PetSwapper] found previous pet: " + petName);
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
                            LogUtils.sendError("Current pet is already the one we want! Pet won't be swapped at the end of this contest.");
                            hasPetChangedDuringThisContest = false;
                            mc.thePlayer.closeScreen();
                            stopMacro();
                            return;
                        }
                        previousPet = petName.toLowerCase().trim();
                        LogUtils.sendDebug("[PetSwapper] previous pet: " + previousPet);
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
                    LogUtils.sendError("[PetSwapper] no previous pet found, disabling...");
                    FarmHelper.config.enablePetSwapper = false;
                    hasPetChangedDuringThisContest = false;
                    mc.thePlayer.closeScreen();
                    stopMacro();
                    return;
                }
                break;
            case FIND_NEW:
                LogUtils.sendDebug("[PetSwapper] waiting for pets menu");
                if (!(mc.thePlayer.openContainer instanceof ContainerChest)) return;
                inventory = mc.thePlayer.openContainer.getInventory();

                for (ItemStack itemStack : inventory) {
                    if (itemStack == null) continue;
                    String petName = StringUtils.stripControlCodes(itemStack.getDisplayName());
                    if (petName.contains("]")) {
                        petName = petName.substring(petName.indexOf("]") + 2);
                    }
                    if (petName.toLowerCase().trim().contains(FarmHelper.config.petSwapperName.toLowerCase())) {
                        LogUtils.sendDebug("[PetSwapper] found new pet: " + petName);
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

                LogUtils.sendError("[PetSwapper] no new pet found, disabling...");
                FarmHelper.config.enablePetSwapper = false;
                hasPetChangedDuringThisContest = false;
                mc.thePlayer.closeScreen();
                stopMacro();
                break;
            case WAITING_FOR_SPAWN:
                break;
            case NONE:
                stopMacro();
                break;
        }
    }

    @SubscribeEvent
    public void onPetSummon(ClientChatReceivedEvent event) {
        if (event.type == 0 && event.message != null && previousPet != null && FarmHelper.config.petSwapperName != null) {
            String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
            String respawnMessage = "you summoned your " + (getPreviousPet ? previousPet : FarmHelper.config.petSwapperName).toLowerCase();
            if (msg.toLowerCase().contains(respawnMessage)) {
                if (!enabled || currentState != State.WAITING_FOR_SPAWN) {
                    hasPetChangedDuringThisContest = false;
                    return;
                }
                currentState = State.NONE;
                LogUtils.sendDebug("[PetSwapper] pet spawned");
                delay.schedule(1000);
            }
        }
    }

    public static void startMacro(boolean getPreviousPet) {
        if (FarmHelper.config.petSwapperName.trim().isEmpty()) {
            LogUtils.sendError("[PetSwapper] no pet name specified, disabling");
            return;
        }
        LogUtils.sendDebug("Disabling macro and enabling petswapper");
        MacroHandler.disableCurrentMacro(true);
        currentState = State.STARTING;
        enabled = true;
        PetSwapper.getPreviousPet = getPreviousPet;
    }

    public static void stopMacro() {
        LogUtils.sendDebug("Disabling petswapper and enabling macro");
        currentState = State.NONE;
        enabled = false;
        if (FarmHelper.config.enableScheduler && !JacobsContestHandler.jacobsContestTriggered)
            Scheduler.resume();
        MacroHandler.enableCurrentMacro();
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
}