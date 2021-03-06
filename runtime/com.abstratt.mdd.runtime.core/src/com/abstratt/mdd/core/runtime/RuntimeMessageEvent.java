package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.MessageEvent;
import org.eclipse.uml2.uml.Trigger;

import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.EventUtils;

public class RuntimeMessageEvent<M> extends RuntimeEvent {
    public static <M> RuntimeMessageEvent<M> build(M message, BasicType target, BasicType... args) {
        return new RuntimeMessageEvent<M>(message, target, args);
    }

    private BasicType[] arguments;
    private M message;

    public RuntimeMessageEvent(M message, BasicType target, BasicType... args) {
        super(target);
        this.message = message;
        this.arguments = args;
    }

    public BasicType[] getArguments() {
        return arguments;
    }

    public M getMessage() {
        return message;
    }

    @Override
    public boolean isMatchedBy(Trigger trigger) {
        MessageEvent event = (MessageEvent) trigger.getEvent();
        return EventUtils.<M> getMessage(event) == this.message;
    }
}
