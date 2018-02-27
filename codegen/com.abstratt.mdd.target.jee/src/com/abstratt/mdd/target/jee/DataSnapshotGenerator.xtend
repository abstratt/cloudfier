package com.abstratt.mdd.target.jee

import com.abstratt.kirra.InstanceRef
import com.abstratt.kirra.NamedElement
import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import java.io.StringWriter
import java.util.List
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.apache.commons.io.IOUtils
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.EnumerationLiteral
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.ValueSpecification

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.isVertexLiteral
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.resolveVertexLiteral
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*

abstract class DataSnapshotGenerator extends AbstractGenerator {
    protected Map<String, Long> idMapping = newLinkedHashMap()

	new(IRepository repository) {
		super(repository)
		if (appPackages != null) {
			// contrary to elsewhere, we do care about non-top-level entities here
			this.entities = appPackages.entities
		}
	}

	def CharSequence generate() {
		val dataUrl = MDDUtil.fromEMFToJava(repository.baseURI.appendSegment("data.json")).toURL
		val contents = new ByteArrayOutputStream()
		var InputStream sourceStream
		try {
			sourceStream = dataUrl.openStream
			IOUtils.copy(sourceStream, contents)
        } catch (FileNotFoundException e) {
        	return '-- NO DATA'		
		} catch (IOException e) {
			val stringWriter = new StringWriter()
			e.printStackTrace(new PrintWriter(stringWriter))
			return '-- ERROR\n' + stringWriter.buffer.toString.split('\n').map['''-- «it»'''].join('\n')
		} finally {
			IOUtils.closeQuietly(sourceStream)
		}
		val jsonTree = parse(new InputStreamReader(new ByteArrayInputStream(contents.toByteArray))) as ObjectNode
		
		val generated = generateContents(jsonTree)
        generated
	}

	def CharSequence generateContents(ObjectNode root) {
		val entities = entities.toMap[qualifiedName]
		val entityNames = entities.keySet()
		val ids = entityNames.toInvertedMap[new AtomicLong(0)]
		root.fields.toIterable.forEach [ entry |
			val namespaceName = entry.key
			val namespaceNode = entry.value as ObjectNode
			allocateIds(entities, ids, namespaceName, namespaceNode)
		]
		val namespaceStatements = root.fields.toIterable.map [ entry |
			val namespaceName = entry.key
			val namespaceNode = entry.value as ObjectNode
			generateNamespace(entities, ids, namespaceName, namespaceNode)
		]
		val contents = namespaceStatements.flatten.join('\n')
		println('''IDs: «ids»''')
		return contents
	}
	
	protected def Iterable<Class> findHierarchy(Class entity) {
		val superClasses = entity.superClasses.filter[it.entity]
		if (superClasses.empty)
			return #[entity]
		if (superClasses.size > 1)
			throw new IllegalStateException('''Multiple base classes found: «superClasses.map[qualifiedName].join(', ')» for «entity.qualifiedName»''')
		return #[entity] + findHierarchy(superClasses.get(0))
	}
	
	def void allocateIds(Map<String, Class> entities, Map<String, AtomicLong> ids,
		String namespace, ObjectNode namespaceContents) {

	    namespaceContents.fields.forEach[ entry |
			val className = entry.key
			val nameComponents = TypeRef.splitName(className)
			val actualNamespace = nameComponents.get(0) ?: namespace
			val actualName = nameComponents.get(1)
			val instanceNodes = entry.value as ArrayNode
			val entity = entities.get(actualNamespace + '::' + actualName)
		    val idAnchorEntity = getIdAnchorEntity(entity)
	    	instanceNodes.elements.forEach[ it, i |
	    		val index = i + 1
	    		val newId = ids.get(idAnchorEntity.qualifiedName).incrementAndGet()
	    		idMapping.put(TypeRef.sanitize(entity.qualifiedName) + "@" + index, newId)
	    	]
	    ]
	}
	
	def getIdAnchorEntity(Class entity) {
		return entity
	}
	
	def Iterable<CharSequence> generateNamespace(Map<String, Class> entities, Map<String, AtomicLong> ids,
		String namespace, ObjectNode namespaceContents) {
		val inserts = generateDataInserts(namespaceContents, entities, namespace)
		val alterSequences = generateAlterSequences(ids, namespace, entities)
		return inserts + alterSequences
	}

	def Iterable<String> generateAlterSequences(Map<String, AtomicLong> ids, String namespace, Map<String, Class> entities) {
		val generated = ids.entrySet.map [ pair |
			val entity = entities.get(pair.key)
			val nextValue = pair.value.get + 1
			'''«generateAlterSequenceStatement(applicationName, entity, nextValue)»'''
		]
		return generated
	}
	
	def generateAlterSequenceStatement(String schemaName, Class entity, long nextValue) {
		'''
			ALTER SEQUENCE «schemaName».«entity.name.toLowerCase»_id_seq RESTART WITH «nextValue»;
		'''
	}
	
		
	def Iterable<CharSequence> generateDataInserts(ObjectNode namespaceContents, Map<String, Class> entities, String namespace) {
		val inserts = namespaceContents.fields.toIterable.map [ entry |
			val className = entry.key
			val nameComponents = TypeRef.splitName(className)
			val actualNamespace = nameComponents.get(0) ?: namespace
			val actualName = nameComponents.get(1)
			
			val instanceNodes = (entry.value as ArrayNode)
			val entity = entities.get(actualNamespace + '::' + actualName)
			val index = new AtomicInteger(0)
			instanceNodes.elements.toList.map [
				generateInstance(entity, actualNamespace, actualName, index.incrementAndGet, it as ObjectNode)
			].flatten().join() as CharSequence
		]
		return inserts
	}

