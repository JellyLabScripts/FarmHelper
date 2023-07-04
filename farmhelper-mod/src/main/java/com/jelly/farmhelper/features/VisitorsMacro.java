package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;



import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


// A lot of "inspiration" from GTC 🫡 Credits to RoseGold (got permission to use this code)
public class VisitorsMacro {
    private static final Minecraft mc = Minecraft.getMinecraft();


    public enum State {
        TRY_TO_SET_SPAWN,
        ROTATE_TO_EDGE,
        MOVE_TO_EDGE,
        ROTATE_TO_CENTER,
        MOVE_TO_CENTER,
        ROTATE_TO_DESK,
        MOVE_TOWARDS_DESK,
        MANAGING_VISITORS,
        OPEN_VISITOR,
        BUY_ITEMS,
        GIVE_ITEMS,
        BACK_TO_FARMING,
        TELEPORT_TO_GARDEN,
        CHANGE_TO_NONE,
        NONE
    }

    public enum BuyState {
        IDLE,
        CLICK_CROP,
        CLICK_SIGN,
        CLICK_BUY,
        SETUP_VISITOR_HAND_IN,
        HAND_IN_CROPS,
    }

    private static boolean enabled = false;

    private static State currentState = State.NONE;
    private static State previousState = State.NONE;
    private static BuyState currentBuyState = BuyState.IDLE;
    private static BuyState previousBuyState = BuyState.IDLE;
    private static Entity currentVisitor = null;

    private static final Clock clock = new Clock();
    public static final Rotation rotation = new Rotation();
    public static final Clock waitAfterTpClock = new Clock();
    public static final Clock delayClock = new Clock();
    public static final Clock stuckClock = new Clock();
    public static final Clock giveBackItemsClock = new Clock();
    public static final Clock haveItemsClock = new Clock();
    public static boolean rejectOffer = false;
    public static String signText = "";

    public static final ArrayList<String> visitors = new ArrayList<>();
    public static final ArrayList<Pair<String, Long>> visitorsFinished = new ArrayList<>();
    public static final ArrayList<Pair<String, Integer>> itemsToBuy = new ArrayList<>();
    public static final ArrayList<Pair<String, Integer>> itemsToBuyForCheck = new ArrayList<>();
    public static Pair<String, Integer> itemToBuy = null;
    public static boolean boughtAllItems = false;
    public static double previousDistanceToCheck = 0;
    public static int retriesToGettingCloser = 0;
    public static boolean disableMacro = false;

    public static final List<BlockPos> barnEdges = Arrays.asList(new BlockPos(33, 85, -6), new BlockPos(-32, 85, -6));
    public static final BlockPos barnCenter = new BlockPos(0, 85, -7);
    public static boolean goingToCenterFirst = false;

    public static BlockPos currentEdge = null;

    public static boolean haveAotv = false;
    public static final Clock aotvTpCooldown = new Clock();
    public static boolean firstTimeOpen = true;
    public static float randomValue = 0;

    public static float purseBeforeVisitors = 0;

    public static final List<String> profitRewards = Arrays.asList("Dedication", "Cultivating", "Delicate", "Replenish", "Music Rune", "Green Bandana", "Overgrown Grass", "Space Helmet");

    public static boolean isEnabled() {
        return enabled;
    }

    public static void stopMacro() {
        System.out.println("Stopping macro");
        currentEdge = null;
        currentState = State.NONE;
        aotvTpCooldown.reset();
        clock.reset();
        haveAotv = false;
        rotation.reset();
        waitAfterTpClock.reset();
        goingToCenterFirst = false;
        boughtAllItems = false;
        KeyBindUtils.stopMovement();
        visitorsFinished.clear();
        itemsToBuy.clear();
        itemsToBuyForCheck.clear();
        itemToBuy = null;
        currentBuyState = BuyState.IDLE;
        currentVisitor = null;
        signText = "";
        stuckClock.reset();
        previousState = State.NONE;
        previousBuyState = BuyState.IDLE;
        enabled = false;
        firstTimeOpen = true;
        retriesToGettingCloser = 0;
        if (disableMacro) {
            FarmHelper.config.visitorsMacro = false;
            FarmHelper.config.save();
        }
        disableMacro = false;
        if (MacroHandler.currentMacro != null) {
            MacroHandler.currentMacro.triggerTpCooldown();
        }
        ProfitCalculator.startingPurse = ProfitCalculator.getCurrentPurse() - (purseBeforeVisitors - ProfitCalculator.startingPurse);
        randomValue = 0;
        purseBeforeVisitors = 0;
        if (FarmHelper.config.visitorsMacro)
            LogUtils.scriptLog("Stopped visitors macro");
    }

