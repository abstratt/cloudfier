package com.abstratt.mdd.core.tests.runtime;

import java.util.Date;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.DateType;
import com.abstratt.mdd.core.runtime.types.IntegerType;

public class RuntimeDateTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeDateTests.class);
    }

    public RuntimeDateTests(String name) {
        super(name);
    }

    public void testDateComparison() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation isLower(d1 : Date,d2 : Date) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 < d2;\n";
        model += "end;\n";
        model += "static operation isLowerOrEqual(d1 : Date,d2 : Date) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 <= d2;\n";
        model += "end;\n";
        model += "static operation isGreater(d1 : Date,d2 : Date) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 > d2;\n";
        model += "end;\n";
        model += "static operation isGreaterOrEqual(d1 : Date,d2 : Date) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 >=d2;\n";
        model += "end;\n";
        model += "static operation isEqual(d1 : Date,d2 : Date) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 = d2;\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        DateType d1 = DateType.fromValue(new Date(2011 - 1900, 9, 11));
        DateType d2 = DateType.fromValue(new Date(2012 - 1900, 1, 10));

        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isLower", d1, d2)).primitiveValue());
        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isLowerOrEqual", d1, d2)).primitiveValue());
        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isLowerOrEqual", d1, d1)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isLower", d2, d1)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isLower", d1, d1)).primitiveValue());

        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isGreater", d2, d1)).primitiveValue());
        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isGreaterOrEqual", d2, d2)).primitiveValue());
        TestCase.assertTrue(((BooleanType) runStaticOperation("tests::DateUtil", "isGreaterOrEqual", d2, d1)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isGreater", d2, d2)).primitiveValue());

        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isGreater", d1, null)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isLower", d1, null)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isGreater", null, d1)).primitiveValue());
        TestCase.assertFalse(((BooleanType) runStaticOperation("tests::DateUtil", "isLower", null, d2)).primitiveValue());
    }

    public void testDateMath() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation dateDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.differenceInDays(d2);\n";
        model += "end;\n";
        model += "static operation dateTranspose(d : Date,diff : Integer) : Date;\n";
        model += "begin\n";
        model += "  return d.transpose(diff);\n";
        model += "end;\n";
        model += "static operation yearDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.differenceInYears(d2);\n";
        model += "end;\n";
        model += "static operation monthDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.differenceInMonths(d2);\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        DateType d1 = DateType.fromValue(new Date(2011 - 1900, 9, 11));
        DateType d2 = DateType.fromValue(new Date(2011 - 1900, 9, 17));

        IntegerType diff = (IntegerType) runStaticOperation("tests::DateUtil", "dateDiff", d1, d2);
        TestCase.assertEquals(6, diff.primitiveValue().intValue());

        DateType transposed = (DateType) runStaticOperation("tests::DateUtil", "dateTranspose", d1, new IntegerType(6));
        TestCase.assertEquals(d2.primitiveValue().getYear(), transposed.primitiveValue().getYear());
        TestCase.assertEquals(d2.primitiveValue().getMonth(), transposed.primitiveValue().getMonth());
        TestCase.assertEquals(d2.primitiveValue().getDate(), transposed.primitiveValue().getDate());

        TestCase.assertEquals(
                10,
                ((IntegerType) runStaticOperation("tests::DateUtil", "yearDiff", DateType.fromValue(new Date(30, 1, 1)),
                        DateType.fromValue(new Date(40, 1, 1)))).primitiveValue().intValue());

        TestCase.assertEquals(
                13,
                ((IntegerType) runStaticOperation("tests::DateUtil", "monthDiff", DateType.fromValue(new Date(40, 9, 10)),
                        DateType.fromValue(new Date(41, 10, 18)))).primitiveValue().intValue());

    }

    public void testLiteral() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation createDate() : Date;\n";
        model += "begin\n";
        model += "  return Date#fromString(\"2011/08/30\");\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        Date created = ((DateType) runStaticOperation("tests::DateUtil", "createDate")).primitiveValue();
        TestCase.assertEquals(2011 - 1900, created.getYear());
        TestCase.assertEquals(7, created.getMonth());
        TestCase.assertEquals(30, created.getDate());
    }

    public void testMakeDate() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation createDate() : Date;\n";
        model += "begin\n";
        model += "  return Date#make(30, 08, 2011);\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        Date created = ((DateType) runStaticOperation("tests::DateUtil", "createDate")).primitiveValue();
        TestCase.assertEquals(2011 - 1900, created.getYear());
        TestCase.assertEquals(7, created.getMonth());
        TestCase.assertEquals(30, created.getDate());
    }

    public void testToday() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation createToday() : Date;\n";
        model += "begin\n";
        model += "  return Date#today();\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        Date today = ((DateType) runStaticOperation("tests::DateUtil", "createToday")).primitiveValue();
        TestCase.assertEquals(today.getYear(), new Date().getYear());
        TestCase.assertEquals(today.getMonth(), new Date().getMonth());
        TestCase.assertEquals(today.getDate(), new Date().getDate());
    }

}
