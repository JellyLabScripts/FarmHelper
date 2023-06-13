package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class AutoPot {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean enabled;
    private static int hoeSlot;
    private static final Clock buyWait = new Clock();
    private static final Rotation rotation = new Rotation();
    private static int potSlot;
    public static int failCount;
    private static final Clock cooldown = new Clock();
    private static final Clock timeout = new Clock();

    private static State currentState;
    enum State {
        WALKING1,
        WALKING2,
        BUYING,
        BOUGHT
    }

    private static PickupState pickupState;
    enum PickupState {
        LIFT,
        SWAP,
        DROP
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void enable() {
        MacroHandler.disableCurrentMacro();
        LogUtils.debugLog("[AutoPot] Going to buy GodPot");
        mc.thePlayer.sendChatMessage("/hub");
        hoeSlot = mc.thePlayer.inventory.currentItem;
        currentState = State.WALKING1;
        pickupState = PickupState.LIFT;
        timeout.schedule(20000);
        enabled = true;
    }

    public static void disable() {
        LogUtils.debugLog("[AutoPot] Finished auto GodPot");
        mc.thePlayer.closeScreen();
        mc.thePlayer.inventory.currentItem = hoeSlot;
        enabled = false;
        MacroHandler.enableCurrentMacro();
        if (cooldown.passed()) {
            cooldown.schedule(1000);
        }
    }



    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || (FarmHelper.tickCount % 5 != 0 && ((currentState != State.WALKING1 && currentState != State.WALKING2) || !enabled)))
            return;

        if (!enabled && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled && cooldown.passed() && FarmHelper.config.autoGodPot && FarmHelper.gameState.godPot == GameState.EffectState.OFF && FarmHelper.gameState.currentLocation == GameState.location.ISLAND) {
            LogUtils.scriptLog("[AutoPot] Detected no GodPot buff");
            MacroHandler.disableCurrentMacro();
            enable();
        }

        if (!enabled) return;

        KeyBindUtils.stopMovement();

        if (timeout.passed()) {
            LogUtils.debugFullLog("[AutoPot] Timed out, exiting");
            cooldown.schedule(5000);
            failCount++;
            if (failCount == 3) {
                LogUtils.debugFullLog("[AutoPot] Disabling auto GodPot, too many fails");
                FarmHelper.config.autoGodPot = false;
            }
            disable();
        }

        if (FarmHelper.gameState.currentLocation != GameState.location.HUB) {
            LogUtils.debugFullLog("[AutoPot] Waiting for hub");
            return;
        }

        if (rotation.rotating) {
            LogUtils.debugFullLog("[AutoPot] Waiting for rotate");
            return;
        }

        if (!buyWait.passed()) {
            LogUtils.debugFullLog("[AutoPot] Waiting after purchase");
            for (Slot item : PlayerUtils.getInventorySlots()) {
                if (item.getStack().getDisplayName().contains("God Potion") && currentState == State.BUYING) {
                    LogUtils.debugFullLog("[AutoPot] Detected GodPot purchase");
                    currentState = State.BOUGHT;
                    pickupState = PickupState.LIFT;
                }
            }
            return;
        }

        switch (currentState) {
            case WALKING1:
                if (mc.currentScreen != null) {
                    LogUtils.debugFullLog("[AutoPot] Closing GUI");
                    mc.thePlayer.closeScreen();
                } else if (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), 195) > 2) {
                    LogUtils.debugFullLog("[AutoPot] Rotating to shop1");
                    rotation.easeTo(195, mc.thePlayer.rotationPitch, 1000);
                } else if (distanceToShop1() < 1.7) {
                    LogUtils.debugFullLog("[AutoPot] Arrived at shop1");
                    currentState = State.WALKING2;
                } else if (distanceToShop1() < 5) {
                    LogUtils.debugFullLog("[AutoPot] Crouching to shop1");
                    KeyBindUtils.updateKeys(true, false, false, false, false, true, false);
                } else {
                    LogUtils.debugFullLog("[AutoPot] Walking to shop1");
                    KeyBindUtils.updateKeys(true, false, false, false, false);
                }
                break;

            case WALKING2:
                if (mc.currentScreen != null) {
                    LogUtils.debugFullLog("[AutoPot] Closing GUI");
                    mc.thePlayer.closeScreen();
                } else if (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), 90) > 2) {
                    LogUtils.debugFullLog("[AutoPot] Rotating to shop2");
                    rotation.easeTo(90, mc.thePlayer.rotationPitch, 1000);
                } else if (distanceToShop2() < 2) {
                    LogUtils.debugFullLog("[AutoPot] Arrived at shop2");
                    currentState = State.BUYING;
                } else if (distanceToShop2() < 4) {
                    LogUtils.debugFullLog("[AutoPot] Crouching to shop2");
                    KeyBindUtils.updateKeys(true, false, false, false, false, true, false);
                } else {
                    LogUtils.debugFullLog("[AutoPot] Walking to shop2");
                    KeyBindUtils.updateKeys(true, false, false, false, false);
                }
                break;

            case BUYING:
                if (mc.currentScreen == null) {
                    LogUtils.debugFullLog("[AutoPot] Attempting to open community shop");
                    final Entity shopNPC = getShopNPC();
                    if (shopNPC == null) {
                        LogUtils.debugFullLog("[AutoPot] Cannot find shop NPC, retrying");
                    } else {
                        LogUtils.debugFullLog("[AutoPot] Sending interact packet");
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, shopNPC);
                    }
                } else if (haveGodPot()) {
                    LogUtils.debugFullLog("[AutoPot] Purchased god potion, switching to consume");
                    currentState = State.BOUGHT;
                    pickupState = PickupState.LIFT;
                } else if (PlayerUtils.inventoryNameContains("Confirm")) {
                    LogUtils.debugFullLog("[AutoPot] Found confirm menu");
                    PlayerUtils.clickOpenContainerSlot(11);
                } else if (PlayerUtils.inventoryNameContains("Community Shop")) {
                    LogUtils.debugFullLog("[AutoPot] Found shop menu");
                    if (PlayerUtils.getStackInOpenContainerSlot(13) != null && PlayerUtils.getStackInOpenContainerSlot(13).getMetadata() == 5 && buyWait.passed()) {
                        LogUtils.debugFullLog("[AutoPot] Correct shop menu, buying");
                        PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotForItem("God"));
                        buyWait.schedule(2000);
                    } else {
                        LogUtils.debugFullLog("[AutoPot] Wrong shop menu, switching");
                        PlayerUtils.clickOpenContainerSlot(4);
                    }
                } else {
                    LogUtils.debugFullLog("[AutoPot] Unknown menu " + PlayerUtils.getInventoryName() + ", closing");
                    mc.thePlayer.closeScreen();
                }
                break;

            case BOUGHT:
                if (mc.currentScreen == null) {
                    if (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), 270) > 5) {
                        LogUtils.debugFullLog("[AutoPot] Looking away from shop");
                        rotation.easeTo(270, mc.thePlayer.rotationPitch, 1000);
                    } else if (PlayerUtils.getItemInHotbar("God Potion") == 6 && pickupState != PickupState.DROP) {
                        LogUtils.debugFullLog("[AutoPot] Detected GodPot in hotbar");
                        if (mc.thePlayer.inventory.currentItem == 6) {
                            LogUtils.debugFullLog("[AutoPot] Using god potion");
                            KeyBindUtils.rightClick();
                        } else {
                            LogUtils.debugFullLog("[AutoPot] Switching to god potion");
                            mc.thePlayer.inventory.currentItem = 6;
                        }
                    } else if (PlayerUtils.getSlotForItem("God Potion") != -1 || pickupState != PickupState.LIFT) {
                        switch (pickupState) {
                            case LIFT:
                                LogUtils.debugFullLog("[AutoPot] Picking up GodPot");
                                potSlot = PlayerUtils.getSlotForItem("God Potion");
                                PlayerUtils.clickOpenContainerSlot(potSlot);
                                pickupState = PickupState.SWAP;
                                break;

                            case SWAP:
                                LogUtils.debugFullLog("[AutoPot] Swapping GodPot with slot 7");
                                PlayerUtils.clickOpenContainerSlot(35 + 7);
                                pickupState = PickupState.DROP;
                                break;

                            case DROP:
                                LogUtils.debugFullLog("[AutoPot] Dropping slot 7 to GodPot slot");
                                PlayerUtils.clickOpenContainerSlot(potSlot);
                                pickupState = PickupState.LIFT;
                                break;
                        }
                    } else {
                        LogUtils.debugFullLog("[AutoPot] No GodPot in inventory, quitting");
                        disable();
                    }
                } else {
                    LogUtils.debugFullLog("[AutoPot] Unknown menu " + PlayerUtils.getInventoryName() + ", closing");
                    mc.thePlayer.closeScreen();
                }
        }
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating && enabled) {
            rotation.update();
        }
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        if (!enabled) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You don't have enough Bits!") && currentState == State.BUYING) {
            LogUtils.debugFullLog("[AutoPot] Not enough bits for a god potion");
            cooldown.schedule(3000);
            failCount++;
            if (failCount == 3) {
                LogUtils.debugFullLog("[AutoPot] Disabling AutoPot, too many fails");
                FarmHelper.config.autoGodPot = false;
            }
            disable();
        }
        if (message.contains("This server is too laggy") && currentState == State.BUYING) {
            LogUtils.debugFullLog("[AutoPot] Server too laggy");
            disable();
        }
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            if (enabled) {
                LogUtils.debugFullLog("[AutoPot] Interrupting GodPot purchase");
                disable();
            }
        }
    }

    private static Entity getShopNPC() {
        for (final Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityArmorStand) {
                final String name = StringUtils.stripControlCodes(e.getDisplayName().getUnformattedText());
                if (name.startsWith("Elizabeth")) {
                    return e;
                }
            }
        }
        return null;
    }

    private static float distanceToShop1() {
        return (float) Math.sqrt(Math.pow(mc.thePlayer.posX - (5), 2) + Math.pow(mc.thePlayer.posZ - (-101), 2));
    }

    private static float distanceToShop2() {
        return (float) Math.sqrt(Math.pow(mc.thePlayer.posX - (0), 2) + Math.pow(mc.thePlayer.posZ - (-101), 2));
    }

    private static boolean haveGodPot() {
        for (Slot item : PlayerUtils.getInventorySlots()) {
            if (item.getStack().getDisplayName().contains("God Potion")) {
                return true;
            }
        }
        return false;
    }

    private static boolean purchaseConfirmations() {
        try {
            return PlayerUtils.getLore(PlayerUtils.getStackInOpenContainerSlot(49)).get(2).toString().contains("Enabled!");
        } catch (Exception e) {
            LogUtils.debugLog("[AutoPot] Failed to get purchase confirmation, sent error to logs");
            System.out.println("Purchase confirmation error: " + e);
        }
        return false;
    }
}
