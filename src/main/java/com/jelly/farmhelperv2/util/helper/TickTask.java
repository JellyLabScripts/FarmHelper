package com.jelly.farmhelperv2.util.helper;

import cc.polyfrost.oneconfig.utils.Multithreading;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

@Getter
public class TickTask {
    private static TickTask instance;
    @Setter
    private Runnable task;
    @Setter
    private Runnable callback;

    public static TickTask getInstance() {
        if (instance == null) {
            instance = new TickTask();
        }
        return instance;
    }

    public void schedule(int delay, Runnable task) {
        Multithreading.schedule(() -> this.task = task, delay, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (task != null) {
            task.run();
            if (callback != null) {
                task = callback;
                callback = null;
            } else {
                task = null;
            }
        }
    }
}
