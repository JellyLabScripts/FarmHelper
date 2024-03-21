package com.jelly.farmhelperv2.mixin.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.event.SendPacketEvent;
import com.jelly.farmhelperv2.event.UpdateScoreboardLineEvent;
import com.jelly.farmhelperv2.feature.impl.Proxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "createNetworkManagerAndConnect", at = @At("HEAD"), cancellable = true)
    private static void createNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport, CallbackInfoReturnable<NetworkManager> cir) {
        if (!FarmHelperConfig.proxyEnabled) {
            return;
        }
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);

        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup eventLoopGroup;
        java.net.Proxy proxy = Proxy.getInstance().getProxy();
        eventLoopGroup = new OioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        bootstrap.channelFactory(new Proxy.ProxyOioChannelFactory(proxy));

        bootstrap.group(eventLoopGroup).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                System.out.println("ILLEGAL CHANNEL INITIALIZATION: This should be patched to net/minecraft/network/NetworkManager$5!");
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException var3) {
                    var3.printStackTrace();
                }
                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new MessageDeserializer2()).addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND)).addLast("prepender", new MessageSerializer2()).addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND)).addLast("packet_handler", networkmanager);
            }
        });

        bootstrap.connect(address, serverPort).syncUninterruptibly();

        cir.setReturnValue(networkmanager);
        cir.cancel();
    }

    @Unique
    private final Map<Integer, String> farmHelperV2$cachedScoreboard = new HashMap<>();

    @Inject(method = "channelRead0*", at = @At("HEAD"))
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        if (packet.getClass().getSimpleName().startsWith("S")) {
            MinecraftForge.EVENT_BUS.post(new ReceivePacketEvent(packet));
        } else if (packet.getClass().getSimpleName().startsWith("C")) {
            MinecraftForge.EVENT_BUS.post(new SendPacketEvent(packet));
        }
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        if (packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3CPacketUpdateScore || packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3EPacketTeams) {
            Scoreboard scoreboard = Minecraft.getMinecraft().thePlayer.getWorldScoreboard();
            Collection<Score> scores;
            try {
                scores = scoreboard.getSortedScores(scoreboard.getObjectiveInDisplaySlot(1));
            } catch (NullPointerException e) {
                return;
            }
            scores.removeIf(score -> score.getPlayerName().startsWith("#"));

            int index = 0;
            for (Score score : scores) {
                ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(score.getPlayerName());
                String string = ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.getPlayerName());
                String clean = farmHelperV2$cleanSB(string);
                if (!clean.equals(farmHelperV2$cachedScoreboard.get(index)) || !farmHelperV2$cachedScoreboard.containsKey(index)) {
                    farmHelperV2$cachedScoreboard.put(index, clean);
                    MinecraftForge.EVENT_BUS.post(new UpdateScoreboardLineEvent(clean));
                }
                index++;
                if (index > 15) break;
            }
        }
    }

    @Unique
    private String farmHelperV2$cleanSB(String scoreboard) {
        StringBuilder cleaned = new StringBuilder();

        for (char c : StringUtils.stripControlCodes(scoreboard).toCharArray()) {
            if (c >= 32 && c < 127 || c == 'ൠ') {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
