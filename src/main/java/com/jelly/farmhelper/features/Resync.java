package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCarrot;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.Sys;
import scala.tools.reflect.quasiquotes.Parsers;

import java.util.Timer;
import java.util.TimerTask;

public class Resync {
    private static boolean enabled;
    private static final Clock checkTimer = new Clock();
    public static BlockPos lastBrokenPos;
    private static BlockPos cachedPos;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void update(BlockPos lastBrokenPos) {
        Resync.lastBrokenPos = lastBrokenPos;
        if (checkTimer.passed()) {
            System.out.println("resync update recieved - " + lastBrokenPos + ", " + mc.theWorld.getBlockState(lastBrokenPos));
            cachedPos = lastBrokenPos;
            Timer t = new Timer();
            t.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (cachedPos != null && mc.theWorld.getBlockState(cachedPos) != null) {
                            // LogUtils.debugLog("Rechecked - " + mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE));
                            if (FarmConfig.cropType == CropEnum.NETHERWART && mc.theWorld.getBlockState(cachedPos).getValue(BlockNetherWart.AGE) > 2) {
                                mc.thePlayer.sendChatMessage("/hub");
                            } else if(FarmConfig.cropType == CropEnum.SUGARCANE && mc.theWorld.getBlockState(cachedPos).getBlock().equals(Blocks.reeds)){
                                mc.thePlayer.sendChatMessage("/hub");
                            } else if (mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE) > 4) {
                                mc.thePlayer.sendChatMessage("/hub");
                            }
                        }
                        t.cancel();
                    }
                },
                4000
            );
            checkTimer.schedule(5000);
        }
    }

//    @SubscribeEvent
//    public final void tick(TickEvent.ClientTickEvent event) {
//        if (enabled && mc.theWorld != null && lastBrokenPos != null && mc.theWorld.getBlockState(lastBrokenPos) != null) {
//            System.out.println(checkTimer.passed() + ", " + lastBrokenPos + ", " + (mc.theWorld.getBlockState(lastBrokenPos).getBlock()));
//            if (checkTimer.passed() && mc.theWorld.getBlockState(lastBrokenPos).getBlock() instanceof BlockBush) {
//                LogUtils.debugFullLog("Resync - in tick " + " " + mc.theWorld.getBlockState(lastBrokenPos));
//                cachedPos = lastBrokenPos;
//                verifyTimer.schedule(5000);
//                checkTimer.schedule(8000);
//            } else if (verifyTimer.passed() && cachedPos != null && mc.theWorld.getBlockState(cachedPos) != null) {
//                verifyTimer.reset();
//                LogUtils.debugLog("Rechecked - " + mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE));
//                if (FarmConfig.cropType == CropEnum.NETHERWART && mc.theWorld.getBlockState(cachedPos).getValue(BlockNetherWart.AGE) > 2) {
//
//                } else if (mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE) > 2) {
//
//                }
//            }
//        }
//    }
}