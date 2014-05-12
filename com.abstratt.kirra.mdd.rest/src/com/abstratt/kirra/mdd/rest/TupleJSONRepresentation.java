package com.abstratt.kirra.mdd.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.annotate.JsonProperty;
import org.restlet.data.Reference;

import com.abstratt.kirra.TopLevelElement;
import com.abstratt.kirra.Tuple;

public class TupleJSONRepresentation {

	@JsonProperty
	public String typeName;
	
	@JsonProperty
	public Map<String, Object> values;
	
	public void build(KirraReferenceBuilder refBuilder, TopLevelElement element, Tuple instance) {
        if (element != null && element.getTypeRef() != null)
            this.typeName = element.getTypeRef().getFullName();
		this.values = new HashMap<String, Object>();
		for (Entry<String, Object> entry : instance.getValues().entrySet()) {
			if (entry.getValue() instanceof Tuple) {
				Tuple childTuple = (Tuple) entry.getValue();
				TupleJSONRepresentation childRepr = new TupleJSONRepresentation();
				childRepr.build(null, null, childTuple);
				this.values.put(entry.getKey(), childRepr);
			} else {
				this.values.put(entry.getKey(), entry.getValue());
			}
		}
	}

	protected Reference buildTypeReference(KirraReferenceBuilder refBuilder,
			String namespace, String entityName) {
		throw new UnsupportedOperationException();
	}
}
