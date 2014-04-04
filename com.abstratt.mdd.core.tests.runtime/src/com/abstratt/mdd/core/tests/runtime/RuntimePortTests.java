package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class RuntimePortTests extends AbstractRuntimeTests {

	public static Test suite() {
		return new TestSuite(RuntimePortTests.class);
	}

	public RuntimePortTests(String name) {
		super(name);
	}
	
	public void testInjection() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "interface UserNotification\n";
		source += "end;\n";
		source += "class Issue\n";
		source += "    port userNotification : UserNotification;\n";
		source += "end;\n";
		source += "class EmailNotification implements UserNotification\n";
		source += "end;\n";
		source += "component MyComponent\n";
		source += "    composition emailNotification : EmailNotification;\n";
		source += "    composition issues : Issue[*];\n";
		source += "    port userNotification : UserNotification connector emailNotification, issues.userNotification;\n";
		source += "end;\n";
		source += "end.";
		
		String[] sources = { source };
		parseAndCheck(sources);
		final RuntimeClass issueClass = getRuntimeClass("tests::Issue");
		final RuntimeObject issue = issueClass.newInstance();
		assertNotNull(readAttribute(issue, "userNotification"));
	}
}
