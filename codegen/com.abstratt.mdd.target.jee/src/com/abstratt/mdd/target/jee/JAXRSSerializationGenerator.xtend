package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.StereotypeUtils.*
import org.eclipse.uml2.uml.Property
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import org.eclipse.uml2.uml.TypedElement
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Operation
import com.abstratt.kirra.TypeRef
import org.eclipse.uml2.uml.MultiplicityElement

class JAXRSSerializationGenerator extends BehaviorlessClassGenerator {
    
    PlainEntityGenerator entityGenerator
    
    new(IRepository repository) {
        super(repository)
        entityGenerator = new PlainEntityGenerator(repository)
    }
    
    def CharSequence generateHelpers(Class entity) {
        val typeRef = entity.convertType
        val properties = entity.properties
        val dataProperties = properties.filter[!derived]
        val derivedProperties = properties.filter[derived]
        val instanceActions = entity.instanceActions
        // avoid duplicates due to redefinitions
        val entityRelationships = entity.entityRelationships      
        '''
        package resource.«entity.packagePrefix»;
        
        import «entity.packagePrefix».*;
        
        import java.util.*;
        import java.util.stream.*;
        import java.text.*;
        import java.util.function.Function;
        import java.io.IOException;
        
        import org.apache.commons.lang3.time.DateUtils;
        
        import java.net.URI;
        
        «entity.entityRelationships.map[type.nearestPackage].toSet.filter[it != entity.package].map[
        	'''
        	import resource.«name».*;
        	'''
        ].join()»
        
        public class «entity.name»JAXRSSerialization {
            private static final String[] DATE_FORMATS = { "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd", "yyyy/MM/dd" };

            public static enum Feature {
                Values,
                Links,
                ActionEnablement
            }

            public static Map<String, Object> toExternalRepresentation(«entity.name» toRender, URI instancesURI, Feature... featureOptions) {
                EnumSet<Feature> features = featureOptions.length == 0 ? EnumSet.noneOf(Feature.class) : EnumSet.copyOf(Arrays.asList(featureOptions));
                Map<String, Object> result = new LinkedHashMap<>();
                boolean persisted = toRender.getId() != null;
                Function<String, String> stringEncoder = (it) -> it == null ? null : it.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
                «IF (properties.exists[type.name == 'Date'])»
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                «ENDIF»
                if (features.contains(Feature.Values)) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    «dataProperties.map[
                        '''values.put("«name»", «getModelValue(it, 'toRender')»);'''
                    ].join('\n')»
                    «if (!derivedProperties.empty)
                    '''
                    if (persisted) {
                        «derivedProperties.map[
                            '''values.put("«name»", «getModelValue(it, 'toRender')»);'''
                        ].join('\n')»
                    } else {
                        «derivedProperties.map[
                            '''values.put("«name»", «it.type.generateDefaultValue»);'''
                        ].join('\n')»
                    }
                    '''»
                    result.put("values", values);
                }
                if (features.contains(Feature.Links)) {
                    Map<String, Object> links = new LinkedHashMap<>();
                    «entityRelationships.filter[!derived && !multiple && userVisible && navigable].map[ relationship |
                        val relationshipName = relationship.name
                        val accessor = '''«relationship.generateAccessorName»()'''
                        val relationshipInstancesURI = '''«relationship.type.name.toFirstLower»InstancesURI'''
                        '''
                        // «relationshipName» is navigable: «relationship.navigable»
                        Map<String, Object> «relationshipName»Link = null;
                        if (toRender.«accessor» != null) {
                            URI «relationshipInstancesURI» = instancesURI.resolve("../..").resolve("«typeRef.fullName»/instances"); 
                            «relationshipName»Link = «relationship.type.name»JAXRSSerialization.toExternalRepresentation(toRender.«accessor», «relationshipInstancesURI»);
                        }    
                        links.put("«relationshipName»", «relationshipName»Link);
                        '''
                    ].join('\n')»
                    result.put("links", links);
                }
                result.put("uri", instancesURI.resolve(persisted ? toRender.getId().toString() : "_template").toString());
                if (persisted) {
                    result.put("objectId", toRender.getId().toString());
                    result.put("shorthand", «getModelValue(entity.mnemonic, 'toRender')»);
                }
                «IF !instanceActions.empty»
                if (features.contains(Feature.ActionEnablement)) {
                    Map<String, String> disabledActions = new LinkedHashMap<>();
                    «FOR action : instanceActions»
                        if (!toRender.is«action.name.toFirstUpper»ActionEnabled())
                            disabledActions.put("«action.name»", "");
                    «ENDFOR»
                    result.put("disabledActions", disabledActions);
                }
                «ENDIF»
                «generateSetTypeRef(typeRef, 'result')»
                return result;                    
            }
            
            public static void updateFromExternalRepresentation(«entity.name» toUpdate, Map<String, Object> external) {
                Map<String, Object> values = (Map<String, Object>) external.get("values");
                «entity.properties.filter[property | !KirraHelper.isReadOnly(property, true)].map[ property | 
                    val core = '''«setModelValue(property, "values", "toUpdate")»;'''
                    if (property.type.name == 'Date')
                        '''
                        try {
                            «core»
                        } catch (ParseException e) {
                            throw new ConversionException("Invalid format for date in '«property.name»': " + values.get("«property.name»"));
                        }
                        '''
                    else
                        core
                    
                ].join('\n')»
                
                Map<String, Map<String, Object>> links = (Map<String, Map<String, Object>>) external.get("links");
                «entityRelationships.filter[!multiple && !KirraHelper.isReadOnly(it, true) && userVisible].map[ relationship |
                    '''
                    Map<String, Object> «relationship.name» = links.get("«relationship.name»");
                    if («relationship.name» != null) {
                        «relationship.type.name» newValue = Optional.ofNullable(«relationship.name».get("objectId")).map(it -> new «relationship.type.name»Service().find(Long.parseLong((String) it))).orElse(null);
                        toUpdate.«relationship.generateSetterName»(newValue);
                    } else {
                        toUpdate.«relationship.generateSetterName»(null);
                    }    
                    '''
                ].join»
            }
        }
        '''
    }
    
