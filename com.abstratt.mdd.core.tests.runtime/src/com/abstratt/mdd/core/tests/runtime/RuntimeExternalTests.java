package com.abstratt.mdd.core.tests.runtime;

import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.external.ExternalObjectDelegate;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.IntegerType;

public class RuntimeExternalTests extends AbstractRuntimeTests {

	public static Test suite() {
		return new TestSuite(RuntimeExternalTests.class);
	}

	public RuntimeExternalTests(String name) {
		super(name);
	}
	
	public void testInvokeExternalOperation() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "external class ExternalService\n";
		source += "    operation someOperation(op1 : Integer, op2 : Integer) : Integer;\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(source);
		final org.eclipse.uml2.uml.Class externalClass = getClass("tests::ExternalService");
		final Operation externalOperation = get("tests::ExternalService::someOperation", UMLPackage.Literals.OPERATION);
		getRuntime().registerExternalDelegate(new ExternalObjectDelegate() {
			@Override
			public BasicType getData(Classifier classifier,
					Operation operation, Object... arguments) {
				assertSame(externalClass, classifier);
				assertSame(externalOperation, operation);
				return ((IntegerType) arguments[0]).add(null, (IntegerType) arguments[1]);
			}
			@Override
			public void receiveSignal(Classifier classifier, Signal signal,
					Object... arguments) {
			}
		});
		IntegerType result = (IntegerType) runOperation(getRuntime().getInstance(externalClass), "someOperation", new IntegerType(30), new IntegerType(12));
		assertEquals(IntegerType.fromValue(42), result);
	}
	
	public void testSendSignalToExternalObject() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "signal ASignal\n";
		source += "    attribute aValue : Integer;\n";
		source += "end;\n";
		source += "external class ExternalService\n";
		source += "    reception someReception(aSignal : ASignal);\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(source);
		final org.eclipse.uml2.uml.Class externalClass = getClass("tests::ExternalService");
		final Signal signal = get("tests::ASignal", UMLPackage.Literals.SIGNAL);
		final RuntimeObject[] eventReceived = {null};
		getRuntime().registerExternalDelegate(new ExternalObjectDelegate() {
			@Override
			public BasicType getData(Classifier classifier,
					Operation operation, Object... arguments) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void receiveSignal(Classifier classifier, Signal received,
					Object... arguments) {
				assertSame(externalClass, classifier);
				assertSame(signal, received);
				assertEquals(1, arguments.length);
				assertTrue(arguments[0] instanceof RuntimeObject);
				RuntimeObject event = (RuntimeObject) arguments[0];
				eventReceived[0] = event;
			}
		});
		sendSignal(getRuntime().getInstance(externalClass), "tests::ASignal", Collections.singletonMap("aValue", (BasicType) IntegerType.fromValue(10)));
		getRuntime().getCurrentContext().processPendingEvents();
		assertNotNull(eventReceived[0]);
		BasicType valueReceived = eventReceived[0].getValue(getProperty("tests::ASignal::aValue"));
		assertEquals(IntegerType.fromValue(10), valueReceived);
	}
}
