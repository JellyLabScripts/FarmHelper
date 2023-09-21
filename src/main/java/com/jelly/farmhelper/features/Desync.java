package com.jelly.farmhelper.features;

import com.jelly.farmhelper.macros.MacroHandler;
import lombok.Getter;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

public class Desync {
//    private static final CopyOnWriteArrayList<BlockPos> cachedPos = new CopyOnWriteArrayList<>();
    public static final CircularFifoQueue<BlockPos> clickedBlocks = new CircularFifoQueue<>(200);
    private static final Minecraft mc = Minecraft.getMinecraft();
//    private static final ScheduledExecutorService executor;
    @Getter
    private static boolean desync;

//    static {
//        executor = Executors.newSingleThreadScheduledExecutor();
//    }

    public static void update(BlockPos lastBrokenPos) {
//        cachedPos.add(lastBrokenPos);
//        if(cachedPos.size() >= 4)
//           executor.schedule(checkCrop, 2500, TimeUnit.MILLISECONDS);
        clickedBlocks.add(lastBrokenPos);

        if (clickedBlocks.isAtFullCapacity()) {
            if (checkIfDesync(0.75f)) {
                desync = true;
                FailsafeNew.emergencyFailsafe(FailsafeNew.FailsafeType.DESYNC);
            } else {
                desync = false;
            }
        }
    }

    private static boolean checkIfDesync(float ratio) {
        List<BlockPos> list = new ArrayList<>(clickedBlocks);
        int count = 0;
        for (BlockPos pos : list) {
            IBlockState state = mc.theWorld.getBlockState(pos);
            if (state == null) continue;

            switch (MacroHandler.crop) {
                case NETHER_WART:
                    if (state.getValue(BlockNetherWart.AGE) == 3) count++;
                    break;
                case SUGAR_CANE:
                    if (state.getBlock().equals(Blocks.reeds)) count++;
                    break;
                case CACTUS:
                case MELON:
                case PUMPKIN:
                    if (!state.getBlock().equals(Blocks.air)) count++;
                    break;
                case MUSHROOM:
                    if (state.getBlock().equals(Blocks.brown_mushroom_block) || state.getBlock().equals(Blocks.red_mushroom_block)) count++;
                    break;
                default:
                    if (!state.getBlock().equals(Blocks.air) && state.getValue(BlockCrops.AGE) == 7) count++;
            }
        }
        System.out.println(count + " / " + list.size());
        return count / (float) list.size() >= ratio;
    }

//    static Runnable checkCrop = () -> {
//        boolean desync = true;
//        for(BlockPos bp : cachedPos) {
//            if (mc.theWorld.getBlockState(bp) != null) {
//                switch (MacroHandler.crop) {
//                    case NETHER_WART:
//                        if (mc.theWorld.getBlockState(bp).getValue(BlockNetherWart.AGE) <= 2) desync = false;
//                        break;
//                    case SUGAR_CANE:
//                        if (!mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.reeds)) desync = false;
//                        break;
//                    case CACTUS:
//                    case MELON:
//                    case PUMPKIN:
//                        if (BlockUtils.isWalkable(mc.theWorld.getBlockState(bp).getBlock())) desync = false;
//                        break;
//                    case MUSHROOM:
//                        if (!mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.brown_mushroom_block) && !mc.theWorld.getBlockState(bp).getBlock().equals(Blocks.red_mushroom_block)) desync = false;
//                        break;
//                    default:
//                        if (mc.theWorld.getBlockState(bp).getValue(BlockCrops.AGE) <= 4) desync = false;
//                }
//            }
//        }
//        cachedPos.clear();
//        if (desync) {
//            if (!MacroHandler.isMacroing) return;
//            if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
//            if (MacroHandler.currentMacro.isTping) return;
//            FailsafeNew.emergencyFailsafe(FailsafeNew.FailsafeType.DESYNC);
//        }
//
//    };
}