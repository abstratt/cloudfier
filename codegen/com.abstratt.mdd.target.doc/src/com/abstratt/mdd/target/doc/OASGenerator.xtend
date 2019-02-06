package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.ParsedProperties
import java.util.Collection
import java.util.Map
import java.util.function.Supplier
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.TypedElement

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

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
        val entities = getEntities(packages).filter[!it.abstract].toSet
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
            	"description": "«toGenerate.description»",
                "type": "object",
                "properties": {
                    «
                    	(generatedProperties + generateSingleBuiltInProperties()).join(",\n")
                    »
                }
            }
        '''
    }

	def Iterable<CharSequence> generateSingleBuiltInProperties() {
		return generateBuiltInProperties("single")
	}
	def Iterable<CharSequence> generateListBuiltInProperties() {
		return generateBuiltInProperties("list")
	}

	def Iterable<CharSequence> generateBuiltInProperties(String kind) {
		val customPropertyNames = customProperties.keySet.map[it.split("\\.").get(0)].toSet
		
		val Iterable<CharSequence> builtInProperties = customPropertyNames
			.filter[isOptionEnabled(it, kind)]
			.map[
				'''
				"«it»" : {
				    "type": "«customProperties.get(it + ".type")»",
				    "format": "«customProperties.get(it + ".format")»"
				}
			    ''']
		
		return builtInProperties
	}
	
	protected def boolean isOptionEnabled(String propertyName, String kind) {
		val enabledOption = propertyName + ".enabled"
		val optionValue = customProperties.get(enabledOption)
		if (Boolean.FALSE.toString == optionValue) return false
		if (Boolean.TRUE.toString == optionValue) return true
		return kind == optionValue
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
            },
            "«basePath»/{id}" : {
                «
                      #[
                          toGenerate.generateEntityRetrieval,
                          toGenerate.generateEntityUpdate,
                          toGenerate.generateEntityDeletion
                      ].generateMany(",\n")[it]»
            }
        '''
    

    def Iterable<? extends CharSequence> generateEntityOperationEndpoints(CharSequence basePath, Class toGenerate) {
        toGenerate.operations.map[generateEntityOperationEndpoint(basePath, toGenerate, it)]
    }
    
    def CharSequence generateEntityOperationEndpoint(CharSequence basePath, Class classToGenerate, Operation operationToGenerate) {
    	val pathSuffix = if (operationToGenerate.static) 
    		operationToGenerate.symbol
		else 
    		'{id}/' + operationToGenerate.symbol
        '''
            "«basePath»/«pathSuffix»" : {
                «generateEntityOperation(classToGenerate, operationToGenerate)»
            }
        '''
    }
    
    def CharSequence generateEntityBody(Supplier<CharSequence> core) {
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
              generateEntityBody['''
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
    
    def CharSequence generateEntityRetrieval(Class toGenerate) {
        '''
            "get" : {
                "operationId": "«toGenerate.qualifiedName.symbol»_Get",
                "summary": "Single object GET endpoint for «toGenerate.label»",
                "tags": ["«toGenerate.qualifiedName»"],
                "responses": {
                    "200": {
                        "description": "OK",
                        «generateEntityBody[
                        	'''
                                "schema": «toGenerate.generateDefinitionReference»
                        	'''
                        ]»
                    }
                }
            }
        '''
    }    

    def CharSequence generateEntityUpdate(Class toGenerate) {
        '''
            "put" : {
                "operationId": "«toGenerate.qualifiedName.symbol»_Put",
                "summary": "Single object PUT endpoint for «toGenerate.label»",
                "tags": ["«toGenerate.qualifiedName»"],
                "responses": {
                    "200": {
                        "description": "OK",
                        «generateEntityBody[
                        	'''
                                "schema": «toGenerate.generateDefinitionReference»
                        	'''
                        ]»
                    }
                }
            }
        '''
    }


	def CharSequence generateEntityDeletion(Class toGenerate) {
        '''
            "delete" : {
                "operationId": "«toGenerate.qualifiedName.symbol»_Put",
                "summary": "Single object DELETE endpoint for «toGenerate.label»",
                "tags": ["«toGenerate.qualifiedName»"],
                "responses": {
                    "202": {
                        "description": "NO CONTENT"
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
                        «generateEntityBody[
                        	'''
                                "schema": «generateDefinitionReferenceAsList(toGenerate)»
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
                "requestBody": {
                    «generateEntityBody[
                    	'''
                            "schema": {
                                "type": "array",
                                "items": «toGenerate.generateDefinitionReference»
                            }
                    	'''
                    ]»
                },
                "responses": {
                    "201": {
                        "description": "Created",
                        «generateEntityBody[
                        	'''
                                "schema": «toGenerate.generateDefinitionReference»
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
            	"description": "«toGenerate.description»",
                «toGenerate.type.toOASType»
            }
        '''
    }

    def CharSequence generateEnumerationObject(Classifier toGenerate) {
        '''
            {
            	"description": "«toGenerate.description»",
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

    def CharSequence generateDefinitionReference(Type toGenerate) {
    	val path = '''components/schemas/«toGenerate.toJavaQName»''' 
        '''
            {
                "$ref": "#/«path»"
            }
        '''
    }
    
    def CharSequence generateDefinitionReferenceAsList(Type toGenerate) {
    	val path = '''components/schemas/«toGenerate.toJavaQName»'''
    	val builtInProperties = generateListBuiltInProperties()
        '''
            {
                "properties": {
                	"content": {
                		"type": "array",
                		"items": {
                		    "$ref": "#/«path»"
                		}
                	},
        		    «builtInProperties.join(",\n")»
            	}  
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
        	toGenerate.generateMultipleDataElementDefinition(inline)
        else
            toGenerate.generateSingleDataElementDefinition(inline)
    }
		
	def CharSequence generateMultipleDataElementDefinition(TypedElement toGenerate, boolean inline) {
		'''
            {
                "description": "«toGenerate.description»",
                "type": "array",
                "items" : «toGenerate.generateSingleDataElementDefinition(inline)»
            }
        '''
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
            toGenerate.type.generateDefinitionReference
    }
}
