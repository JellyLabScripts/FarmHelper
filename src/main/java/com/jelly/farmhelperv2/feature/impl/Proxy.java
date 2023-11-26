package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.socket.oio.OioSocketChannel;

import java.net.*;
import java.util.Objects;

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
        if (!Objects.equals(username, "Username"))
            FarmHelperConfig.proxyUsername = username;
        if (!Objects.equals(password, "Password"))
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

        if (FarmHelperConfig.proxyUsername != null && FarmHelperConfig.proxyPassword != null)
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FarmHelperConfig.proxyUsername, FarmHelperConfig.proxyPassword.toCharArray());
                }
            });

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
