package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A very simple state machine implementation,
 */
public class SSM {

    public static class AndGuard implements Guard {
        private Guard[] delegates;

        public AndGuard(Guard[] delegates) {
            this.delegates = delegates;
        }

        @Override
        public boolean enabled(Object system) {
            for (int i = 0; i < delegates.length; i++)
                if (!delegates[i].enabled(system))
                    return false;
            return true;
        }
    }

    /**
     * Describes a state transition event.
     */
    public static class Event {
        private Object newState;

        private Object originalState;

        public Event(Object originalState, Object newState) {
            this.originalState = originalState;
            this.newState = newState;
        }

        public Object getNewState() {
            return newState;
        }

        public Object getOriginalState() {
            return originalState;
        }

        @Override
        public String toString() {
            return originalState + " > " + newState;
        }

    }

    /**
     * A guard is a condition for a transition to be enabled.
     */
    public static interface Guard {
        public boolean enabled(Object system);
    }

    public static interface Listener {
        public void handleEvent(Event event);
    }

    /**
     * A guard that negates the value of its delegate.
     */
    public static class NotGuard implements Guard {
        private Guard delegate;

        public NotGuard(Guard delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean enabled(Object system) {
            return !delegate.enabled(system);
        }
    }

    private static class Transition {
        Guard guard;

        Object source;

        Object target;

        private Runnable trigger;

        public Transition(Object source, Object target, Guard guard, Runnable trigger) {
            this.source = source;
            this.target = target;
            this.guard = guard;
            this.trigger = trigger;
        }
    }

    private Object current;

    private Object initial;

    private Set<Listener> listeners = new HashSet<Listener>();

    private Set<Object> states = new HashSet<Object>();

    private Object system;

    /**
     * A dictionary of all transitions, keyed by source state.
     */
    private Map<Object, List<Transition>> transitions = new HashMap<Object, List<Transition>>();

    /**
     * Creates a state machine based on the given system provided by the user.
     */
    public SSM(Object system) {
        this.system = system;
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void addState(Object newState) {
        states.add(newState);
    }

    public void addTransition(Object source, Object target, Guard guard, Runnable trigger) {
        List<Transition> existing = transitions.get(source);
        if (existing == null)
            transitions.put(source, existing = new ArrayList<Transition>());
        existing.add(new Transition(source, target, guard, trigger));
    }

    /**
     * Returns whether a state transition actually occurred.
     */
    public boolean animate() {
        List<Transition> availableTransitions = transitions.get(current);
        if (availableTransitions == null)
            // no transitions for the current state (final state)
            return false;
        for (Object element : availableTransitions)
            // tries every transition available
            if (tryToTransition((Transition) element))
                // state transition performed
                return true;
        // no enabled transitions were found for the current state
        return false;
    }

    /**
     * Returns the current state.
     */
    public Object getCurrent() {
        return current;
    }

    /**
     * Returns the initial state.
     */
    public Object getInitial() {
        return initial;
    }

    /**
     * Returns whether the given state is a final state.
     */
    public boolean isFinal(Object state) {
        return !transitions.containsKey(state);
    }

    /**
     * Returns whether the state machine is finished, i.e., its current state is
     * a final state,
     */
    public boolean isFinished() {
        return isFinal(current);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Restarts the state machine, making the initial state the current state.
     */
    public void restart() {
        this.current = initial;
    }

    /**
     * Sets the initial state. Restarts the state machine as a side-effect.
     */
    public void setInitial(Object initial) {
        this.initial = initial;
        this.current = initial;
    }

    private void fireEvent(Event event) {
        Listener[] current = listeners.toArray(new Listener[listeners.size()]);
        for (Listener element : current)
            element.handleEvent(event);
    }

    private boolean tryToTransition(Transition transition) {
        if (!transition.guard.enabled(system))
            return false;
        current = transition.target;
        if (transition.trigger != null)
            // a trigger (when exists) must be executed when a state transition
            // occurs
            transition.trigger.run();
        fireEvent(new Event(transition.source, transition.target));
        return true;
    }
}
