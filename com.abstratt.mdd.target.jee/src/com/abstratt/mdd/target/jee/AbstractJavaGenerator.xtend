package com.abstratt.mdd.target.jee

import com.abstratt.kirra.TypeRef
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.ConditionalNode
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.Variable

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.kirra.mdd.schema.KirraMDDSchemaBuilder.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension org.apache.commons.lang3.text.WordUtils.*

abstract class AbstractJavaGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities

    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }

    def generateComment(Element element) {
        if (!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).
                split('<br>').map['''* «it»'''].join('\n')
            '''
                /**
                 «reformattedParagraphs»
                 */
            '''
        }
    }

    def String packageSuffix(Class contextual) {
        contextual.nearestPackage.toJavaPackage
    }

    def String toJavaPackage(Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper) {
        return items.generateMany(mapper, '\n')
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper, String separator) {
        return items.map[mapper.apply(it)].join(separator)
    }

    def CharSequence generateActivity(Activity activity) {
        '''
            «generateActivityRootAction(activity)»
        '''
    }

    def dispatch CharSequence generateAction(Action toGenerate) {
        if (toGenerate.cast)
            toGenerate.sourceAction.generateAction
        else
            generateActionProper(toGenerate)
    }

    def generateActivityRootAction(Activity activity) {
        val rootActionGenerated = generateAction(activity.rootAction)
        '''
            «rootActionGenerated»
        '''
    }

    def dispatch CharSequence generateAction(Void input) {
        throw new NullPointerException;
    }

    def dispatch CharSequence generateAction(InputPin input) {
        generateActionProper(input.sourceAction)
    }

    def CharSequence generateActionProper(Action toGenerate) {
        doGenerateAction(toGenerate)
    }

    def generateStatement(Action statementAction) {
        val isBlock = statementAction instanceof StructuredActivityNode
        val generated = generateAction(statementAction)
        if (isBlock)
            // actually a block
            return generated

        // else generate as a statement
        '''«generated»;'''
    }

    def dispatch CharSequence doGenerateAction(Action action) {

        // should never pick this version - a more specific variant should exist for all supported actions
        '''Unsupported «action.eClass.name»'''
    }

    def dispatch CharSequence doGenerateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def generateAddVariableValueAction(AddVariableValueAction action) {
        if (action.variable.name == '') '''return «generateAction(action.value)»''' else '''«action.variable.name» = «generateAction(
            action.value)»'''
    }

    def dispatch CharSequence doGenerateAction(StructuredActivityNode node) {
        val container = node.eContainer

        // avoid putting a comma at a conditional node clause test 
        if (container instanceof ConditionalNode)
            if (container.clauses.exists[tests.contains(node)])
                return '''«node.findStatements.head.generateAction»'''

        // default path, generate as a statement
        generateStructuredActivityNodeAsBlock(node)
    }

    def dispatch CharSequence doGenerateAction(AddStructuralFeatureValueAction action) {
        generateAddStructuralFeatureValueAction(action)
    }

    def generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        val target = action.object
        val value = action.value
        val featureName = action.structuralFeature.name

        '''«generateAction(target)».«featureName» = «generateAction(value)»'''
    }

    def dispatch CharSequence doGenerateAction(CreateObjectAction action) {
        generateCreateObjectAction(action)
    }

    def generateCreateObjectAction(CreateObjectAction action) {
        '''new «action.classifier.name»()'''
    }

    def generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        '''«generateVariables(node)»«node.findTerminals.map[generateStatement].join('\n')»'''
    }

    def generateVariables(StructuredActivityNode node) {
        generateVariableBlock(node.variables)
    }

    def generateVariableBlock(Iterable<Variable> variables) {
        if(variables.empty) '' else variables.map['''«type.convertType.toJavaType» «name»;'''].join('\n') + '\n'
    }

    def dispatch CharSequence doGenerateAction(ReadVariableAction action) {
        generateReadVariableValueAction(action)
    }

    def generateReadVariableValueAction(ReadVariableAction action) {
        '''«action.variable.name»'''
    }

    def dispatch CharSequence doGenerateAction(ReadSelfAction action) {
        generateReadSelfAction(action)
    }

    def CharSequence generateReadSelfAction(ReadSelfAction action) {
        generateSelfReference()
    }

    def generateSelfReference() {
        'this'
    }

    def toJavaType(TypeRef type) {
        switch (type.kind) {
            case Entity:
                type.typeName
            case Enumeration:
                'String'
            case Primitive:
                switch (type.typeName) {
                    case 'Integer': 'Long'
                    case 'Double': 'Double'
                    case 'Date': 'Date'
                    case 'String': 'String'
                    case 'Memo': 'String'
                    case 'Boolean': 'Boolean'
                    default: 'UNEXPECTED TYPE: «type.typeName»'
                }
            default: '''UNEXPECTED KIND: «type.kind»'''
        }
    }

}
