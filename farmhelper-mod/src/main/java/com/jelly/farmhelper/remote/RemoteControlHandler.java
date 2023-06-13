package com.jelly.farmhelper.remote;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.analytic.AnalyticBaseCommand;
import com.jelly.farmhelper.remote.command.Adapter;
import com.jelly.farmhelper.remote.command.BaseCommand;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.reflections.Reflections;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class RemoteControlHandler {
    public static int tick = 1;
    public static int tick2 = 1;
    public static Client client;
    public static Client analytic;

    static Minecraft mc = Minecraft.getMinecraft();
    JsonObject z = new JsonObject();

    public RemoteControlHandler() {
        registerCommands();
    }

    @SneakyThrows
    public void analyticConnect() {
        /*
        Analytics shit
        */

        String serverId = UUID.randomUUID().toString().replace("-", "");

        String comment = "This is just to check if the account actually exists (by authenticating on Mojang Servers), just like Skytils does (https://github.com/Skytils/SkytilsMod/blob/1.x/src/main/kotlin/skytils/skytilsmod/features/impl/handlers/MayorInfo.kt)";
        String for_ = "We want this data for research purposes and to improve the mod, all data is stored anonymously.";
        String decompilers = "THERE'S NO ACTUAL SSID BEING SENT TO US";
        try {
            mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), serverId);
            z.addProperty("modversion", FarmHelper.MODVERSION);
            z.addProperty("botversion", FarmHelper.BOTVERSION);
            z.addProperty("username", mc.getSession().getUsername());
            analytic = new Client(new URI("ws://" + FarmHelper.analyticUrl));
            analytic.addHeader("auth", Base64.getEncoder().encodeToString(z.toString().getBytes(StandardCharsets.UTF_8)));
            analytic.connect();
        } catch (Exception e) {
        }
    }
    public void connect() {
        try {
            /*
            Remote control shit
             */
            JsonObject j = new JsonObject();
            j.addProperty("password", FarmHelper.config.webSocketPassword);
            j.addProperty("name", mc.getSession().getUsername());
            j.addProperty("modversion", FarmHelper.MODVERSION);
            j.addProperty("botversion", FarmHelper.BOTVERSION);
            String data = Base64.getEncoder().encodeToString(j.toString().getBytes(StandardCharsets.UTF_8));
            client = new Client(new URI("ws://" + FarmHelper.config.webSocketIP.split(":")[0] + ":58637/farmhelperws"));
            client.addHeader("auth", data);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void registerCommands() {
        Brigadier.getInstance().setAdapter(new Adapter());
        Set<Class<? extends BaseCommand>> classes = new Reflections("com.jelly.farmhelper.remote.command.commands").getSubTypesOf(BaseCommand.class);
        Set<Class<? extends AnalyticBaseCommand>> analyticClasses = new Reflections("com.jelly.farmhelper.remote.analytic.commands").getSubTypesOf(AnalyticBaseCommand.class);

        for (Class<?> clazz : analyticClasses) {
            try {
                Brigadier.getInstance().register(clazz.newInstance()).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Class<?> clazz: classes) {
            try {
                Brigadier.getInstance().register(clazz.newInstance()).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @SneakyThrows
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        // TEMPORARY DISABLED BECAUSE NO ANALYTICS SERVER
//        if (z.    get("username") != null && !z.get("username").getAsString().equals(mc.getSession().getUsername())) {
//            analytic.close();
//        } else if (tick2 % 200 == 0 ) {
//            if ((analytic == null || !analytic.isOpen()) && minecraftUtils.isHypixel()) {
//                analyticConnect();
//            }
//            tick2 = 1;
//        } else {
//            tick2++;
//        }

        if (!FarmHelper.config.enableRemoteControl) {
            if (client != null && client.isOpen()) {
                client.closeConnection(69, "a");
            }
            return;
        }

        if (client != null && client.isOpen()) {
            connecting = "Connected to Socket";
            return;
        }

        if (tick % 20 == 0) {
            connect();
            tick = 1;
        } else {
            tick++;
        }


    }
}
