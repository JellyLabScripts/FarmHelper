package com.jelly.farmhelperv2.util;

import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.struct.DiscordWebhook;
import com.jelly.farmhelperv2.feature.impl.BanInfoWS;
import com.jelly.farmhelperv2.feature.impl.ProfitCalculator;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Tuple;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static String lastDebugMessage;
    private static long statusMsgTime = -1;
    private static int retries = 0;

    public synchronized static void sendLog(ChatComponentText chat) {
        if (mc.thePlayer != null && !FarmHelperConfig.streamerMode)
            mc.thePlayer.addChatMessage(chat);
        else if (mc.thePlayer == null)
            System.out.println("[Farm Helper] " + chat.getUnformattedText());
    }

    public static void sendSuccess(String message) {
        sendLog(new ChatComponentText("§2§lFarm Helper §8» §a" + message));
    }

    public static void sendWarning(String message) {
        sendLog(new ChatComponentText("§6§lFarm Helper §8» §e" + message));
    }

    public static void sendError(String message) {
        sendLog(new ChatComponentText("§4§lFarm Helper §8» §c" + message));
    }

    public static void sendDebug(String message) {
        if (lastDebugMessage != null && lastDebugMessage.equals(message)) {
            return;
        }
        if (FarmHelperConfig.debugMode && mc.thePlayer != null)
            sendLog(new ChatComponentText("§3§lFarm Helper §8» §7" + message));
        else
            System.out.println("[Farm Helper] " + message);
        lastDebugMessage = message;
    }

    public static void sendNotification(String title, String message, float duration) {
        if (!FarmHelperConfig.streamerMode)
            Notifications.INSTANCE.send(title, message, duration);
    }

    public static void sendNotification(String title, String message) {
        if (!FarmHelperConfig.streamerMode)
            Notifications.INSTANCE.send(title, message);
    }

    public static void sendFailsafeMessage(String message) {
        sendFailsafeMessage(message, false);
    }

    public static void sendFailsafeMessage(String message, boolean pingAll) {
        sendLog(new ChatComponentText("§5§lFarm Helper §8» §d" + message));
        webhookLog(StringUtils.stripControlCodes(message), pingAll);
    }

    public static String getRuntimeFormat() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled())
            return "0h 0m 0s";
        long millis = MacroHandler.getInstance().getMacroingTimer().getElapsedTime();
        return formatTime(millis) + (MacroHandler.getInstance().getMacroingTimer().paused ? " (Paused)" : "");
    }

    public static String formatTime(long millis) {

        if (TimeUnit.MILLISECONDS.toHours(millis) > 0) {
            return String.format("%dh %dm %ds",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        } else if (TimeUnit.MILLISECONDS.toMinutes(millis) > 0) {
            return String.format("%dm %ds",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(millis) + "." + (TimeUnit.MILLISECONDS.toMillis(millis) -
                    TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))) / 100 + "s";
        }
    }

    public static String capitalize(String message) {
        String[] words = message.split("_|\\s");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public static void webhookStatus() {
        if (!FarmHelperConfig.enableWebHook) return;

        if (statusMsgTime == -1) {
            statusMsgTime = System.currentTimeMillis();
        }
        long timeDiff = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - statusMsgTime);
        if (timeDiff >= FarmHelperConfig.statusUpdateInterval && FarmHelperConfig.sendStatusUpdates) {
            DiscordWebhook webhook = new DiscordWebhook(FarmHelperConfig.webHookURL.replace(" ", "").replace("\n", "").trim());
            String randomColor = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
            webhook.setUsername("Jelly - Farm Helper");
            webhook.setAvatarUrl("https://cdn.discordapp.com/attachments/1152966451406327858/1160577992876109884/icon.png");
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle("Farm Helper")
                    .setAuthor("Instance name -> " + mc.getSession().getUsername(), AvatarUtils.getAvatarUrl(mc.getSession().getPlayerID()), AvatarUtils.getAvatarUrl(mc.getSession().getPlayerID()))
                    .setDescription("## I'm still alive!")
                    .setColor(Color.decode(randomColor))
                    .setFooter("Farm Helper Webhook Status", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png")
                    .setThumbnail(AvatarUtils.getFullBodyUrl(mc.getSession().getPlayerID()))
                    .addField("Username", mc.getSession().getUsername(), true)
                    .addField("Runtime", getRuntimeFormat(), true)
                    .addField("Total Profit", ProfitCalculator.getInstance().getRealProfitString(), true)
                    .addField("Profit / hr", ProfitCalculator.getInstance().getProfitPerHourString(), true)
                    .addField("Crop Type", capitalize(String.valueOf(MacroHandler.getInstance().getCrop())), true)
                    .addField("Location", capitalize(GameStateHandler.getInstance().getLocation().getName()), true)
                    .addField("Pests", String.valueOf(GameStateHandler.getInstance().getPestsCount()), true)
                    .addField("Staff Bans", String.valueOf(BanInfoWS.getInstance().getStaffBans()), true)
                    .addField("Detected by FH", String.valueOf(BanInfoWS.getInstance().getBansByMod()), true)
            );
            Multithreading.schedule(() -> {
                try {
                    webhook.execute();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, TimeUnit.MILLISECONDS);
            statusMsgTime = System.currentTimeMillis();
        }
    }

    public static void webhookLog(String message) {
        webhookLog(message, false);
    }

    @SafeVarargs
    public static void webhookLog(String message, boolean mentionAll, Tuple<String, String>... fields) {
        if (!FarmHelperConfig.enableWebHook) return;
        if (!FarmHelperConfig.sendLogs) return;

        DiscordWebhook webhook = new DiscordWebhook(FarmHelperConfig.webHookURL.replace(" ", "").replace("\n", "").trim());
        webhook.setUsername("Jelly - Farm Helper");
        webhook.setAvatarUrl("https://cdn.discordapp.com/attachments/1152966451406327858/1160577992876109884/icon.png");
        String randomColor = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
        if (mentionAll) {
            webhook.setContent("@everyone");
        }
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject()
                .setTitle("Farm Helper")
                .setThumbnail(AvatarUtils.getFullBodyUrl(mc.getSession().getPlayerID()))
                .setDescription("### " + message)
                .setColor(Color.decode(randomColor))
                .setAuthor("Instance name -> " + mc.getSession().getUsername(), AvatarUtils.getAvatarUrl(mc.getSession().getPlayerID()), AvatarUtils.getAvatarUrl(mc.getSession().getPlayerID()))
                .setFooter("Farm Helper Webhook Status", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
        for (Tuple<String, String> field : fields) {
            embedObject.addField(field.getFirst(), field.getSecond(), false);
        }
        retries = 0;
        webhook.addEmbed(embedObject);
        sendWebhook(webhook);
    }

    private static void sendWebhook(DiscordWebhook webhook) {
        Multithreading.schedule(() -> {
            try {
                webhook.execute();
            } catch (IOException e) {
                if (retries >= 3) {
                    sendError("[Webhook Log] Error: " + e.getMessage());
                    retries = 0;
                    return;
                }
                retries++;
                sendDebug("[Webhook Log] Retrying sending a webhook message...");
                sendWebhook(webhook);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }, (retries == 0 ? 0 : 1000), TimeUnit.MILLISECONDS);
    }
}
