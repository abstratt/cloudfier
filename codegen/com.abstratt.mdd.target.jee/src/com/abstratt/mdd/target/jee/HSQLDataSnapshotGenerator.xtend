package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import org.eclipse.uml2.uml.Property
import java.util.Map
import java.util.concurrent.atomic.AtomicLong
import org.eclipse.uml2.uml.Class

class HSQLDataSnapshotGenerator extends DataSnapshotJoinedTableGenerator {

	new(IRepository repository) {
		super(repository)
	}

	override CharSequence toSqlValue(Property property, JsonNode propertyValue) {
		val superValue = super.toSqlValue(property, propertyValue)
		if (propertyValue != null && propertyValue.asToken() == JsonToken.VALUE_STRING &&
			"Date" == property.type.name) {
			return '''DATE «superValue»'''
		}
		return superValue
	}
}