package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Resync {
    private static final CopyOnWriteArrayList<BlockPos> cachedPos = new CopyOnWriteArrayList<>();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ScheduledExecutorService executor;

    static {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public static void update(BlockPos lastBrokenPos) {
        cachedPos.add(lastBrokenPos);
        if(cachedPos.size() >= 4)
           executor.schedule(checkCrop, 2500, TimeUnit.MILLISECONDS);
    }

    static Runnable checkCrop = () -> {
        boolean desync = true;
        for(BlockPos bp : cachedPos) {
            if (mc.theWorld.getBlockState(bp) != null) {
                switch (MacroHandler.crop) {
                    case NETHER_WART:
                        if (mc.theWorld.getBlockState(bp).getValue(BlockNetherWart.AGE) <= 2) desync = false;
                        break;
                    case SUGAR_CANE:
                        if (!mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.reeds)) desync = false;
                        break;
                    case CACTUS:
                    case MELON:
                    case PUMPKIN:
                        if (BlockUtils.isWalkable(mc.theWorld.getBlockState(bp).getBlock())) desync = false;
                        break;
                    case MUSHROOM:
                        if (!mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.brown_mushroom_block) && !mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.red_mushroom_block)) desync = false;
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