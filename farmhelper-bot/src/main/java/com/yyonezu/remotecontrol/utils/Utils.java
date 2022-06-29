package com.yyonezu.remotecontrol.utils;

import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;

public class Utils {
    public static Session getSessionFromIGN(String ign) {
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            if (entry.getValue().equals(ign)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String getIgnFromSession(Session s) {
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            if (entry.getKey().equals(s)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
