package com.abstratt.mdd.core.tests.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.IntegerType;

public class RuntimeAssociationTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeAssociationTests.class);
    }

    public RuntimeAssociationTests(String name) {
        super(name);
    }

    public void testDerivedLink() throws CoreException {
        String model = "";
        model += "package simple;\n";
        model += "import base;\n";
        model += "class MyNumber\n";
        model += "  attribute value : Integer[0,1];\n";
        model += "end;\n";
        model += "class Referrer\n";
        model += "  attribute attrib1 : String[0,1];\n";
        model += "  attribute numbers : MyNumber[*];\n";
        model += "  derived attribute positiveNumbers : MyNumber[*] :=  { self->numbers.select((n : MyNumber) : Boolean {n.value > 0}) };\n";
        model += "end;\n";
        model += "composition role Referrer.numbers; role owner : Referrer; end;\n";
        model += "end.\n";
        parseAndCheck(model);

        RuntimeObject referrer = newInstance("simple::Referrer");
        RuntimeObject number1 = newInstance("simple::MyNumber");
        writeAttribute(number1, "value", new IntegerType(10));
        RuntimeObject number2 = newInstance("simple::MyNumber");
        writeAttribute(number2, "value", new IntegerType(-10));
        RuntimeObject number3 = newInstance("simple::MyNumber");
        writeAttribute(number3, "value", new IntegerType(20));

        List<BasicType> allNumbers = Arrays.<BasicType> asList(number1, number2, number3);
        List<BasicType> positiveNumbers = Arrays.<BasicType> asList(number1, number3);

        Classifier numberClass = number1.getRuntimeClass().getModelClassifier();

        writeAttribute(referrer, "numbers", CollectionType.createCollection(numberClass, true, true, allNumbers));

        saveContext();

        TestCase.assertTrue(((CollectionType) readAttribute(referrer, "numbers")).getBackEnd().containsAll(allNumbers));
        TestCase.assertEquals(2, ((CollectionType) readAttribute(referrer, "positiveNumbers")).getBackEnd().size());
        TestCase.assertTrue(((CollectionType) readAttribute(referrer, "positiveNumbers")).getBackEnd().containsAll(positiveNumbers));
    }

    public void testOneToManyMakeLink() throws CoreException {
        String[] sources = { buildOneToMany() };
        parseAndCheck(sources);
        RuntimeObject e1a = newInstance("tests::Associated1");
        RuntimeObject e1b = newInstance("tests::Associated1");
        RuntimeObject e2a = newInstance("tests::Associated2");
        RuntimeObject e2b = newInstance("tests::Associated2");

        Property endMany = getProperty("tests::OneToMany::endMany");
        e1a.setValue(endMany, CollectionType.createCollectionFor(endMany, Arrays.asList(e2a, e2b)));

        saveContext();

        Collection<BasicType> e1Many = ((CollectionType) e1a.getValue(endMany)).getBackEnd();
        TestCase.assertEquals(2, e1Many.size());
        TestCase.assertTrue(e1Many.contains(e2a));
        TestCase.assertTrue(e1Many.contains(e2b));
        TestCase.assertEquals(e1a, e2a.getValue(endMany.getOtherEnd()));
        TestCase.assertEquals(e1a, e2b.getValue(endMany.getOtherEnd()));

        saveContext();

        e1b.setValue(endMany, CollectionType.createCollectionFor(endMany, Arrays.asList(e2b)));
        Collection<BasicType> e2Many = ((CollectionType) e1b.getValue(endMany)).getBackEnd();
        TestCase.assertEquals(1, e2Many.size());
        TestCase.assertTrue(e2Many.contains(e2b));
        TestCase.assertEquals(e1b, e2b.getValue(endMany.getOtherEnd()));
        TestCase.assertEquals(e1a, e2a.getValue(endMany.getOtherEnd()));

        TestCase.assertEquals(1, ((CollectionType) e1a.getValue(endMany)).getBackEnd().size());
    }

    public void testOneToManyQualified() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "  class TestDriver\n";
        behavior += "    static operation getEndMany(e1 : Associated1) : Associated2[*];\n";
        behavior += "    begin\n";
        behavior += "      return  e1<-OneToMany->endMany;\n";
        behavior += "    end;\n";
        behavior += "    static operation getEndOne(e2 : Associated2) : Associated1;\n";
        behavior += "    begin\n";
        behavior += "      return  e2<-OneToMany->endOne;\n";
        behavior += "    end;\n";
        behavior += "    static operation build(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      link OneToMany(endOne := e1,endMany := e2);\n";
        behavior += "    end;\n";
        behavior += "  end;\n";
        behavior += "end.\n";

        testOneToMany(behavior);
    }

    public void testOneToManySimple() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "  class TestDriver\n";
        behavior += "    static operation getEndMany(e1 : Associated1) : Associated2[*];\n";
        behavior += "    begin\n";
        behavior += "      return  e1->endMany;\n";
        behavior += "    end;\n";
        behavior += "    static operation getEndOne(e2 : Associated2) : Associated1;\n";
        behavior += "    begin\n";
        behavior += "      return  e2->endOne;\n";
        behavior += "    end;\n";
        behavior += "    static operation build(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      link OneToMany(endOne := e1,endMany := e2);\n";
        behavior += "    end;\n";
        behavior += "  end;\n";
        behavior += "end.\n";

        testOneToMany(behavior);
    }

    public void testOneToOne(String behavior) throws CoreException {

        String[] sources = { buildOneToOne(), behavior };
        parseAndCheck(sources);

        RuntimeObject e1 = newInstance("tests::Associated1");
        RuntimeObject e2 = newInstance("tests::Associated2");
        // create link
        runStaticOperation("tests::TestDriver", "build", e1, e2);

        saveContext();

        // check link
        TestCase.assertEquals(1, e1.getRelated(getProperty("tests::OneToOne::end2")).size());
        TestCase.assertTrue(e1.getRelated(getProperty("tests::OneToOne::end2")).contains(e2));

        TestCase.assertEquals(e2, runStaticOperation("tests::TestDriver", "getEnd2", e1));
        TestCase.assertEquals(e1, runStaticOperation("tests::TestDriver", "getEnd1", e2));

        // cancel link
        runStaticOperation("tests::TestDriver", "cancel", e1, e2);

        saveContext();

        // check link
        TestCase.assertEquals(0, e1.getRelated(getProperty("tests::OneToOne::end2")).size());

        TestCase.assertNull(runStaticOperation("tests::TestDriver", "getEnd2", e1));
        TestCase.assertNull(runStaticOperation("tests::TestDriver", "getEnd1", e2));
    }

    public void testOneToOneMakeLink() throws CoreException {
        String[] sources = { buildOneToOne() };
        parseAndCheck(sources);
        RuntimeObject e1 = newInstance("tests::Associated1");
        RuntimeObject e2 = newInstance("tests::Associated2");

        Property end2 = getProperty("tests::OneToOne::end2");
        e1.setValue(end2, e2);

        saveContext();

        // check link
        TestCase.assertEquals(e2, e1.getValue(end2));
        TestCase.assertEquals(e1, e2.getValue(end2.getOtherEnd()));
    }

    public void testOneToOneQualified() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "  class TestDriver\n";
        behavior += "    static operation getEnd2(e1 : Associated1) : Associated2;\n";
        behavior += "    begin\n";
        behavior += "      return  e1<-OneToOne->end2;\n";
        behavior += "    end;\n";
        behavior += "    static operation getEnd1(e2 : Associated2) : Associated1;\n";
        behavior += "    begin\n";
        behavior += "      return  e2<-OneToOne->end1;\n";
        behavior += "    end;\n";
        behavior += "    static operation build(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      link OneToOne(end1 := e1,end2 := e2);\n";
        behavior += "    end;\n";
        behavior += "    static operation cancel(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      unlink OneToOne(end1 := e1,end2 := e2);\n";
        behavior += "    end;\n";
        behavior += "  end;\n";
        behavior += "end.\n";
        testOneToOne(behavior);
    }

    public void testOneToOneSimple() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "  class TestDriver\n";
        behavior += "    static operation getEnd2(e1 : Associated1) : Associated2;\n";
        behavior += "    begin\n";
        behavior += "      return  e1->end2;\n";
        behavior += "    end;\n";
        behavior += "    static operation getEnd1(e2 : Associated2) : Associated1;\n";
        behavior += "    begin\n";
        behavior += "      return  e2->end1;\n";
        behavior += "    end;\n";
        behavior += "    static operation build(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      link OneToOne(end1 := e1,end2 := e2);\n";
        behavior += "    end;\n";
        behavior += "    static operation cancel(e1 : Associated1,e2 : Associated2);\n";
        behavior += "    begin\n";
        behavior += "      unlink OneToOne(end1 := e1,end2 := e2);\n";
        behavior += "    end;\n";
        behavior += "  end;\n";
        behavior += "end.\n";
        testOneToOne(behavior);
    }

    public void testReflexive() throws CoreException {
        String structure = "";
        structure += "model tests;\n";
        structure += "  class Associated\n";
        structure += "      attribute attr : String[0,1];\n";
        structure += "  end;\n";
        structure += "  association Reflexive\n";
        structure += "    navigable role end1 : Associated; \n";
        structure += "    navigable role end2 : Associated; \n";
        structure += "  end;\n";
        structure += "end.";
        parseAndCheck(structure);
        final Property end1 = getProperty("tests::Reflexive::end1");
        final Property end2 = getProperty("tests::Reflexive::end2");

        final RuntimeObject e1 = newInstance("tests::Associated");
        final RuntimeObject e2 = newInstance("tests::Associated");

        getRuntime().runSession(new Runtime.Session<Object>() {
            @Override
            public Object run() {
                e1.link(end1, e2);
                return null;
            }
        });

        saveContext();

        TestCase.assertEquals(e2.getObjectId(), e1.getRelated(end1).iterator().next().getObjectId());
        TestCase.assertEquals(e1.getObjectId(), e2.getRelated(end2).iterator().next().getObjectId());
    }

    private String buildOneToMany() {
        String structure = "";
        structure += "model tests;\n";
        structure += "  class Associated1\n";
        structure += "      attribute attr1 : String[0,1];\n";
        structure += "  end;\n";
        structure += "  class Associated2\n";
        structure += "      attribute attr2 : String[0,1];\n";
        structure += "  end;\n";
        structure += "  association OneToMany\n";
        structure += "    navigable role endOne : Associated1; \n";
        structure += "    navigable role endMany : Associated2[*]; \n";
        structure += "  end;\n";
        structure += "end.";
        return structure;
    }

    private String buildOneToOne() {
        String structure = "";
        structure += "model tests;\n";
        structure += "  class Associated1\n";
        structure += "      attribute attr1 : String[0,1];\n";
        structure += "  end;\n";
        structure += "  class Associated2\n";
        structure += "      attribute attr2 : String[0,1];\n";
        structure += "  end;\n";
        structure += "  association OneToOne\n";
        structure += "    navigable role end1 : Associated1; \n";
        structure += "    navigable role end2 : Associated2; \n";
        structure += "  end;\n";
        structure += "end.";
        return structure;
    }

    private void saveContext() {
        getRuntime().saveContext(true);
    }

    private void testOneToMany(String behavior) throws CoreException {
        String[] sources = { buildOneToMany(), behavior };
        parseAndCheck(sources);

        RuntimeObject e1 = newInstance("tests::Associated1");
        RuntimeObject e2a = newInstance("tests::Associated2");
        RuntimeObject e2b = newInstance("tests::Associated2");
        RuntimeObject e2c = newInstance("tests::Associated2");
        // create links
        runStaticOperation("tests::TestDriver", "build", e1, e2a);
        runStaticOperation("tests::TestDriver", "build", e1, e2b);

        saveContext();

        // check links
        TestCase.assertEquals(e1, runStaticOperation("tests::TestDriver", "getEndOne", e2a));
        TestCase.assertEquals(e1, runStaticOperation("tests::TestDriver", "getEndOne", e2b));
        TestCase.assertNull(runStaticOperation("tests::TestDriver", "getEndOne", e2c));
        CollectionType allMany = (CollectionType) runStaticOperation("tests::TestDriver", "getEndMany", e1);
        TestCase.assertEquals(2, allMany.getBackEnd().size());
        TestCase.assertTrue(allMany.contains(e2a));
        TestCase.assertTrue(allMany.contains(e2b));
        TestCase.assertFalse(allMany.contains(e2c));
    }
}
