package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class AutoSlugSell implements IFeature {

    private static AutoSlugSell instance;
    public static AutoSlugSell getInstance() {
        if (instance == null) {
            instance = new AutoSlugSell();
        }

        return instance;
    }

    @Getter
    private boolean teleporting = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private State state = State.STARTING;
    private final Clock delay = new Clock();

    @Override
    public String getName() {
        return "AutoSlugSell";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        state = State.STARTING;
        delay.reset();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoSlugSellMode != 0;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) return;
        if (!isToggled()) return;

        MacroHandler.getInstance().pauseMacro();
        enabled = true;
        delay.schedule(500);
        state = FarmHelperConfig.autoSlugSellMode == 1 ? State.STARTING : State.FIND_ABIPHONE;

        LogUtils.sendWarning("[Auto Slug Sell] Starting.");
    }

    @Override
    public void stop() {
        if (!enabled) {
            return;
        }

        enabled = false;
        state = State.SUSPEND;
        delay.reset();

        LogUtils.sendWarning("[Auto Slug Sell] Stopping.");
        MacroHandler.getInstance().resumeMacro();
    }

    private Entity george = null;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (mc.currentScreen != null && state.ordinal() < 3) {
            stop();
            return;
        }

        switch (state) {
            case STARTING:
                if (delay.passed()) {
                    teleporting = true;
                    mc.thePlayer.sendChatMessage("/hub");
                    delay.schedule(500);
                    state = State.WALK;
                }
                break;
            case WALK:
                if (delay.passed() && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                    teleporting = false;
                    BaritoneHandler.walkToBlockPos(new BlockPos(39, 71, -99));
                    george = null;
                    state = State.ROTATE;
                }
                break;
            case ROTATE:
                if (!BaritoneHandler.isPathing() && !BaritoneHandler.isWalkingToGoalBlock()) {
                    if (george == null) {
                        george = getGeorge();
                        return;
                    }

                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(getGeorge()),
                        FarmHelperConfig.getRandomRotationTime(),
                        null
                    ));

                    delay.schedule(500);
                    state = State.CLICK_GEORGE;
                }
                break;
            case CLICK_GEORGE:
                if (!RotationHandler.getInstance().isRotating()) {
                    RotationHandler.getInstance().reset();
                    KeyBindUtils.leftClick();
                    delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    state = State.CLICK_PET;
                }
                break;
            case FIND_ABIPHONE:
                if (delay.passed()) {
                    if (InventoryUtils.holdItem("Abiphone")) {
                        delay.schedule(500);
                        state = State.ABIPHONE_CALL;
                    } else {
                        LogUtils.sendError("[Auto Slug Sell] Unable to find Abiphone. Disabling Macro");
                        state = State.SUSPEND;
                        FarmHelperConfig.autoSlugSellMode = 0;
                        MacroHandler.getInstance().disableMacro();
                        return;
                    }
                }
                break;
            case ABIPHONE_CALL:
                if (delay.passed()) {
                    KeyBindUtils.rightClick();
                    delay.schedule(500);
                    state = State.FIND_GEORGE;
                }
                break;
            case FIND_GEORGE:
                Slot george = InventoryUtils.getSlotOfItemInContainer("George");
                if (mc.currentScreen != null && delay.passed()) {
                    if (george != null) {
                        InventoryUtils.clickContainerSlot(george.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        state = State.CLICK_PET;
                    } else {
                        LogUtils.sendError("[Auto Slug Sell] Unable to find George (you need his contact). Disabling Macro");
                        state = State.SUSPEND;
                        FarmHelperConfig.autoSlugSellMode = 0;
                        MacroHandler.getInstance().disableMacro();
                    }
                }
                break;
            case CLICK_PET:
                if (delay.passed() && InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Offer Pets")) {
                    int id = InventoryUtils.getSlotIdOfItemInContainer("[Lvl 1] Slug");
                    InventoryUtils.clickContainerSlot(id, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.QUICK_MOVE);
                    delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    state = State.SELL_PET;
                }
                break;
            case SELL_PET:
                if (delay.passed()) {
                    int id1 = InventoryUtils.getSlotIdOfItemInContainer("Accept Offer");
                    InventoryUtils.clickContainerSlot(id1, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delay.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    state = State.SELL_VERIFY;
                }
                break;
            case SELL_VERIFY:
                if (delay.passed()) {
                    int id2 = InventoryUtils.getSlotIdOfItemInContainer("Confirm");
                    InventoryUtils.clickContainerSlot(id2, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delay.schedule(500);
                    state = State.FINISH;
                }
                break;
            case FINISH:
                if (delay.passed()) {
                    if (countSlug() > 0) {
                        state = FarmHelperConfig.autoSlugSellMode == 1 ? State.CLICK_GEORGE : State.FIND_ABIPHONE;
                    } else {
                        if (FarmHelperConfig.autoSlugSellMode == 1)
                            MacroHandler.getInstance().triggerWarpGarden(true, true, false);

                        stop();
                    }
                }

                break;
        }
    }

    @Nullable
    private Entity getGeorge() {
        return mc.theWorld.getLoadedEntityList()
                .stream()
                .filter(entity -> {
                    if (!(entity instanceof EntityOtherPlayerMP)) return false;
                    EntityOtherPlayerMP player = (EntityOtherPlayerMP) entity;
                    return player.getGameProfile().getProperties().get("textures")
                            .stream().anyMatch(p -> p.getValue().contains("eyJ0aW1lc3RhbXAiOjE1ODU0NTcyMDE0MzQsInByb2ZpbGVJZCI6IjllOGI4NzQ3YTMxNzQ0ZTY4YTQ4NzEzMzQwM2I0ZDM1IiwicHJvZmlsZU5hbWUiOiJFdmlsRGN0ciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTliYjEzNzM3OTc5Zjk4YmEyMzZlMzdhZTQ4ZmQ1ZGRkZWRiYTcxOWVjNjIwNjhlOGFjOTYyYWJmN2UwN2Q4MyJ9fX0="));
                })
                .min(Comparator.comparingDouble(entity -> entity.getDistanceSqToCenter(mc.thePlayer.getPosition()))).orElse(null);
    }

    public int countSlug() {
        int count = 0;

        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String name = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (name.contains("[Lvl 1] Slug")) count++;
            }
        }

        return count;
    }

    public boolean canEnable() {
        return countSlug() > 2 && isToggled();
    }

    enum State {
        STARTING,

        WALK,
        ROTATE,
        CLICK_GEORGE,

        FIND_ABIPHONE,
        ABIPHONE_CALL,
        FIND_GEORGE,

        CLICK_PET,
        SELL_PET,
        SELL_VERIFY,

        FINISH,
        SUSPEND
    }
}
