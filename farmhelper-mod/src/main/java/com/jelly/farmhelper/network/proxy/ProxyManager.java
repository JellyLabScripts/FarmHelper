package com.jelly.farmhelper.network.proxy;

import com.jelly.farmhelper.config.interfaces.ProxyConfig;
import com.jelly.farmhelper.gui.ProxyScreen;
import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ProxyManager {
    public static boolean isTesting = false;

    public static void hook(Channel channel) {
        if (ProxyScreen.state != ConnectionState.CONNECTED && ProxyScreen.state != ConnectionState.CONNECTING) return;
        int port = Integer.parseInt(ProxyConfig.address.split(":")[1]);
        String address = ProxyConfig.address.split(":")[0];

        switch (ProxyConfig.type) {
            case SOCKS4:
                Socks4ProxyHandler socks4 = new Socks4ProxyHandler(new InetSocketAddress(address, port), ProxyConfig.username.isEmpty() ? null : ProxyConfig.username);
                channel.pipeline().addFirst(socks4);
                break;
            case SOCKS5:
                Socks5ProxyHandler socks5 = new Socks5ProxyHandler(new InetSocketAddress(address, port), ProxyConfig.username.isEmpty() ? null : ProxyConfig.username, ProxyConfig.password.isEmpty() ? null : ProxyConfig.password);
                channel.pipeline().addFirst(socks5);
                break;
            // someone make it work with http/s plzz
        }
    }

    public static void testProxy() {
        new Thread( () -> {
            try {
                isTesting = true;
                final boolean[] success = {false};
                NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName("us.mineplex.com"), 25565, false);
                manager.setNetHandler(new INetHandlerStatusClient() {
                    public void onDisconnect(IChatComponent reason) {}

                    public void handleServerInfo(S00PacketServerInfo packetIn) {
                        manager.sendPacket(new C01PacketPing());
                    }

                    public void handlePong(S01PacketPong packetIn) {
                        // Proxy is working
                        success[0] = true;
                        ProxyScreen.state = ConnectionState.CONNECTED;
                        ProxyScreen.setStateString("Connected to proxy");
                        manager.closeChannel(new ChatComponentText("Finished"));
                    }
                });

                // Doesn't seem to work without this timeout??
                Thread.sleep(1000);
                manager.sendPacket(new C00Handshake(47, "us.mineplex.com", 25565, EnumConnectionState.STATUS));
                manager.sendPacket(new C00PacketServerQuery());
                Thread.sleep(4000);
                if (!success[0]) {
                    ProxyScreen.state = ConnectionState.DISCONNECTED;
                    ProxyScreen.setStateString("Timed out, perhaps wrong username/pass?");
                }
                isTesting = false;
            } catch (Exception e) {
                ProxyScreen.state = ConnectionState.DISCONNECTED;
                ProxyScreen.setStateString("Invalid proxy IP");
            }
        }).start();

    }
}
