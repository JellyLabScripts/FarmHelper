package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Antistuck {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean stuck;
    private static double lastX;
    private static double lastZ;
    public static final Clock cooldown = new Clock();

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END)
            return;

        //TODO: Add for garden!
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled || mc.thePlayer == null || mc.theWorld == null || FarmHelper.gameState.currentLocation != GameState.location.ISLAND) {
            lastX = 10000;
            lastZ = 10000;
            cooldown.reset();
            return;
        }
        if(!MacroHandler.isMacroing)
            return;
        if (cooldown.passed()) {
            Block blockIn = BlockUtils.getRelativeBlock(0, 0, 0);
            stuck = (!blockIn.equals(Blocks.end_portal_frame) && !BlockUtils.isWalkable(blockIn)) || (Math.abs(mc.thePlayer.posX - lastX) < 1 && Math.abs(mc.thePlayer.posZ - lastZ) < 1);
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            cooldown.schedule(3000);
        }
    }
}
