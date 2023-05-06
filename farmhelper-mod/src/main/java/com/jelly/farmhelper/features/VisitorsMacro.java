package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


// A lot of "inspiration" from GTC 🫡 Credits to RoseGold (got permission to use this code)
public class VisitorsMacro {
    private static final Minecraft mc = Minecraft.getMinecraft();


    public enum State {
        FARMING,
        TRY_TO_SET_SPAWN,
        FLYING_TO_110_Y,
        ROTATE_TO_DESK,
        MOVE_TOWARDS_DESK,
        TELEPORT_TO_DESK,
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
    public static boolean rejectOffer = false;
    private static final Pattern pattern = Pattern.compile("^ [\\w ]+", Pattern.CASE_INSENSITIVE);
    public static String signText = "";

    public static final ArrayList<String> visitors = new ArrayList<>();
    public static final ArrayList<String> visitorsFinished = new ArrayList<>();
    public static final ArrayList<Pair<String, Integer>> itemsToBuy = new ArrayList<>();
    public static Pair<String, Integer> itemToBuy = null;
    public static boolean boughtAllItems = false;

    public static boolean macroNotRunning() {
        return currentState == State.FARMING || currentState == State.NONE;
    }

    public static void stopMacro() {
        System.out.println("Stopping macro");
        currentState = State.NONE;
        clock.reset();
        rotation.reset();
        waitAfterTpClock.reset();
        boughtAllItems = false;
        KeyBindUtils.stopMovement();
        visitorsFinished.clear();
        itemsToBuy.clear();
        itemToBuy = null;
        currentBuyState = BuyState.IDLE;
        currentVisitor = null;
        signText = "";
        stuckClock.reset();
        previousState = State.NONE;
        previousBuyState = BuyState.IDLE;
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
    public void onTick(TickEvent.ClientTickEvent event) {
        if (FarmHelper.gameState.currentLocation != GameState.location.ISLAND) return;
        if (!MacroHandler.isMacroing) return;
        if (!MiscConfig.visitorsMacro) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (Failsafe.waitAfterVisitorMacroCooldown.isScheduled() && !Failsafe.waitAfterVisitorMacroCooldown.passed()) {
            return;
        }

        if (clock.isScheduled() && !clock.passed()) {
            return;
        } else if (clock.isScheduled()) {
            clock.reset();
            return;
        }


        if (macroNotRunning() && TablistUtils.getTabList().stream().noneMatch(line -> StringUtils.stripControlCodes(line).contains("Queue Full!"))) {
            LogUtils.debugLog("Queue is not full, waiting...");
            return;
        }

        if (macroNotRunning() && !isAboveHeadClear()) {
            LogUtils.debugLog("Player doesn't have clear space above their head, still going.");
            clock.schedule(5000);
            return;
        }
        Block blockUnder = BlockUtils.getRelativeBlock(0, -1, 0);
        if (macroNotRunning() && !BlockUtils.canSetSpawn(blockUnder)) {
            LogUtils.debugLog("Can't setspawn here, still going.");
            return;
        }
        int aspectOfTheVoid = PlayerUtils.getItemInHotbar("Aspect of the Void");
        if (aspectOfTheVoid == -1) {
            ConfigHandler.set("visitorsMacro", false);
            LogUtils.debugLog("Disabling this feature, player doesn't have AOTV (Aspect of the Void)");
            return;
        }

        if ((MiscConfig.visitorsDeskPosX == 0 && MiscConfig.visitorsDeskPosY == 0 && MiscConfig.visitorsDeskPosZ == 0) ) {
            LogUtils.scriptLog("Desk position is not set, disabling this feature. Please set it up with the keybind");
            ConfigHandler.set("visitorsMacro", false);
            return;
        }

        if (currentState == State.NONE && MacroHandler.isMacroing) {
            currentState = State.FARMING;
            rotation.reset();
            rotation.completed = true;
            LogUtils.debugLog("Visitors macro is started");
            clock.schedule(3000);
            stuckClock.schedule(60_000);
            return;
        }

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.scriptLog("Player is stuck, resetting macro");
            stopMacro();
            mc.thePlayer.closeScreen();
            currentState = State.MANAGING_VISITORS;
            currentBuyState = BuyState.IDLE;
            stuckClock.reset();
            return;
        }


        switch (currentState) {
            case FARMING:
                PlayerUtils.setSpawn();
                System.out.println("Setting spawn");
                currentState = State.TRY_TO_SET_SPAWN;
                clock.schedule(1500);
                break;
            case TRY_TO_SET_SPAWN:
                // idle to wait for chatmessage
                break;
            case FLYING_TO_110_Y:
                int playerY = mc.thePlayer.getPosition().getY();

                if (playerY < 100) {
                    // toggle flying
                    if (mc.thePlayer.capabilities.isFlying) {
                        KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
                    } else {
                        mc.thePlayer.capabilities.isFlying = true;
                        mc.thePlayer.sendPlayerAbilities();
                    }
                } else {
                    KeyBindUtils.stopMovement();
                    currentState = State.ROTATE_TO_DESK;
                }
                break;
            case ROTATE_TO_DESK:
                if (mc.currentScreen != null) break;

                BlockPos deskPosTemp = new BlockPos(MiscConfig.visitorsDeskPosX + 0.5, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), MiscConfig.visitorsDeskPosZ + 0.5);

                Pair<Float, Float> rotationToDesk = AngleUtils.getRotation(deskPosTemp);

                rotation.easeTo(rotationToDesk.getLeft(), rotationToDesk.getRight(), 500);

                currentState = State.MOVE_TOWARDS_DESK;

                break;
            case MOVE_TOWARDS_DESK:
                if (rotation.rotating) break;
                if (mc.currentScreen != null) break;

                BlockPos finalDeskPos = new BlockPos(MiscConfig.visitorsDeskPosX, MiscConfig.visitorsDeskPosY + 1, MiscConfig.visitorsDeskPosZ);

                if (PlayerUtils.getItemInHotbar("Void") == -1) {
                    LogUtils.scriptLog("Player doesn't have Aspect of the Void, stopping macro.");
                    stopMacro();
                    ConfigHandler.set("visitorsMacro", false);
                    break;
                }

                mc.thePlayer.inventory.currentItem = PlayerUtils.getItemInHotbar("Void");

                double distance = mc.thePlayer.getDistance(finalDeskPos.getX(), finalDeskPos.getY(), finalDeskPos.getZ());

                if (BlockUtils.isBlockVisible(finalDeskPos) && distance < 50) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, true, false);
                    if (FarmHelper.gameState.dx > 0.01 || FarmHelper.gameState.dz > 0.01) {
                        break;
                    }

