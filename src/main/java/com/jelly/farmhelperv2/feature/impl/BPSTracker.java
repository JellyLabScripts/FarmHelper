package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.event.PlayerDestroyBlockEvent;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockReed;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class BPSTracker implements IFeature {
    private static BPSTracker instance;
    private final Queue<Long> bpsQueue = new LinkedList<>();
    private final Clock bpsClock = new Clock();
    private long bps = 0;
    private long blocksBroken = 0;
    private final NumberFormat oneDecimalDigitFormatter = NumberFormat.getNumberInstance(Locale.US);

    private BPSTracker() {
        oneDecimalDigitFormatter.setMaximumFractionDigits(1);
    }

    public static BPSTracker getInstance() {
        if (instance == null) {
            instance = new BPSTracker();
        }
        return instance;
    }

    @SubscribeEvent
    public void onTickCheckBPS(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!MacroHandler.getInstance().isCurrentMacroEnabled()) return;
        if (event.phase != TickEvent.Phase.START) return;

        if (!bpsClock.isScheduled() || bpsClock.passed()) {
            bpsClock.schedule(333);
            bpsQueue.add(blocksBroken);
            bps += blocksBroken;
            if (bpsQueue.size() > 60) {
                bps -= bpsQueue.poll();
            }
            blocksBroken = 0;
        }
    }

    public String getBPS() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled()) return "0.0 BPS";
        return oneDecimalDigitFormatter.format(getBPSFloat()) + " BPS";
    }

    public float getBPSFloat() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled()) return 0;
        return ((int) ((double) this.bps / this.bpsQueue.size() * 10.0D)) / 10.0F;
    }

    @SubscribeEvent
    public void onBlockChange(PlayerDestroyBlockEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        switch (MacroHandler.getInstance().getCrop()) {
            case NETHER_WART:
            case CARROT:
            case POTATO:
            case WHEAT:
                if (event.block instanceof BlockCrops ||
                        event.block instanceof BlockNetherWart) {
                    blocksBroken++;
                }
                break;
            case SUGAR_CANE:
                if (event.block instanceof BlockReed) {
                    blocksBroken++;
                }
                break;
            case MELON:
                if (event.block.equals(Blocks.melon_block)) {
                    blocksBroken++;
                }
                break;
            case PUMPKIN:
                if (event.block.equals(Blocks.pumpkin)) {
                    blocksBroken++;
                }
                break;
            case CACTUS:
                if (event.block.equals(Blocks.cactus)) {
                    blocksBroken++;
                }
                break;
            case COCOA_BEANS:
                if (event.block.equals(Blocks.cocoa)) {
                    blocksBroken++;
                }
                break;
            case MUSHROOM:
                if (event.block.equals(Blocks.red_mushroom) ||
                        event.block.equals(Blocks.brown_mushroom)) {
                    blocksBroken++;
                }
                break;
        }
    }

    @Override
    public String getName() {
        return "BPSTracker";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false; // that one is always running, no need to start
    }

    @Override
    public void start() {
        IFeature.super.start();
    }

    @Override
    public void stop() {
        IFeature.super.stop();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        bpsQueue.clear();
        bpsClock.reset();
        bps = 0;
        blocksBroken = 0;
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }
}