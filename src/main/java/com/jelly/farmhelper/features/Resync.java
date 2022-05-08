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

public class Resync {
    private boolean enabled;
    private Clock verifyTimer = new Clock();
    private Clock checkTimer = new Clock();
    public BlockPos lastBrokenPos;
    private BlockPos cachedPos;
    private final Minecraft mc = Minecraft.getMinecraft();

    public void enable() {
        enabled = true;
        checkTimer.schedule(8000);
    }

    public void disable() {
        enabled = false;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        LogUtils.debugFullLog("Broken block - " + event.state.getBlock());
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (enabled && mc.theWorld != null && lastBrokenPos != null && mc.theWorld.getBlockState(lastBrokenPos) != null) {
            System.out.println(checkTimer.passed() + ", "+ lastBrokenPos + ", " + (mc.theWorld.getBlockState(lastBrokenPos).getBlock()));
            if (checkTimer.passed() && mc.theWorld.getBlockState(lastBrokenPos).getBlock() instanceof BlockBush) {
                LogUtils.debugFullLog("Resync - in tick " + " " + mc.theWorld.getBlockState(lastBrokenPos));
                cachedPos = lastBrokenPos;
                verifyTimer.schedule(5000);
                checkTimer.schedule(8000);
            } else if (verifyTimer.passed() && cachedPos != null && mc.theWorld.getBlockState(cachedPos) != null) {
                verifyTimer.reset();
                LogUtils.debugLog("Rechecked - " + mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE));
                if (FarmConfig.cropType == CropEnum.NETHERWART && mc.theWorld.getBlockState(cachedPos).getValue(BlockNetherWart.AGE) > 2) {

                } else if (mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE) > 2) {

                }
            }
        }
    }
}
