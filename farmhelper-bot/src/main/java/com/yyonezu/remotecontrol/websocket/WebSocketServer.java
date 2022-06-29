package com.yyonezu.remotecontrol.websocket;

import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.Main;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.utils.Utils;
import com.yyonezu.remotecontrol.event.MessageEvent;
import com.yyonezu.remotecontrol.event.wait.EventWaiter;
import io.javalin.Javalin;
import org.eclipse.jetty.websocket.api.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class WebSocketServer {
    public static HashMap<Session, String> minecraftInstances = new HashMap<>();
    public static Javalin app;
    public static void start() {
        app = Javalin.create().start(7070);
        app.ws("/farmhelperws", ws -> {
            ws.onConnect(ctx -> {
                try {
                    String b64header = ctx.session.getUpgradeRequest().getHeader("auth");
                    JSONObject decoded = (JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(b64header), StandardCharsets.UTF_8));
                    if (decoded.get("password").equals(SecretConfig.password)) {
                        minecraftInstances.put(ctx.session, decoded.get("name").toString());
                    } else {
                        ctx.closeSession(69, "lol");
                    }
                } catch (Exception ignored) {
                    ctx.closeSession();
                }

            });
            ws.onClose(ctx -> {
                minecraftInstances.remove(ctx.session);
            });
            ws.onMessage(ctx -> {
                    EventWaiter.onMessage(new MessageEvent(ctx, Utils.getIgnFromSession(ctx.session)));
            });
        });

    }
}
