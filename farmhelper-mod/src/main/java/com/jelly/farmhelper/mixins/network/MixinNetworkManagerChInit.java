package com.jelly.farmhelper.mixins.network;

import com.jelly.farmhelper.config.interfaces.ProxyConfig;
import com.jelly.farmhelper.gui.ProxyScreen;
import com.jelly.farmhelper.network.proxy.ConnectionState;
import com.jelly.farmhelper.network.proxy.ProxyManager;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public abstract class MixinNetworkManagerChInit {

    @Inject(method = "initChannel", at = @At(value = "HEAD"))
    private void onInitChannel(Channel channel, CallbackInfo ci) {
        if (ProxyConfig.connectAtStartup && ProxyScreen.state == null) {
            ProxyScreen.state = ConnectionState.CONNECTING;
            ProxyManager.testProxy();
        }

        ProxyManager.hook(channel);
    }
}
