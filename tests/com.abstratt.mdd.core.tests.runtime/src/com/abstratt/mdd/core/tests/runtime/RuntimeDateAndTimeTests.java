package com.abstratt.mdd.core.tests.runtime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.DateType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.RealType;
import com.abstratt.mdd.core.runtime.types.TimeType;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RuntimeDateAndTimeTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeDateAndTimeTests.class);
    }

    public RuntimeDateAndTimeTests(String name) {
        super(name);
    }
    
    private void buildTimestampTypeComparisonModel(String timeType) throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation isLower(d1 : {{TimestampTime}},d2 : {{TimestampTime}}) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 < d2;\n";
        model += "end;\n";
        model += "static operation isLowerOrEqual(d1 : {{TimestampTime}},d2 : {{TimestampTime}}) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 <= d2;\n";
        model += "end;\n";
        model += "static operation isGreater(d1 : {{TimestampTime}},d2 : {{TimestampTime}}) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 > d2;\n";
        model += "end;\n";
        model += "static operation isGreaterOrEqual(d1 : {{TimestampTime}},d2 : {{TimestampTime}}) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 >=d2;\n";
        model += "end;\n";
        model += "static operation isEqual(d1 : {{TimestampTime}},d2 : {{TimestampTime}}) : Boolean;\n";
        model += "begin\n";
        model += "  return d1 = d2;\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";
        model = model.replaceAll("\\{\\{TimestampTime\\}\\}", timeType);

        parseAndCheck(model);
    }
    

    private <DT extends PrimitiveType<?>> void testTimestampTypeComparison(String timestampType, DT d1, DT d2) throws CoreException {
    	buildTimestampTypeComparisonModel(timestampType);

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
    
    public void testDateComparison() throws CoreException {
        DateType d1 = DateType.fromValue(makeDate(2011, 9, 11));
        DateType d2 = DateType.fromValue(makeDate(2012, 1, 10));
        testTimestampTypeComparison("Date", d1, d2);
    }
    
    public void testTimeComparison() throws CoreException {
    	TimeType d1 = TimeType.fromValue(makeTime(13, 12, 11, 10));
    	TimeType d2 = TimeType.fromValue(makeTime(14, 11, 10, 9));
        testTimestampTypeComparison("Time", d1, d2);
    }

    private static Date makeDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }
    
    private static LocalTime makeTime(int hour, int minute, int second, int millisecond) {
        return LocalTime.of(hour, minute, second, millisecond * 1000 * 1000);
    }
        
    private void buildDateMathModel(String dateType) throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation dateDiff(d1 : {{DateType}},d2 : {{DateType}}) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toDays();\n";
        model += "end;\n";
        model += "static operation {{DateType}}Transpose(d : {{DateType}},diff : Integer) : {{DateType}};\n";
        model += "begin\n";
        model += "  return d.transpose(Duration#days(diff));\n";
        model += "end;\n";
        model += "static operation yearDiff(d1 : {{DateType}},d2 : {{DateType}}) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toYears();\n";
        model += "end;\n";
        model += "static operation monthDiff(d1 : {{DateType}},d2 : {{DateType}}) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toMonths();\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);    	
    }
    
    public void testDateMath() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation dateDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toDays();\n";
        model += "end;\n";
        model += "static operation dateTranspose(d : Date,diff : Integer) : Date;\n";
        model += "begin\n";
        model += "  return d.transpose(Duration#days(diff));\n";
        model += "end;\n";
        model += "static operation yearDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toYears();\n";
        model += "end;\n";
        model += "static operation monthDiff(d1 : Date,d2 : Date) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toMonths();\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        

        DateType d1 = DateType.fromValue(makeDate(2011, 9, 11));
        DateType d2 = DateType.fromValue(makeDate(2011, 9, 17));

        IntegerType diff = (IntegerType) runStaticOperation("tests::DateUtil", "dateDiff", d1, d2);
        TestCase.assertEquals(6, diff.primitiveValue().intValue());

        DateType transposed = (DateType) runStaticOperation("tests::DateUtil", "dateTranspose", d1, RealType.fromValue(6));
        TestCase.assertEquals(d2.primitiveValue().getYear(), transposed.primitiveValue().getYear());
        TestCase.assertEquals(d2.primitiveValue().getMonthValue(), transposed.primitiveValue().getMonthValue());
        TestCase.assertEquals(d2.primitiveValue().getDayOfMonth(), transposed.primitiveValue().getDayOfMonth());

        TestCase.assertEquals(
                10,
                ((IntegerType) runStaticOperation("tests::DateUtil", "yearDiff", DateType.fromValue(makeDate(1930, 1, 1)),
                        DateType.fromValue(makeDate(1940, 1, 1)))).primitiveValue().intValue());

        TestCase.assertEquals(
                13,
                ((IntegerType) runStaticOperation("tests::DateUtil", "monthDiff", DateType.fromValue(new Date(40, 9, 10)),
                        DateType.fromValue(makeDate(1941, 10, 18)))).primitiveValue().intValue());

    }
    
    public void testTimeMath() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class TimeUtil\n";
        model += "static operation timeDiff(d1 : Time,d2 : Time) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toMilliseconds();\n";
        model += "end;\n";
        model += "static operation timeTranspose(d : Time, diff : Integer) : Time;\n";
        model += "begin\n";
        model += "  return d.transpose(Duration#milliseconds(diff));\n";
        model += "end;\n";
        model += "static operation hourDiff(d1 : Time,d2 : Time) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toHours();\n";
        model += "end;\n";
        model += "static operation minuteDiff(d1 : Time,d2 : Time) : Integer;\n";
        model += "begin\n";
        model += "  return d1.difference(d2).toMinutes();\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        
    	TimeType d1 = TimeType.fromValue(makeTime(13, 12, 11, 10));
    	TimeType d2 = TimeType.fromValue(makeTime(15, 11, 10, 9));

        IntegerType diff = (IntegerType) runStaticOperation("tests::TimeUtil", "timeDiff", d1, d2);
        long expectedDiffInMillis = d2.primitiveValue().minusNanos(d1.primitiveValue().toNanoOfDay()).toNanoOfDay() / 1000000;
		TestCase.assertEquals(expectedDiffInMillis, diff.primitiveValue().intValue());

        TimeType transposed = (TimeType) runStaticOperation("tests::TimeUtil", "timeTranspose", d1, IntegerType.fromValue(expectedDiffInMillis));
        TestCase.assertEquals(d2.primitiveValue().getHour(), transposed.primitiveValue().getHour());
        TestCase.assertEquals(d2.primitiveValue().getMinute(), transposed.primitiveValue().getMinute());
        TestCase.assertEquals(d2.primitiveValue().getSecond(), transposed.primitiveValue().getSecond());
        TestCase.assertEquals(d2.primitiveValue().getNano(), transposed.primitiveValue().getNano());

        TestCase.assertEquals(
                1,
                ((IntegerType) runStaticOperation("tests::TimeUtil", "hourDiff", d1, d2)).primitiveValue().intValue());

        TestCase.assertEquals(
                118,
                ((IntegerType) runStaticOperation("tests::TimeUtil", "minuteDiff", d1,
                        d2)).primitiveValue().intValue());

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
        LocalDate created = ((DateType) runStaticOperation("tests::DateUtil", "createDate")).primitiveValue();
        TestCase.assertEquals(2011, created.getYear());
        TestCase.assertEquals(8, created.getMonthValue());
        TestCase.assertEquals(30, created.getDayOfMonth());
    }

    public void testMakeDate() throws CoreException {
        String model = "";
        model += "model tests;\n";
        model += "import mdd_types;\n";
        model += "class DateUtil\n";
        model += "static operation createDate() : Date;\n";
        model += "begin\n";
        model += "  return Date#make(2011, 08, 30);\n";
        model += "end;\n";
        model += "end;\n";
        model += "end.";

        parseAndCheck(model);
        LocalDate created = ((DateType) runStaticOperation("tests::DateUtil", "createDate")).primitiveValue();
        TestCase.assertEquals(2011, created.getYear());
        TestCase.assertEquals(8	, created.getMonthValue());
        TestCase.assertEquals(30, created.getDayOfMonth());
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
        LocalDate today = ((DateType) runStaticOperation("tests::DateUtil", "createToday")).primitiveValue();
        TestCase.assertEquals(today.getYear(), LocalDateTime.now().getYear());
        TestCase.assertEquals(today.getMonthValue(), LocalDateTime.now().getMonthValue());
        TestCase.assertEquals(today.getDayOfMonth(), LocalDateTime.now().getDayOfMonth());
    }

}
