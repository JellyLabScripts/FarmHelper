package com.jelly.farmhelper.config;

public class Config {
    private Object value;
    public Config(Object defaultvalue) {
        this.value = defaultvalue;
    }

    public Object get() {
        return this.value;
    }

    public Object set(Object newValue) {
        value = newValue;
        return value;
    }
}
