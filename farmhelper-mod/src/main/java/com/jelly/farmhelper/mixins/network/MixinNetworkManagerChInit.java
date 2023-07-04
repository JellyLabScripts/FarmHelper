package com.jelly.farmhelper.mixins.network;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.gui.ProxyScreen;
import com.jelly.farmhelper.network.proxy.ConnectionState;
import com.jelly.farmhelper.network.proxy.ProxyManager;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.NetworkManager;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public abstract class MixinNetworkManagerChInit {

    @Inject(method = "initChannel", at = @At(value = "HEAD"))
    private void onInitChannel(Channel channel, CallbackInfo ci) {
        if (FarmHelper.config.connectAtStartup && ProxyScreen.state == null) {
            ProxyScreen.state = ConnectionState.CONNECTING;
            ProxyManager.testProxy();
        }

        ProxyManager.hook(channel);
    }
}
