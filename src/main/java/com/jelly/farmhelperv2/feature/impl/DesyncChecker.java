package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ClickedBlockEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.FifoQueue;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DesyncChecker implements IFeature {
    private static DesyncChecker instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final FifoQueue<ClickedBlockEvent> clickedBlocks = new FifoQueue<>(60);
    private boolean enabled = false;

    public static DesyncChecker getInstance() {
        if (instance == null) {
            instance = new DesyncChecker();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Desync Checker";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return enabled;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        clickedBlocks.clear();
        IFeature.super.start();
    }

    @Override
    public void stop() {
        clickedBlocks.clear();
        IFeature.super.stop();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        enabled = false;
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.checkDesync;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @SubscribeEvent
    public void onClickedBlock(ClickedBlockEvent event) {
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!isCrop(mc.theWorld.getBlockState(event.getPos()).getBlock())) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        clickedBlocks.add(event);
        if (!clickedBlocks.isAtFullCapacity()) return;
        if (!checkIfDesync()) return;
        if (enabled) return;
        enabled = true;
        stop();
        LogUtils.sendWarning("[Desync Checker] Desync detected, pausing macro for " + Math.floor((double) FarmHelperConfig.desyncPauseDelay / 1_000) + " seconds to prevent further desync.");
        MacroHandler.getInstance().pauseMacro();
        Multithreading.schedule(() -> {
            if (!MacroHandler.getInstance().isMacroToggled()) return;
            enabled = false;
            LogUtils.sendWarning("[Desync Checker] Desync should be over, resuming macro execution");
            MacroHandler.getInstance().resumeMacro();
        }, FarmHelperConfig.desyncPauseDelay, TimeUnit.MILLISECONDS);
    }

    private boolean isCrop(Block block) {
        return block instanceof BlockNetherWart ||
                block instanceof BlockCrops ||
                block.equals(Blocks.melon_block) ||
                block.equals(Blocks.pumpkin) ||
                block.equals(Blocks.reeds) ||
                block.equals(Blocks.cactus) ||
                block.equals(Blocks.cocoa) ||
                block.equals(Blocks.brown_mushroom_block) ||
                block.equals(Blocks.red_mushroom_block);
    }

    private boolean checkIfDesync() {
        float RATIO = 0.75f;
        List<ClickedBlockEvent> list = new ArrayList<>(clickedBlocks);
        int count = 0;
        for (ClickedBlockEvent pos : list) {
            IBlockState state = mc.theWorld.getBlockState(pos.getPos());
            if (state == null) continue;

            switch (MacroHandler.getInstance().getCrop()) {
                case NETHER_WART:
                    if (state.getBlock() instanceof BlockNetherWart && state.getValue(BlockNetherWart.AGE) == 3)
                        count++;
                    break;
                case SUGAR_CANE:
                    if (state.getBlock().equals(Blocks.reeds)) count++;
                    break;
                case CACTUS:
                    if (state.getBlock().equals(Blocks.cactus)) count++;
                    break;
                case MELON:
                case PUMPKIN:
                    if (!state.getBlock().equals(Blocks.air)) count++;
                    break;
                case MUSHROOM:
                    if (state.getBlock().equals(Blocks.brown_mushroom_block) || state.getBlock().equals(Blocks.red_mushroom_block))
                        count++;
                    break;
                case COCOA_BEANS:
                    if (state.getBlock().equals(Blocks.cocoa) && state.getValue(BlockCocoa.AGE) == 2) count++;
                    break;
                case CARROT:
                case POTATO:
                case WHEAT:
                    if (state.getBlock() instanceof BlockCrops && state.getValue(BlockCrops.AGE) == 7) count++;
                    break;
                default:
                    // Unknown crop
            }
        }
        return count / (float) list.size() >= RATIO;
    }
}
