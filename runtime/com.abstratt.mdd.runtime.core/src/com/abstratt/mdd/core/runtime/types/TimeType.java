package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class TimeType extends PrimitiveType<LocalTime> {
    public static TimeType fromString(@SuppressWarnings("unused") ExecutionContext context, StringType literal) {
        try {
            return new TimeType(new SimpleDateFormat("hh:mm:ss.SSS").parse(literal.primitiveValue()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private TimeType(ZonedDateTime value) {
        this(value.toLocalTime());
    }
    
    private TimeType(Date value) {
        this(value.toInstant().atZone(ZoneId.systemDefault()));
    }

    public static TimeType fromValue(Date original) {
        return new TimeType(original);
    }
    
    public static TimeType fromValue(LocalTime original) {
        return new TimeType(original);
    }

    public static TimeType make(@SuppressWarnings("unused") ExecutionContext context, IntegerType hour, IntegerType minute, IntegerType second, IntegerType millisecond) {
        return new TimeType(LocalTime.of(hour.primitiveValue().intValue(), minute.primitiveValue().intValue(), second.primitiveValue().intValue(), millisecond.primitiveValue().intValue() * 1000 * 1000));
    }

    public static TimeType now(@SuppressWarnings("unused") ExecutionContext context) {
        return new TimeType(LocalTime.now(ZoneOffset.UTC));
    }
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private TimeType(LocalTime value) {
        super(value);
    }
    
    public DurationType difference(@SuppressWarnings("unused") ExecutionContext context, TimeType end) {
    	long result; 
    	if (this.value.isAfter(end.value)) {
    		result = ChronoUnit.MILLIS.between(this.value.atDate(LocalDate.now()), end.value.atDate(LocalDate.now().plusDays(1)));
    	} else {
    		result = ChronoUnit.MILLIS.between(this.value, end.value);
    	}
		return DurationType.fromValue(result);
    }

    @Override
    public String getClassifierName() {
        return "mdd_types::Time";
    }

    @Override
    public boolean isEmpty() {
        return this.value == null;
    }
    
    public IntegerType hour(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.CLOCK_HOUR_OF_DAY));
    }

    public IntegerType minute(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.MINUTE_OF_HOUR));
    }

    
    public IntegerType second(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.SECOND_OF_MINUTE));
    }
    
    public IntegerType millisecond(@SuppressWarnings("unused") ExecutionContext context) {
        return IntegerType.fromValue(this.primitiveValue().get(ChronoField.MILLI_OF_SECOND));
    }

    @Override
    public StringType toString(ExecutionContext context) {
    	String asString = DateTimeFormatter.ofPattern("hh:mmZ").format(this.primitiveValue());
        return new StringType(asString);
    }

    public TimeType transpose(ExecutionContext context, DurationType delta) {
    	LocalTime transposed = this.value.plus(Duration.ofMillis(delta.primitiveValue()));
        return new TimeType(transposed);
    }
}