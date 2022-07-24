package com.yyonezu.remotecontrol.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.Main;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.event.MessageEvent;
import com.yyonezu.remotecontrol.event.wait.EventWaiter;
import com.yyonezu.remotecontrol.utils.Utils;
import io.javalin.Javalin;
import org.eclipse.jetty.websocket.api.Session;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

public class WebSocketServer {
    public static HashMap<Session, String> minecraftInstances = new HashMap<>();
    public static Javalin app;
    public static void start() {
        app = Javalin.create(javalinConfig -> javalinConfig.wsFactoryConfig(c -> c.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE))).start(Main.port);
        app.ws("/farmhelperws", ws -> {
            ws.onConnect(ctx -> {
                try {
                    String b64header = ctx.session.getUpgradeRequest().getHeader("auth");
                    JsonObject decoded = new Gson().fromJson(new String(Base64.getDecoder().decode(b64header), StandardCharsets.UTF_8), JsonObject.class);
                    if (!Objects.equals(decoded.get("modversion").getAsString(), "-1") && !Main.MODVERSION.equals("-1") && !Main.BOTVERSION.equals("-1")) {
                        if (!decoded.get("modversion").getAsString().equals(Main.MODVERSION) || !decoded.get("botversion").getAsString().equals(Main.BOTVERSION)) {
                            ctx.send("VERSIONERROR");
                        }
                    }

                    if (minecraftInstances.containsValue(decoded.get("name").getAsString())) {
                        ctx.closeSession();
                    }

                    if (decoded.get("password").getAsString().equals(SecretConfig.password)) {
                        minecraftInstances.put(ctx.session, decoded.get("name").getAsString());
                    } else {
                        ctx.closeSession(1016, "Why");
                    }
                } catch (Exception ignored) {
                    ctx.closeSession();
                }

            });
            ws.onClose(ctx -> minecraftInstances.remove(ctx.session));
            ws.onMessage(ctx -> EventWaiter.onMessage(new MessageEvent(ctx, Utils.getIgnFromSession(ctx.session))));
        });

    }
}
