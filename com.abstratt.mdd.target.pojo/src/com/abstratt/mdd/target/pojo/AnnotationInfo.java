package com.abstratt.mdd.target.pojo;

import java.util.Map;

public class AnnotationInfo {
	private String name;
	private Map<String, Object> values;

	public AnnotationInfo(String name, Map<String, Object> values) {
		this.name = name;
		this.values = values;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getValueMap() {
		return values;
	}
}
