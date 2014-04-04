package com.abstratt.mdd.core.runtime.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DateConverter implements ValueConverter {
	public BasicType convertToBasicType(Object original) {
		if (original == null)
			return null;
		if (original instanceof String) {
			if (((String) original).trim().isEmpty())
			    return null;
			try {
				return DateType.fromValue(new SimpleDateFormat("yyyy/MM/dd").parse((String) original));
			} catch (ParseException e) {
				try {
					return DateType.fromValue(new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z (z)").parse((String) original));					
				} catch (ParseException e2) {
					throw new RuntimeException(e2);
				}
			}
		}
		if (original instanceof Date)
			return DateType.fromValue((Date) original);
		throw new IllegalArgumentException("Unsupported type: " + original);
	}
}
