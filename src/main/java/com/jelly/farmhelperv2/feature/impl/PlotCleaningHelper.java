package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.MillisecondEvent;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// Credits to GTC's nuker
public class PlotCleaningHelper implements IFeature {
    private static PlotCleaningHelper instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final CopyOnWriteArrayList<BlockPos> scytheBlockPos = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BlockPos> treeCapitatorBlockPos = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BlockPos> pickaxeBlockPos = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Tuple<BlockPos, Long>> brokenBlockPosArrayList = new CopyOnWriteArrayList<>();
    private final Clock stuckClock = new Clock();
    private final String[] tools = {"Treecapitator", "Axe", "Scythe", "Pickaxe", "Stonk"};
    private final Random rand = new Random();
    private final Color colorGreen = new Color(0, 255, 0, 100);
    private final Color colorRock = new Color(230, 230, 230, 100);
    private final Color colorWood = new Color(150, 50, 50, 100);
    private final Color targetColor = new Color(255, 0, 0, 100);
    private boolean enabled = false;
    private BlockPos longBreakTarget = null;
    private BlockPos target = null;
    private long lastBlockBroken = 0;

    public static PlotCleaningHelper getInstance() {
        if (instance == null) {
            instance = new PlotCleaningHelper();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Plot Cleaning Helper";
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
        enabled = true;
        longBreakTarget = null;
        target = null;
        scytheBlockPos.clear();
        treeCapitatorBlockPos.clear();
        pickaxeBlockPos.clear();
        brokenBlockPosArrayList.clear();
        lastBlockBroken = 0;
        stuckClock.schedule(7_000);
        LogUtils.sendSuccess("[Plot Cleaning Helper] Enabled.");
        IFeature.super.start();
    }

    @Override
    public void stop() {
        if (!enabled) return;
        enabled = false;
        longBreakTarget = null;
        target = null;
        LogUtils.sendSuccess("[Plot Cleaning Helper] Disabled.");
        stop();
    }

    public void toggle() {
        if (enabled) {
            stop();
        } else {
            start();
        }
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return enabled;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) return;
        if (mc.currentScreen != null) {
            RotationHandler.getInstance().reset();
            return;
        }

        if (ScoreboardUtils.getScoreboardLines(true).stream().noneMatch(line -> line.contains("Cleanup"))) {
            scytheBlockPos.clear();
            treeCapitatorBlockPos.clear();
            pickaxeBlockPos.clear();
            return;
        }

        scytheBlockPos.clear();
        treeCapitatorBlockPos.clear();
        pickaxeBlockPos.clear();
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 1, mc.thePlayer.posZ);
        Vec3i vec3Top = new Vec3i(4, 2, 4);
        Vec3i vec3Bottom = new Vec3i(4, 3, 4);
        BlockPos blockTop = playerPos.add(vec3Top);
        BlockPos blockBottom = playerPos.subtract(vec3Bottom);
        for (BlockPos blockpos : BlockPos.getAllInBox(blockBottom, blockTop)) {
            if (brokenBlockPosArrayList.stream().anyMatch(tup -> tup.getFirst().equals(blockpos))) continue;
            List<Tuple<Integer, Integer>> chunks = PlotUtils.getPlotChunksBasedOnLocation(blockpos);
            if (chunks == null || chunks.isEmpty()) continue;
            PlotUtils.Plot plot = PlotUtils.getPlotNumberBasedOnLocation(blockpos);
            if (plot == null) continue;
            int plotNumber = plot.number;
            if (plotNumber != GameStateHandler.getInstance().getCurrentPlot()) continue;
            AxisAlignedBB aabb = new AxisAlignedBB(chunks.get(0).getFirst() * 16 + 1, 66, chunks.get(0).getSecond() * 16 + 1, chunks.get(chunks.size() - 1).getFirst() * 16 + 16 - 1, 200, chunks.get(chunks.size() - 1).getSecond() * 16 + 16 - 1);
            if (!aabb.isVecInside(new Vec3(blockpos))) continue;
            Vec3 target = new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
            float fovToVec = fovToVec3(target);
            float wrappedYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
            float wrappedAll = MathHelper.wrapAngleTo180_float(fovToVec - wrappedYaw);
            float var = Math.abs(wrappedAll);
            if (var >= (float) 120 / 2)
                continue;

            Block block = mc.theWorld.getBlockState(blockpos).getBlock();
            if (checkIfScythe(block)) {
                scytheBlockPos.add(blockpos);
            } else if (checkIfTreecap(block)) {
                treeCapitatorBlockPos.add(blockpos);
            } else if (checkIfPickaxe(block)) {
                pickaxeBlockPos.add(blockpos);
            }
        }

