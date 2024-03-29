package com.jelly.farmhelperv2.remote.command.commands.impl;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.command.commands.Command;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;

/*
    Credits to mostly Yuro with few changes by May2Bee for this superb class
*/
@Command(label = "setspeed")
public class SetSpeedCommand extends ClientCommand {
    private static final Clock clock = new Clock();
    private static final RotationHandler rotation = RotationHandler.getInstance();
    private static int speed = -1;
    private static JsonObject data;
    private static boolean enabled = false;
    private static State currentState = State.NONE;
    private static Rotation lastRotation = new Rotation(0f, 0f);
    private final float[][] vectors = {
            {0.5F, 3.5F, 3.5F},
            {-6.5F, 1.5F, 0F},
            {6.5F, 1.5F, 0F},
            {0.5F, 7.5F, 0F}
    };
    int freeSlot = -1;
    int freeHotbarSlot = -1;
    boolean wasMacroing = false;

    @Override
    public void execute(RemoteMessage message) {
        JsonObject args = message.args;
        freeSlot = -1;
        freeHotbarSlot = -1;
        speed = args.get("speed").getAsInt();
        data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("speed", speed);

        if (InventoryUtils.getRancherBootSpeed() == -1) {
            data.addProperty("error", "You need to have Rancher's boots equipped for this command to work.");
        } else if (InventoryUtils.getRancherBootSpeed() == speed) {
            data.addProperty("error", "Your Rancher's boots are already at " + speed + " speed.");
        } else {
            for (int i = 36; i < 44; i++) {
                if (ClientCommand.mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
                    LogUtils.sendDebug("Found free slot: " + i);
                    freeSlot = i;
                    freeHotbarSlot = i - 36;
                    break;
                }
            }
            if (freeSlot == -1) {
                disableWithError("You don't have any free slot in your hotbar! Disabling...");
                return;
            }
            if (freeHotbarSlot < 0 || freeHotbarSlot > 7) {
                disableWithError("Free hotbar slot is invalid! Disabling...");
                return;
            }
            try {
                enabled = true;
                currentState = State.START;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (ClientCommand.mc.thePlayer == null || ClientCommand.mc.theWorld == null) return;
        if (clock.isScheduled() && !clock.passed()) return;

        switch (currentState) {
            case NONE:
                clock.reset();
                enabled = false;
                break;
            case START:
                if (ClientCommand.mc.thePlayer.inventoryContainer.getSlot(8) == null || !ClientCommand.mc.thePlayer.inventoryContainer.getSlot(8).getStack().getDisplayName().contains("Rancher's Boots")) {
                    disableWithError("You don't wear Rancher's Boots! Disabling...");
                    break;
                }
                wasMacroing = MacroHandler.getInstance().isMacroToggled();
                if (wasMacroing) {
                    MacroHandler.getInstance().pauseMacro();
                }
                currentState = State.OPEN_INVENTORY;
                clock.schedule(500);
                break;
            case OPEN_INVENTORY:
                InventoryUtils.openInventory();
                currentState = State.TAKE_OFF_BOOTS;
                clock.schedule(500);
                break;
            case TAKE_OFF_BOOTS:
                if (ClientCommand.mc.thePlayer.openContainer == null) {
                    currentState = State.OPEN_INVENTORY;
                    clock.schedule(500);
                    break;
                }
                InventoryUtils.clickContainerSlot(8, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                InventoryUtils.clickContainerSlot(freeSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                currentState = State.CLOSE_INVENTORY;
                clock.schedule(500);
                break;
            case CLOSE_INVENTORY:
                PlayerUtils.closeScreen();
                lastRotation = new Rotation(AngleUtils.get360RotationYaw(), ClientCommand.mc.thePlayer.rotationPitch);
                currentState = State.LOOK_IN_THE_AIR;
                clock.schedule(500);
                break;
            case LOOK_IN_THE_AIR:
                lookInTheAir();
                currentState = State.HOLD_RANCHER_BOOTS;
                clock.schedule(500);
                break;
            case HOLD_RANCHER_BOOTS:
                if (ClientCommand.mc.currentScreen != null) {
                    currentState = State.CLOSE_INVENTORY;
                    clock.schedule(500);
                    break;
                }
                ClientCommand.mc.thePlayer.inventory.currentItem = freeHotbarSlot;
                currentState = State.CLICK_RANCHER_BOOTS;
                clock.schedule(500);
                break;
            case CLICK_RANCHER_BOOTS:
                if (ClientCommand.mc.thePlayer.inventoryContainer.getSlot(freeSlot).getStack() == null || ClientCommand.mc.thePlayer.inventory.currentItem != freeHotbarSlot) {
                    currentState = State.HOLD_RANCHER_BOOTS;
                    clock.schedule(500);
                    break;
                }
                KeyBindUtils.leftClick();
                currentState = State.TYPE_IN_SPEED;
                clock.schedule(500);
                break;
            case TYPE_IN_SPEED:
                if (ClientCommand.mc.currentScreen == null) {
                    currentState = State.CLICK_RANCHER_BOOTS;
                    clock.schedule(500);
                    break;
                }
                SignUtils.setTextToWriteOnString(String.valueOf(speed));
                currentState = State.CLOSE_SIGN;
                clock.schedule(500);
                break;
            case CLOSE_SIGN:
                if (ClientCommand.mc.currentScreen == null) {
                    currentState = State.TYPE_IN_SPEED;
                    clock.schedule(500);
                    break;
                }
                SignUtils.confirmSign();
                currentState = State.LOOK_BACK;
                clock.schedule(500);
                break;
            case LOOK_BACK:
                rotation.reset();
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(lastRotation.getYaw(), lastRotation.getPitch()),
                                300, null
                        )
                );
                currentState = State.OPEN_INVENTORY_AGAIN;
                clock.schedule(500);
                break;
            case OPEN_INVENTORY_AGAIN:
                InventoryUtils.openInventory();
                currentState = State.PUT_ON_BOOTS;
                clock.schedule(500);
                break;
            case PUT_ON_BOOTS:
                if (ClientCommand.mc.thePlayer.openContainer == null) {
                    currentState = State.OPEN_INVENTORY_AGAIN;
                    clock.schedule(500);
                    break;
                }
                InventoryUtils.clickContainerSlot(freeSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                InventoryUtils.clickContainerSlot(8, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                currentState = State.CLOSE_INVENTORY_AGAIN;
                clock.schedule(500);
                break;
            case CLOSE_INVENTORY_AGAIN:
                PlayerUtils.closeScreen();
                currentState = State.END;
                clock.schedule(500);
                break;
            case END:
                PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), false, false);
                if (wasMacroing) {
                    MacroHandler.getInstance().resumeMacro();
                }
                LogUtils.sendSuccess("Rancher's Boots speed has been set to " + speed + ".");
                currentState = State.NONE;
                clock.reset();
                enabled = false;
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
                break;
        }
    }

    private void disableWithError(String message) {
        currentState = State.NONE;
        clock.reset();
        enabled = false;
        LogUtils.sendError(message);
        data.addProperty("error", message);
        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    private void lookInTheAir() {
        LogUtils.sendDebug("Looking in the air...");
        for (float[] vector : vectors) {
            Vec3 target = new Vec3(BlockUtils.getRelativeBlockPos(vector[0], vector[1], vector[2]));
            MovingObjectPosition rayTraceResult = ClientCommand.mc.theWorld.rayTraceBlocks(ClientCommand.mc.thePlayer.getPositionEyes(1), target, false, false, true);

            if (rayTraceResult != null) {
                if (rayTraceResult.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                    BlockPos blockPos = rayTraceResult.getBlockPos();
                    if (ClientCommand.mc.theWorld.getBlockState(blockPos).getBlock() == null || ClientCommand.mc.theWorld.getBlockState(blockPos).getBlock() == Blocks.air) {
                        LogUtils.sendDebug("Looking at " + Arrays.toString(vector));
                        rotation.reset();
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Rotation(rotation.getRotation(target, true).getYaw(), rotation.getRotation(target, true).getPitch()),
                                        300, null
                                )
                        );
                        return;
                    }
                }
            }
            LogUtils.sendDebug("Couldn't find an empty space to look at! Trying the next vector...");
        }
        disableWithError("Couldn't find an empty space to look at! Disabling...");
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (enabled && event.type == 0 && event.message != null && event.message.getFormattedText().contains("§eClick in the air!")) {
            lookInTheAir();
            currentState = State.HOLD_RANCHER_BOOTS;
            clock.schedule(500);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (enabled) {
            disableWithError("World change detected! Disabling...");
        }
    }

    public enum State {
        NONE,
        START,
        OPEN_INVENTORY,
        TAKE_OFF_BOOTS,
        CLOSE_INVENTORY,
        LOOK_IN_THE_AIR,
        HOLD_RANCHER_BOOTS,
        CLICK_RANCHER_BOOTS,
        TYPE_IN_SPEED,
        CLOSE_SIGN,
        LOOK_BACK,
        OPEN_INVENTORY_AGAIN,
        PUT_ON_BOOTS,
        CLOSE_INVENTORY_AGAIN,
        END
    }
}
