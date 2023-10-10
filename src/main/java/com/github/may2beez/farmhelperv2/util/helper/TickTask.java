package com.github.may2beez.farmhelperv2.util.helper;

import cc.polyfrost.oneconfig.utils.Multithreading;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

@Getter
public class TickTask {
    private static TickTask instance;
    public static TickTask getInstance() {
        if (instance == null) {
            instance = new TickTask();
        }
        return instance;
    }

    @Setter
    private Runnable task;

    public void schedule(int delay, Runnable task) {
        Multithreading.schedule(() -> this.task = task, delay, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (task != null) {
            task.run();
            task = null;
        }
    }
}
