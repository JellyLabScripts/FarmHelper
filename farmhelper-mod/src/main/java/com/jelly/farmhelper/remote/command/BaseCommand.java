package com.jelly.farmhelper.remote.command;

import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.Client;
import com.jelly.farmhelper.remote.RemoteControlHandler;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class BaseCommand {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }
    public static void send(String content) {
        RemoteControlHandler.client.send(content);
    }
    public static DiscordWebhook.EmbedObject embed() {
        return new DiscordWebhook.EmbedObject()
                .setColor(new Color(9372933))
                .setFooter("FarmHelper Remote Control | by yonezu#5542", "https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png")
                .setAuthor(mc.getSession().getUsername(), null, "https://crafatar.com/avatars/" + mc.getSession().getPlayerID());
    }
}
