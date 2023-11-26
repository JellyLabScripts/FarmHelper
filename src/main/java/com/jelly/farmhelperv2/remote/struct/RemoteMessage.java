package com.jelly.farmhelperv2.remote.struct;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

public class RemoteMessage {
    @Expose
    public String command;
    @Expose
    public JsonObject args;
    @Expose
    public String embed;

    public RemoteMessage(String command, JsonObject args) {
        this.command = command;
        this.args = args;
    }

    public RemoteMessage(String command, JsonObject args, String embed) {
        this.command = command;
        this.args = args;
        this.embed = embed;
    }
}
