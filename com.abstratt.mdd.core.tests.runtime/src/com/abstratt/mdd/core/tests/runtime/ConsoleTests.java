package com.abstratt.mdd.core.tests.runtime;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeClass;

public class ConsoleTests extends AbstractRuntimeTests {

	public static Test suite() {
		return new TestSuite(ConsoleTests.class);
	}

	public ConsoleTests(String name) {
		super(name);
	}

	public void testPrint() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "import base;\n";
		source += "class Simple\n";
		source += "static operation testing();\n";
		source += "begin\n";
		source += "Console#writeln(\"This is a test\");\n";
		source += "end;\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(new String[] {source});
		Classifier classDef = (Classifier) getRepository().findNamedElement("tests::Simple", IRepository.PACKAGE.getClassifier(), null);
		final Operation operation = classDef.getOperation("testing", null, null);
		final RuntimeClass runtimeClass = getRuntime().getRuntimeClass(classDef);
		ConsoleCapture capture = new ConsoleCapture();
		List<String> output = capture.collect(new Runnable() {
			public void run() {
				getRuntime().runOperation(null, runtimeClass.getClassObject(), operation);
			}
		});
		assertEquals(output.toString(), 1, output.size());
		assertEquals("This is a test", output.get(0));
	}
}