    private static boolean InJacobContest() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if ((cleanedLine.toLowerCase()).contains("collected")) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onTickCheckVisitors(TickEvent.ClientTickEvent event) {
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        List<String> tabList = TablistUtils.getTabList();
        if (tabList.size() < 2) return;
        boolean foundVisitors = false;
        ArrayList<String> newVisitors = new ArrayList<>();
        for (String line : tabList) {
            if (line.contains("Visitors:")) {
                foundVisitors = true;
                continue;
            }
            if (StringUtils.stripControlCodes(line).trim().isEmpty()) {
                if (foundVisitors) {
                    break;
                }
                continue;
            }
            if (foundVisitors) {
                newVisitors.add(StringUtils.stripControlCodes(line));
            }
        }
        if (newVisitors.equals(visitors)) return;
        visitors.clear();
        visitors.addAll(newVisitors);
        System.out.println("Visitors: " + visitors.size());
    }

    @SubscribeEvent
    public void onTickStart(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.visitorsMacro) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (Failsafe.emergency) return;
        if (!MacroHandler.isMacroing) return;
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;

        if (clock.isScheduled() && !clock.passed()) {
            return;
        } else if (clock.passed()) {
            clock.reset();
        }

        if (!MacroHandler.currentMacro.isSpawnLocationSet() && (!FarmHelper.config.ladderDesign || FarmHelper.config.macroType)) {
            LogUtils.debugLog("Spawn pos is not yet determined, will run after rewarping or setting spawn.");
            return;
        }

        if ((!FarmHelper.config.ladderDesign || FarmHelper.config.macroType) && !MacroHandler.currentMacro.isStandingOnSpawnLocation()) {
            LogUtils.debugLog("Not standing on spawn location, will run after rewarping or setting spawn.");
            return;
        }

        if (ProfitCalculator.getCurrentPurse() < FarmHelper.config.visitorsMacroCoinsThreshold * 1_000_000) {
            LogUtils.debugLog("Not enough coins to start visitors macro, waiting...");
            clock.schedule(5000);
            return;
        }

        if (TablistUtils.getTabList().stream().noneMatch(line -> StringUtils.stripControlCodes(line).contains("Queue Full!"))) {
            LogUtils.debugLog("Queue is not full, waiting...");
            clock.schedule(1000);
            return;
        }

        if (!canSeeClosestEdgeOfBarn()) {
            LogUtils.debugLog("Can't see any edge of barn, still going.");
            clock.schedule(1000);
            enabled = false;
            return;
        }

        if (!isAboveHeadClear()) {
            LogUtils.debugLog("Player doesn't have clear space above their head, still going.");
            clock.schedule(5000);
            enabled = false;
            return;
        }

        BlockPos blockUnder = BlockUtils.getRelativeBlockPos(0, 0, 0);
        if (!BlockUtils.canSetSpawn(blockUnder) && FarmHelper.config.ladderDesign) {
            LogUtils.debugLog("Can't setspawn here, still going.");
            clock.schedule(1000);
            enabled = false;
            return;
        }

        int aspectOfTheVoid = PlayerUtils.getItemInHotbar("Aspect of the");
        if (aspectOfTheVoid == -1) {
            LogUtils.debugLog("Player doesn't have AOTE nor AOTV)");
            haveAotv = false;
        } else {
            haveAotv = true;
        }

        if (FarmHelper.config.pauseWhenInContests && InJacobContest()) {
            LogUtils.debugLog("Player is in Jacob's contest, waiting...");
            clock.schedule(5000);
            return;
        }

        if (FarmHelper.config.visitorsDeskPosX == 0 && FarmHelper.config.visitorsDeskPosY == 0 && FarmHelper.config.visitorsDeskPosZ == 0) {
            LogUtils.scriptLog("Desk position is not set, disabling this feature. Please set it up with the keybind");
            FarmHelper.config.visitorsMacro = false;
            FarmHelper.config.save();

            enabled = false;
            return;
        }

        if (currentState == State.NONE && clock.passed()) {
            if ((!FarmHelper.config.ladderDesign || FarmHelper.config.macroType)) {
                currentState = State.ROTATE_TO_EDGE;
                clock.schedule(1500L);
                LogUtils.debugLog("Visitors macro can be started");
                KeyBindUtils.stopMovement();
                enabled = true;
            } else {
                currentState = State.TRY_TO_SET_SPAWN;
                PlayerUtils.setSpawn();
            }
            rotation.reset();
            rotation.completed = true;
            clock.schedule(1_500);
            stuckClock.schedule(25_000);
            mc.thePlayer.closeScreen();
            purseBeforeVisitors = ProfitCalculator.getCurrentPurse();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.visitorsMacro) return;
        if (!MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (Failsafe.emergency) return;
        if (!enabled) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (clock.isScheduled() && !clock.passed()) {
            return;
        } else if (clock.isScheduled() && clock.passed()) {
            System.out.println("Clock passed");
            clock.reset();
            if (MacroHandler.currentMacro != null && MacroHandler.currentMacro.enabled)
                MacroHandler.disableCurrentMacro(true);
            return;
        }

        visitorsFinished.removeIf(visitor -> System.currentTimeMillis() - visitor.getRight() > 10_000);

        if (MacroHandler.currentMacro != null && MacroHandler.currentMacro.enabled) {
            MacroHandler.disableCurrentMacro(true);
            delayClock.schedule(1000);
            return;
        }

        System.out.println("Current state: " + currentState);
        System.out.println("Current buy state: " + currentBuyState);

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.scriptLog("Player is stuck, resetting macro");
            clock.reset();
            rotation.reset();
            waitAfterTpClock.reset();
            boughtAllItems = false;
            visitorsFinished.clear();
            itemsToBuy.clear();
            itemToBuy = null;
            currentVisitor = null;
            signText = "";
            stuckClock.reset();
            mc.thePlayer.closeScreen();
            currentState = State.MANAGING_VISITORS;
            currentBuyState = BuyState.IDLE;
            stuckClock.reset();
            delayClock.schedule(3000);
            return;
        }


