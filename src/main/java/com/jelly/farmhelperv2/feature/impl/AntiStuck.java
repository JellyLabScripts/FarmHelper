package com.jelly.farmhelperv2.feature.impl;

import com.google.common.collect.ImmutableMap;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/*
    Credits to Nirox for this superb class
*/
@Getter
public class AntiStuck implements IFeature {
    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0.5, 1), // South
            new Vec3(0, 0.5, 0.5), // West
            new Vec3(0.5, 0.5, 0), // North
            new Vec3(1, 0.5, 0.5)  // East
    };
    private static AntiStuck instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Clock delayBetweenMovementsClock = new Clock();
    private final Map<Integer, KeyBinding> keyBindMap = ImmutableMap.of(
            0, mc.gameSettings.keyBindForward,
            90, mc.gameSettings.keyBindLeft,
            180, mc.gameSettings.keyBindBack,
            -90, mc.gameSettings.keyBindRight
    );
    private UnstuckState unstuckState = UnstuckState.NONE;
    private boolean enabled = false;
    @Setter
    private BlockPos intersectingBlockPos = null;
    private ArrayList<KeyBinding> oppositeKeys = null;

    public static AntiStuck getInstance() {
        if (instance == null) {
            instance = new AntiStuck();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "AntiStuck";
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
    public void start() {
        if (enabled) return;
        LogUtils.sendWarning("[Anti Stuck] Enabled");
        if (Failsafe.getInstance().getEmergencyQueue().contains(Failsafe.EmergencyType.TELEPORT_CHECK) ||
                Failsafe.getInstance().getEmergencyQueue().contains(Failsafe.EmergencyType.ROTATION_CHECK))
            Failsafe.getInstance().stop();
        enabled = true;
        unstuckState = UnstuckState.NONE;
        KeyBindUtils.stopMovement();
    }

    @Override
    public void stop() {
        if (enabled) {
            LogUtils.sendWarning("[Anti Stuck] Disabled");
        }
        long randomTime = FarmHelperConfig.getRandomTimeBetweenChangingRows();
        if (randomTime < 350) {
            randomTime = 350;
        }
        GameStateHandler.getInstance().scheduleNotMoving((int) randomTime);
        enabled = false;
        unstuckState = UnstuckState.NONE;
        intersectingBlockPos = null;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enableAntiStuck;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled() ||
                MacroHandler.getInstance().isTeleporting() ||
                !AntiStuck.getInstance().isToggled() ||
                AntiStuck.getInstance().isRunning() ||
                FeatureManager.getInstance().isAnyOtherFeatureEnabled(this) ||
                !MacroHandler.getInstance().isCurrentMacroEnabled() ||
                !GameStateHandler.getInstance().inGarden()) {
            return;
        }

        getIntersectingPos().ifPresent(pos -> {
            intersectingBlockPos = pos;
            start();
        });
    }

    private KeyBinding getOppositeKey(KeyBinding key) {
        if (key.equals(mc.gameSettings.keyBindForward)) {
            return mc.gameSettings.keyBindBack;
        } else if (key.equals(mc.gameSettings.keyBindBack)) {
            return mc.gameSettings.keyBindForward;
        } else if (key.equals(mc.gameSettings.keyBindLeft)) {
            return mc.gameSettings.keyBindRight;
        } else if (key.equals(mc.gameSettings.keyBindRight)) {
            return mc.gameSettings.keyBindLeft;
        } else {
            return null;
        }
    }

    private Optional<BlockPos> getIntersectingPos() {
        BlockPos playerPos = mc.thePlayer.getPosition();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);

                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (block.isPassable(mc.theWorld, pos)) {
                        continue;
                    }

                    AxisAlignedBB blockBox = mc.theWorld.getBlockState(pos).getBlock().getSelectedBoundingBox(mc.theWorld, pos);
                    if (blockBox == null) {
                        continue;
                    }

                    if (mc.thePlayer.getEntityBoundingBox().intersectsWith(blockBox)) {
                        Vec3 posCenter = BlockUtils.getBlockPosCenter(pos);
                        if (BlockUtils.getHorizontalDistance(mc.thePlayer.getPositionVector(), posCenter) < 0.95) {
                            return Optional.of(pos);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    @SubscribeEvent
    public void onTickUnstuck(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) {
            if (isRunning())
                stop();
            return;
        }
        if (mc.currentScreen != null) return;
        if (!enabled) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (delayBetweenMovementsClock.isScheduled() && !delayBetweenMovementsClock.passed()) return;

        if (delayBetweenMovementsClock.getRemainingTime() < -1000) {
            LogUtils.sendError("[Anti Stuck] Something went wrong. Resuming macro execution...");
            stop();
            return;
        }

        switch (unstuckState) {
            case NONE:
                KeyBindUtils.stopMovement();
                unstuckState = UnstuckState.PRESS;
                delayBetweenMovementsClock.schedule(150 + (int) (Math.random() * 150));
                break;
            case PRESS:
                if (intersectingBlockPos == null) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindBack);
                    unstuckState = UnstuckState.RELEASE;
                    delayBetweenMovementsClock.schedule(100 + (int) (Math.random() * 100));
                    break;
                }
                Optional<EnumFacing> closestSide = findClosestSide(intersectingBlockPos);
                if (!closestSide.isPresent()) {
                    LogUtils.sendError("[Anti Stuck] Can't unstuck from this place. That's a rare occurrence. Warping back to spawn...");
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                    unstuckState = UnstuckState.DISABLE;
                    delayBetweenMovementsClock.schedule(150 + (int) (Math.random() * 250));
                    return;
                }
                EnumFacing facing = closestSide.get();
                Vec3 movementTarget = getMovementTarget(intersectingBlockPos, facing);
                List<KeyBinding> keys = getNeededKeyPresses(mc.thePlayer.getPositionVector(), movementTarget);
                oppositeKeys = new ArrayList<>();
                for (KeyBinding key : keys) {
                    oppositeKeys.add(getOppositeKey(key));
                }
                keys.add(mc.gameSettings.keyBindSneak);
                keys.add(mc.gameSettings.keyBindAttack);
                KeyBindUtils.holdThese(keys.toArray(new KeyBinding[0]));
                unstuckState = UnstuckState.RELEASE;
                delayBetweenMovementsClock.schedule(100 + (int) (Math.random() * 100));
                break;
            case RELEASE:
                KeyBindUtils.stopMovement();
                unstuckState = UnstuckState.COME_BACK;
                delayBetweenMovementsClock.schedule(100 + (int) (Math.random() * 100));
                break;
            case COME_BACK:
                KeyBindUtils.holdThese(oppositeKeys.toArray(new KeyBinding[0]));
                unstuckState = UnstuckState.DISABLE;
                delayBetweenMovementsClock.schedule(100 + (int) (Math.random() * 100));
                break;
            case DISABLE:
                KeyBindUtils.stopMovement();
                stop();
                break;
        }
    }

    private Vec3 getMovementTarget(BlockPos pos, EnumFacing facing) {
        Vec3i directionVec = facing.getDirectionVec();
        return BLOCK_SIDE_MULTIPLIERS[facing.getHorizontalIndex()]
                .addVector(pos.getX(), pos.getY(), pos.getZ())
                .addVector(directionVec.getX(), directionVec.getY(), directionVec.getZ());
    }

    private Optional<EnumFacing> findClosestSide(BlockPos pos) {
        return Arrays.stream(EnumFacing.HORIZONTALS)
                .filter(facing -> isSideClear(pos, facing))
                .min(Comparator.comparingDouble(facing -> getDistanceToSide(pos, facing)));
    }

    private double getDistanceToSide(BlockPos pos, EnumFacing facing) {
        Vec3 sideCenter = BLOCK_SIDE_MULTIPLIERS[facing.getHorizontalIndex()].addVector(pos.getX(), pos.getY(), pos.getZ());
        return BlockUtils.getHorizontalDistance(mc.thePlayer.getPositionVector(), sideCenter);
    }

    private boolean isSideClear(BlockPos pos, EnumFacing facing) {
        BlockPos adjacentPos = pos.add(facing.getDirectionVec());
        return BlockUtils.getBlock(adjacentPos).isPassable(mc.theWorld, adjacentPos);
    }

    private List<KeyBinding> getNeededKeyPresses(Vec3 orig, Vec3 dest) {
        List<KeyBinding> keys = new ArrayList<>();

        double[] delta = {orig.xCoord - dest.xCoord, orig.zCoord - dest.zCoord};
        float requiredAngle = (float) (MathHelper.atan2(delta[0], -delta[1]) * (180.0 / Math.PI));

        float angleDifference = normalizeYaw(requiredAngle - mc.thePlayer.rotationYaw) * -1;

        keyBindMap.forEach((yaw, key) -> {
            if (Math.abs(yaw - angleDifference) < 67.5 || Math.abs(yaw - (angleDifference + 360.0)) < 67.5) {
                keys.add(key);
            }
        });
        return keys;
    }

    private float normalizeYaw(float yaw) {
        float newYaw = yaw % 360F;
        if (newYaw < -180F) {
            newYaw += 360F;
        }
        if (newYaw > 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }

    enum UnstuckState {
        NONE,
        PRESS,
        RELEASE,
        COME_BACK,
        DISABLE
    }

}
