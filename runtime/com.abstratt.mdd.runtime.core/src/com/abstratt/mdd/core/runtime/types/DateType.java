package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class DateType extends PrimitiveType<LocalDate> {
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
    
    public static DateType fromValue(LocalDate original) {
        return new DateType(original);
    }
    
    public DateTimeType at(@SuppressWarnings("unused") ExecutionContext context, TimeType time) {
        return DateTimeType.fromValue(this.primitiveValue().atTime(time.primitiveValue()));
    }
    

    public static DateType make(@SuppressWarnings("unused") ExecutionContext context, IntegerType year, IntegerType month, IntegerType day) {
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
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private DateType(Date value) {
        super(value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }
    
    private DateType(LocalDate value) {
        super(value);
    }
    
    public DurationType difference(@SuppressWarnings("unused") ExecutionContext context, DateType end) {
        return DurationType.fromValue(ChronoUnit.DAYS.between(this.value, end.value) * (1000 * 60 * 60 * 24));
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
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.YEAR_OF_ERA));
    }

    public IntegerType month(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.MONTH_OF_YEAR));
    }
    
    public IntegerType day(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.DAY_OF_MONTH));
    }

    @Override
    public StringType toString(ExecutionContext context) {
    	String asString = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(this.primitiveValue());
        return new StringType(asString);
    }

    public DateType transpose(ExecutionContext context, DurationType delta) {
    	LocalDate transposed = this.value.plusDays(Duration.ofMillis(delta.primitiveValue()).toDays());
        return new DateType(transposed);
    }
}