package com.jelly.farmhelper.remote.event;

import com.jelly.farmhelper.remote.Client;
import org.json.simple.JSONObject;

public class MessageEvent {
    public Client ws;
    public JSONObject obj;

    public MessageEvent(Client ws, JSONObject obj) {
        this.ws = ws;
        this.obj = obj;
    }
}
