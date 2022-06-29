package com.yyonezu.remotecontrol.event;

import io.javalin.websocket.WsMessageContext;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MessageEvent {
    public final WsMessageContext ctx;
    public final JSONObject message;
    public final String user;

    @SneakyThrows
    public MessageEvent(WsMessageContext ctx, String user) {
        this.ctx = ctx;
        this.message = (JSONObject) new JSONParser().parse(ctx.message());
        this.user = user;
    }

    public boolean matchesMetadata(JSONObject data) {
        return ( (JSONObject) data.get("metadata")).toJSONString()
                .equals(( (JSONObject) this.message.get("metadata")).toJSONString());
    }
}
