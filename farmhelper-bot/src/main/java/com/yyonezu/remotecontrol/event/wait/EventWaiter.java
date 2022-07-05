package com.yyonezu.remotecontrol.event.wait;

import com.yyonezu.remotecontrol.event.MessageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class EventWaiter {
    private static final HashMap<Double, Waiter> waiterMap = new HashMap<>();
    private static final ArrayList<Waiter> toDelete = new ArrayList<>();

    public static void onMessage(MessageEvent e) {
        waiterMap.values().forEach(waiter -> {
            if (waiter.getConditions().test(e)) {
                waiter.getAction().accept(new WaiterAction(e.ctx, e.user, waiter.getId()));
                if (waiter.autoRemove)
                    toDelete.add(waiter);
            }
        });
        waiterMap.values().removeAll(toDelete);
    }

    public static void register(Waiter waiterToRegister) {
        waiterToRegister.setId(Math.random());
        waiterMap.put(waiterToRegister.getId(), waiterToRegister);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(waiterMap.get(waiterToRegister.getId()) != null && waiterToRegister.getTimeoutAction() != null) { //if waiter was in the list && there is a timeoutAction
                    waiterToRegister.getTimeoutAction().run();
                    unregister(waiterToRegister);
                }
            }
        }, TimeUnit.MILLISECONDS.convert(waiterToRegister.getExpirationTime(), waiterToRegister.getTimeUnit()));
    }

    public static void unregister(Waiter waiter) {
        toDelete.add(waiter);
    }

    public static void unregister(WaiterAction action) {
        toDelete.add(waiterMap.get(action.id));
    }

}
