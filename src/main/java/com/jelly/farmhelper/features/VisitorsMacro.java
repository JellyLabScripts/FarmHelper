package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import com.jelly.farmhelper.world.JacobsContestHandler;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jelly.farmhelper.FarmHelper.config;


// A lot of "inspiration" from GTC 🫡 Credits to RoseGold (got permission to use this code)
public class VisitorsMacro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Random random = new Random();

    public enum State {
        ROTATE_TO_EDGE,
        MOVE_TO_EDGE,
        ROTATE_TO_CENTER,
        MOVE_TO_CENTER,
        ROTATE_TO_DESK,
        MOVE_TOWARDS_DESK,
        DISABLE_COMPACTORS,
        MANAGING_VISITORS,
        OPEN_VISITOR,
        BUY_ITEMS,
        GIVE_ITEMS,
        ENABLE_COMPACTORS,
        BACK_TO_FARMING,
        TELEPORT_TO_GARDEN,
        CHANGE_TO_NONE,
        NONE
    }

    public enum BuyState {
        GET_LIST,
        OPEN_BZ,
        CLICK_CROP,
        CLICK_BUY,
        CLICK_SIGN,
        CLICK_CONFIRM,
        SETUP_VISITOR_HAND_IN,
        HAND_IN_CROPS,
    }

    public enum CompactorState {
        IDLE,
        GET_LIST,
        HOLD_COMPACTOR,
        OPEN_COMPACTOR,
        TOGGLE_COMPACTOR,
        CLOSE_COMPACTOR
    }

    private static State currentState = State.NONE;
    private static State previousState = State.NONE;
    private static BuyState currentBuyState = BuyState.GET_LIST;
    private static BuyState previousBuyState = BuyState.GET_LIST;
    private static CompactorState currentCompactorState = CompactorState.GET_LIST;
    private static Entity currentVisitor = null;
    private static final Clock clock = new Clock();
    private static final Rotation rotation = new Rotation();
    private static final Clock waitAfterTpClock = new Clock();
    private static final Clock delayClock = new Clock();
    private static final Clock stuckClock = new Clock();
    private static final Clock giveBackItemsClock = new Clock();
    private static final Clock haveItemsClock = new Clock();
    private static boolean rejectOffer = false;
    private static final ArrayList<String> visitors = new ArrayList<>();
    private static final ArrayList<Pair<String, Long>> visitorsFinished = new ArrayList<>();
    private static final ArrayList<Pair<String, Integer>> itemsToBuy = new ArrayList<>();
    private static final ArrayList<Pair<String, Integer>> itemsToBuyForCheck = new ArrayList<>();
    private static final ArrayList<Integer> compactorSlots = new ArrayList<>();
    private static Pair<String, Integer> itemToBuy = null;
    private static boolean boughtAllItems = false;
    private static double previousDistanceToCheck = 0;
    private static int retriesToGettingCloser = 0;
    private static boolean disableMacro = false;
    private static final List<BlockPos> barnEdges = Arrays.asList(new BlockPos(33, 85, -6), new BlockPos(-32, 85, -6));
    private static final BlockPos barnCenter = new BlockPos(0, 85, -7);
    private static boolean goingToCenterFirst = false;

    private static BlockPos currentEdge = null;

    private static boolean haveAotv = false;
    private static final Clock aotvTpCooldown = new Clock();
    private static boolean firstTimeOpen = true;
    private static float randomValue = 0;

    private static float purseBeforeVisitors = 0;
    public static boolean triggeredManually = false;
    private static boolean autoSellInvoked = false;

    public static final List<String> profitRewards = Arrays.asList("Dedication", "Cultivating", "Delicate", "Replenish", "Music Rune", "Green Bandana", "Overgrown Grass", "Space Helmet");

    private final Locale locale = new Locale("en", "US");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);

    private static final int STUCK_DELAY = (int) (7_500 + config.visitorsMacroGuiDelay * 1000 + config.visitorsMacroGuiDelayRandomness * 1000);

    public static void stopMacro() {
        if (FarmHelper.config.visitorsMacro)
            LogUtils.sendDebug("[Visitors Macro] Stopping visitors macro");
        KeyBindUtils.stopMovement();
        currentState = State.NONE;
        previousState = State.NONE;
        currentBuyState = BuyState.GET_LIST;
        previousBuyState = BuyState.GET_LIST;
        currentCompactorState = CompactorState.IDLE;
        aotvTpCooldown.reset();
        clock.reset();
        rotation.reset();
        delayClock.reset();
        waitAfterTpClock.reset();
        stuckClock.reset();
        visitorsFinished.clear();
        itemsToBuy.clear();
        itemsToBuyForCheck.clear();
        compactorSlots.clear();
        Utils.signText = "";
        retriesToGettingCloser = 0;
        currentEdge = null;
        itemToBuy = null;
        currentVisitor = null;
        haveAotv = false;
        goingToCenterFirst = false;
        boughtAllItems = false;
        autoSellInvoked = false;
        firstTimeOpen = true;
        if (triggeredManually)
            UngrabUtils.regrabMouse();
        triggeredManually = false;
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
        if (FarmHelper.config.enableScheduler && !JacobsContestHandler.jacobsContestTriggered)
            Scheduler.resume();
        if (FarmHelper.config.visitorsMacro)
            LogUtils.sendDebug("[Visitors Macro] Stopped visitors macro");
    }

    @SubscribeEvent
    public void onTickCheckVisitors(TickEvent.ClientTickEvent event) {
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
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
        LogUtils.sendDebug("[Visitors Macro] Visitors: " + visitors.size());
    }

    public static boolean canEnableMacro(boolean manual) {
        if (!FarmHelper.config.visitorsMacro) return false;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return false;
        if (FailsafeNew.emergency) return false;
        if (!manual && (!PlayerUtils.isSpawnLocationSet() || !PlayerUtils.isStandingOnSpawnLocation())) {
            LogUtils.sendError("[Visitors Macro] Player is not standing on spawn location, skipping...");
            return false;
        }

        if (FarmHelper.gameState.cookie != GameState.EffectState.ON) {
            LogUtils.sendError("[Visitors Macro] Cookie buff is not active, skipping...");
            return false;
        }

        if (FarmHelper.config.pauseVisitorsMacroDuringJacobsContest && GameState.inJacobContest()) {
            LogUtils.sendError("[Visitors Macro] Player is in Jacob's contest, skipping...");
            return false;
        }

        if (PetSwapper.isEnabled()) {
            LogUtils.sendError("[Visitors Macro] Pet swapper is working, stopping visitors macro...");
            return false;
        }

        if (ProfitCalculator.getCurrentPurse() < FarmHelper.config.visitorsMacroCoinsThreshold * 1_000_000) {
            LogUtils.sendError("[Visitors Macro] Not enough coins to start visitors macro, skipping...");
            return false;
        }

        if (!manual && TablistUtils.getTabList().stream().noneMatch(line -> StringUtils.stripControlCodes(line).contains("Queue Full!"))) {
            LogUtils.sendError("[Visitors Macro] Queue is not full, skipping...");
            return false;
        }

        if (FarmHelper.config.visitorsMacroTeleportToPlot == 0 && !canSeeClosestEdgeOfBarn()) {
            LogUtils.sendError("[Visitors Macro] Can't see any edge of barn, still going.");
            return false;
        }

        if (FarmHelper.config.visitorsMacroTeleportToPlot == 0 && !isAboveHeadClear()) {
            LogUtils.sendError("[Visitors Macro] Player doesn't have clear space above their head, still going.");
            return false;
        }

        if (!manual) {
            int aspectOfTheVoid = PlayerUtils.getItemInHotbar("Aspect of the");
            if (aspectOfTheVoid == -1) {
                LogUtils.sendWarning("[Visitors Macro] Player doesn't have AOTE nor AOTV)");
                haveAotv = false;
            } else {
                haveAotv = true;
            }
        }

        if (!isDeskPosSet()) {
            LogUtils.sendError("[Visitors Macro] Desk position is not set! Disabling this feature...");
            FarmHelper.config.visitorsMacro = false;
            FarmHelper.config.save();
            return false;
        }

        return currentState == State.NONE;
    }

    public static void enableMacro(boolean manual) {
        if (manual) {
            triggeredManually = true;
            currentState = State.DISABLE_COMPACTORS;
            currentCompactorState = CompactorState.GET_LIST;
        } else {
            currentState = State.ROTATE_TO_EDGE;
        }
        LogUtils.sendDebug("[Visitors Macro] Visitors macro can be started");
        KeyBindUtils.stopMovement();
        if (FarmHelper.config.enableScheduler)
            Scheduler.pause();
        rotation.reset();
        rotation.completed = true;
        stuckClock.schedule(STUCK_DELAY);
        mc.thePlayer.closeScreen();
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
        delayClock.schedule(1000);
    }

    public static boolean isEnabled() {
        return currentState != State.NONE;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.visitorsMacro) return;
