package com.jelly.farmhelper.mixins.network;

import com.jelly.farmhelper.utils.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "channelRead0", at = @At(value = "HEAD"))
    private void onChannelRead0(ChannelHandlerContext p_channelRead0_1_, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof S47PacketPlayerListHeaderFooter) {
            LogUtils.scriptLog(((S47PacketPlayerListHeaderFooter) packet).getHeader().getFormattedText());
        }
    }
}
