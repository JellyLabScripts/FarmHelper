package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.AngleUtils;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.RotationUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.Timer;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.OptionalInt;

@Getter
public class AntiStuck implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static AntiStuck instance;

    public static AntiStuck getInstance() {
        if (instance == null) {
            instance = new AntiStuck();
        }
        return instance;
    }

    enum UnstuckState {
        NONE,
        PRESS,
        RELEASE,
        DISABLE
    }

    private UnstuckState unstuckState = UnstuckState.NONE;

    private boolean enabled = false;

    private final Clock delayBetweenMovementsClock = new Clock();
    private final Timer notMovingTimer = new Timer();

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
        notMovingTimer.schedule();
        LogUtils.sendWarning("[Anti Stuck] Enabled");
        enabled = true;
        unstuckState = UnstuckState.NONE;
        notMovingTimer.schedule();
        KeyBindUtils.stopMovement();
    }

    @Override
    public void stop() {
        if (enabled) {
            LogUtils.sendWarning("[Anti Stuck] Disabled");
        }
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

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0.5, 1), // South
            new Vec3(0, 0.5, 0.5), // West
            new Vec3(0.5, 0.5, 0), // North
            new Vec3(1, 0.5, 0.5)  // East
    };

    private BlockPos intersectingBlockPos = null;

    @SubscribeEvent
    public void onTick2(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().isTeleporting() || !AntiStuck.getInstance().isToggled() || AntiStuck.getInstance().isRunning() || FeatureManager.getInstance().isAnyOtherFeatureEnabled(this) || !MacroHandler.getInstance().isCurrentMacroEnabled())
            return;

        Pair<Boolean, BlockPos> isPlayerIntersectingWithBlock = isPlayerIntersectingWithBlock();

        if (!isPlayerIntersectingWithBlock.getLeft()) return;

        intersectingBlockPos = isPlayerIntersectingWithBlock.getRight();
        start();
    }

    @SubscribeEvent
    public void onTickUnstuck(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (mc.currentScreen != null) return;
        if (!enabled) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (delayBetweenMovementsClock.isScheduled() && !delayBetweenMovementsClock.passed()) return;

        switch (unstuckState) {
            case NONE:
                KeyBindUtils.stopMovement();
                unstuckState = UnstuckState.PRESS;
                delayBetweenMovementsClock.schedule(300 + (int) (Math.random() * 250));
                break;
            case PRESS:
                OptionalInt facingInt = findClosestSide(mc.thePlayer.getPositionVector(), intersectingBlockPos);
                if (!facingInt.isPresent() || facingInt.getAsInt() == -1) {
                    LogUtils.sendError("[Anti Stuck] Can't unstuck from this place. That's a rare occurrence. Warping back to spawn");
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                    unstuckState = UnstuckState.DISABLE;
                    delayBetweenMovementsClock.schedule(150 + (int) (Math.random() * 250));
                    return;
                }
                EnumFacing facing = EnumFacing.HORIZONTALS[facingInt.getAsInt()];
                Vec3i blockDirectionVec = facing.getDirectionVec();
                Vec3 blockVec = new Vec3(intersectingBlockPos);
                blockVec = blockVec.add(BLOCK_SIDE_MULTIPLIERS[facingInt.getAsInt()]);
                blockVec = blockVec.add(new Vec3(blockDirectionVec.getX(), blockDirectionVec.getY(), blockDirectionVec.getZ()));
                ArrayList<KeyBinding> keys = getNeededKeyPresses(blockVec);
                keys.add(mc.gameSettings.keyBindSneak);
                KeyBindUtils.holdThese(keys.toArray(new KeyBinding[0]));
                unstuckState = UnstuckState.RELEASE;
                delayBetweenMovementsClock.schedule(150 + (int) (Math.random() * 250));
                break;
            case RELEASE:
                KeyBindUtils.stopMovement();
                unstuckState = UnstuckState.DISABLE;
                delayBetweenMovementsClock.schedule(150 + (int) (Math.random() * 250));
                break;
            case DISABLE:
                stop();
                break;
        }
    }

    private Pair<Boolean, BlockPos> isPlayerIntersectingWithBlock() {
        BlockPos playerPos = mc.thePlayer.getPosition();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (block.isPassable(mc.theWorld, pos)) continue;

                    AxisAlignedBB blockBox = mc.theWorld.getBlockState(pos).getBlock().getSelectedBoundingBox(mc.theWorld, pos);
                    if (blockBox == null) {
                        continue;
                    }
                    if (mc.thePlayer.getEntityBoundingBox().intersectsWith(blockBox)) {
                        double deltaX = mc.thePlayer.posX - (pos.getX() + 0.5);
                        double deltaZ = mc.thePlayer.posZ - (pos.getZ() + 0.5);
                        if (deltaX * deltaX + deltaZ * deltaZ < 0.95) { //DISTANCE THRESHOLD HERE
                            return Pair.of(true, pos);
                        }
                    }
                }
            }
        }
        return Pair.of(false, null);
    }

    private final HashMap<Integer, KeyBinding> keyBindMap = new HashMap<Integer, KeyBinding>() {
        {
            put(0, mc.gameSettings.keyBindForward);
            put(90, mc.gameSettings.keyBindLeft);
            put(180, mc.gameSettings.keyBindBack);
            put(-90, mc.gameSettings.keyBindRight);
        }
    };

    public ArrayList<KeyBinding> getNeededKeyPresses(final Vec3 to) {
        final ArrayList<KeyBinding> e = new ArrayList<>();
        RotationUtils.Rotation rotation = AngleUtils.getRotation(to);
        final RotationUtils.Rotation neededRot = RotationUtils.getNeededChange(new RotationUtils.Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), rotation);
        final double neededYaw = neededRot.getYaw() * -1.0f;
        keyBindMap.forEach((k, v) -> {
            if (Math.abs(k - neededYaw) < 67.5 || Math.abs(k - (neededYaw + 360.0)) < 67.5) {
                e.add(v);
            }
        });
        return e;
    }

    private OptionalInt findClosestSide(Vec3 from, BlockPos pos) {
        int closestIndex = -1;
        for (int i = 0; i < BLOCK_SIDE_MULTIPLIERS.length; i++) {
            BlockPos adjacentPos = pos.add(EnumFacing.getHorizontal(i).getDirectionVec());
            if (!mc.theWorld.getBlockState(adjacentPos).getBlock().isPassable(mc.theWorld, adjacentPos)) { //IDK IF TRAPDOORS, CARPETS... SHOULD BE INCLUDED/EXCLUDED HERE
                continue;
            }

            if (closestIndex == -1) {
                closestIndex = i;
                continue;
            }

            Vec3 sideOffset = BLOCK_SIDE_MULTIPLIERS[i];
            Vec3 closest = BLOCK_SIDE_MULTIPLIERS[closestIndex];
            if (horizontalDistance(sideOffset.add(new Vec3(pos)), from) < horizontalDistance(closest.add(new Vec3(pos)), from)) {
                closestIndex = i;
            }
        }
        if (closestIndex == -1) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(closestIndex);
    }

    private double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.xCoord - b.xCoord;
        double dz = a.zCoord - b.zCoord;
        return dx * dx + dz * dz;
    }
}
