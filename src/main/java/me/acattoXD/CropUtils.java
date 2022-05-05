package me.acattoXD;

import net.minecraft.client.*;
import net.minecraft.init.Blocks;
import net.minecraft.world.*;
import net.minecraft.util.*;
import net.minecraft.block.state.*;
import net.minecraft.block.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CropUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final AxisAlignedBB[] CARROT_POTATO_BOX = new AxisAlignedBB[] { new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.1875, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.25, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.3125, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.375, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.4375, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5625, 1.0) };
    public static final AxisAlignedBB[] WHEAT_BOX = new AxisAlignedBB[] { new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.25, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.375, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.625, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.75, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.875, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) };
    public static final AxisAlignedBB[] NETHER_WART_BOX = new AxisAlignedBB[] { new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.3125, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.6875, 1.0), new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.875, 1.0) };

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().objectMouseOver == null || Minecraft.getMinecraft().objectMouseOver.getBlockPos() == null/*|| !Config.crophitboxes*/) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld.getBlockState(Minecraft.getMinecraft().objectMouseOver.getBlockPos()).getBlock() == Blocks.air)
            return;
        BlockPos blockpos = Minecraft.getMinecraft().objectMouseOver.getBlockPos();
        Block block = Minecraft.getMinecraft().theWorld.getBlockState(Minecraft.getMinecraft().objectMouseOver.getBlockPos()).getBlock();
    }
}