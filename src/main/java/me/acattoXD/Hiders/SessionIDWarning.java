package me.acattoXD.Hiders;

import com.jelly.FarmHelper.utils.Utils;

public class SessionIDWarning extends RuntimeException {
    public SessionIDWarning() {
        super(Utils.getExecutor() + " is probably a rat. Please delete this mod as soon as you seen this message, change your password's etc.");
    }
}