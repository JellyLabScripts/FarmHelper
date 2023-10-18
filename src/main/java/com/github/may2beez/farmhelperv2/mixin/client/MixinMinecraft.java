package com.github.may2beez.farmhelperv2.mixin.client;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.BanInfoWS;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public boolean inGameHasFocus;

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Shadow
    private Entity renderViewEntity;

    @Shadow
    public PlayerControllerMP playerController;

    @Shadow
    public WorldClient theWorld;

    @Shadow
    public EntityPlayerSP thePlayer;

    @Inject(method = "sendClickBlockToController", at = @At("RETURN"))
    private void sendClickBlockToController(CallbackInfo ci) {
        if (!FarmHelperConfig.fastBreak || !(MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().isMacroToggled())) {
            return;
        }
        if (FarmHelperConfig.disableFastBreakDuringBanWave && BanInfoWS.getInstance().isBanwave()) {
            return;
        }
        if (FarmHelperConfig.disableFastBreakDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            return;
        }

        boolean shouldClick = this.currentScreen == null && this.gameSettings.keyBindAttack.isKeyDown() && this.inGameHasFocus;
        if (this.objectMouseOver != null && shouldClick)
            for (int i = 0; i < FarmHelperConfig.fastBreakSpeed + 1; i++) {
                BlockPos clickedBlock = this.objectMouseOver.getBlockPos();
                this.objectMouseOver = this.renderViewEntity.rayTrace(this.playerController.getBlockReachDistance(), 1.0F);

                if (this.objectMouseOver == null || this.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                    break;
                }

                // checking first block
                BlockPos newBlock = this.objectMouseOver.getBlockPos();
                Block blockTryBreak = this.theWorld.getBlockState(newBlock).getBlock();

                if (!newBlock.equals(clickedBlock)
                        && (blockTryBreak instanceof BlockCrops ||
                        blockTryBreak instanceof BlockNetherWart ||
                        blockTryBreak == Blocks.reeds ||
                        blockTryBreak == Blocks.cactus ||
                        blockTryBreak == Blocks.brown_mushroom_block ||
                        blockTryBreak == Blocks.red_mushroom_block ||
                        blockTryBreak == Blocks.pumpkin ||
                        blockTryBreak == Blocks.melon_block ||
                        blockTryBreak == Blocks.cocoa)
                ) {
                    this.playerController.clickBlock(newBlock, this.objectMouseOver.sideHit);
                }

                if (i % 3 == 0) this.thePlayer.swingItem();
            }
    }
}