//        if (!triggeredManually && !MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
        if (FailsafeNew.emergency) return;
        if (!isEnabled()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        visitorsFinished.removeIf(visitor -> System.currentTimeMillis() - visitor.getRight() > 10_000);

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendError("[Visitors Macro] Player is stuck! Resetting the macro...");
            clock.reset();
            rotation.reset();
            waitAfterTpClock.reset();
            boughtAllItems = false;
            visitorsFinished.clear();
            itemsToBuy.clear();
            itemToBuy = null;
            currentVisitor = null;
            Utils.signText = "";
            stuckClock.schedule(STUCK_DELAY);
            mc.thePlayer.closeScreen();
            if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
            currentState = State.MANAGING_VISITORS;
            currentBuyState = BuyState.GET_LIST;
            delayClock.schedule(3000);
            return;
        }


        switch (currentState) {
            case ROTATE_TO_EDGE:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;

                BlockPos closestEdge = barnEdges.stream().min(Comparator.comparingInt(edge -> (int) mc.thePlayer.getDistance(edge.getX(), edge.getY(), edge.getZ()))).orElse(null);

                LogUtils.sendDebug("[Visitors Macro] Closest edge: " + closestEdge);
                if (closestEdge != null) {
                    if (mc.thePlayer.getDistanceSq(barnCenter) < mc.thePlayer.getDistanceSq(closestEdge)) {
                        closestEdge = barnCenter;
                        goingToCenterFirst = true;
                    }
                    Pair<Float, Float> rotationToEdge = AngleUtils.getRotation(closestEdge.add(new Vec3i(0, -0.5, 0)));

                    rotation.easeTo(rotationToEdge.getLeft(), (float) (Math.random() * 4 - 2), 500);

                    currentState = State.MOVE_TO_EDGE;
                    currentEdge = closestEdge;
                    previousDistanceToCheck = Integer.MAX_VALUE;
                } else {
                    LogUtils.sendDebug("[Visitors Macro] Can't find the closest edge, waiting...");
                    delayClock.schedule(1000);
                }

                break;
            case MOVE_TO_EDGE:
                if ((mc.thePlayer.openContainer instanceof ContainerChest)) break;
                if (rotation.rotating) break;

                int aspectOfTheEnd = PlayerUtils.getItemInHotbar("Aspect of the End");
                int aspectOfTheVoid = PlayerUtils.getItemInHotbar("Aspect of the Void");
                if (aspectOfTheEnd == -1 && aspectOfTheVoid == -1) {
                    LogUtils.sendDebug("[Visitors Macro] Player doesn't have AOTE nor AOTV");
                    haveAotv = false;
                } else {
                    mc.thePlayer.inventory.currentItem = aspectOfTheVoid != -1 ? aspectOfTheVoid : aspectOfTheEnd;
                    haveAotv = true;
                }

                stuckClock.schedule(STUCK_DELAY);


                double distanceToEdge = mc.thePlayer.getDistance(currentEdge.getX(), mc.thePlayer.posY, currentEdge.getZ());
                int playerY = mc.thePlayer.getPosition().getY();

                if (distanceToEdge > previousDistanceToCheck && distanceToEdge > 15) {
                    retriesToGettingCloser++;
                    currentState = State.ROTATE_TO_EDGE;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(2000);
                    previousDistanceToCheck = Integer.MAX_VALUE;
                    if (retriesToGettingCloser >= 2) {
                        LogUtils.sendError("[Visitors Macro] Player is not getting closer to the desk! Set the desk position in more open to sight area. Stopping visitors macro...");
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
                        KeyBindUtils.updateKeys(false, false, false, false, false, false, true, false);
                    } else if (playerY > 78) {
                        KeyBindUtils.updateKeys(true, false, false, false, false, false, playerY < 85, false);

                        if (distanceToEdge > 14) {
                            if ((aotvTpCooldown.passed() || !aotvTpCooldown.isScheduled()) && haveAotv && !rotation.rotating) {
                                PlayerUtils.rightClick();
                                long aotvCoold = (long) (150 + Math.random() * 150);
                                aotvTpCooldown.schedule(aotvCoold);
                                rotation.reset();
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
                if ((Math.abs(mc.thePlayer.rotationYaw - rotationToEdge.getLeft()) > 5 || Math.abs(mc.thePlayer.rotationPitch - randomValue) > 5) && !rotation.rotating && aotvTpCooldown.isScheduled()) {
                    aotvTpCooldown.schedule((long) (200 + Math.random() * 200));
                    rotation.reset();
                    rotation.easeTo(rotationToEdge.getLeft(), randomValue, 275 + (int) (Math.random() * 100));
                    delayClock.schedule(500);
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

                if (BlockUtils.isBlockVisible(finalDeskPos.up()) && mc.thePlayer.getDistance(finalDeskPos.getX(), finalDeskPos.getY(), finalDeskPos.getZ()) < 23) {
                    currentState = State.ROTATE_TO_DESK;
                    KeyBindUtils.stopMovement();
                    break;
                }

                stuckClock.schedule(STUCK_DELAY);

                if (distanceToCenter > previousDistanceToCheck && distanceToCenter > 15) {
                    retriesToGettingCloser++;
                    currentState = State.ROTATE_TO_CENTER;
                    KeyBindUtils.stopMovement();
                    delayClock.schedule(2000);
                    previousDistanceToCheck = Integer.MAX_VALUE;
                    if (retriesToGettingCloser >= 2) {
                        LogUtils.sendError("[Visitors Macro] Player is not getting closer to the desk! Set the desk position in more open to sight area. Stopping visitors macro...");
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

                rotationToDesk = AngleUtils.getRotation(finalDeskPos.up());
                double distance = mc.thePlayer.getDistance(finalDeskPos.getX() + 0.5, finalDeskPos.getY() + 0.5, finalDeskPos.getZ() + 0.5);
                if ((Math.abs(rotationToDesk.getLeft() - mc.thePlayer.rotationYaw) > 1 || Math.abs(rotationToDesk.getRight() - mc.thePlayer.rotationPitch) > 1) && !rotation.rotating) {
                    rotation.easeTo(rotationToDesk.getLeft(), rotationToDesk.getRight(), 250);
                }

                stuckClock.schedule(STUCK_DELAY);

                if (distance > 4.5f) {
                    KeyBindUtils.updateKeys(true, false, false, false, false, mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround, shouldJump(), !mc.thePlayer.capabilities.isFlying);
                } else if (distance <= 0.75f) {
                    currentState = State.DISABLE_COMPACTORS;
                    currentCompactorState = CompactorState.GET_LIST;
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                } else
                    KeyBindUtils.updateKeys(true, false, false, false, false, mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround, shouldJump(), false);

                previousDistanceToCheck = distance;

                break;
            case DISABLE_COMPACTORS:
                stuckClock.schedule(STUCK_DELAY);
                switch (currentCompactorState) {
                    case IDLE:
                        break;
                    case GET_LIST:
                        LogUtils.sendDebug("[Visitors Macro] Getting list of compactors");
                        for (int i = 0; i < 9; i++) {
                            if (mc.thePlayer.inventory.getStackInSlot(i) != null && mc.thePlayer.inventory.getStackInSlot(i).getDisplayName().contains("Compactor")) {
                                compactorSlots.add(i);
                            }
                        }
                        if (compactorSlots.isEmpty()) {
                            LogUtils.sendDebug("[Visitors Macro] No compactors found in the hotbar, skipping...");
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.MANAGING_VISITORS;
                            delayClock.schedule(getRandomGuiDelay());
                        } else {
                            currentCompactorState = CompactorState.HOLD_COMPACTOR;
                            delayClock.schedule(getRandomGuiDelay());
                        }
                        break;
                    case HOLD_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Compactor slots: " + compactorSlots);
                        if (compactorSlots.isEmpty()) {
                            LogUtils.sendDebug("[Visitors Macro] compactorSlots array is empty");
                            return;
                        }
                        mc.thePlayer.inventory.currentItem = compactorSlots.get(0);
                        LogUtils.sendDebug("[Visitors Macro] Switching slot to compactor");
                        currentCompactorState = CompactorState.OPEN_COMPACTOR;
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    case OPEN_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Opening compactor");
                        if (mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem) != null && mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem).getDisplayName().contains("Compactor")) {
                            LogUtils.sendDebug("[Visitors Macro] Right clicking compactor");
                            KeyBindUtils.rightClick();
                            currentCompactorState = CompactorState.TOGGLE_COMPACTOR;
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] Compactor not found in the hand, skipping...");
                            compactorSlots.clear();
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.MANAGING_VISITORS;
                        }
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    case TOGGLE_COMPACTOR:
                        if (!(mc.thePlayer.openContainer instanceof ContainerChest)) break;
                        if (PlayerUtils.getSlotFromGui("Compactor Currently") == -1) break;
                        if (PlayerUtils.getSlotFromGui("Compactor Currently ON!") != -1) {
                            LogUtils.sendDebug("[Visitors Macro] Disabling compactor");
                            PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui("Compactor Currently ON!"));
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] Compactor is already OFF, skipping...");
                        }
                        LogUtils.sendDebug("[Visitors Macro] Compactor slots: " + compactorSlots);
                        if (compactorSlots.isEmpty())
                            LogUtils.sendError("[Visitors Macro] compactorSlots is empty! This should never happen. Report this to the developers.");
                        else
                            compactorSlots.remove(0);
                        currentCompactorState = CompactorState.CLOSE_COMPACTOR;
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    case CLOSE_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Closing compactor");
                        mc.thePlayer.closeScreen();
                        if (!compactorSlots.isEmpty()) {
                            currentCompactorState = CompactorState.HOLD_COMPACTOR;
                            LogUtils.sendDebug("[Visitors Macro] Holding next compactor");
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] All compactors disabled, managing visitors");
                            compactorSlots.clear();
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.MANAGING_VISITORS;
                        }
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                }
                break;
            case MANAGING_VISITORS:
                if (config.visitorsMacroAutosellBeforeServing) {
                    if (Autosell.isEnabled()) {
                        stuckClock.schedule(STUCK_DELAY);
                        break;
                    } else {
                        if (!autoSellInvoked) {
                            stuckClock.schedule(STUCK_DELAY);
                            Autosell.enable(true);
                            autoSellInvoked = true;
                            delayClock.schedule(1500);
                            break;
                        }
                    }
                }

                if (purseBeforeVisitors == 0) {
                    purseBeforeVisitors = ProfitCalculator.getCurrentPurse();
                }

                if ((mc.thePlayer.openContainer instanceof ContainerChest)) {
                    break;
                }
                if (waitAfterTpClock.isScheduled() && !waitAfterTpClock.passed()) break;
                if (delayClock.isScheduled() && !delayClock.passed()) break;
                if (rotation.rotating) break;
                KeyBindUtils.stopMovement();
                LogUtils.sendDebug("[Visitors Macro] Looking for a visitor...");

                if (getNonToolItem() == -1) {
                    LogUtils.sendWarning("[Visitors Macro] You don't have any free (non tool) slot in your hotbar, that would be less sus if you had one." +
                            "Clicking with AOTV or hoe in your hand will be more sus, but it will still work.");
                } else {
                    mc.thePlayer.inventory.currentItem = getNonToolItem();
                }

                if (mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = false;
                    mc.thePlayer.sendPlayerAbilities();
                }

                if (noMoreVisitors()) {
                    currentState = State.ENABLE_COMPACTORS;
                    currentCompactorState = CompactorState.GET_LIST;
                    delayClock.schedule(getRandomGuiDelay());
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
                    LogUtils.sendDebug("[Visitors Macro] No visitors found, waiting...");
                    delayClock.schedule(1500);
                    return;
                }

                Entity character = PlayerUtils.getEntityCuttingOtherEntity(closest);

                if (character != null) {
                    LogUtils.sendDebug("[Visitors Macro] Found a visitor...");
                    currentVisitor = closest;
                    currentState = State.OPEN_VISITOR;
                    rotation.reset();
                    firstTimeOpen = true;
                    Pair<Float, Float> rotationToCharacter = AngleUtils.getRotation(character, true);
                    rotation.easeTo(rotationToCharacter.getLeft(), rotationToCharacter.getRight(), getRandomRotationDelay());
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

                if (mc.currentScreen == null) {
                    if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at the visitor, opening the GUI...");
                        Entity armorStand = PlayerUtils.getEntityCuttingOtherEntity(mc.objectMouseOver.entityHit, true);
                        if (armorStand != null) {
                            PlayerUtils.rightClick();
                        } else {
                            currentState = State.MANAGING_VISITORS;
                            LogUtils.sendDebug("[Visitors Macro] Unknown entity");
                        }
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Rotating again...");
                        currentState = State.MANAGING_VISITORS;
                    }
                    delayClock.schedule(getRandomGuiDelay());
                    return;
                }

                if (boughtAllItems) {
                    currentState = State.GIVE_ITEMS;
                    delayClock.schedule(getRandomGuiDelay());
                } else
                    currentState = State.BUY_ITEMS;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case BUY_ITEMS:
                String chestName = mc.thePlayer.openContainer.inventorySlots.get(0).inventory.getName();
                String visitorName = StringUtils.stripControlCodes(currentVisitor.getCustomNameTag());
                if (firstTimeOpen) {
                    firstTimeOpen = false;
                    PlayerUtils.rightClick();
                    delayClock.schedule(getRandomGuiDelay());
                    break;
                }

                if (chestName != null) {
                    switch (currentBuyState) {
                        case GET_LIST:
                            Slot npcSlot = mc.thePlayer.openContainer.inventorySlots.get(13);
                            ArrayList<String> lore = PlayerUtils.getItemLore(npcSlot.getStack());
                            boolean isNpc = lore.size() == 4 && StringUtils.stripControlCodes(lore.get(3)).startsWith("Offers Accepted: ");
                            String npcName = isNpc ? StringUtils.stripControlCodes(npcSlot.getStack().getDisplayName()) : "";
                            if (!isNpc) {
                                delayClock.schedule(750);
                                break;
                            }
                            if (!npcName.contains(visitorName)) {
                                LogUtils.sendDebug("[Visitors Macro] Opened wrong visitor GUI, closing...");
                                LogUtils.sendDebug("[Visitors Macro] Expected: " + visitorName + ", got: " + npcName);
                                mc.thePlayer.closeScreen();
                                currentState = State.MANAGING_VISITORS;
                                delayClock.schedule(getRandomGuiDelay());
                                return;
                            }
                            for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                                if (slot.getHasStack() && slot.getStack().getDisplayName().contains("Accept Offer")) {
                                    ArrayList<Pair<String, Integer>> cropsToBuy = new ArrayList<>();
                                    boolean foundRequiredItems = false;
                                    boolean foundProfit = false;
                                    ArrayList<String> offerLore = PlayerUtils.getItemLore(slot.getStack());
                                    if (FarmHelper.config.onlyAcceptProfitableVisitors) {
                                        for (String line : offerLore) {
                                            if (profitRewards.stream().anyMatch(r -> StringUtils.stripControlCodes(line.toLowerCase()).contains(StringUtils.stripControlCodes(r.toLowerCase())))) {
                                                foundProfit = true;
                                                break;
                                            }
                                        }
                                        if (!foundProfit) {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor offers a reward that is not in the profit rewards list, rejecting...");
                                            rejectOffer = true;
                                            Utils.signText = "";
                                            boughtAllItems = true;
                                            currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                            delayClock.schedule(getRandomGuiDelay());
                                            return;
                                        }
                                    }

                                    if (mc.thePlayer.openContainer.inventorySlots.get(13).getHasStack()) {
                                        if (!Config.visitorsAcceptUncommon && lore.stream().anyMatch(l -> l.contains("UNCOMMON"))) {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor is uncommon rarity, rejecting...");
                                            rejectOffer = true;
                                        }
                                        if (!Config.visitorsAcceptRare && lore.stream().anyMatch(l -> l.contains("RARE"))) {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor is common rarity, rejecting...");
                                            rejectOffer = true;
                                        }
                                        if (!Config.visitorsAcceptLegendary && lore.stream().anyMatch(l -> l.contains("LEGENDARY"))) {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor is legendary rarity, rejecting...");
                                            rejectOffer = true;
                                        }
                                        if (!Config.visitorsAcceptSpecial && lore.stream().anyMatch(l -> l.contains("SPECIAL"))) {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor is special rarity, rejecting...");
                                            rejectOffer = true;
                                        }
                                        if (rejectOffer) {
                                            Utils.signText = "";
                                            boughtAllItems = true;
                                            currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                            delayClock.schedule(getRandomGuiDelay());
                                            return;
                                        }
                                    }

                                    for (String line : offerLore) {
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
                                        LogUtils.sendDebug("[Visitors Macro] No items to buy");
                                        currentState = State.CHANGE_TO_NONE;
                                        return;
                                    }
                                    itemsToBuyForCheck.clear();
                                    itemsToBuy.addAll(cropsToBuy);
                                    itemsToBuyForCheck.addAll(cropsToBuy);
                                    currentBuyState = BuyState.OPEN_BZ;
                                    boughtAllItems = false;
                                    mc.thePlayer.closeScreen();
                                    break;
                                }
                            }
                            delayClock.schedule(getRandomGuiDelay());
                            break;
                        case OPEN_BZ:
                            if (mc.thePlayer.openContainer instanceof ContainerChest) break;
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
                                        clickSlot(slot.slotNumber);
                                        currentBuyState = BuyState.CLICK_BUY;
                                        rejectOffer = false;
                                        delayClock.schedule(getRandomGuiDelay());
                                        return;
                                    }
                                }
                                LogUtils.sendError("[Visitors Macro] Item not found on Bazaar. Rejecting...");
                                LogUtils.sendDebug(itemToBuy.getLeft());
                                rejectOffer = true;
                                Utils.signText = "";
                                boughtAllItems = true;
                                currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                delayClock.schedule(getRandomGuiDelay());
                            }
                            break;
                        case CLICK_BUY:
                            if (itemToBuy == null) {
                                currentState = State.MANAGING_VISITORS;
                                break;
                            }
                            if (PlayerUtils.getSlotFromGui("Buy Instantly") == -1) break;
                            if (PlayerUtils.getSlotFromGui("Sell Instantly") == -1) break;

                            String itemName1 = getLoreFromGuiByItemName("Buy Instantly");
                            String itemName2 = getLoreFromGuiByItemName("Sell Instantly");
                            if (itemName1 == null || itemName2 == null) break;

                            if (extractPrice(itemName1) > extractPrice(itemName2) * FarmHelper.config.visitorsMacroPriceManipulationMultiplier) {
                                LogUtils.sendWarning("[Visitors Macro] The price for " + itemToBuy.getLeft() + " has been manipulated. Rejecting...");
                                rejectOffer = true;
                                Utils.signText = "";
                                boughtAllItems = true;
                                currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                delayClock.schedule(getRandomGuiDelay());
                                break;
                            }
                            clickSlot(PlayerUtils.getSlotFromGui("Buy Instantly"));

                            Utils.signText = String.valueOf(itemToBuy.getRight());
                            currentBuyState = BuyState.CLICK_SIGN;
                            delayClock.schedule(getRandomGuiDelay());
                            break;
                        case CLICK_SIGN:
                            if (itemToBuy == null) {
                                currentState = State.MANAGING_VISITORS;
                                break;
                            }
                            if (PlayerUtils.getSlotFromGui("Custom Amount") == -1) return;
                            clickSlot(PlayerUtils.getSlotFromGui("Custom Amount"));

                            Utils.signText = String.valueOf(itemToBuy.getRight());
                            currentBuyState = BuyState.CLICK_CONFIRM;
                            delayClock.schedule(getRandomGuiDelay());
                            break;
                        case CLICK_CONFIRM:
                            if (itemToBuy == null) {
                                currentState = State.MANAGING_VISITORS;
                                break;
                            }
                            if (chestName.equals("Confirm Instant Buy")) {
                                Utils.signText = "";
                                clickSlot(13);
                                itemsToBuy.remove(itemToBuy);
                                if (!itemsToBuy.isEmpty()) {
                                    currentBuyState = BuyState.GET_LIST;
                                } else {
                                    currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                }
                                delayClock.schedule(getRandomGuiDelay());
                            }
                            break;
                        case SETUP_VISITOR_HAND_IN:
                            mc.theWorld.loadedEntityList.stream()
                                    .filter(
                                            entity -> entity.hasCustomName() && StringUtils.stripControlCodes(entity.getCustomNameTag()).contains(StringUtils.stripControlCodes(currentVisitor.getCustomNameTag()))
                                    ).filter(
                                            entity -> entity.getDistanceToEntity(mc.thePlayer) < 4
                                    ).findAny().ifPresent(visitorEntity -> {
                                        LogUtils.sendDebug("[Visitors Macro] Found visitor to give items");
                                        currentVisitor = visitorEntity;

                                        Entity characterr = PlayerUtils.getEntityCuttingOtherEntity(currentVisitor);

                                        long rotationTime = getRandomRotationDelay();
                                        if (characterr != null) {
                                            LogUtils.sendDebug("[Visitors Macro] Found a visitor and going to give items to him");
                                            rotation.reset();
                                            Pair<Float, Float> rotationTo = AngleUtils.getRotation(characterr, true);
                                            rotation.easeTo(rotationTo.getLeft(), rotationTo.getRight(), rotationTime);
                                            currentState = State.OPEN_VISITOR;
                                            boughtAllItems = true;
                                            mc.thePlayer.closeScreen();
                                            if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
                                            delayClock.schedule(rotationTime);
                                        } else {
                                            LogUtils.sendDebug("[Visitors Macro] Visitor might be a bit too far away, going to wait for him to get closer");
                                            delayClock.schedule(500);
                                        }
                                    });
                            break;
                    }
                } else {
                    currentState = State.MANAGING_VISITORS;
                }

                break;
            case GIVE_ITEMS:
                LogUtils.sendDebug("[Visitors Macro] Have items: " + haveRequiredItemsInInventory());
                if (!haveRequiredItemsInInventory() && !rejectOffer) {
                    if (!giveBackItemsClock.isScheduled()) {
                        giveBackItemsClock.schedule(15_000);
                    } else if (giveBackItemsClock.passed()) {
                        LogUtils.sendError("[Visitors Macro] Can't give items to visitor! Going to buy again...");
                        visitorsFinished.clear();
                        delayClock.schedule(3_000);
                        currentState = State.MANAGING_VISITORS;
                        currentBuyState = BuyState.GET_LIST;
                        break;
                    }
                    delayClock.schedule(getRandomGuiDelay());
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
            case ENABLE_COMPACTORS:
                switch (currentCompactorState) {
                    case IDLE:
                        break;
                    case GET_LIST:
                        LogUtils.sendDebug("[Visitors Macro] Getting list of compactors");
                        for (int i = 0; i < 9; i++) {
                            if (mc.thePlayer.inventory.getStackInSlot(i) != null && mc.thePlayer.inventory.getStackInSlot(i).getDisplayName().contains("Compactor")) {
                                compactorSlots.add(i);
                            }
                        }
                        if (compactorSlots.isEmpty()) {
                            LogUtils.sendDebug("[Visitors Macro] No compactors found in the hotbar, skipping...");
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.BACK_TO_FARMING;
                            delayClock.schedule(getRandomGuiDelay());
                        } else {
                            currentCompactorState = CompactorState.HOLD_COMPACTOR;
                            delayClock.schedule(getRandomGuiDelay());
                        }
                        break;
                    case HOLD_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Compactor slots: " + compactorSlots);
                        if (compactorSlots.isEmpty()) {
                            LogUtils.sendDebug("[Visitors Macro] compactorSlots array is empty");
                            return;
                        }
                        mc.thePlayer.inventory.currentItem = compactorSlots.get(0);
                        LogUtils.sendDebug("[Visitors Macro] Switching slot to compactor");
                        currentCompactorState = CompactorState.OPEN_COMPACTOR;
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    case OPEN_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Opening compactor");
                        if (mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem) != null && mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem).getDisplayName().contains("Compactor")) {
                            LogUtils.sendDebug("[Visitors Macro] Right clicking compactor");
                            KeyBindUtils.rightClick();
                            currentCompactorState = CompactorState.TOGGLE_COMPACTOR;
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] Compactor not found in the hand, skipping...");
                            compactorSlots.clear();
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.BACK_TO_FARMING;
                        }
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    case TOGGLE_COMPACTOR:
                        if (!(mc.thePlayer.openContainer instanceof ContainerChest)) break;
                        if (PlayerUtils.getSlotFromGui("Compactor Currently") == -1) break;
                        if (PlayerUtils.getSlotFromGui("Compactor Currently OFF!") != -1) {
                            LogUtils.sendDebug("[Visitors Macro] Disabling compactor");
                            PlayerUtils.clickOpenContainerSlot(PlayerUtils.getSlotFromGui("Compactor Currently OFF!"));
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] Compactor is already ON, skipping...");
                        }
                        LogUtils.sendDebug("[Visitors Macro] Compactor slots: " + compactorSlots);
                        if (compactorSlots.isEmpty())
                            LogUtils.sendError("[Visitors Macro] compactorSlots is empty! This should never happen. Report this to the developers.");
                        else
                            compactorSlots.remove(0);
                        currentCompactorState = CompactorState.CLOSE_COMPACTOR;
                        delayClock.schedule(750);
                        break;
                    case CLOSE_COMPACTOR:
                        LogUtils.sendDebug("[Visitors Macro] Closing compactor");
                        mc.thePlayer.closeScreen();
                        if (!compactorSlots.isEmpty()) {
                            currentCompactorState = CompactorState.HOLD_COMPACTOR;
                            LogUtils.sendDebug("[Visitors Macro] Holding next compactor");
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] All compactors enabled, going back to farm");
                            compactorSlots.clear();
                            currentCompactorState = CompactorState.IDLE;
                            currentState = State.BACK_TO_FARMING;
                        }
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                }
                break;
            case BACK_TO_FARMING:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
                    delayClock.schedule(getRandomGuiDelay());
                    break;
                }
                if (noMoreVisitors() || disableMacro) {
                    if (triggeredManually) {
                        currentState = State.CHANGE_TO_NONE;
                    } else {
                        currentState = State.TELEPORT_TO_GARDEN;
                    }
                } else {
                    currentState = State.MANAGING_VISITORS;
                }
                delayClock.schedule(1500);
                break;
            case TELEPORT_TO_GARDEN:
                MacroHandler.currentMacro.triggerWarpGarden();
                currentState = State.CHANGE_TO_NONE;
                delayClock.schedule(2000);
                break;
            case CHANGE_TO_NONE:
                LogUtils.sendSuccess("[Visitors Macro] Spent §2" + currencyFormatter.format(purseBeforeVisitors - ProfitCalculator.getCurrentPurse()) + "§a on visitors.");
                currentState = State.NONE;
                stopMacro();
                if (triggeredManually) {
                    MacroHandler.disableMacro();
                    if (config.autoUngrabMouse) {
                        UngrabUtils.regrabMouse();
                    }
                    return;
                }
                delayClock.schedule(10_000);
                if (FarmHelper.config.autoUngrabMouse) {
                    UngrabUtils.regrabMouse();
                    UngrabUtils.ungrabMouse();
                }
                return;
        }

        if (previousState != currentState) {
            LogUtils.sendDebug("[Visitors Macro] State changed from " + previousState + " to " + currentState);
            stuckClock.schedule(STUCK_DELAY);
            previousState = currentState;
        }
        if (previousBuyState != currentBuyState) {
            LogUtils.sendDebug("[Visitors Macro] Buy state changed from " + previousBuyState + " to " + currentBuyState);
            stuckClock.schedule(STUCK_DELAY);
            previousBuyState = currentBuyState;
        }
    }

    private void finishVisitor(Slot slot) {
        clickSlot(slot.slotNumber);
        visitorsFinished.add(Pair.of(StringUtils.stripControlCodes(currentVisitor.getCustomNameTag()), System.currentTimeMillis()));
        currentVisitor = null;
        currentState = State.BACK_TO_FARMING;
        currentBuyState = BuyState.GET_LIST;
        boughtAllItems = false;
        rejectOffer = false;
        itemsToBuy.clear();
        itemsToBuyForCheck.clear();
        itemToBuy = null;
        delayClock.schedule(getRandomGuiDelay());
        haveItemsClock.reset();
    }

    private boolean shouldJump() {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        EnumFacing playerFacing = mc.thePlayer.getHorizontalFacing();
        BlockPos blockInFront = playerPos.offset(playerFacing);
        Block block = mc.theWorld.getBlockState(blockInFront).getBlock();
        LogUtils.sendDebug("[Visitors Macro] Block in front: " + block);
        if (mc.thePlayer.onGround && !block.equals(Blocks.air) && !(block instanceof BlockSlab) && !(block instanceof BlockStairs)) {
            rotation.reset();
            return true;
        }
        return false;
    }

    private static boolean isAboveHeadClear() {
        for (int y = (int) mc.thePlayer.posY + 1; y < 100; y++) {
            BlockPos blockPos = BlockUtils.getRelativeBlockPos(0, y, 0);
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            if (!mc.theWorld.isAirBlock(blockPos) && !block.equals(Blocks.reeds) && !block.equals(Blocks.water) && !block.equals(Blocks.flowing_water)) {
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

    private static boolean canSeeClosestEdgeOfBarn() {
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

    private void clickSlot(int slot) {
        mc.playerController.windowClick(
                mc.thePlayer.openContainer.windowId,
                slot,
                0,
                0,
                mc.thePlayer
        );
    }

    private int getNonToolItem() {
        ArrayList<Integer> slotsWithoutItems = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] == null || mc.thePlayer.inventory.mainInventory[i].getItem() == null) {
                slotsWithoutItems.add(i);
            }
        }
        if (!slotsWithoutItems.isEmpty()) {
            return slotsWithoutItems.get(0);
        }
        for (int i = 0; i < 8; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() != null && !(itemStack.getItem() instanceof ItemTool) && !(itemStack.getItem() instanceof ItemSword) && !(itemStack.getItem() instanceof ItemHoe) && !(itemStack.getItem() instanceof ItemSpade) && !itemStack.getDisplayName().contains("Compactor")) {
                return i;
            }
        }
        return -1;
    }

    private float extractPrice(String input) {
//        Pattern pattern = Pattern.compile("§6(\\d+(?:,\\d+)*(?:\\.\\d+)?)");
        Pattern pattern = Pattern.compile("Price per unit: ([\\d,.]+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String numberString = matcher.group(1);
            String cleanNumberString = numberString.replaceAll(",", "");
            return Float.parseFloat(cleanNumberString);
        } else {
            return -1;
        }
    }

    private String getLoreFromGuiByItemName(String name) {
        if (!(mc.thePlayer.openContainer instanceof ContainerChest)) return null;
        if (PlayerUtils.getSlotFromGui(name) == -1) return null;
        if (PlayerUtils.getStackInOpenContainerSlot(PlayerUtils.getSlotFromGui(name)) == null) return null;
        if (PlayerUtils.getLore(PlayerUtils.getStackInOpenContainerSlot(PlayerUtils.getSlotFromGui(name))) == null)
            return null;
        return StringUtils.stripControlCodes(Objects.requireNonNull(PlayerUtils.getLore(PlayerUtils.getStackInOpenContainerSlot(PlayerUtils.getSlotFromGui(name)))).toString());
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onChatSetSpawn(ClientChatReceivedEvent event) {
        if (event.type != 0 || event.message == null) return;
        if (event.message.getUnformattedText().contains("Your spawn location has been set!")) {
            PlayerUtils.setSpawnLocation();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (rotation.rotating && !(mc.thePlayer.openContainer instanceof ContainerChest))
            rotation.update();
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;

        if (isDeskPosSet() && config.drawVisitorsDeskLocation) {
            BlockPos deskPosTemp = new BlockPos(FarmHelper.config.visitorsDeskPosX, FarmHelper.config.visitorsDeskPosY, FarmHelper.config.visitorsDeskPosZ);
            RenderUtils.drawBlockBox(deskPosTemp, new Color(Color.DARK_GRAY.getRed(), Color.DARK_GRAY.getGreen(), Color.DARK_GRAY.getBlue(), 80));
        }

        if (config.drawSpawnLocation) {
            BlockPos spawnLocation = new BlockPos(PlayerUtils.getSpawnLocation());
            RenderUtils.drawBlockBox(spawnLocation, new Color(Color.orange.getRed(), Color.orange.getGreen(), Color.orange.getBlue(), 80));
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (!FarmHelper.config.visitorsMacro) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!FarmHelper.config.debugMode) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;

        ArrayList<Pair<String, Integer>> itemsToBuyCopy = new ArrayList<>(itemsToBuy);

        int x = 120;

        mc.fontRendererObj.drawString("State: " + currentState, x, 2, Color.WHITE.hashCode(), true);
        mc.fontRendererObj.drawString("Buy state: " + currentBuyState, x, 12, Color.WHITE.hashCode(), true);
        mc.fontRendererObj.drawString("Stuck timer: " + (stuckClock.getRemainingTime() > 0 ? stuckClock.getRemainingTime() : "None"), x, 22, Color.WHITE.hashCode(), true);
        mc.fontRendererObj.drawString("Distance to check: " + previousDistanceToCheck, x, 32, Color.WHITE.hashCode(), true);
        mc.fontRendererObj.drawString("Items to buy: ", x, 42, Color.WHITE.hashCode(), true);
        for (Pair<String, Integer> item : itemsToBuyCopy) {
            mc.fontRendererObj.drawString(item.getLeft() + " x" + item.getRight(), x + 5, 52 + (itemsToBuyCopy.indexOf(item) * 10), Color.WHITE.hashCode(), true);
        }
        if (!itemsToBuyCopy.isEmpty())
            mc.fontRendererObj.drawString("Have items in inventory: " + haveRequiredItemsInInventory(), x, 52 + (itemsToBuyCopy.size() * 10), Color.WHITE.hashCode(), true);

    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (event.type != 0 || event.message == null) return;
        if (event.message.getUnformattedText().contains("[Bazaar] You cannot afford this!")) {
            disableMacro = true;
            currentState = State.ENABLE_COMPACTORS;
            LogUtils.sendError("[Visitors Macro] You cannot afford this! Stopping visitors macro...");
        }
    }

    public static boolean isPlayerInsideBarn() {
        BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        return playerPos.getX() >= -33 && playerPos.getX() <= 35 && playerPos.getZ() >= -46 && playerPos.getZ() <= -5;
    }

    public static boolean isDeskPosSet() {
        return FarmHelper.config.visitorsDeskPosX != 0 || FarmHelper.config.visitorsDeskPosY != 0 || FarmHelper.config.visitorsDeskPosZ != 0;
    }

    private static long getRandomRotationDelay() {
        return (long) ((random.nextDouble() * config.rotationTimeRandomness * 1000) + (config.rotationTime * 1000));
    }

    private static long getRandomGuiDelay() {
        return (long) ((random.nextDouble() * config.visitorsMacroGuiDelayRandomness * 1000) + (config.visitorsMacroGuiDelay * 1000));
    }
}