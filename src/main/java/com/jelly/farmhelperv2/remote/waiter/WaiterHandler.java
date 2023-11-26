package com.jelly.farmhelperv2.remote.waiter;

import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.util.Tuple;

import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class WaiterHandler {
    private static final HashMap<Waiter, Integer> waiterMap = new HashMap<>();

    public static void register(Waiter waiterToRegister) {
        waiterMap.put(waiterToRegister, waiterToRegister.getTimeout());
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Tuple<Waiter, Integer> waiterTuple = null;
                for (Waiter waiter : waiterMap.keySet()) {
                    if (waiter == waiterToRegister) {
                        waiterTuple = new Tuple<>(waiter, waiterMap.get(waiter));
                    }
                }
                if (waiterTuple != null && waiterTuple.getSecond() != null && !waiterTuple.getFirst().isAnsweredAtLeastOnce()) { //if waiter was in the list && there is a timeoutAction
                    waiterToRegister.getTimeoutAction().accept(null);
                } else if (waiterTuple != null && waiterTuple.getFirst().isAnsweredAtLeastOnce()) {
                    waiterMap.remove(waiterTuple.getFirst());
                }
            }
        }, waiterToRegister.getTimeout());
    }

    public static void onMessage(RemoteMessage websocketMessage) {
        String command = websocketMessage.command;
        LogUtils.sendDebug("Received message: " + command);
        Set<Waiter> waiters = waiterMap.keySet();
        for (Waiter waiter : waiters) {
            if (waiter.getCommand().equalsIgnoreCase(command)) {
                waiter.getAction().accept(websocketMessage);
                waiter.setAnsweredAtLeastOnce(true);
                return;
            }
        }
        LogUtils.sendDebug("No waiter found for command: " + command);
    }
}
