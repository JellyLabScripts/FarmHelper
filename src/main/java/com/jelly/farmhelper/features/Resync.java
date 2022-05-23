package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.world.GameState;
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
    private static final Clock checkTimer = new Clock();
    public static BlockPos lastBrokenPos;
    private static BlockPos cachedPos;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void update(BlockPos lastBrokenPos) {
        Resync.lastBrokenPos = lastBrokenPos;
        if (checkTimer.passed()) {
            cachedPos = lastBrokenPos;
            Timer t = new Timer();
            t.schedule(
              new TimerTask() {
                  @Override
                  public void run() {
                      if (cachedPos != null && mc.theWorld.getBlockState(cachedPos) != null) {
                          if (FarmConfig.cropType == CropEnum.NETHERWART && mc.theWorld.getBlockState(cachedPos).getValue(BlockNetherWart.AGE) > 2 ||
                            FarmConfig.cropType == CropEnum.SUGARCANE && mc.theWorld.getBlockState(cachedPos).getBlock().equals(Blocks.reeds) ||
                            mc.theWorld.getBlockState(cachedPos).getValue(BlockCrops.AGE) > 4) {
                              LogUtils.debugLog("Desync detected");
                              LogUtils.webhookLog("Desync detected");
                              if(FarmHelper.gameState.currentLocation == GameState.location.ISLAND) mc.thePlayer.sendChatMessage("/hub");
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

}