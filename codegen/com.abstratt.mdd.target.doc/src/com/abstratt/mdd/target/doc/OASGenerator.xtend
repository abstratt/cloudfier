package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.ParsedProperties
import java.util.Collection
import java.util.function.Supplier
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.TypedElement

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.target.base.GeneratorUtils.*
import java.util.Map

/**
 * Generates API specification that is compatible with OpenAPI.
 */
class OASGenerator extends AbstractGenerator {

		Map<String, String> customProperties
		
		new(IRepository repository) {
        super(repository)
		val prefix = "mdd.target.openapi.properties."
		this.customProperties = new ParsedProperties(repositoryProperties).filterProperties(prefix)
    }

    def generatePackages(Collection<Package> packages) {
        val enumerations = getEnumerations(packages).toSet
        val entities = getEntities(packages).toSet
        val entityComponents = entities.map[generateEntityDefinition]
        val enumerationComponents = enumerations.map[generateEnumerationAsComponent]
        '''
{
    "openapi": "3.0.0",
    "info": {
        "version": "1.0.0",
        "title": "«getApplicationLabel(repository)»"
    },
    "paths": {
        «entities.generateMany(",\n")[generateEntityPaths]»
    },
    "components": {
        "schemas": {
            «(enumerationComponents + entityComponents).join(",\n")»
        }
    }
}
    '''
    }
    
    def CharSequence generateEnumerationAsComponent(Classifier enumerationToGenerate) {
        '''
            "«enumerationToGenerate.toJavaQName»" : «enumerationToGenerate.generateEnumerationObject()»
        '''
    }

    def CharSequence generateObjectDefinition(Class toGenerate) {
    	val generatedProperties = (toGenerate.properties + toGenerate.entityRelationships)
                    		.map [generatePropertyDefinition(false)]
        '''
            {
                "type": "object",
                "properties": {
                    «
                    	(generatedProperties + generateBuiltInProperties()).join(",\n")
                    »
                }
            }
        '''
    }

	def Iterable<CharSequence> generateBuiltInProperties() {
		val customPropertyNames = customProperties.keySet.map[it.split("\\.").get(0)].toSet
		
		val Iterable<CharSequence> builtInProperties = customPropertyNames
			.filter[Boolean.FALSE.toString != customProperties.get(it + ".enabled")]
			.map[
				'''
				"«it»" : {
				    "type": "«customProperties.get(it + ".type")»",
				    "format": "«customProperties.get(it + ".format")»"
				}
			    ''']
		
		return builtInProperties
	}
		
    def CharSequence generateEntityDefinition(Class toGenerate) {
        '''
            "«toGenerate.toJavaQName»" : «generateObjectDefinition(toGenerate)»
        '''
    }
    def CharSequence getBasePath(NamedElement toGenerate) {
        var nameComponents = toGenerate.qualifiedName.split(NamedElement.SEPARATOR).tail
        var basePath = "/" + nameComponents.map[symbol].join("/")
        return basePath
    }

    def CharSequence generateEntityPaths(Class toGenerate) {
        var basePath = getBasePath(toGenerate)
        // generateEntityRetrieval(toGenerate),
        // generateEntityUpdate(toGenerate),
        // generateEntityDeletion(toGenerate)
        return 
            (
                #[generateDefaultEndpoints(basePath, toGenerate)] +
                generateEntityOperationEndpoints(basePath, toGenerate)
            ).join(",\n") 
    }
    
    protected def CharSequence generateDefaultEndpoints(CharSequence basePath, Class toGenerate)
        '''
            "«basePath»" : {
                «
                      #[
                          toGenerate.generateEntityList,
                          toGenerate.generateEntityCreation
                      ].generateMany(",\n")[it]»
            }
        '''
    

    def Iterable<? extends CharSequence> generateEntityOperationEndpoints(CharSequence basePath, Class toGenerate) {
        toGenerate.operations.map[generateEntityOperationEndpoint(basePath, toGenerate, it)]
    }
    
    def CharSequence generateEntityOperationEndpoint(CharSequence basePath, Class classToGenerate, Operation operationToGenerate) {
        '''
            "«basePath»/«operationToGenerate.symbol»" : {
                «generateEntityOperation(classToGenerate, operationToGenerate)»
            }
        '''
    }
    
    def CharSequence generateResponseContent(Supplier<CharSequence> core) {
      '''
      "content": {
          "application/json": {
              «core.get»
          }
      }
      '''    	
    }

