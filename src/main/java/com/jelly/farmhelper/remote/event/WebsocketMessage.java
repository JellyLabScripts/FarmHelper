package com.jelly.farmhelper.remote.event;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.jelly.farmhelper.network.DiscordWebhook;

public class WebsocketMessage {
    @Expose
    public String command;
    @Expose
    public JsonObject args;
    @Expose
    public String embed;

    public WebsocketMessage(String command, JsonObject args) {
        this.command = command;
        this.args = args;
    }

    public WebsocketMessage(String command, JsonObject args, String embed) {
        this.command = command;
        this.args = args;
        this.embed = embed;
    }
}
