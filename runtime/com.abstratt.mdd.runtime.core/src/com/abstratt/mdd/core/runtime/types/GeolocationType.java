package com.abstratt.mdd.core.runtime.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class GeolocationType extends PrimitiveType<String> {
	private static final long serialVersionUID = 1L;
	
	private final static Pattern PATTERN = Pattern.compile("^([\\-+]\\d+(\\.\\d+)?),(\\-?\\d+(\\.\\d+)?)$");
	
	protected GeolocationType(String value) {
		super(value);
		Matcher matcher = PATTERN.matcher((String) value);
		if (matcher.matches()) {
			latitude = matcher.group(1);
			longitude = matcher.group(3);
		}
	}

	private String latitude;
	
	private String longitude;
	
	@Override
	public String getClassifierName() {
		return "mdd_types::GeoLocation";
	}

	public static BasicType fromString(String contents) {
		if (contents instanceof String) {
			return new GeolocationType((String) contents);
		}
		return null;
	}
	
    public StringType latitude(ExecutionContext context) {
        return new StringType(latitude);
    }
    
    public StringType longitude(ExecutionContext context) {
        return new StringType(longitude);
    }

}