    def CharSequence generateEntityOperation(Class classToGenerate, Operation operationToGenerate) {
    	val hasResult = !operationToGenerate.returnResult().isEmpty
        var responseDetails = if (hasResult) {
            #[
              generateResponseContent['''
                  "schema": «operationToGenerate.returnResult().get(0).generateDataElementDefinition»                  	
              '''],
              '''"description": "«operationToGenerate.returnResult().get(0).label»"'''
            ]
        } else {
            #['''"description": "Result for «operationToGenerate.name»"''']
        }
        '''
            "post" : {
                "operationId": "«operationToGenerate.qualifiedName.symbol»",
                "tags": ["«classToGenerate.qualifiedName»"],
                "summary": "«operationToGenerate.description.replaceAll("\n", " ")»",
                "responses": {
                    "200": {
                        «responseDetails.join(",\n")»
                    }
                }
            }
        '''
    }

    def CharSequence generateEntityList(Class toGenerate) {
        '''
            "get" : {
                "operationId": "«toGenerate.qualifiedName.symbol»_List",
                "summary": "List endpoint for «toGenerate.label»",
                "tags": ["«toGenerate.qualifiedName»"],
                "responses": {
                    "200": {
                        "description": "OK",
                        «generateResponseContent[
                        	'''
                                "schema": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/components/schemas/«toGenerate.toJavaQName»"
                                    }
                                }
                        	'''
                        ]»
                    }
                }
            }
        '''
    }

    def CharSequence generateEntityCreation(Class toGenerate) {
        '''
            "post" : {
                "operationId": "«toGenerate.qualifiedName.symbol»_Creation",
                "tags": ["«toGenerate.qualifiedName»"],
                "summary": "Creation endpoint for «toGenerate.label»",
                "responses": {
                    "204": {
                        "description": "Created",
                        «generateResponseContent[
                        	'''
                                "schema": {
                                    "$ref": "#/components/schemas/«toGenerate.toJavaQName»"
                                }
                            '''
                    	]»
                    }
                }
            }
        '''
    }

    def CharSequence generatePrimitivePropertyDefinition(TypedElement toGenerate) {
        '''
            {
                «toGenerate.type.toOASType»
            }
        '''
    }

    def CharSequence generateEnumerationObject(Classifier toGenerate) {
        '''
            {
                "type": "string",
                "enum": [
                    «toGenerate.enumerationLiterals.generateMany(",\n")['''"«it.name»"''']»
                ]
            }
        '''
    }

    def CharSequence generateEnumeratedPropertyDefinition(TypedElement toGenerate) {
    	(toGenerate.type as Enumeration).generateEnumerationObject
    }

    def CharSequence generateRelationshipPropertyDefinition(TypedElement toGenerate) {
        generateObjectDefinition(toGenerate.type as Class)
    }

    def CharSequence generateDefinitionReference(TypedElement toGenerate) {
    	val path = '''components/schemas/«toGenerate.type.toJavaQName»''' 
        '''
            {
                "$ref": "#/«path»"
            }
        '''
    }

    def String toOASType(Type type) {
        return switch (type.name) {
            case 'String':
                '"type": "string"'
            case 'Memo':
                '"type": "string"'
            case 'Email':
                '''
                "type": "string",
                "format": "email"
                '''
            case 'Date': '''
                "type": "string",
                "format": "date"
                '''
            case 'DateTime': '''
                "type": "string",
                "format": "date-time"
                '''
            case 'Time':
                '"type": "string"'
            case 'Integer': '''
                "type": "integer",
                "format": "int64"
                '''
            case 'Real': '''
                "type": "number",
                "format": "double"
                '''
            case 'Boolean':
                '"type": "boolean"'
        }
    }

    def generatePropertyDefinition(Property toGenerate, boolean inline) {
        '''
            "«toGenerate.name»": «generateDataElementDefinition(toGenerate, inline)»
        '''
    }

    def CharSequence generateDataElementDefinition(TypedElement toGenerate) {
        toGenerate.generateDataElementDefinition(true)        
    }

    def CharSequence generateDataElementDefinition(TypedElement toGenerate, boolean inline) {
        if (toGenerate instanceof MultiplicityElement && (toGenerate as MultiplicityElement).multivalued)
            '''
            {
                "type": "array",
                "items" : «toGenerate.generateSingleDataElementDefinition(inline)»
            }
            '''
        else
            toGenerate.generateSingleDataElementDefinition(inline)
    }

    def CharSequence generateSingleDataElementDefinition(TypedElement toGenerate, boolean inline) {
        if (toGenerate.type.isPrimitive)
            toGenerate.generatePrimitivePropertyDefinition
        else if (inline) {
            if (toGenerate.type.enumeration)
                toGenerate.generateEnumeratedPropertyDefinition
            else
                toGenerate.generateRelationshipPropertyDefinition
        } else
            toGenerate.generateDefinitionReference
    }
}