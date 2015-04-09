package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import java.util.Arrays
import java.util.Deque
import java.util.LinkedList
import java.util.List
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.ActivityNode
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.ReadExtentAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.DestroyLinkAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.ValueSpecificationAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ConditionalNode

class AbstractJavaBehaviorGenerator extends PlainJavaGenerator implements IBehaviorGenerator {

    public static ThreadLocal<Deque<IExecutionContext>> currentContextStack = new ThreadLocal<Deque<IExecutionContext>>() {
        override protected initialValue() {
            new LinkedList(Arrays.asList(new SimpleContext("this")))
        }
    }  

    new(IRepository repository) {
        super(repository)
    }
    
    final def getContextStack() {
        currentContextStack.get()
    }

    final override enterContext(IExecutionContext context) {
        contextStack.push(context)
    }

    final override leaveContext(IExecutionContext context) {
        val top = contextStack.peek
        if (context != top)
            throw new IllegalStateException
        contextStack.pop
    }

    final override getContext() {
        contextStack.peek
    }

    def dispatch CharSequence generateAction(Action toGenerate) {
        generateActionProper(toGenerate)
    }

    def dispatch CharSequence generateAction(Void input) {
        throw new NullPointerException
    }

    def dispatch CharSequence generateAction(InputPin input) {
        generateAction(input.sourceAction)
    }

    def CharSequence generateActionProper(Action toGenerate) {
        doGenerateAction(toGenerate)
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override CharSequence generateActivityAsExpression(Activity toGenerate) {
        return this.generateActivityAsExpression(toGenerate, false, Arrays.<Parameter>asList())
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        generateActivityAsExpression(toGenerate, asClosure, toGenerate.closureInputParameters)
    }

    def dispatch CharSequence doGenerateAction(Action action) {

        // should never pick this version - a more specific variant should exist for all supported actions
        unsupported(action.eClass.name)
    }

    def dispatch generateAction(ActivityNode action) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def dispatch CharSequence doGenerateAction(ReadExtentAction action) {
        generateReadExtentAction(action)
    }

    def CharSequence generateReadExtentAction(ReadExtentAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(AddStructuralFeatureValueAction action) {
        generateAddStructuralFeatureValueAction(action)
    }

    def generateAddStructuralFeatureValueAction(AddStructuralFeatureValueAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(AddVariableValueAction action) {
        generateAddVariableValueAction(action)
    }

    def CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(DestroyLinkAction action) {
        generateDestroyLinkAction(action)
    }

    def dispatch CharSequence doGenerateAction(ReadLinkAction action) {
        generateReadLinkAction(action)
    }

    def generateReadLinkAction(ReadLinkAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(CreateObjectAction action) {
        generateCreateObjectAction(action)
    }

    def dispatch CharSequence doGenerateAction(ReadStructuralFeatureAction action) {
        generateReadStructuralFeatureAction(action)
    }

    def generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(SendSignalAction action) {
        generateSendSignalAction(action)
    }

    def generateSendSignalAction(SendSignalAction action) {
        unsupported(action.eClass.name)
    }
    
    def dispatch CharSequence doGenerateAction(TestIdentityAction action) {
        '''«generateTestIdentityAction(action)»'''
    }

    def CharSequence generateTestIdentityAction(TestIdentityAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(ConditionalNode node) {
        generateConditionalNode(node)
    }
    
    def generateConditionalNode(ConditionalNode action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(StructuredActivityNode action) {
        generateStructuredActivityNode(action)
    }

    def generateStructuredActivityNode(StructuredActivityNode action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(ValueSpecificationAction action) {
        generateValueSpecificationAction(action)
    }

    def generateValueSpecificationAction(ValueSpecificationAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(ReadVariableAction action) {
        generateReadVariableAction(action)
    }

    def generateReadVariableAction(ReadVariableAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(ReadSelfAction action) {
        generateReadSelfAction(action)
    }

    def CharSequence generateReadSelfAction(ReadSelfAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(CallOperationAction action) {
        generateCallOperationAction(action)
    }

    def CharSequence generateCallOperationAction(CallOperationAction action) {
        unsupported(action.eClass.name)
    }

    def generateCreateObjectAction(CreateObjectAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(DestroyObjectAction action) {
        generateDestroyObjectAction(action)
    }

    def generateDestroyObjectAction(DestroyObjectAction action) {
        unsupported(action.eClass.name)
    }

    def dispatch CharSequence doGenerateAction(CreateLinkAction action) {
        generateCreateLinkAction(action)
    }

    def generateCreateLinkAction(CreateLinkAction action) {
        unsupported(action.eClass.name)
    }

    def generateDestroyLinkAction(DestroyLinkAction action) {
        unsupported(action.eClass.name)
    }

    override generateActivity(Activity activity) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

}
