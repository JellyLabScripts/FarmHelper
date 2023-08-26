package com.jelly.farmhelper.mixins.network;

import com.jelly.farmhelper.FarmHelper;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
    @Shadow @Final private NetworkManager netManager;
    private static final String TARGET = "Lnet/minecraft/entity/player/EntityPlayer;" +
        "setPositionAndRotation(DDDFF)V";

    @Redirect(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = TARGET))
    public void handlePlayerPosLook_setPositionAndRotation(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        player.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    @Inject(method = "handleSpawnMob", at = @At("RETURN"))
    public void handleSpawnMob(S0FPacketSpawnMob packetIn, CallbackInfo ci) {
        //CollectionLogManager.getInstance().onEntityMetadataUpdated(packetIn.getEntityID());
    }

    @Inject(method = "handleSetSlot", at = @At("RETURN"))
    public void handleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci) {

    }

    @Inject(method = "handleOpenWindow", at = @At("RETURN"))
    public void handleOpenWindow(S2DPacketOpenWindow packetIn, CallbackInfo ci) {

    }

    @Inject(method = "handleCloseWindow", at = @At("RETURN"))
    public void handleCloseWindow(S2EPacketCloseWindow packetIn, CallbackInfo ci) {

    }

    @Inject(method = "handleWindowItems", at = @At("RETURN"))
    public void handleOpenWindow(S30PacketWindowItems packetIn, CallbackInfo ci) {

    }

    @Inject(method = "handleRespawn", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V",
        shift = At.Shift.AFTER))
    public void handleOpenWindow(S07PacketRespawn packetIn, CallbackInfo ci) {

    }

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    public void handleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci) {

    }

    @Inject(method = "addToSendQueue", at = @At("HEAD"))
    public void addToSendQueue(Packet packet, CallbackInfo ci) {
        if (packet instanceof C0EPacketClickWindow) {

        }
    }

    @Inject(method = "handlePlayerListHeaderFooter", at = @At("HEAD"))
    public void handlePlayerListHeaderFooter(S47PacketPlayerListHeaderFooter packetIn, CallbackInfo ci) {
        FarmHelper.gameState.header = packetIn.getHeader().getFormattedText().length() == 0 ? null : packetIn.getHeader();
        FarmHelper.gameState.footer = packetIn.getFooter().getFormattedText().length() == 0 ? null : packetIn.getFooter();
    }
}