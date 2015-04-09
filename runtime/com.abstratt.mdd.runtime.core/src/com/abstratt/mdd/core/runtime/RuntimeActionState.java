package com.abstratt.mdd.core.runtime;

public enum RuntimeActionState {
    /** The action has completed execution. All its output pins have been fed. */
    COMPLETE(null),
    /** The action is currently executing. */
    EXECUTING(COMPLETE),
    /** The action has not executed yet, but has all its input pins fed. */
    READY(EXECUTING),
    /** The action has input pins yet to be fed. */
    WAITING(READY);

    private RuntimeActionState next;

    RuntimeActionState(RuntimeActionState next) {
        this.next = next;
    }

    public boolean follows(RuntimeActionState another) {
        if (another.next == null)
            return false;
        return another.next == this ? true : follows(another.next);
    }

    public RuntimeActionState getNextState() {
        return next;
    }
}
