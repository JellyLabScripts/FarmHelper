package com.jelly.farmhelper.remote.waiter;

import com.jelly.farmhelper.remote.struct.RemoteMessage;
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
                if (waiterTuple != null && waiterTuple.getSecond() != null && !waiterTuple.getFirst().isAnsweredAtleastOnce()) { //if waiter was in the list && there is a timeoutAction
                    waiterToRegister.getTimeoutAction().accept(null);
                } else if (waiterTuple != null && waiterTuple.getFirst().isAnsweredAtleastOnce()) {
                    waiterMap.remove(waiterTuple.getFirst());
                }
            }
        }, waiterToRegister.getTimeout());
    }

    public static void unregister(Waiter waiter) {
        waiterMap.remove(waiter);
    }

    public static void onMessage(RemoteMessage websocketMessage) {
        String command = websocketMessage.command;
        System.out.println("Received message: " + command);
        Set<Waiter> waiters = waiterMap.keySet();
        for (Waiter waiter : waiters) {
            if (waiter.getCommand().equalsIgnoreCase(command)) {
                waiter.getAction().accept(websocketMessage);
                waiter.setAnsweredAtleastOnce(true);
                return;
            }
        }
        System.out.println("No waiter found for command: " + command);
    }
}