	def Iterable<CharSequence> generateInstance(Class entity, String namespace, String className, long index, ObjectNode node) {
		val id = idMapping.get(InstanceRef.toString(namespace, className, "" + index))
		val allEntityProperties = entity.properties
		val allEntityRelationships = entity.entityRelationships
		#[generateInsert(allEntityProperties, allEntityRelationships, node, className, id)]
	}    

	def CharSequence generateInsert(List<Property> allEntityProperties, List<Property> allEntityRelationships, ObjectNode node, String className, long id) {
		val sqlPropertyValues = allEntityProperties.filter[!autoGenerated].toMap[name].mapValues [ property |
			val jsonValue = node.get(property.name)
			toSqlValue(property, jsonValue)
		]
		
		val sqlForeignKeys = allEntityRelationships.filter[!derived && primary && !multivalued].toMap[name].filter [ relationshipName, value |
			node.get(relationshipName) != null
		].mapValues [ relationship |
			getRelatedInstanceId(relationship, node.get(relationship.name))
		]
		
		val sqlValues = newLinkedHashMap()
		sqlValues.put('id', id)
		sqlPropertyValues.forEach[key, value|sqlValues.put(key, value)]
		sqlForeignKeys.
			forEach[key, value|sqlValues.put(key + '_id', value)]
			
		val columns = sqlValues.keySet
		val values = sqlValues.keySet.map[sqlValues.get(it)] 
		
		'''
			INSERT INTO «applicationName».«className» («columns.join(', ')») VALUES («values.join(', ')»);
		'''
	}

	def CharSequence getRelatedInstanceId(Property relationship, JsonNode propertyValue) {
		val referenceString = (propertyValue as TextNode).textValue
		val addressSeparatorIndex = referenceString.indexOf('@')
		if (addressSeparatorIndex <= 0 || addressSeparatorIndex == referenceString.length() - 1)
			return null
		return referenceString.substring(addressSeparatorIndex + 1)
	}

	def CharSequence toSqlValue(Property property, JsonNode propertyValue) {
		if (propertyValue == null)
			return property.generateDefaultSqlValue
		return switch (propertyValue.asToken()) {
			case VALUE_NULL:
				'null'
			case VALUE_TRUE:
				'true'
			case VALUE_FALSE:
				'false'
			case VALUE_STRING: {
				val baseValue = (propertyValue as TextNode).textValue.replace("'", "''").replace("'", "''")
				var value = ''' '«baseValue»' '''.toString.trim()
				if ("Date" == property.type.name) {
					value = value.replace("/", "-")
				}
				return value
			}
			case VALUE_NUMBER_INT: '''«propertyValue.asText()»'''
			case VALUE_NUMBER_FLOAT: '''«propertyValue.asText()»'''
		}
	}

	def CharSequence generateDefaultSqlValue(Property attribute) {
		if (attribute.defaultValue != null) {
			if (attribute.defaultValue.behaviorReference)
				(attribute.defaultValue.resolveBehaviorReference as Activity).generateActivityAsExpression
			else
				attribute.defaultValue.generateValue
		} else if (attribute.required || attribute.type.enumeration)
			// enumeration covers state machines as well
			attribute.type.generateDefaultValue
	}

	def CharSequence generateActivityAsExpression(Activity activity) {
		// TODO
		'null'
	}

	def CharSequence unsupported(CharSequence message) {
		'''Unsupported: «message»'''
	}

	def CharSequence unsupportedElement(Element e, String message) {
		unsupported('''«e.eClass.name»> «if (message != null) '''(«message»)''' else ''»''')
	}

	def CharSequence unsupportedElement(Element e) {
		unsupportedElement(e, if(e instanceof NamedElement) e.name else null)
	}

	def CharSequence generateValue(ValueSpecification value) {
		switch (value) {
			// the TextUML compiler maps all primitive values to LiteralString
			LiteralString:
				switch (value.type.name) {
					case 'String': '''«'\''»«value.stringValue»«'\''»'''
					case 'Integer': '''«value.stringValue»'''
					case 'Double': '''«value.stringValue»'''
					case 'Boolean': '''«value.stringValue»'''
					default:
						unsupported(value.stringValue)
				}
			LiteralBoolean: '''«value.booleanValue»'''
			LiteralNull:
				switch (value) {
					case value.isVertexLiteral: '''«'\''»«value.resolveVertexLiteral.name»«'\''»'''
					default:
						'null'
				}
			OpaqueExpression case value.behaviorReference:
				(value.resolveBehaviorReference as Activity).generateActivityAsExpression()
			InstanceValue case value.
				instance instanceof EnumerationLiteral: '''«value.instance.namespace.name».«value.instance.name»'''
			default:
				unsupportedElement(value)
		}
	}

	def CharSequence generateDefaultValue(Type type) {
		switch (type) {
			StateMachine: '''«'\''»«type.initialVertex.name»«'\''»'''
			Enumeration: '''«type.name».«type.ownedLiterals.head.name»'''
			Class:
				switch (type.name) {
					case 'Boolean': 'false'
					case 'Integer': '0'
					case 'Double': '0'
					case 'Date': 'now()'
					case 'String': '\'\''
					case 'Memo': '\'\''
				}
			default:
				null
		}
	}

	private def static JsonNode parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
		val parser = jsonFactory.createJsonParser(contents);
		parser.setCodec(new ObjectMapper());
		return parser.readValueAsTree();
	}

	private static JsonFactory jsonFactory = [|
		val factory = new JsonFactory();
		factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		factory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		factory.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, false);
		return factory;
	].apply
}