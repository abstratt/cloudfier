package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.mdd.rest.impl.v1.representation.TupleParser;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.fasterxml.jackson.databind.JsonNode;

public class TupleParserTests extends AbstractKirraMDDRuntimeTests {
    public TupleParserTests(String name) {
        super(name);
    }

    public void testObject() throws CoreException, IOException {
        String model1 = "";
        model1 += "package mypackage1;\n";
        model1 += "import base;\n";
        model1 += "apply kirra;\n";
        model1 += "datatype MyClass\n";
        model1 += "  attribute attr1 : String;\n";
        model1 += "  attribute attr2 : Integer;\n";
        model1 += "  attribute attr3 : Boolean;\n";
        model1 += "end;\n";
        model1 += "end.\n";

        parseAndCheck(model1);
        
        SchemaManagement schemaManagement = getKirraRepository();
        TupleParser tupleParser = new TupleParser(schemaManagement);
        JsonNode tupleRepresentation = JsonHelper.parse(new StringReader("{ attr1 : 'test', attr2 : 4, attr3 : true }"));
        TupleType tupleType = schemaManagement.getTupleType("mypackage1", "MyClass");
        assertNotNull(tupleType);
        Tuple tuple = tupleParser.getTupleFromJsonRepresentation(tupleRepresentation, tupleType);
        assertEquals("test", tuple.getValue("attr1"));
        assertEquals(4L, tuple.getValue("attr2"));
        assertEquals(true, tuple.getValue("attr3"));
    }
    
    public void testArrayOfObjects() throws CoreException, IOException {
        String model1 = "";
        model1 += "package mypackage1;\n";
        model1 += "import base;\n";
        model1 += "apply kirra;\n";
        model1 += "datatype MyClass1\n";
        model1 += "  attribute attr1 : String;\n";
        model1 += "  attribute attr2 : MyClass2[*];\n";
        model1 += "end;\n";
        model1 += "datatype MyClass2\n";
        model1 += "  attribute attr3 : String;\n";
        model1 += "end;\n";
        model1 += "end.\n";

        parseAndCheck(model1);
        
        SchemaManagement schemaManagement = getKirraRepository();
        TupleParser tupleParser = new TupleParser(schemaManagement);
        JsonNode tupleRepresentation = JsonHelper.parse(new StringReader("{ attr1 : 'test', attr2 : [ { attr3 : 'foo' }, { attr3 : 'bar', attr4 : 'zed' } ], attr5 : 'fred' }"));
        TupleType tupleType = schemaManagement.getTupleType("mypackage1", "MyClass1");
        assertNotNull(tupleType);
        Tuple tuple = tupleParser.getTupleFromJsonRepresentation(tupleRepresentation, tupleType);
        assertEquals("test", tuple.getValue("attr1"));
        List<?> list = (List<?>) tuple.getValue("attr2");
        assertEquals(2, list.size());
        Tuple sub1 = (Tuple) list.get(0);
        assertEquals("foo", sub1.getValue("attr3"));
        Tuple sub2 = (Tuple) list.get(1);
        assertEquals("bar", sub2.getValue("attr3"));
    }
}
