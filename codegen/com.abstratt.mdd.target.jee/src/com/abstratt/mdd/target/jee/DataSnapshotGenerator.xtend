package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.abstratt.mdd.target.jse.AbstractGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.Map
import java.util.concurrent.atomic.AtomicLong
import org.apache.commons.io.IOUtils
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.JsonParseException
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.ArrayNode
import org.codehaus.jackson.node.ObjectNode
import org.codehaus.jackson.node.TextNode
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Property

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import java.util.List
import java.util.Iterator
import java.io.StringWriter
import java.io.PrintWriter

class DataSnapshotGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generate() {
        val dataUrl = MDDUtil.fromEMFToJava(repository.baseURI.appendSegment("data.json")).toURL
        val contents = new ByteArrayOutputStream()
        var InputStream sourceStream
        try {
            sourceStream = dataUrl.openStream
            IOUtils.copy(sourceStream, contents)
        } catch (IOException e) {
            val stringWriter = new StringWriter()
            e.printStackTrace(new PrintWriter(stringWriter))
            return '-- ERROR\n'+ stringWriter.buffer.toString.split('\n').map['''-- «it»'''].join('\n')  
        } finally {
            IOUtils.closeQuietly(sourceStream)
        }
        val jsonTree = parse(new InputStreamReader(new ByteArrayInputStream(contents.toByteArray)))
        generateContents(jsonTree as ObjectNode)
    }
    
    def CharSequence generateContents(ObjectNode root) {
        val entities = entities.toMap[qualifiedName]
        val ids = entities.keySet.toInvertedMap[ new AtomicLong(0) ]
        val namespaceStatements = root.fields.toIterable.map[ entry |
            val namespaceName = entry.key
            val namespaceNode = entry.value as ObjectNode
            generateNamespace(entities, ids, namespaceName, namespaceNode)
        ]
        return namespaceStatements.flatten.join('\n') 
    }
    
    def Iterable<CharSequence> generateNamespace(Map<String, Class> entities, Map<String, AtomicLong> ids, String namespace, ObjectNode namespaceContents) {
        val inserts = namespaceContents.fields.toIterable.map[ entry |
            val className = entry.key
            val instanceNodes = (entry.value as ArrayNode)
            val entity = entities.get(namespace + '::' + className)
            val id = ids.get(namespace + '::' + className)
            instanceNodes.elements.map[generateInstance(entity, namespace, className, it as ObjectNode, id.incrementAndGet)].join()
        ]
        val alterSequences = ids.entrySet.map[ pair |
            '''
            ALTER SEQUENCE «namespace».«entities.get(pair.key).name.toLowerCase»_id_seq RESTART WITH «pair.value.get + 1»;
            '''
        ]
        return (inserts + alterSequences)
    }
    
    def CharSequence generateInstance(Class entity, String namespace, String className, ObjectNode node, Long id) {
        val sqlPropertyValues = entity.properties.filter[!derived].toMap [ name ].mapValues[ property |
            val jsonValue = node.get(property.name)
            toSqlValue(property, jsonValue)
        ]
        
        val sqlForeignKeys = entity.entityRelationships.filter[!derived && primary].toMap [ name ].mapValues[ relationship |
            getRelatedInstanceId(node.get(relationship.name))
        ]
        
        val sqlValues = newHashMap()
        sqlPropertyValues.forEach[key, value | sqlValues.put(key, value)]
        sqlForeignKeys.forEach[key, value | sqlValues.put(key + '_id', value)]
        
        '''
        INSERT INTO «namespace».«className» (id, «sqlValues.keySet.join(', ')») VALUES («id», «sqlValues.keySet.map[sqlValues.get(it)].join(', ')»);
        '''
    }
    
    def CharSequence getRelatedInstanceId(JsonNode propertyValue) {
        val referenceString = (propertyValue as TextNode).textValue
        val addressSeparatorIndex = referenceString.indexOf('@')
        if (addressSeparatorIndex <= 0 || addressSeparatorIndex == referenceString.length() - 1)
            return null
        return referenceString.substring(addressSeparatorIndex + 1)
    }
    
    def CharSequence toSqlValue(Property property, JsonNode propertyValue) {
        if (propertyValue == null)
            return 'null'
        return switch (propertyValue.asToken()) {
            case VALUE_NULL:
                'null'
            case VALUE_TRUE:
                'true'
            case VALUE_FALSE:
                'false'
            case VALUE_STRING:
                ''' '«(propertyValue as TextNode).textValue»' '''.toString.trim()
            case VALUE_NUMBER_INT:
                '''«propertyValue.asText()»'''
            case VALUE_NUMBER_FLOAT:
                '''«propertyValue.asText()»'''
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
        factory.configure(JsonParser.Feature.CANONICALIZE_FIELD_NAMES, false);
        return factory;
    ].apply
}