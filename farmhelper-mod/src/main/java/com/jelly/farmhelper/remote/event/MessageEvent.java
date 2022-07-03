package com.jelly.farmhelper.remote.event;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.Client;

public class MessageEvent {
    public Client ws;
    public JsonObject obj;

    public MessageEvent(Client ws, JsonObject obj) {
        this.ws = ws;
        this.obj = obj;
    }
}
