package com.abstratt.mdd.core.tests.runtime;

import java.util.Collections;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.StateMachineType;
import com.abstratt.mdd.core.util.StateMachineUtils;

public class RuntimeStateMachineTests extends AbstractRuntimeTests {

	static String model = "";

	static {
		model += "model tests;\n";
		model += "  signal CancelOrder end;\n";
		model += "  signal DeliverOrder end;\n";
		model += "  signal ShipOrder end;\n";
		model += "  signal CheckoutOrder end;\n";
		model += "  class Order\n";
		model += "    attribute status : Status;\n";
		model += "    operation bump(); begin end;\n";
		model += "    operation checkout(); begin end;\n";
		model += "    operation cancel(); begin end;\n";
		model += "    operation ship(); begin end;\n";
		model += "    operation deliver(); begin end;\n";
		model += "    statemachine Status\n";
		model += "        initial state Preparing\n";
		model += "            transition on call(checkout),signal(CheckoutOrder) to Open;\n";
		model += "            transition on call(cancel),signal(CancelOrder) to Cancelled;\n";
		model += "        end;\n";
		model += "        state Open\n";
		model += "            transition on call(ship),signal(ShipOrder) to Shipping;\n";
		model += "            transition on call(cancel),signal(CancelOrder) to Cancelled;\n";
		model += "        end;\n";
		model += "        state Shipping\n";
		model += "            transition on call(deliver), signal(DeliverOrder) to Delivered;\n";
		model += "        end;\n";
		model += "        terminate state Cancelled\n";
		model += "        end;\n";
		model += "        terminate state Delivered\n";
		model += "        end;\n";
		model += "    end;\n";
		model += "    operation isShipping() : Boolean;\n";
		model += "    begin\n";
		model += "        return self.status == Status#Shipping;\n";
		model += "    end;\n";
        model += "  end;\n";        
		model += "end.";
	}

	public static Test suite() {
		return new TestSuite(RuntimeStateMachineTests.class);
	}

	public RuntimeStateMachineTests(String name) {
		super(name);
	}
	
	public void testInitialState() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		
		final org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("tests::Order", UMLPackage.Literals.CLASS, null);
		
