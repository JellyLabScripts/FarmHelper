package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.socket.oio.OioSocketChannel;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Proxy {
    private static Proxy instance;
    public static Proxy getInstance() {
        if (instance == null) {
            instance = new Proxy();
        }
        return instance;
    }

    public enum ProxyType {
        SOCKS,
        HTTP,
    }

    public void setProxy(boolean enabled, String host, ProxyType type, String username, String password) {
        FarmHelperConfig.proxyEnabled = enabled;
        FarmHelperConfig.proxyAddress = host;
        FarmHelperConfig.proxyType = type;
        FarmHelperConfig.proxyUsername = username;
        FarmHelperConfig.proxyPassword = password;
        FarmHelper.config.save();
    }

    public java.net.Proxy getProxy() {
        if (!FarmHelperConfig.proxyEnabled) return null;
        String addressStr = FarmHelperConfig.proxyAddress.split(":")[0];
        int port = Integer.parseInt(FarmHelperConfig.proxyAddress.split(":")[1]);
        InetSocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(addressStr), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        switch (FarmHelperConfig.proxyType) {
            case SOCKS:
                return new java.net.Proxy(java.net.Proxy.Type.SOCKS, address);
            case HTTP:
                return new java.net.Proxy(java.net.Proxy.Type.HTTP, address);
            default:
                return null;
        }
    }

    public static class ProxyOioChannelFactory implements ChannelFactory<OioSocketChannel> {

        private final java.net.Proxy proxy;

        public ProxyOioChannelFactory(java.net.Proxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public OioSocketChannel newChannel() {
            return new OioSocketChannel(new Socket(proxy));
        }
    }
}