        switch (currentState) {
            case TRY_TO_SET_SPAWN:
                // idle to wait for chatmessage
                break;
            case ROTATE_TO_EDGE:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;

                BlockPos closestEdge = barnEdges.stream().min(Comparator.comparingInt(edge -> (int) mc.thePlayer.getDistance(edge.getX(), edge.getY(), edge.getZ()))).orElse(null);

                System.out.println("Closest edge: " + closestEdge);
                if (closestEdge != null) {
                    if (mc.thePlayer.getDistanceSq(barnCenter) < mc.thePlayer.getDistanceSq(closestEdge)) {
                        closestEdge = barnCenter;
                        goingToCenterFirst = true;
                    }
                    Pair<Float, Float> rotationToEdge = AngleUtils.getRotation(closestEdge.add(new Vec3i(0, -0.5, 0)));

                    rotation.easeTo(rotationToEdge.getLeft(), 0 + (float) (Math.random() * 4 - 2), 500);

                    currentState = State.MOVE_TO_EDGE;
                    currentEdge = closestEdge;
                    previousDistanceToCheck = Integer.MAX_VALUE;
                } else {
                    LogUtils.debugLog("Can't find closest edge, waiting...");
                    delayClock.schedule(1000);
                }

                break;
            case MOVE_TO_EDGE:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;
                if (rotation.rotating) break;

                int aspectOfTheVoid = PlayerUtils.getItemInHotbar("Aspect of the");
                if (aspectOfTheVoid == -1) {
                    LogUtils.debugLog("Player doesn't have AOTE nor AOTV)");
                    haveAotv = false;
                } else {
                    mc.thePlayer.inventory.currentItem = aspectOfTheVoid;
                    haveAotv = true;
                }

                stuckClock.schedule(25_000);


                double distanceToEdge = mc.thePlayer.getDistance(currentEdge.getX(), mc.thePlayer.posY, currentEdge.getZ());
                int playerY = mc.thePlayer.getPosition().getY();

                if (distanceToEdge > previousDistanceToCheck && distanceToEdge > 15) {
                    retriesToGettingCloser++;
                    currentState = State.ROTATE_TO_EDGE;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(2000);
                    previousDistanceToCheck = Integer.MAX_VALUE;
                    if (retriesToGettingCloser >= 2) {
                        LogUtils.scriptLog("Player is not getting closer to the desk, stopping Visitors Macro. Set the desk position in more open to sight area.");
                        disableMacro = true;
                        currentState = State.BACK_TO_FARMING;
                        break;
                    }
                    break;
                }

                if (distanceToEdge > 3) {
                    if (playerY < 78 || mc.thePlayer.onGround) {
                        if (!mc.thePlayer.capabilities.isFlying) {
                            mc.thePlayer.capabilities.isFlying = true;
                            mc.thePlayer.capabilities.allowFlying = true;
                            mc.thePlayer.sendPlayerAbilities();
                        }
                        System.out.println("Flying");
                        KeyBindUtils.updateKeys(false, false, false, false, false, false, true, false);
                    } else if (playerY > 78) {
                        KeyBindUtils.updateKeys(true, false, false, false, false, false, playerY < 85, true);

                        if (distanceToEdge > 14) {
                            if ((aotvTpCooldown.passed() || !aotvTpCooldown.isScheduled()) && haveAotv && !rotation.rotating) {
                                PlayerUtils.rightClick();
                                aotvTpCooldown.schedule((long) (200 + Math.random() * 200));
                            }
                        }
                    }
                }

                if (distanceToEdge <= 3 || (distanceToEdge > previousDistanceToCheck && distanceToEdge < 6)) {
                    KeyBindUtils.stopMovement();
                    if (goingToCenterFirst) {
                        currentState = State.ROTATE_TO_DESK;
                    } else {
                        currentState = State.ROTATE_TO_CENTER;
                    }
                    break;
                }

                previousDistanceToCheck = distanceToEdge;

                Pair<Float, Float> rotationToEdge = AngleUtils.getRotation(currentEdge);
                randomValue = playerY > 85 ? 5 + (float) (Math.random() * 1 - 0.5) : 1 + (float) (Math.random() * 1 - 0.5);
                if ((Math.abs(mc.thePlayer.rotationYaw - rotationToEdge.getLeft()) > 0.5 || Math.abs(mc.thePlayer.rotationPitch - randomValue) > 0.5) && !rotation.rotating && !aotvTpCooldown.passed()) {
                    rotation.easeTo(rotationToEdge.getLeft(), randomValue, 275 + (int) (Math.random() * 100));
                }

                break;
            case ROTATE_TO_CENTER:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;
                if (rotation.rotating) break;

                if (FarmHelper.gameState.dx > 0.25 || FarmHelper.gameState.dz > 0.25) {
                    KeyBindUtils.updateKeys(false, true, false, false, false, false, false, false);
                    break;
                }

                BlockPos finalDeskPos = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY + 1, FarmHelper.config.visitorsDeskPosZ);

