package com.abstratt.mdd.core.tests.runtime;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.abstratt.mdd.core.runtime.SSM;
import com.abstratt.mdd.core.runtime.SSM.Event;
import com.abstratt.mdd.core.runtime.SSM.Guard;

public class SSMTests extends TestCase {

    static class RemainderGuard implements Guard {
        private int divisor;
        private int remainder;

        RemainderGuard(int divisor, int remainder) {
            this.remainder = remainder;
            this.divisor = divisor;
        }

        @Override
        public boolean enabled(Object system) {
            List list = (List) system;
            if (list.isEmpty())
                return false;
            if (((Integer) list.get(0)).intValue() % divisor == remainder) {
                list.remove(0);
                return true;
            }
            return false;
        }

    }

    static class TestListener implements SSM.Listener {
        private List<Event> collected = new ArrayList<Event>();

        @Override
        public void handleEvent(Event event) {
            collected.add(event);
        }

        public Event next() {
            return collected.isEmpty() ? null : (Event) collected.remove(0);
        }
    }

    public static Test suite() {
        return new TestSuite(SSMTests.class);
    }

    public SSMTests(String name) {
        super(name);
    }

    public SSM buildMachine(int[] input) {
        Guard zero = new RemainderGuard(3, 0);
        Guard one = new RemainderGuard(3, 1);
        Guard two = new RemainderGuard(3, 2);
        final List<Integer> system = new ArrayList<Integer>(input.length);
        for (int element : input)
            system.add(element);
        SSM machine = new SSM(system);
        machine.addState("s0");
        machine.addState("s1");
        machine.addState("s2");
        machine.addState("s3");
        machine.setInitial("s0");

        machine.addTransition("s0", "s1", one, null);
        machine.addTransition("s0", "s2", two, null);
        machine.addTransition("s0", "s0", zero, null);
        machine.addTransition("s1", "s1", zero, null);
        machine.addTransition("s1", "s0", one, null);
        machine.addTransition("s1", "s2", two, null);
        machine.addTransition("s2", "s0", one, null);
        machine.addTransition("s2", "s2", zero, null);
        machine.addTransition("s2", "s3", two, null);

        return machine;
    }

    public void testBasic() {
        SSM machine = buildMachine(new int[] { 1, 2, 2 });
        TestCase.assertEquals(machine.getCurrent(), machine.getInitial());
        TestCase.assertEquals("s0", machine.getCurrent());
        machine.animate();
        TestCase.assertEquals("s1", machine.getCurrent());
        machine.animate();
        TestCase.assertEquals("s2", machine.getCurrent());
        machine.animate();
        TestCase.assertEquals("s3", machine.getCurrent());
        TestCase.assertTrue(machine.isFinal(machine.getCurrent()));
    }

    public void testEvents() {
        SSM machine = buildMachine(new int[] { 1, 2, 2 });
        TestListener listener = new TestListener();
        machine.addListener(listener);
        while (machine.animate())
            ;
        Event event;
        event = listener.next();
        TestCase.assertNotNull(event);
        TestCase.assertEquals("s0", event.getOriginalState());
        TestCase.assertEquals("s1", event.getNewState());

        event = listener.next();
        TestCase.assertNotNull(event);
        TestCase.assertEquals("s1", event.getOriginalState());
        TestCase.assertEquals("s2", event.getNewState());

        event = listener.next();
        TestCase.assertNotNull(event);
        TestCase.assertEquals("s2", event.getOriginalState());
        TestCase.assertEquals("s3", event.getNewState());

        event = listener.next();
        TestCase.assertNull(event);
    }

    public void testLoop() {
        SSM machine = buildMachine(new int[] { 2, 1, 1, 0, 1, 2, 0, 2 });
        Object[] current = { "s2", "s0", "s1", "s1", "s0", "s2", "s2", "s3" };
        for (int i = 0; i < current.length; i++) {
            TestCase.assertTrue(machine.animate());
            TestCase.assertEquals(i + " - ", current[i], machine.getCurrent());
        }
        TestCase.assertTrue(machine.isFinal(machine.getCurrent()));
    }
}
