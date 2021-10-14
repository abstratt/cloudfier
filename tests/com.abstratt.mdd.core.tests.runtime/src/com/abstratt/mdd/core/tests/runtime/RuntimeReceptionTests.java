package com.abstratt.mdd.core.tests.runtime;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.StringType;

public class RuntimeReceptionTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeReceptionTests.class);
    }

    public RuntimeReceptionTests(String name) {
        super(name);
    }

    public void testSignalReception() throws CoreException {
        String model = "";

        model += "model tests;\n";
        model += "  import base;\n";
        model += "  signal NameChange\n";
        model += "    attribute newName : String;\n";
        model += "  end;\n";
        model += "  class Person\n";
        model += "    attribute name : String[0,1];\n";
        model += "    operation setName(newName : String);\n";
        model += "    begin\n";
        model += "      send NameChange(newName := newName) to self;\n";
        model += "    end;\n";
        model += "    reception(s : NameChange);\n";
        model += "    begin\n";
        model += "      self.name := s.newName;\n";
        model += "    end;\n";
        model += "  end;\n";
        model += "end.";

        String[] sources = { model };
        parseAndCheck(sources);
        final RuntimeClass personClass = getRuntimeClass("tests::Person");
        final RuntimeObject[] person = { null };

        runAndProcessEvents(new Runnable() {
            @Override
            public void run() {
                person[0] = personClass.newInstance();
            }
        });

        TestCase.assertNull(readAttribute(person[0], "name"));

        runAndProcessEvents(new Runnable() {
            @Override
            public void run() {
                RuntimeReceptionTests.this.runOperation(person[0], "setName", new StringType("Foo"));
            }
        });

        TestCase.assertEquals(new StringType("Foo"), readAttribute(person[0], "name"));
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        return defaultSettings;
    }
}
