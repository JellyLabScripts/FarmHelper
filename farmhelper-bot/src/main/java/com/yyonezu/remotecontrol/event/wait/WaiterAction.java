package com.yyonezu.remotecontrol.event.wait;

import com.yyonezu.remotecontrol.event.MessageEvent;
import io.javalin.websocket.WsMessageContext;

public class WaiterAction extends MessageEvent {
    public final Double id;
    public WaiterAction(WsMessageContext ctx, String user, Double id) {
        super(ctx, user);
        this.id = id;
    }
}