        if (stuckClock.passed()) {
            LogUtils.sendDebug("Stuck clock passed, resetting.");
            stuckClock.reset();
            brokenBlockPosArrayList.clear();
            longBreakTarget = null;
        }

        if (this.longBreakTarget != null) mc.thePlayer.swingItem();
    }

    private float fovToVec3(Vec3 vec) {
        double x = vec.xCoord - mc.thePlayer.posX;
        double z = vec.zCoord - mc.thePlayer.posZ;
        double yaw = Math.atan2(x, z) * 57.2957795;
        return (float) (yaw * -1.0);
    }

    @SubscribeEvent
    public void onMillisecond(MillisecondEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (longBreakTarget != null && (mc.thePlayer.getDistanceSq(longBreakTarget) > 25 || !canMine(longBreakTarget))) {
            longBreakTarget = null;
        }

        if (event.timestamp - lastBlockBroken > 1000f / 20 && (!RotationHandler.getInstance().isRotating() || (RotationHandler.getInstance().isRotating() && RotationHandler.getInstance().getConfiguration() != null && RotationHandler.getInstance().getConfiguration().goingBackToClientSide()))) {
            lastBlockBroken = event.timestamp;

            brokenBlockPosArrayList.removeIf(tup -> tup.getSecond() + 2_500 < System.currentTimeMillis());

            if (longBreakTarget == null) {
                ArrayList<BlockPos> blockPosCopy = new ArrayList<>(scytheBlockPos);
                if (mc.thePlayer.onGround) {
                    blockPosCopy.addAll(treeCapitatorBlockPos);
                    blockPosCopy.addAll(pickaxeBlockPos);
                }
                target = BlockUtils.getEasiestBlock(blockPosCopy, this::canMine);
            }

            if (target != null) {
                if (longBreakTarget != null && (longBreakTarget.compareTo(target) != 0 || Objects.requireNonNull(BlockUtils.getBlockState(longBreakTarget)).getBlock() != Objects.requireNonNull(BlockUtils.getBlockState(target)).getBlock())) {
                    longBreakTarget = null;
                }
                if (FarmHelperConfig.autoChooseTool) {
                    int id = getBestTool(Objects.requireNonNull(BlockUtils.getBlockState(target)).getBlock());
                    if (id != -1 && mc.thePlayer.inventory.currentItem != id) {
                        mc.thePlayer.inventory.currentItem = id;
                        lastBlockBroken = event.timestamp + 100;
                        return;
                    }
                }
                if (isSlow(Objects.requireNonNull(BlockUtils.getBlockState(target)))) {
//                    LogUtils.sendDebug("Target is slow mining.");
//                    if (longBreakTarget == null) {
//                        mineBlock(target);
//                    }
                    LogUtils.sendDebug("Won't destroy this block, because you are flying");
                } else {
                    pinglessMineBlock(target);
                    longBreakTarget = null;
                }
            } else {
                longBreakTarget = null;
            }
        }
    }

    private void mineBlock(BlockPos blockPos) {
        RotationHandler.getInstance().easeTo(
                new RotationConfiguration(
                        new Target(blockPos),
                        FarmHelperConfig.getRandomPlotCleaningHelperRotationTime(),
                        RotationConfiguration.RotationType.SERVER,
                        () -> {
                            breakBlock(blockPos);
                            longBreakTarget = blockPos;
                            RotationHandler.getInstance().easeBackFromServerRotation();
                            stuckClock.schedule(7000);
                        }
                )
        );
    }

    private void pinglessMineBlock(BlockPos blockPos) {
        RotationHandler.getInstance().easeTo(
                new RotationConfiguration(
                        new Target(blockPos),
                        FarmHelperConfig.getRandomPlotCleaningHelperRotationTime(),
                        RotationConfiguration.RotationType.SERVER,
                        () -> {
                            RotationHandler.getInstance().easeBackFromServerRotation();
                            mc.thePlayer.swingItem();
                            breakBlock(blockPos);
                            int radius = getRadius();
                            if (radius > 0) {
                                for (int x = -radius; x <= radius; x++) {
                                    for (int y = -radius; y <= radius; y++) {
                                        for (int z = -radius; z <= radius; z++) {
                                            BlockPos blockPos1 = blockPos.add(x, y, z);
                                            Block block = mc.theWorld.getBlockState(blockPos1).getBlock();
                                            if (checkIfScythe(block)) {
                                                brokenBlockPosArrayList.add(new Tuple<>(blockPos1, System.currentTimeMillis()));
                                            }
                                        }
                                    }
                                }
                            } else {
                                brokenBlockPosArrayList.add(new Tuple<>(blockPos, System.currentTimeMillis()));
                            }
                            stuckClock.schedule(7000);
                        }
                )
        );
    }

    private void breakBlock(BlockPos blockPos) {
        EnumFacing enumFacing = BlockUtils.calculateEnumfacing(new Vec3(blockPos).add(randomVec()));
        if (enumFacing != null) {
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    blockPos,
                    enumFacing
            ));
        } else {
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    blockPos,
                    mc.thePlayer.getHorizontalFacing().getOpposite()
            ));
        }
    }

    private int getRadius() {
        ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();
        if (currentItem == null) return 0;
        String displayName = currentItem.getDisplayName();
        if (displayName.contains("Sam") && displayName.contains("Scythe")) {
            return 1;
        } else if (displayName.contains("Garden") && displayName.contains("Scythe")) {
            return 2;
        }
        return 0;
    }

    public Vec3 randomVec() {
        return new Vec3(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
    }

    private boolean isSlow(IBlockState blockState) {
        boolean flying = !mc.thePlayer.onGround;
        if (checkIfScythe(blockState.getBlock())) {
            return false;
        } else if (checkIfTreecap(blockState.getBlock())) {
            return flying;
        } else if (checkIfPickaxe(blockState.getBlock())) {
            return flying;
        }
        return false;
    }

    private boolean checkIfScythe(Block block) {
        return block instanceof BlockTallGrass || block instanceof BlockFlower || block instanceof BlockLeaves || block instanceof BlockDoublePlant;
    }

    private boolean checkIfTreecap(Block block) {
        return block instanceof BlockLog || block.getMaterial().equals(Material.wood);
    }

    private boolean checkIfPickaxe(Block block) {
        return (block.getMaterial().equals(Material.rock) && !block.equals(Blocks.bedrock)) || block.equals(Blocks.stone_slab) || block.equals(Blocks.double_stone_slab) || block.equals(Blocks.cobblestone) || block.equals(Blocks.stone_stairs);
    }

    private boolean canMine(BlockPos blockPos) {
        if (brokenBlockPosArrayList.stream().anyMatch(tup -> tup.getFirst().equals(blockPos))) return false;

        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        if (FarmHelperConfig.autoChooseTool) {
            if (checkIfScythe(block)) {
                return InventoryUtils.hasItemInHotbar("Scythe");
            } else if (checkIfTreecap(block)) {
                return InventoryUtils.hasItemInHotbar("Treecapitator", "Axe");
            } else if (checkIfPickaxe(block)) {
                return InventoryUtils.hasItemInHotbar("Pickaxe") || InventoryUtils.hasItemInHotbar("Stonk");
            }
        } else {
            ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();
            if (currentItem == null) return false;
            if (Arrays.stream(tools).noneMatch(currentItem.getDisplayName()::contains)) return false;
            if (checkIfScythe(block)) {
                return currentItem.getDisplayName().contains("Scythe");
            } else if (checkIfTreecap(block)) {
                return currentItem.getDisplayName().contains("Treecapitator") || (currentItem.getDisplayName().contains("Axe") && !currentItem.getDisplayName().contains("Pick"));
            } else if (checkIfPickaxe(block)) {
                return currentItem.getDisplayName().contains("Pickaxe") || currentItem.getDisplayName().contains("Stonk");
            }
        }
        return false;
    }

    private int getBestTool(Block block) {
        if (block instanceof BlockTallGrass || block instanceof BlockFlower || block instanceof BlockLeaves || block instanceof BlockDoublePlant) {
            return InventoryUtils.getSlotIdOfItemInHotbar("Scythe");
        } else if (block.getMaterial().equals(Material.wood)) {
            return InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator", "Axe");
        } else if (block.getMaterial().equals(Material.rock)) {
            return InventoryUtils.getSlotIdOfItemInHotbar("Pickaxe", "Stonk");
        }
        return -1;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        for (BlockPos blockPos : scytheBlockPos) {
            RenderUtils.drawBlockBox(blockPos, colorGreen);
        }
        for (BlockPos blockPos : treeCapitatorBlockPos) {
            RenderUtils.drawBlockBox(blockPos, colorWood);
        }
        for (BlockPos blockPos : pickaxeBlockPos) {
            RenderUtils.drawBlockBox(blockPos, colorRock);
        }

        if (target != null) {
            RenderUtils.drawBlockBox(target, targetColor);
        } else if (longBreakTarget != null) {
            RenderUtils.drawBlockBox(longBreakTarget, targetColor);
        }
    }
}
