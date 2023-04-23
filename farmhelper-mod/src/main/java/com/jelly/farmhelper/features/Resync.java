package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Resync {
    private static final ArrayList<BlockPos> cachedPos = new ArrayList<>();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ScheduledExecutorService executor;

    static {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public static void update(BlockPos lastBrokenPos) {
        cachedPos.add(lastBrokenPos);
        LogUtils.debugLog("Desync check cache size: " + cachedPos.size());
        if(cachedPos.size() >= 4)
           executor.schedule(checkCrop, 2, TimeUnit.SECONDS);
    }

    static Runnable checkCrop = () -> {
        boolean desync = true;
        for(BlockPos bp : cachedPos) {
            if (mc.theWorld.getBlockState(bp) != null) {
                switch (FarmConfig.cropType) {
                    case NETHERWART:
                        if (mc.theWorld.getBlockState(bp).getValue(BlockNetherWart.AGE) <= 2) desync = false;
                        break;
                    case SUGARCANE:
                        if (!mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.reeds)) desync = false;
                        break;
                    case CACTUS:
                    case MELON:
                    case PUMPKIN:
                        if (BlockUtils.isWalkable(mc.theWorld.getBlockState(bp).getBlock())) desync = false;
                        break;
                    default:
                        if (mc.theWorld.getBlockState(bp).getValue(BlockCrops.AGE) <= 4) desync = false;
                }
            }
        }
        cachedPos.clear();
        if (desync && FarmHelper.gameState.currentLocation == GameState.location.ISLAND) {
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.DESYNC);
        }

    };
}