                if (BlockUtils.isBlockVisible(finalDeskPos) && mc.thePlayer.getDistance(finalDeskPos.getX(), finalDeskPos.getY(), finalDeskPos.getZ()) < 15) {
                    currentState = State.ROTATE_TO_DESK;
                    break;
                }

                Pair<Float, Float> rotationToCenter = AngleUtils.getRotation(barnCenter);

                rotation.easeTo(rotationToCenter.getLeft(), 4 + (float) (Math.random() * 2), 375);

                currentState = State.MOVE_TO_CENTER;
                previousDistanceToCheck = Integer.MAX_VALUE;

                break;
            case MOVE_TO_CENTER:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) return;

                BlockPos center = barnCenter;

                rotationToCenter = AngleUtils.getRotation(barnCenter);

                double distanceToCenter = mc.thePlayer.getDistance(center.getX(), mc.thePlayer.posY, center.getZ());
                finalDeskPos = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY, FarmHelper.config.visitorsDeskPosZ);

                if (BlockUtils.isBlockVisible(finalDeskPos.up().up()) && mc.thePlayer.getDistance(finalDeskPos.getX(), finalDeskPos.getY(), finalDeskPos.getZ()) < 23) {
                    currentState = State.ROTATE_TO_DESK;
                    KeyBindUtils.stopMovement();
                    break;
                }

                if (distanceToCenter > previousDistanceToCheck && distanceToCenter > 15) {
                    retriesToGettingCloser++;
                    currentState = State.ROTATE_TO_CENTER;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(2000);
                    previousDistanceToCheck = Integer.MAX_VALUE;
                    if (retriesToGettingCloser >= 2) {
                        LogUtils.scriptLog("Player is not getting closer to the desk, stopping Visitors Macro. Set the desk position in more open to sight area.");
                        disableMacro = true;
                        currentState = State.BACK_TO_FARMING;
                        break;
                    }
                    break;
                }

                if (distanceToCenter <= 3 || (distanceToCenter > previousDistanceToCheck && distanceToCenter < 6)) {
                    KeyBindUtils.stopMovement();
                    currentState = State.ROTATE_TO_DESK;
                    break;
                } else {
                    if (Math.abs(rotationToCenter.getLeft() - mc.thePlayer.rotationYaw) > 1 && !rotation.rotating) {
                        rotation.easeTo(rotationToCenter.getLeft(), 4 + (float) (Math.random() * 2), 175);
                    }
                    KeyBindUtils.updateKeys(true, false, false, false, false, false, false, true);
                }

                previousDistanceToCheck = distanceToCenter;

                break;
            case ROTATE_TO_DESK:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;
                if (FarmHelper.gameState.dx > 0.25 || FarmHelper.gameState.dz > 0.25) {
                    KeyBindUtils.updateKeys(false, true, false, false, false, false, false, false);
                    break;
                }
                KeyBindUtils.stopMovement();
                BlockPos deskPosTemp = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY, FarmHelper.config.visitorsDeskPosZ);

                Pair<Float, Float> rotationToDesk = AngleUtils.getRotation(deskPosTemp.up());

                rotation.easeTo(rotationToDesk.getLeft(), rotationToDesk.getRight(), 500);

                currentState = State.MOVE_TOWARDS_DESK;
                previousDistanceToCheck = Integer.MAX_VALUE;

                break;
            case MOVE_TOWARDS_DESK:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;

                finalDeskPos = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY, FarmHelper.config.visitorsDeskPosZ);
                BlockPos deskPosWithoutY = new BlockPos(FarmHelper.config.visitorsDeskPosX, mc.thePlayer.posY, FarmHelper.config.visitorsDeskPosZ);

                rotationToDesk = AngleUtils.getRotation(finalDeskPos.up().up().up());
                double distance = mc.thePlayer.getDistance(finalDeskPos.getX(), finalDeskPos.getY(), finalDeskPos.getZ());
                double distance2 = mc.thePlayer.getDistance(deskPosWithoutY.getX(), deskPosWithoutY.getY(), deskPosWithoutY.getZ());
                if ((Math.abs(rotationToDesk.getLeft() - mc.thePlayer.rotationYaw) > 1 || Math.abs(rotationToDesk.getRight() - mc.thePlayer.rotationPitch) > 1) && !rotation.rotating) {
                    rotation.easeTo(rotationToDesk.getLeft(), rotationToDesk.getRight(), 250);
                }
                if (distance > 4.5f) {
                    KeyBindUtils.updateKeys(distance2 > 1f, false, false, false, false, mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround, shouldJump(), !mc.thePlayer.capabilities.isFlying);
                } else if (distance <= 1.5f) {
                    currentState = State.MANAGING_VISITORS;
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                } else
                    KeyBindUtils.updateKeys(distance2 > 1f, false, false, false, false, distance <= 3.5f || (mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround), shouldJump(), false);

                previousDistanceToCheck = distance;

                break;
            case MANAGING_VISITORS:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) {
                    break;
                }
                if (waitAfterTpClock.isScheduled() && !waitAfterTpClock.passed()) break;
                if (delayClock.isScheduled() && !delayClock.passed()) break;
                if (rotation.rotating) break;
                KeyBindUtils.stopMovement();
                LogUtils.scriptLog("Looking for a visitor...");

                if (getNonToolItem() == -1) {
                    LogUtils.scriptLog("You don't have any free (non tool) slot in your hotbar, that would be less sus if you had one." +
                            "Clicking with AOTV or hoe in your hand will be more sus, but it will still work.");
                } else {
                    mc.thePlayer.inventory.currentItem = getNonToolItem();
                }

                if (mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = false;
                    mc.thePlayer.sendPlayerAbilities();
                }

                if (noMoreVisitors()) {
                    currentState = State.BACK_TO_FARMING;
                    delayClock.schedule(250L);
                    return;
                }

                List<Entity> entities = mc.theWorld.loadedEntityList.stream()
                        .filter(
                                entity -> entity.hasCustomName() && visitors.stream().anyMatch(v -> StringUtils.stripControlCodes(v).contains(StringUtils.stripControlCodes(entity.getCustomNameTag())))
                        ).filter(
                                entity -> entity.getDistanceToEntity(mc.thePlayer) < 4
                        ).collect(Collectors.toList());
                Entity closest = null;

                for (Entity entity : entities) {
                    if (visitorsFinished.stream().anyMatch(v -> StringUtils.stripControlCodes(v.getLeft()).contains(StringUtils.stripControlCodes(entity.getCustomNameTag())))) {
                        continue;
                    }
                    if (closest == null || mc.thePlayer.getDistanceToEntity(entity) < mc.thePlayer.getDistanceToEntity(closest)) {
                        closest = entity;
                    }
                }

                if (closest == null) {
                    LogUtils.scriptLog("No visitors found, waiting...");
                    delayClock.schedule(2500L);
                    return;
                }

                Entity character = PlayerUtils.getEntityCuttingOtherEntity(closest);

                if (character != null) {
                    LogUtils.scriptLog("Found a visitor...");
                    currentVisitor = closest;
                    currentState = State.OPEN_VISITOR;
                    rotation.reset();
                    firstTimeOpen = true;
                    Pair<Float, Float> rotationToCharacter = AngleUtils.getRotation(character, true);
                    rotation.easeTo(rotationToCharacter.getLeft(), rotationToCharacter.getRight(), 400);
                    return;
                }

                break;
            case OPEN_VISITOR:
                if (currentVisitor == null) {
                    currentState = State.MANAGING_VISITORS;
                    break;
                }
                if (rotation.rotating) break;

                itemsToBuy.clear();
                itemsToBuyForCheck.clear();
                mc.playerController.interactWithEntitySendPacket(mc.thePlayer, currentVisitor);
                if (boughtAllItems) {
                    currentState = State.GIVE_ITEMS;
                    delayClock.schedule(250);
                } else
                    currentState = State.BUY_ITEMS;
                delayClock.schedule(250);
                break;
            case BUY_ITEMS:
                String chestName = mc.thePlayer.openContainer.inventorySlots.get(0).inventory.getName();
                if (firstTimeOpen) {
                    mc.playerController.interactWithEntitySendPacket(mc.thePlayer, currentVisitor);
                    firstTimeOpen = false;
                }
                System.out.println(chestName);

                if (chestName != null) {
                    switch (currentBuyState) {
                        case IDLE:
                            if (!itemsToBuy.isEmpty()) {
                                if (haveRequiredItemsInInventory()) {
                                    currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                    boughtAllItems = true;
                                    delayClock.schedule(750);
                                    break;
                                }
                                itemToBuy = itemsToBuy.get(0);
                                mc.thePlayer.sendChatMessage("/bz " + itemToBuy.getLeft());
                                delayClock.schedule(750);
                                currentBuyState = BuyState.CLICK_CROP;
                                break;
                            }
                            for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                                if (slot.getHasStack() && slot.getStack().getDisplayName().contains("Accept Offer")) {
                                    ArrayList<Pair<String, Integer>> cropsToBuy = new ArrayList<>();
                                    boolean foundRequiredItems = false;
                                    boolean foundProfit = false;
                                    ArrayList<String> lore = PlayerUtils.getItemLore(slot.getStack());
                                    if (FarmHelper.config.onlyAcceptProfitableVisitors) {
                                        for (String line : lore) {
                                            if (profitRewards.stream().anyMatch(r -> StringUtils.stripControlCodes(line.toLowerCase()).contains(StringUtils.stripControlCodes(r.toLowerCase())))) {
                                                foundProfit = true;
                                                break;
                                            }
                                        }
                                        if (!foundProfit) {
                                            LogUtils.scriptLog("Visitor offers a reward that is not in the profit rewards list, skipping...");
                                            rejectOffer = true;
                                            signText = "";
                                            boughtAllItems = true;
                                            currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                            delayClock.schedule(250);
                                            return;
                                        }
                                    }
                                    for (String line : lore) {
                                        if (line.contains("Required:")) {
                                            foundRequiredItems = true;
                                            continue;
                                        }
                                        if (line.trim().contains("Rewards:") || line.trim().isEmpty()) {
                                            break;
                                        }
                                        if (foundRequiredItems) {
                                            if (line.contains("x")) {
                                                String[] split = line.split("x");
                                                cropsToBuy.add(Pair.of(split[0].trim(), Integer.parseInt(split[1].replace(",", "").trim())));
                                            } else {
                                                cropsToBuy.add(Pair.of(line.trim(), 1));
                                            }
                                        }
                                    }
                                    if (cropsToBuy.isEmpty()) {
                                        LogUtils.scriptLog("No items to buy");
                                        stopMacro();
                                        return;
                                    }
                                    itemsToBuyForCheck.clear();
                                    itemsToBuy.addAll(cropsToBuy);
                                    itemsToBuyForCheck.addAll(cropsToBuy);
                                    currentBuyState = BuyState.IDLE;
                                    boughtAllItems = false;
                                }
                            }
                            break;
                        case CLICK_CROP:
                            if (chestName.startsWith("Bazaar ➜ \"")) {
                                if (itemToBuy == null) {
                                    currentState = State.MANAGING_VISITORS;
                                    break;
                                }
                                for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                                    if (!slot.getHasStack()) continue;
                                    if (StringUtils.stripControlCodes(slot.getStack().getDisplayName()).trim().equals(itemToBuy.getLeft().trim())) {
                                        clickSlot(slot.slotNumber, 0);
                                        currentBuyState = BuyState.CLICK_SIGN;
                                        rejectOffer = false;
                                        delayClock.schedule(250);
                                        return;
                                    }
                                }
                                LogUtils.debugLog("Item not found in BZ. Rejecting");
                                System.out.println(itemToBuy.getLeft());
                                rejectOffer = true;
                                signText = "";
                                boughtAllItems = true;
                                currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                            }
                            break;
                        case CLICK_SIGN:
                            if (itemToBuy == null) {
                                currentState = State.MANAGING_VISITORS;
                                break;
                            }
                            if (chestName.contains("➜") && !chestName.contains("Bazaar")) {
                                clickSlot(10, 0);
                                clickSlot(16, 1);

                                signText = String.valueOf(itemToBuy.getRight());
                                currentBuyState = BuyState.CLICK_BUY;
                                delayClock.schedule(250);
                            }
                            break;
                        case CLICK_BUY:
                            if (itemToBuy == null) {
                                currentState = State.MANAGING_VISITORS;
                                break;
                            }
                            if (chestName.equals("Confirm Instant Buy")) {
                                signText = "";
                                clickSlot(13, 0);
                                itemsToBuy.remove(itemToBuy);
                                if (!itemsToBuy.isEmpty()) {
                                    currentBuyState = BuyState.IDLE;
                                } else {
                                    currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                }
                                delayClock.schedule(250);
                            }
                            break;
                        case SETUP_VISITOR_HAND_IN:
                            mc.theWorld.loadedEntityList.stream()
                                    .filter(
                                            entity -> entity.hasCustomName() && StringUtils.stripControlCodes(entity.getCustomNameTag()).contains(StringUtils.stripControlCodes(currentVisitor.getCustomNameTag()))
                                    ).filter(
                                            entity -> entity.getDistanceToEntity(mc.thePlayer) < 4
                                    ).findAny().ifPresent(visitorEntity -> {
                                        System.out.println("Found visitor to give items");
                                        currentVisitor = visitorEntity;

                                        Entity characterr = PlayerUtils.getEntityCuttingOtherEntity(currentVisitor);

                                        if (characterr != null) {
                                            LogUtils.scriptLog("Found a visitor and going to give items to him");
                                            rotation.reset();
                                            Pair<Float, Float> rotationTo = AngleUtils.getRotation(characterr, true);
                                            rotation.easeTo(rotationTo.getLeft(), rotationTo.getRight(), (long) (350 + Math.random() * 200));
                                            currentState = State.OPEN_VISITOR;
                                            boughtAllItems = true;
                                            mc.thePlayer.closeScreen();
                                        } else {
                                            LogUtils.scriptLog("Visitor might a bit too far away, going to wait for him to get closer");
                                        }
                                        delayClock.schedule(750);
                                    });
                            break;
                    }
                } else {
                    currentState = State.MANAGING_VISITORS;
                }

                break;
            case GIVE_ITEMS:
                LogUtils.debugLog("Have items: " + haveRequiredItemsInInventory());
                if (!haveRequiredItemsInInventory() && !rejectOffer) {
                    if (!giveBackItemsClock.isScheduled()) {
                        giveBackItemsClock.schedule(15_000);
                    } else if (giveBackItemsClock.passed()) {
                        LogUtils.scriptLog("Can't give items to visitor, going to buy again");
                        visitorsFinished.clear();
                        delayClock.schedule(3_000);
                        currentState = State.MANAGING_VISITORS;
                        currentBuyState = BuyState.IDLE;
                        break;
                    }
                    delayClock.schedule(250);
                    break;
                } else if (haveRequiredItemsInInventory() && !rejectOffer) {
                    if (!haveItemsClock.isScheduled()) {
                        haveItemsClock.schedule(500);
                    } else if (haveItemsClock.isScheduled() && !haveItemsClock.passed()) {
                        return;
                    }
                }

                for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                    if (rejectOffer) {
                        if (slot.getHasStack() && slot.getStack().getDisplayName().contains("Refuse Offer")) {
                            finishVisitor(slot);
                            break;
                        }
                    } else {
                        if (slot.getHasStack() && slot.getStack().getDisplayName().contains("Accept Offer")) {
                            finishVisitor(slot);
                            break;
                        }
                    }
                }
                break;
            case BACK_TO_FARMING:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delayClock.schedule(250);
                    break;
                }
                if (noMoreVisitors() || disableMacro) {
                    currentState = State.TELEPORT_TO_GARDEN;
                } else {
                    currentState = State.MANAGING_VISITORS;
                }
                delayClock.schedule(2000);
                break;
            case TELEPORT_TO_GARDEN:
                LogUtils.scriptLog("Spent: " + (purseBeforeVisitors - ProfitCalculator.getCurrentPurse()) + " coins on visitors");
                mc.thePlayer.sendChatMessage("/warp garden");
                currentState = State.CHANGE_TO_NONE;
                delayClock.schedule(2500);
                break;
            case CHANGE_TO_NONE:
                currentState = State.NONE;
                stopMacro();
                delayClock.schedule(10_000);
                MacroHandler.enableCurrentMacro();
                return;
        }

        if (previousState != currentState) {
            System.out.println("State changed from " + previousState + " to " + currentState);
            stuckClock.schedule(25_000);
            previousState = currentState;
        }
        if (previousBuyState != currentBuyState) {
            System.out.println("Buy state changed from " + previousBuyState + " to " + currentBuyState);
            stuckClock.schedule(25_000);
            previousBuyState = currentBuyState;
        }
    }

    private void finishVisitor(Slot slot) {
        clickSlot(slot.slotNumber, 0);
        visitorsFinished.add(Pair.of(StringUtils.stripControlCodes(currentVisitor.getCustomNameTag()), System.currentTimeMillis()));
        currentVisitor = null;
        currentState = State.BACK_TO_FARMING;
        currentBuyState = BuyState.IDLE;
        boughtAllItems = false;
        rejectOffer = false;
        itemsToBuy.clear();
        itemsToBuyForCheck.clear();
        itemToBuy = null;
        delayClock.schedule(250);
        haveItemsClock.reset();
    }

    private boolean shouldJump() {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        EnumFacing playerFacing = mc.thePlayer.getHorizontalFacing();
        BlockPos blockInFront = playerPos.offset(playerFacing);
        Block block = mc.theWorld.getBlockState(blockInFront).getBlock();
        LogUtils.debugLog("Block in front: " + block);
        if (mc.thePlayer.onGround && !block.equals(Blocks.air) && !(block instanceof BlockSlab) && !(block instanceof BlockStairs)) {
            rotation.reset();
            return true;
        }
        return false;
    }

    private boolean isAboveHeadClear() {
        for (int y = (int) mc.thePlayer.posY + 1; y < 100; y++) {
            BlockPos blockPos = new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
            if (!mc.theWorld.isAirBlock(blockPos) && !mc.theWorld.getBlockState(blockPos).getBlock().equals(Blocks.reeds)) {
                return false;
            }
        }
        return true;
    }

    private boolean haveRequiredItemsInInventory() {
        for (Pair<String, Integer> item : itemsToBuyForCheck) {
            if (!hasItem(item.getLeft(), item.getRight())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasItem(String name, int amount) {
        int count = 0;
        for (ItemStack itemStack : mc.thePlayer.inventory.mainInventory) {
            if (itemStack != null && StringUtils.stripControlCodes(StringUtils.stripControlCodes(itemStack.getDisplayName())).equals(name)) {
                count += itemStack.stackSize;
            }
        }
        return count >= amount;
    }

    private boolean noMoreVisitors() {
        return TablistUtils.getTabList().stream().noneMatch(l -> l.contains("Visitors: "));
    }

    private boolean canSeeClosestEdgeOfBarn() {
        Vec3 positionFrom = new Vec3(mc.thePlayer.posX, 80, mc.thePlayer.posZ);
        BlockPos closestEdge = null;
        double closestDistance = Double.MAX_VALUE;
        List<BlockPos> tempBarnEdges = new ArrayList<>(barnEdges);
        tempBarnEdges.add(barnCenter);
        for (BlockPos edge : tempBarnEdges) {
            double distance = positionFrom.distanceTo(new Vec3(edge.getX(), edge.getY(), edge.getZ()));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEdge = edge;
            }
        }
        return closestEdge != null && mc.theWorld.rayTraceBlocks(positionFrom, new Vec3(closestEdge.getX() + 0.5, closestEdge.getY() + 0.5, closestEdge.getZ() + 0.5), false, true, false) == null;
    }

    private void clickSlot(int slot, int windowAdd) {
        mc.playerController.windowClick(
                mc.thePlayer.openContainer.windowId + windowAdd,
                slot,
                0,
                0,
                mc.thePlayer
        );
    }

    private int getNonToolItem() {
        for (int i = 0; i < 8; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null || itemStack.getItem() == null || itemStack.getItem() != null && !(itemStack.getItem() instanceof ItemTool) && !(itemStack.getItem() instanceof ItemSword) && !(itemStack.getItem() instanceof ItemHoe) && !(itemStack.getItem() instanceof ItemSpade)) {
                return i;
            }
        }
        return -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatSetSpawn(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("Your spawn location has been set!")) {
            MacroHandler.currentMacro.setSpawnLocation();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;
        if (!FarmHelper.config.visitorsMacro) return;
        if (currentState != State.TRY_TO_SET_SPAWN) return;
        if (event.type != 0) return;
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You cannot set your spawn here!")) {
            currentState = State.NONE;
            if (!clock.isScheduled()) {
                clock.schedule(5_000L);
            }
        } else if (message.contains("Your spawn location has been set!")) {
            currentState = State.ROTATE_TO_EDGE;
            clock.schedule(1500L);
            LogUtils.debugLog("Visitors macro can be started");
            KeyBindUtils.stopMovement();
            enabled = true;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (rotation.rotating && !(mc.thePlayer.openContainer instanceof ContainerChest))
            rotation.update();

        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;

        if ((FarmHelper.config.visitorsDeskPosX == 0 && FarmHelper.config.visitorsDeskPosY == 0 && FarmHelper.config.visitorsDeskPosZ == 0)) {
            return;
        }

        BlockPos deskPosTemp = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY, FarmHelper.config.visitorsDeskPosZ);
        RenderUtils.drawBlockBox(deskPosTemp, new Color(Color.DARK_GRAY.getRed(), Color.DARK_GRAY.getGreen(), Color.DARK_GRAY.getBlue(), 80));
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!MacroHandler.isMacroing) return;
        if (!FarmHelper.config.visitorsMacro) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!FarmHelper.config.debugMode) return;
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;

        ArrayList<Pair<String, Integer>> itemsToBuyCopy = new ArrayList<>(itemsToBuy);

        int x = 120;

        FontUtils.drawString("State: " + currentState, x, 2, Color.WHITE.hashCode(), true);
        FontUtils.drawString("Buy state: " + currentBuyState, x, 12, Color.WHITE.hashCode(), true);
        FontUtils.drawString("Stuck timer: " + (stuckClock.getRemainingTime() > 0 ? stuckClock.getRemainingTime() : "None"), x, 22, Color.WHITE.hashCode(), true);
        FontUtils.drawString("Distance to check: " + previousDistanceToCheck, x, 32, Color.WHITE.hashCode(), true);
        FontUtils.drawString("Items to buy: ", x, 42, Color.WHITE.hashCode(), true);
        for (Pair<String, Integer> item : itemsToBuyCopy) {
            FontUtils.drawString(item.getLeft() + " x" + item.getRight(), x + 5, 52 + (itemsToBuyCopy.indexOf(item) * 10), Color.WHITE.hashCode(), true);
        }
        if (!itemsToBuyCopy.isEmpty())
            FontUtils.drawString("Have items in inventory: " + haveRequiredItemsInInventory(), x, 52 + (itemsToBuyCopy.size() * 10), Color.WHITE.hashCode(), true);

    }
}