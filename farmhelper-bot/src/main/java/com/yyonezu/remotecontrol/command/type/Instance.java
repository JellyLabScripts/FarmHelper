package com.yyonezu.remotecontrol.command.type;

import com.yyonezu.remotecontrol.utils.Utils;
import lombok.Getter;
import org.eclipse.jetty.websocket.api.Session;

import java.util.UUID;

public class Instance {
    @Getter
    Session session;
    @Getter
    String user;
    @Getter
    String id;

    public Instance(String user) {
        this.id = generateUUID();
        this.user = user;
        if (Utils.getSessionFromIGN(user) == null) {
            throw new IllegalArgumentException(user + " isn't a valid instance name. PD: check !status");
        } else {
            session = Utils.getSessionFromIGN(user);
        }
    }
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