    def getModelValue(Property property, String varName) {
        val core = '''«varName».«property.generateAccessorName»()'''
        return getValueExpression(core, property)
    }
    
    def getValueExpression(CharSequence core, TypedElement element) {
        val optional = (element as MultiplicityElement).lowerBound == 0
        if (element.type.enumeration) {
            '''«IF optional»«core» == null ? null : «ENDIF»«core».name()'''
        } else if (element.type.name == 'String' || element.type.name == 'Memo') 
            '''stringEncoder.apply(«core»)'''            
        else if (element.type.name == 'Date') 
            '''«IF optional»«core» == null ? null : «ENDIF»dateFormat.format(«core»)'''            
        else
            core
    }
    
    def convertToInternal(TypedElement typedElement, CharSequence expression) {
        switch (typedElement.type.name) {
            case 'Double' : '''Double.parseDouble(«expression».toString())'''
            case 'Integer' : '''Long.parseLong(«expression».toString())'''
            case 'Date' : '''DateUtils.parseDate((String) «expression», DATE_FORMATS)'''
            default: 
                if (typedElement.type.entity) 
                    convertIdToInternal(typedElement, expression)
                else if (typedElement.type.enumeration) 
                    '''«typedElement.toJavaType()».valueOf((String) «expression»)'''
                else 
                    '''(«typedElement.toJavaType(true)») «expression»'''
        }
    }
    
    def convertIdToInternal(TypedElement typedElement, CharSequence expression) {
        '''((List<«typedElement.type.toJavaType»>) «expression»)'''
    }
    
    def setModelValue(Property property, String sourceVarName, String targetVarName) {
        val generateStatement = [ CharSequence value | '''«targetVarName».«property.generateSetterName»(«value»)''' ]
        val valueRetrieval = '''«sourceVarName».get("«property.name»")'''
        val core = generateStatement.apply(convertToInternal(property, valueRetrieval))
        return if (property.required)
            '''
            if («valueRetrieval» != null)
                «core»;
            else
                «generateStatement.apply('''«entityGenerator.generateAttributeDefaultValue(property)»''')»
            '''.toString.trim 
        else
            core
    }
    
    def generateSetTypeRef(TypeRef typeRef, String map) {
        '''
                Map<String, Object> «map»TypeRef = new LinkedHashMap<>();
                «map»TypeRef.put("entityNamespace", "«typeRef.entityNamespace»");
                «map»TypeRef.put("kind", "«typeRef.kind»");
                «map»TypeRef.put("typeName", "«typeRef.typeName»");
                «map»TypeRef.put("fullName", "«typeRef.fullName»");
                «map».put("typeRef", «map»TypeRef);   
                «map».put("scopeName", "«typeRef.typeName»");
                «map».put("scopeNamespace", "«typeRef.entityNamespace»");
                «map».put("instanceCapabilityUri", instancesURI.toString() + "/capabilities");
                «map».put("entityUri", instancesURI.resolve("../..").resolve("«typeRef.fullName»").toString());
        '''
    }
    
}