package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.base.IBasicBehaviorGenerator
import java.util.Set
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.EnumerationLiteral
import org.eclipse.uml2.uml.InstanceValue
import org.eclipse.uml2.uml.Interface
import org.eclipse.uml2.uml.LiteralBoolean
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Namespace
import org.eclipse.uml2.uml.OpaqueExpression
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.PackageableElement
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.TypedElement
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.ElementUtils.*
import static extension com.abstratt.mdd.core.util.DataTypeUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.isVertexLiteral
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.resolveVertexLiteral
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*

abstract class PlainJavaGenerator extends com.abstratt.kirra.mdd.target.base.AbstractGenerator implements IBasicBehaviorGenerator {
    
    public static Set<String> JAVA_KEYWORDS = #{ "abstract", "assert", "boolean",
                "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "extends", "false",
                "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native",
                "new", "null", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch",
                "synchronized", "this", "throw", "throws", "transient", "true",
                "try", "void", "volatile", "while" }
    
    new(IRepository repository) {
        super(repository)
    }
    
    def String packagePrefix(PackageableElement contextual) {
        contextual.nearestPackage.toJavaPackage
    }
    
    def render(CharSequence cs, String quote) {
        '''«quote»«cs.toString.replaceAll(quote, '\\\'' + quote).replaceAll('\n', '\\\'')»«quote»'''
    }
    
    def generateImports(Namespace namespaceContext) {
        val modelImports = namespaceContext.nearestPackage.packageImports.filter[importedPackage?.kirraPackage].generateMany(['''import «importedPackage.toJavaPackage».*;'''])
        
        val roleClassesToImport = appPackages.entities.filter[ isRole(true)].filter[it.package != namespaceContext]
        
        '''
        import «applicationName».*;
        «modelImports»
        «roleClassesToImport.generateMany['''import «it.package.toJavaPackage».«it.name»;''']»
        '''
    }
    
    def generateComment(Element element) {
        val comments = element.comments
        if (!comments.empty) {
            val reformattedParagraphs = comments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).
                split('<br>').map['''* «it»'''].join('\n')
            '''
                /**
                 «reformattedParagraphs»
                 */
            '''
        }
    }
    
    def boolean isJavaPrimitive(Type toCheck) {
        switch (toCheck.name) {
            case 'Boolean' : true
            case 'Integer' : true
            case 'Double' : true
            default : false
        }
    }
    
    def CharSequence toJavaVisibility(VisibilityKind visibility) {
        if (visibility == VisibilityKind.PACKAGE_LITERAL)
            ''
        else
            visibility.getName
    }
    
    def CharSequence toJavaVisibility(NamedElement element) {
        element.visibility.toJavaVisibility
    }
    
    def CharSequence append(CharSequence value, String suffix) {
        if (value.toString.endsWith(suffix))
            value
        else
            value + suffix
    }
    
    def CharSequence generateOperationReturnType(Operation operation) {
        operation.javaReturnType
    }
    
    def CharSequence generateJavaMethodSignature(Operation operation, VisibilityKind visibility, boolean staticOperation) {
        val methodName = operation.name
        val modifiers = newLinkedList()
        modifiers.add(visibility.toJavaVisibility)
        if (staticOperation) 
            modifiers.add('static')
            
        '''
        «operation.generateComment»
        «modifiers.map[toString].filter[!empty].join(' ').append(' ')»«operation.generateOperationReturnType» «methodName»(«operation.parameters.generateMany([ generateJavaMethodParameter ], ', ')»)'''
    }
    
    def CharSequence generateJavaMethodParameter(Parameter parameter) {
        '''«parameter.toJavaType» «parameter.name»'''
    }    
    
    def CharSequence generateJavaMethod(Operation operation, VisibilityKind visibility, boolean staticOperation) {
        '''
        «operation.generateJavaMethodSignature(visibility, staticOperation)» {
            «operation.generateParameterDefaults»
            «operation.activity.generateJavaMethodBody»
        }
        '''
    }
	
	def CharSequence generateParameterDefaults(Operation operation) {
        val parameterWithDefaults = operation.ownedParameters.inputParameters.filter[(^default !== null || defaultValue !== null)]
        return parameterWithDefaults.generateMany[generateParameterDefault(it)]
	}
	
	def CharSequence generateParameterDefault(Parameter parameter) {
		'''
        if («parameter.name» == null) «parameter.name» = «parameter.defaultValue.generateValue»; 
        '''		
	}
    
    def CharSequence generateJavaMethodBody(Activity activity) {
        activity.generateActivity
    }
    
    def Iterable<DataType> getAnonymousDataTypes(Activity activity) {
        val allActions = activity.bodyNode.findMatchingActions(UMLPackage.Literals.ACTION)
        return allActions.map[ action |
            val outputTypes = action.outputs.map[type]
            val dataTypes = outputTypes.filter(typeof(DataType))
            val anonymousDataTypes = (dataTypes).filter[anonymousDataType]
            return anonymousDataTypes
        ].flatten.toSet
    }
    
    def CharSequence toJavaType(TypedElement element, boolean honorOptionality) {
        var nullable = true 
        var multivalued = false
        if (element instanceof MultiplicityElement) {
            nullable = element.lower == 0 || (element instanceof Parameter && (element as Parameter).defaultValue != null)
            multivalued = element.multivalued
        }
        element.type.toJavaType(if (honorOptionality) nullable else true, multivalued)
    }
    
    def toJavaType(TypedElement element) {
        toJavaType(element, true)
    }
    
        
    def CharSequence getJavaReturnType(Operation op) {
        return if (op.getReturnResult == null) "void" else op.getReturnResult().toJavaType(true)
    }
    
    def toJavaName(String qualifiedName) {
        return qualifiedName.replace(NamedElement.SEPARATOR, '.')
    }
    
    def <T extends MultiplicityElement> toJavaGeneralCollection(T element) {
        val unique = element.unique
        if (unique)
            'Collection'
        else
            'Collection'
    }
    
    def <T extends MultiplicityElement&TypedElement> toJavaCollection(T element) {
        val unique = element.unique
        if (unique)
            'LinkedHashSet'
        else
            'ArrayList'
    }

    def String toJavaType(Type type) {
        toJavaType(type, true, false)
    }
    
    def String toJavaType(Type type, boolean nullable, boolean multivalued) {
        val baseType = switch (type.kind) {
            case Entity:
                type.name
            case Enumeration:
                if (type.namespace instanceof Package) type.name else type.namespace.name + '.' + type.name
            case Tuple:
                type.name
            case Primitive:
                switch (type.name) {
                    case 'Integer': if (nullable) 'Long' else 'long'
                    case 'Double': if (nullable) 'Double' else 'double'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Memo': 'String'
                    case 'Email': 'String'
                    case 'Boolean': if (nullable) 'Boolean' else 'boolean'
                    default: '''String'''
                }
            case Blob:
                '''String'''
            case null: switch (type) {
                case type instanceof Interface && type.isSignature : (type as Interface).toJavaClosureType
                case type instanceof Activity : (type as Activity).generateActivityAsExpression(true).toString
                case type instanceof DataType && type.visibility == VisibilityKind.PRIVATE_LITERAL : (type as DataType).generateAnonymousDataTypeName                 
                default : type.qualifiedName.toJavaName
            }
            default: '''UNEXPECTED KIND: «type.kind»'''
        }
        return if (multivalued) 
            '''Collection<«baseType»>'''
        else
        	baseType
    }
    
    def generateAnonymousDataTypeName(DataType type) {
        '''«type.getAllAttributes().map[ attribute |
            attribute.generateAttributeName.toFirstUpper 
        ].join()»Tuple'''.toString    
    }
    
    def generateAttributeName(Property property) {
        return if (property.name != null) property.name else '''«property.type?.name?.toFirstLower»'''.toString;
    }
    
    def generateAttributeNameAsJavaSymbol(Property property) {
        val candidate = property.generateAttributeName
        return if (JAVA_KEYWORDS.contains(candidate)) '_' + candidate else candidate
    }
    
    def toJavaClosureType(Activity activity) {
        val inputs = activity.closureInputParameters
        val result = activity.closureReturnParameter
        if (inputs.size() == 1) {
            if (result == null)
                return '''Consumer<«inputs.head.toJavaType(false)»>'''
            return '''Function<«inputs.head.toJavaType(false)», «result.toJavaType(false)»>'''
        } else if (inputs.size() == 0) {
            if (result != null)
                return '''Supplier<«result.toJavaType(false)»>'''
        }
        return '''/*Unsupported closure*/'''
    }
    
    def toJavaClosureType(Interface signature) {
        val signatureParameters = signature.signatureParameters
        val inputs = signatureParameters.getInputParameters()
        val result = signatureParameters.filterParameters(ParameterDirectionKind.RETURN_LITERAL).head 
        if (inputs.size() == 1) {
            if (result == null)
                return '''Consumer<«inputs.head.toJavaType(false)»>'''
            return '''Function<«inputs.head.toJavaType(false)», «result.toJavaType(false)»>'''
        } else if (inputs.size() == 0) {
            if (result != null)
                return '''Supplier<«result.toJavaType(false)»>'''
        }
        return '''/*Unsupported closure*/'''
    }
    

    def generateClassReference(Classifier classifier) {
        classifier.name
    }
    
        
    def generateDataType(DataType dataType, boolean topLevel) {
        val dataTypeName = if (dataType.anonymousDataType) dataType.generateAnonymousDataTypeName else dataType.toJavaType
        '''
        public «IF !topLevel»static «ENDIF»class «dataTypeName» {
        	private static final long serialVersionUID = 1L;
            «dataType.getAllAttributes().generateMany['''
                public final «toJavaType» «generateAttributeNameAsJavaSymbol»;
            ''']»
            
            public «dataTypeName»(«dataType.getAllAttributes().generateMany([
                '''«toJavaType» «generateAttributeNameAsJavaSymbol»'''
            ], ', ')») {
                «dataType.getAllAttributes().generateMany([
                  '''this.«generateAttributeNameAsJavaSymbol» = «generateAttributeNameAsJavaSymbol»;'''  
                ])»
            }
            
            «dataType.getAllAttributes().generateMany['''
                public «toJavaType» get«generateAttributeName.toFirstUpper»() {
                    return «generateAttributeNameAsJavaSymbol»;
                }    
            ''']» 
        }
        
        '''
    }
    
    def generatePredicate(Constraint predicate, boolean negate) {
        val predicateActivity = predicate.specification.resolveBehaviorReference as Activity
        val core = predicateActivity.generateActivityAsExpression
        val needsParenthesis = core.toString.contains(' ')
        val rootSourceAction = predicateActivity.rootAction.findSingleStatement.sourceAction
        if (negate) {
            if (rootSourceAction instanceof CallOperationAction && (rootSourceAction as CallOperationAction).operation.qualifiedName == 'mdd_types::Boolean::not')
                '''«core.toString.replaceFirst('!', '')»'''
            else
                '''!«IF needsParenthesis»(«ENDIF»«core»«IF needsParenthesis»)«ENDIF»'''
        } else 
            '''«core»'''
    }

    def CharSequence unsupportedElement(Element e) {
        unsupportedElement(e, if (e instanceof NamedElement) e.name else null)
    }
    
    def CharSequence unsupported(CharSequence message) {
        '''Unsupported: «message»''' 
    } 
    
    def CharSequence unsupportedElement(Element e, String message) {
        unsupported('''«e.eClass.name»> «if (message != null) '''(«message»)''' else ''»''')
    }

    def generateValue(ValueSpecification value) {
        generateValue(value, true)
    }

    def generateValue(ValueSpecification value, boolean inline) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString : switch (value.type.name) {
                case 'String' : '''"«value.stringValue»"'''
                case 'Integer' : '''«value.stringValue»L'''
                case 'Double' : '''«value.stringValue»'''
                case 'Boolean' : '''«value.stringValue»'''
                default : unsupported(value.stringValue)
            }
            LiteralBoolean : '''«value.booleanValue»'''
            LiteralNull : switch (value) {
                case value.isVertexLiteral : '''«value.toJavaType».«value.resolveVertexLiteral.name»'''
                default : 'null'
            }
            OpaqueExpression case value.behaviorReference : (value.resolveBehaviorReference as Activity).generateActivityAsExpression(!inline)
            InstanceValue case value.instance instanceof EnumerationLiteral: '''«value.instance.namespace.name».«value.instance.name»'''
            default : unsupportedElement(value)
        }
    }
    
    def CharSequence generateAttributeDefaultValue(Property attribute) {
        if (attribute.defaultValue != null) {
            return if (attribute.defaultValue.behaviorReference)
                (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivityAsExpression 
            else
                attribute.defaultValue.generateValue
        } else if (attribute.required || attribute.type.enumeration)
            // enumeration covers state machines as well
            attribute.type.generateDefaultValue
    }

    def generateDefaultValue(TypedElement typed) {
        //TODO-RC probably should be taking multiplicity into account
        generateDefaultValue(typed.type)
    }
    
    def generateDefaultValue(Type type) {
        switch (type) {
            StateMachine : '''«type.stateMachineContext.name».«type.name».«type.initialVertex.name»'''
            Enumeration : '''«type.name».«type.ownedLiterals.head.name»'''
            Class : switch (type.name) {
                case 'Boolean' : 'false'
                case 'Integer' : '0L'
                case 'Double' : '0.0'
                case 'Date' : 'new Date()'
                case 'String' : '""'
                case 'Memo' : '""'
                case 'Email' : '""'
            }
            default : 'null'
        }
    }
    
    def generateSampleValue(Type type) {
        switch (type) {
            StateMachine : '''«type.stateMachineContext.name».«type.name».«(if (type.states.size > 1) type.states.findFirst[!it.initial] else type.initialVertex).name»'''
            Enumeration : '''«type.name».«type.ownedLiterals.last.name»'''
            Class : switch (type.name) {
                case 'Boolean' : 'true'
                case 'Integer' : '100L'
                case 'Double' : '100.0'
                case 'Date' : 'new Date(new Date().getTime() + 24 * 60 * 60 * 1000L)'
                case 'String' : '"A string value"'
                case 'Memo' : '"A memo value"'
                case 'Email' : '"simpleemail@acme.com"'
            }
            default : null
        }
    }
    
    def generateAccessorName(Property attribute) {
        val prefix = if ("Boolean".equals(attribute.type.name)) "is" else "get"
        '''«prefix»«attribute.name.toFirstUpper»'''
    }
    
    def generateSetterName(Property attribute) {
        val prefix = "set"
        '''«prefix»«attribute.name.toFirstUpper»'''
    }
    
    def CharSequence toJavaStringLiteral(CharSequence toConvert) {
        '''"«toConvert.toString().trim().replaceAll('[\n\t ]+', ' ')»"'''
    }
}
