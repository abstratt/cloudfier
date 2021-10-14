package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class DateTimeType extends PrimitiveType<LocalDateTime> {
    public static DateTimeType fromString(@SuppressWarnings("unused") ExecutionContext context, StringType literal) {
        try {
            return new DateTimeType(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(literal.primitiveValue()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static DateTimeType fromValue(Date original) {
        return new DateTimeType(original);
    }
    
    public static DateTimeType fromValue(LocalDateTime original) {
        return new DateTimeType(original);
    }

    public static DateTimeType make(@SuppressWarnings("unused") ExecutionContext context, IntegerType year, IntegerType month, IntegerType day) {
        return new DateTimeType(new Date(year.primitiveValue().intValue() - 1900, month.primitiveValue().intValue() - 1, day.primitiveValue()
                .intValue()));
    }
    
    public static DateTimeType today(@SuppressWarnings("unused") ExecutionContext context) {
        Date value = new Date();
        value.setHours(0);
        value.setMinutes(0);
        value.setSeconds(0);
        value.setTime((value.getTime() / 1000) * 1000);
        return new DateTimeType(value);
    }
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private DateTimeType(Date value) {
        super(value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
    
    private DateTimeType(LocalDateTime value) {
        super(value);
    }
    
    public DurationType difference(@SuppressWarnings("unused") ExecutionContext context, DateTimeType end) {
        return DurationType.fromValue(ChronoUnit.DAYS.between(this.value, end.value) * (1000 * 60 * 60 * 24));
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::DateTime";
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
    
    public DateType date(@SuppressWarnings("unused") ExecutionContext context) {
        return DateType.fromValue(this.primitiveValue().toLocalDate());
    }
    
    public TimeType time(@SuppressWarnings("unused") ExecutionContext context) {
        return TimeType.fromValue(this.primitiveValue().toLocalTime());
    }    
    
    public static DateTimeType now(@SuppressWarnings("unused") ExecutionContext context) {
        return new DateTimeType(LocalDateTime.now());
    }

    @Override
    public StringType toString(ExecutionContext context) {
    	String asString = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").format(this.primitiveValue());
        return new StringType(asString);
    }

    public DateTimeType transpose(ExecutionContext context, DurationType delta) {
    	LocalDateTime transposed = this.value.plus(Duration.ofMillis(delta.primitiveValue()));
        return new DateTimeType(transposed);
    }
}