                    rotationToDesk = AngleUtils.getRotation(finalDeskPos);
                    rotation.easeTo(rotationToDesk.getLeft(), rotationToDesk.getRight(), 800);

                    currentState = State.TELEPORT_TO_DESK;
                    break;
                }

                KeyBindUtils.updateKeys(true, false, false, false, false, false, false, true);

                break;
            case TELEPORT_TO_DESK:
                if (rotation.rotating) break;

                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                currentState = State.MANAGING_VISITORS;
                waitAfterTpClock.schedule(1000L);

                break;
            case MANAGING_VISITORS:
                if (mc.currentScreen != null) {

                    break;
                }
                if (waitAfterTpClock.isScheduled() && !waitAfterTpClock.passed()) break;
                if (delayClock.isScheduled() && !delayClock.passed()) break;
                if (rotation.rotating) break;
                KeyBindUtils.stopMovement();
                LogUtils.scriptLog("Looking for a visitor...");

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
                    if (visitorsFinished.contains(StringUtils.stripControlCodes(entity.getCustomNameTag()))) {
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
                    rotation.easeTo(AngleUtils.getRotation(character).getLeft(), AngleUtils.getRotation(character).getRight(), 1000);
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
                mc.playerController.interactWithEntitySendPacket(mc.thePlayer, currentVisitor);
                if (boughtAllItems)
                    currentState = State.GIVE_ITEMS;
                else
                    currentState = State.BUY_ITEMS;
                delayClock.schedule(250);
                break;
            case BUY_ITEMS:
                if (delayClock.isScheduled() && !delayClock.passed()) break;
                String chestName = mc.thePlayer.openContainer.inventorySlots.get(0).inventory.getName();

                System.out.println(chestName);

                if (chestName != null) {
                    switch (currentBuyState) {
                        case IDLE:
                            if (!itemsToBuy.isEmpty()) {
                                itemToBuy = itemsToBuy.get(0);
                                mc.thePlayer.sendChatMessage("/bz " + itemToBuy.getLeft());
                                delayClock.schedule(750);
                                currentBuyState = BuyState.CLICK_CROP;
                                break;
                            }
                            for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                                if (slot.getHasStack() && slot.getStack().getDisplayName().contains("Accept Offer")) {
                                    ArrayList<Pair<String, Integer>> cropsToBuy = new ArrayList<>();
                                    for (int i = 1; i < 4; i++) {
                                        String lore = PlayerUtils.getItemLore(slot.getStack(), i);
                                        System.out.println(lore);
                                        if (lore != null && pattern.matcher(StringUtils.stripControlCodes(lore)).matches()) {
                                            String cleanLore = StringUtils.stripControlCodes(lore);
                                            if (cleanLore.contains("x")) {
                                                String[] split = cleanLore.split("x");
                                                cropsToBuy.add(Pair.of(split[0].trim(), Integer.parseInt(split[1].replace(",", "").trim())));
                                            } else {
                                                cropsToBuy.add(Pair.of(cleanLore.trim(), 1));
                                            }
                                        }
                                    }
                                    if (cropsToBuy.isEmpty()) {
                                        LogUtils.scriptLog("No items to buy");
                                        stopMacro();
                                        return;
                                    }
                                    itemsToBuy.addAll(cropsToBuy);
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
                                System.out.println("Item not found");
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
                                itemsToBuy.remove(itemToBuy);
                                if (!itemsToBuy.isEmpty()) {
                                    currentBuyState = BuyState.IDLE;
                                } else {
                                    currentBuyState = BuyState.SETUP_VISITOR_HAND_IN;
                                }
                                clickSlot(13, 0);
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

                                        currentState = State.OPEN_VISITOR;
                                        boughtAllItems = true;
                                        mc.thePlayer.closeScreen();
                                        delayClock.schedule(250);
                                    });
                            break;
                    }
                } else {
                    currentState = State.MANAGING_VISITORS;
                }