		final RuntimeObject[] order = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				order[0] = getRuntimeClass(clazz.getQualifiedName()).newInstance();
				StateMachineType state = (StateMachineType) readAttribute(order[0], "status");
				assertEquals("Preparing", state.getValue().getName());
			}
		});

		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				assertTrue(order[0].isEnabledOperation(clazz.getOperation("checkout", null, null), null));
				assertTrue(!order[0].isEnabledOperation(clazz.getOperation("ship", null, null), null));
				assertTrue(order[0].isEnabledOperation(clazz.getOperation("cancel", null, null), null));
				assertTrue(!order[0].isEnabledOperation(clazz.getOperation("deliver", null, null), null));
				assertTrue(order[0].isEnabledOperation(clazz.getOperation("bump", null, null), null));
			}
		});
	}
	
	public void testCheckState() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		
		final org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("tests::Order", UMLPackage.Literals.CLASS, null);
		
		final RuntimeObject[] order = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				order[0] = getRuntimeClass(clazz.getQualifiedName()).newInstance();
				StateMachine stateMachine = (StateMachine) clazz.getOwnedBehavior("Status");
				assertTrue(BooleanType.FALSE.equals(runOperation(order[0], "isShipping")));
				writeAttribute(order[0], "status", new StateMachineType(StateMachineUtils.getVertex(stateMachine, "Shipping")));
				StateMachineType state = (StateMachineType) readAttribute(order[0], "status");
				assertEquals("Shipping", state.getValue().getName());
				assertEquals(BooleanType.TRUE, runOperation(order[0], "isShipping"));
			}
		});
	}

	
	public void testTransitionOnCall() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		
		final org.eclipse.uml2.uml.Class orderClass = getRepository().findNamedElement("tests::Order", UMLPackage.Literals.CLASS, null);
		
		final RuntimeObject[] order = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				order[0] = getRuntimeClass(orderClass.getQualifiedName()).newInstance();
				runOperation(order[0], "cancel");
			}
		});
		
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				StateMachineType state = (StateMachineType) readAttribute(order[0], "status");
				assertEquals("Cancelled", state.getValue().getName());
				
				assertTrue(!order[0].isEnabledOperation(orderClass.getOperation("checkout", null, null), null));
				assertTrue(!order[0].isEnabledOperation(orderClass.getOperation("ship", null, null), null));
				assertTrue(!order[0].isEnabledOperation(orderClass.getOperation("cancel", null, null), null));
				assertTrue(!order[0].isEnabledOperation(orderClass.getOperation("deliver", null, null), null));
				assertTrue(order[0].isEnabledOperation(orderClass.getOperation("bump", null, null), null));
			}
		});
	}
	
	public void testTransitionOnSignal() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		
		final org.eclipse.uml2.uml.Class orderClass = getRepository().findNamedElement("tests::Order", UMLPackage.Literals.CLASS, null);
		
		final RuntimeObject[] order = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				order[0] = getRuntimeClass(orderClass.getQualifiedName()).newInstance();
				
				StateMachineType state = (StateMachineType) readAttribute(order[0], "status");
				assertEquals("Preparing", state.getValue().getName());
				
				RuntimeObject signal = getRuntimeClass("tests::CheckoutOrder").newInstance(false, false);
				
				sendSignal(order[0], signal);
			}
		});
		
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				StateMachineType state = (StateMachineType) readAttribute(order[0], "status");
				assertEquals("Open", state.getValue().getName());
			}
		});
    }
	
	public void testStateBehaviors() throws CoreException {
		String localModel = "";
		localModel += "model tests;\n";
		localModel += "  signal Sig1 end;\n";
		localModel += "  class Class1\n";
		localModel += "    attribute status : Status;\n";
		localModel += "    attribute value : String := \"\";\n";
		localModel += "    operation debug(s : String);\n";
		localModel += "    begin\n";
		localModel += "        self.value := self.value + \"|\" + s;\n";
		localModel += "    end;\n";
		localModel += "    statemachine Status\n";
		localModel += "        initial state state0\n";
		localModel += "            transition on signal(Sig1) to state1;\n";
		localModel += "        end;\n";
		localModel += "        state state1\n";
		localModel += "            entry { self.debug(\"entry-state1\"); };\n";
		localModel += "            do { self.debug(\"do-state1\"); };\n";
		localModel += "            exit { self.debug(\"exit-state1\"); };\n";
		localModel += "            transition on signal(Sig1) to state2;\n";
		localModel += "        end;\n";
		localModel += "        terminate state state2\n";
		localModel += "        end;\n";
		localModel += "    end;\n";
        localModel += "  end;\n";        
		localModel += "end.";

		String[] sources = { localModel };
		parseAndCheck(sources);
		
		final RuntimeObject[] instance = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				instance[0] = getRuntimeClass("tests::Class1").newInstance();
				
				StateMachineType state = (StateMachineType) readAttribute(instance[0], "status");
				assertEquals("state0", state.getValue().getName());
				
				sendSignal(instance[0], "tests::Sig1", Collections.<String, BasicType>emptyMap());
			}
		});
		
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				StateMachineType state = (StateMachineType) readAttribute(instance[0], "status");
				assertEquals("state1", state.getValue().getName());
				assertEquals("|entry-state1|do-state1", readAttribute(instance[0], "value").toString());
				
				sendSignal(instance[0], "tests::Sig1", Collections.<String, BasicType>emptyMap());
			}
		});
		
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				StateMachineType state = (StateMachineType) readAttribute(instance[0], "status");
				assertEquals("state2", state.getValue().getName());
				assertEquals("|entry-state1|do-state1|exit-state1", readAttribute(instance[0], "value").toString());
			}
		});
    }
	
	public void testTriggerBehaviors() throws CoreException {
		String localModel = "";
		localModel += "model tests;\n";
		localModel += "  signal Sig1 end;\n";
		localModel += "  class Class1\n";
		localModel += "    attribute status : Status;\n";
		localModel += "    attribute value : String := \"\";\n";
		localModel += "    operation debug(s : String);\n";
		localModel += "    begin\n";
		localModel += "        self.value := s;\n";
		localModel += "    end;\n";
		localModel += "    statemachine Status\n";
		localModel += "        initial state state0\n";
		localModel += "            transition on signal(Sig1) to state1 when {false} do { self.debug(\"t1\"); };\n";
		localModel += "            transition on signal(Sig1) to state2 when {false} do { self.debug(\"t2\"); };\n";
		localModel += "            transition on signal(Sig1) to state3 when {true} do { self.debug(\"t3\"); };\n";
		localModel += "            transition on signal(Sig1) to state4 when {false} do { self.debug(\"t4\"); };\n";
		localModel += "        end;\n";
		localModel += "        state state1 end;\n";
		localModel += "        state state2 end;\n";
		localModel += "        state state3 end;\n";
		localModel += "        state state4 end;\n";
		localModel += "    end;\n";
        localModel += "  end;\n";        
		localModel += "end.";

		String[] sources = { localModel };
		parseAndCheck(sources);
		
		final RuntimeObject[] instance = {null};
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				instance[0] = getRuntimeClass("tests::Class1").newInstance();
				
				StateMachineType state = (StateMachineType) readAttribute(instance[0], "status");
				assertEquals("state0", state.getValue().getName());
				
				sendSignal(instance[0], "tests::Sig1", Collections.<String, BasicType>emptyMap());
			}
		});
		
		runAndProcessEvents(new Runnable() {
			@Override
			public void run() {
				StateMachineType state = (StateMachineType) readAttribute(instance[0], "status");
				assertEquals("state3", state.getValue().getName());
				assertEquals("t3", readAttribute(instance[0], "value").toString());
			}
		});
    }
	
	@Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        return defaultSettings;
    }
}
