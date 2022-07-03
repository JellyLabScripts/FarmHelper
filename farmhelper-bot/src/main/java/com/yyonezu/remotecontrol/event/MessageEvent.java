package com.yyonezu.remotecontrol.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.websocket.WsMessageContext;
import lombok.SneakyThrows;


public class MessageEvent {
    public final WsMessageContext ctx;
    public final JsonObject message;
    public final String user;

    @SneakyThrows
    public MessageEvent(WsMessageContext ctx, String user) {
        this.ctx = ctx;
        this.message = new Gson().fromJson(ctx.message(), JsonObject.class);
        this.user = user;
    }

    public boolean matchesMetadata(JsonObject data) {
        return  data.get("metadata").toString()
                .equals(this.message.get("metadata").toString());
    }
}
