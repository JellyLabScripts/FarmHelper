package com.yyonezu.remotecontrol.event.wait;

import com.yyonezu.remotecontrol.event.MessageEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class Waiter {
    @Getter @Setter
    private Predicate<MessageEvent> conditions;
    @Getter @Setter
    private Consumer<WaiterAction> action;
    @Getter @Setter
    boolean autoRemove;
    @Getter @Setter
    private Long expirationTime;
    @Getter
    private TimeUnit timeUnit;
    @Getter @Setter
    private Runnable timeoutAction;
    @Getter @Setter
    private Double id;

    public Waiter(Predicate<MessageEvent> conditions, Consumer<WaiterAction> action, boolean autoRemove, Long expirationTime, TimeUnit timeUnit, Runnable timeoutAction) {
        this.conditions = conditions;
        this.action = action;
        this.autoRemove = autoRemove;
        this.expirationTime = expirationTime;
        this.timeUnit = timeUnit;
        this.timeoutAction = timeoutAction;
    }

    public Waiter(Predicate<MessageEvent> conditions, Consumer<WaiterAction> action, boolean autoRemove, Long expirationTime, TimeUnit timeUnit) {
        this(conditions,action,autoRemove,expirationTime,timeUnit,() -> {});
    }

    public Waiter(Predicate<MessageEvent> conditions, Consumer<WaiterAction> action, boolean autoRemove) {
        this(conditions,action,autoRemove,1L,TimeUnit.MINUTES);
    }

    public Waiter() {}
}
