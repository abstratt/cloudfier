package com.abstratt.mdd.target.jee

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.List
import java.util.Map
import java.util.concurrent.atomic.AtomicLong
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Property

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class DataSnapshotTablePerClassGenerator extends DataSnapshotGenerator {
	
	new(IRepository repository) {
		super(repository)
	}
	
	def override Iterable<CharSequence> generateInstance(Class entity, String namespace, String className, long index, ObjectNode node, Map<String, AtomicLong> ids) {
		val id = ids.get(namespace+ '::' + className).incrementAndGet()
		val allEntityProperties = entity.properties
		val allEntityRelationships = entity.entityRelationships
		#[generateInsert(allEntityProperties, allEntityRelationships, node, className, id)]
	}
}