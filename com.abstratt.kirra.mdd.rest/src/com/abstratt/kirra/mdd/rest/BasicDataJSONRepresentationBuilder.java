package com.abstratt.kirra.mdd.rest;

import java.util.HashMap;
import java.util.Map.Entry;

import com.abstratt.kirra.TopLevelElement;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.mdd.rest.TupleJSONRepresentation;

public abstract class BasicDataJSONRepresentationBuilder<T extends TupleJSONRepresentation> {
	public void build(T representation, KirraReferenceBuilder refBuilder, 
			TopLevelElement element, Tuple instance) {
		if (element != null && element.getTypeRef() != null)
			representation.typeName = element.getTypeRef().getFullName();
		representation.values = new HashMap<String, Object>();
		for (Entry<String, Object> entry : instance.getValues().entrySet()) {
			if (entry.getValue() instanceof Tuple) {
				Tuple childTuple = (Tuple) entry.getValue();
				TupleJSONRepresentation childRepr = new TupleJSONRepresentation();
				new TupleJSONRepresentationBuilder().build(childRepr, null, null, childTuple);
				representation.values.put(entry.getKey(), childRepr);
			} else {
				representation.values.put(entry.getKey(), entry.getValue());
			}
		}
	}

}
