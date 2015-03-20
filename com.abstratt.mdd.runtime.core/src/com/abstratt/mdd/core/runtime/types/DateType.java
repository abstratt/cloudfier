package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class DateType extends PrimitiveType<Date> {
    public static DateType fromString(@SuppressWarnings("unused") ExecutionContext context, StringType literal) {
        try {
            return new DateType(new SimpleDateFormat("yyyy/MM/dd").parse(literal.primitiveValue()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static DateType fromValue(Date original) {
        return new DateType(original);
    }

    public static DateType make(@SuppressWarnings("unused") ExecutionContext context, IntegerType day, IntegerType month, IntegerType year) {
        return new DateType(new Date(year.primitiveValue().intValue() - 1900, month.primitiveValue().intValue() - 1, day.primitiveValue()
                .intValue()));
    }

    public static DateType today(@SuppressWarnings("unused") ExecutionContext context) {
        Date value = new Date();
        value.setHours(0);
        value.setMinutes(0);
        value.setSeconds(0);
        value.setTime((value.getTime() / 1000) * 1000);
        return new DateType(value);
    }
    
    public static DateType now(@SuppressWarnings("unused") ExecutionContext context) {
        return new DateType(new Date());
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Date value;

    private DateType(Date value) {
        this.value = value;
    }
    
    public DurationType difference(@SuppressWarnings("unused") ExecutionContext context, DateType end) {
        return DurationType.fromValue(end.value.getTime() - this.value.getTime());
    }

    public IntegerType differenceInDays(@SuppressWarnings("unused") ExecutionContext context, DateType end) {
        return dateDifference(this, end, 1);
    }

    public IntegerType differenceInMonths(ExecutionContext context, DateType end) {
        return dateDifference(this, end, 30);
    }

    public IntegerType differenceInYears(ExecutionContext context, DateType end) {
        return dateDifference(this, end, 365);
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Date";
    }

    @Override
    public boolean isEmpty() {
        return this.value == null;
    }
    
    public IntegerType year(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(1900 + this.primitiveValue().getYear());
    }

    public IntegerType month(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(1 + this.primitiveValue().getMonth());
    }
    
    public IntegerType day(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().getDate());
    }

    @Override
    public Date primitiveValue() {
        return value;
    }

    @Override
    public StringType toString(ExecutionContext context) {
        return new StringType(new SimpleDateFormat("yyyy/MM/dd").format(this.primitiveValue()));
    }

    public DateType transpose(ExecutionContext context, DurationType delta) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.primitiveValue());
        cal.add(Calendar.DATE, (int) (delta.primitiveValue() / 1000 / 60 / 60 / 24));
        return new DateType(cal.getTime());
    }

    private IntegerType dateDifference(DateType start, DateType end, int divider) {

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start.primitiveValue());

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end.primitiveValue());

        startCal.clear(Calendar.HOUR);
        startCal.clear(Calendar.MINUTE);
        startCal.clear(Calendar.SECOND);
        startCal.clear(Calendar.MILLISECOND);

        endCal.clear(Calendar.HOUR);
        endCal.clear(Calendar.MINUTE);
        endCal.clear(Calendar.SECOND);
        endCal.clear(Calendar.MILLISECOND);

        startCal.setTimeZone(TimeZone.getTimeZone("GMT"));
        endCal.setTimeZone(TimeZone.getTimeZone("GMT"));

        long timeElapsed = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        return IntegerType.fromValue(timeElapsed / (divider * 1000L * 60 * 60 * 24));
    }
}