                break;
            case GIVE_ITEMS:
                if (!hasRequiredItemsInInventory() && !rejectOffer) {
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
                if (noMoreVisitors()) {
                    currentState = State.TELEPORT_TO_GARDEN;
                    delayClock.schedule(2000);
                    break;
                } else {
                    currentState = State.MANAGING_VISITORS;
                }
                delayClock.schedule(1500);
                break;
            case TELEPORT_TO_GARDEN:
                Failsafe.waitAfterVisitorMacroCooldown.schedule(7_500);
                mc.thePlayer.sendChatMessage("/warp garden");
                currentState = State.CHANGE_TO_NONE;
                delayClock.schedule(2500);
                break;
            case CHANGE_TO_NONE:
                currentState = State.NONE;
                stopMacro();
                delayClock.schedule(10_000);
                break;
        }

        if (previousState != currentState) {
            System.out.println("State changed from " + previousState + " to " + currentState);
            stuckClock.schedule(60_000);
            previousState = currentState;
        }
        if (previousBuyState != currentBuyState) {
            System.out.println("Buy state changed from " + previousBuyState + " to " + currentBuyState);
            stuckClock.schedule(60_000);
            previousBuyState = currentBuyState;
        }
    }

    private void finishVisitor(Slot slot) {
        clickSlot(slot.slotNumber, 0);
        visitorsFinished.add(StringUtils.stripControlCodes(currentVisitor.getCustomNameTag()));
        currentVisitor = null;
        currentState = State.BACK_TO_FARMING;
        currentBuyState = BuyState.IDLE;
        boughtAllItems = false;
        rejectOffer = false;
        itemsToBuy.clear();
        itemToBuy = null;
        delayClock.schedule(250);
    }

    private boolean isAboveHeadClear() {
        for (int y = (int) mc.thePlayer.posY + 1; y < 110; y++) {
            BlockPos blockPos = new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
            if (!mc.theWorld.isAirBlock(blockPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasRequiredItemsInInventory() {
        for (Pair<String, Integer> item : itemsToBuy) {
            if (!hasItem(item.getLeft(), item.getRight())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasItem(String name, int amount) {
        int count = 0;
        for (ItemStack itemStack : mc.thePlayer.inventory.mainInventory) {
            if (itemStack != null && StringUtils.stripControlCodes(itemStack.getDisplayName()).equals(name)) {
                count += itemStack.stackSize;
            }
        }
        return count >= amount;
    }

    private boolean noMoreVisitors() {
        return visitors.isEmpty() || visitorsFinished.size() == 5;
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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You cannot set your spawn here!")) {
            currentState = State.FARMING;
            if (!clock.isScheduled()) {
                clock.schedule(5000L);
            }
        } else if (message.contains("Your spawn location has been set!")) {
            currentState = State.FLYING_TO_110_Y;
            clock.schedule(1500L);
            KeyBindUtils.stopMovement();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (rotation.rotating)
            rotation.update();

        if ((MiscConfig.visitorsDeskPosX == 0 && MiscConfig.visitorsDeskPosY == 0 && MiscConfig.visitorsDeskPosZ == 0)) {
            return;
        }

        BlockPos deskPosTemp = new BlockPos(MiscConfig.visitorsDeskPosX, MiscConfig.visitorsDeskPosY, MiscConfig.visitorsDeskPosZ);
        RenderUtils.drawBlockBox(deskPosTemp, Color.DARK_GRAY);
    }
}