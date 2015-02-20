package com.abstratt.mdd.target.mean

import com.abstratt.mdd.target.mean.ActivityContext.Stage
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.SendSignalAction
import com.google.common.base.Function

class AsyncJSGenerator extends JSGenerator {
    
    final protected ApplicationContext application = new ApplicationContext


    def generateInNewContext(Activity activity, Function<?,CharSequence> toRun) {
        application.newActivityContext(activity)
        try {
            return toRun.apply(null)
        } finally {
            application.dropActivityContext
        }
    }

    override generateActivityRootAction(Activity activity) {
        generateInNewContext(activity, [generateActivityRootActionInCurrentContext])
    }
    
    def generateActivityRootActionInCurrentContext() {
        val activity = application.activityContext.activity 
        if (!application.isAsynchronous(activity))
            return '''«super.generateActivityRootAction(activity)»'''
        application.activityContext.buildPipeline(activity.rootAction)
        '''«generatePipeline()»'''
    }
    
    def ActivityContext getContext() {
        application.activityContext
    }
    
    def CharSequence generateStage(Stage stage, boolean expression) {
        val kernel = switch (stage.substages.size) {
            case 0 : generateLeafStage(stage)
            case 1 : generateStageSingleChild(stage)
            default : generateStageMultipleChildren(stage)
        }
        val optionalReturn = if (expression) '' else 'return '
        val optionalSemicolon = if (expression || kernel.toString.trim.endsWith(';')) '' else ';'
        '''
        «optionalReturn»«decorateStage(stage, kernel)»«optionalSemicolon»
        '''
    }
    
    def CharSequence decorateStage(Stage stage, CharSequence output) {
        output
    }
    
    
    def generateReturn(Action rootAction) {
        val kernel = rootAction.generateAction
        if (rootAction.outputs.empty && !application.isAsynchronous(rootAction)) {
            val optionalSemicolon = if (kernel.toString.trim.endsWith(';')) '' else ';'
            return '''
            «kernel»«optionalSemicolon»
            '''
        }
        '''return «kernel»;'''
    }
    
    def generateLeafStage(Stage stage) {
        val kernel = stage.rootAction.generateReturn
        '''
        Q().then(function() {
            «kernel.dump»
            «kernel»
        })'''
    }
    
    def generateStageMultipleChildren(Stage stage) {
        val rootAction = stage.rootAction
        if (rootAction instanceof StructuredActivityNode)
            // ignore mustIsolate (which TextUML should be generating but may not)
            return generateStageMultipleChildrenSequential(stage)
        return generateStageMultipleChildrenParallel(stage)
    }
    
    def boolean isNoOpStage(Stage toCheck) {
        // only no-op case so far involves casting (JS does not need casting)
        if (!toCheck.rootAction.cast)
            return false
        val sourceActionStage = context.findStage(toCheck.rootAction.sourceAction)
        return sourceActionStage != null && sourceActionStage != toCheck
    }
    
    def generateStageMultipleChildrenSequential(Stage stage) {
        '''Q()«stage.substages.filter[!isNoOpStage(it)].map[generateStage(false)].map[
        '''
        .then(function() {
            «it»
        })'''.toString.trim].join('')»''' 
    }
    
    def generateStageMultipleChildrenParallel(Stage stage) {
        '''
        Q.all([
            «stage.substages.map[generateStage(true).toString.trim].join(',\n')»
        ]).spread(function(«stage.substages.map[alias].join(', ')») {
            «stage.rootAction.generateReturn»
        })'''
    }
    
    def generateStageSingleChild(Stage stage) {
        val singleChild = stage.substages.head
        val isBlock = stage.rootAction instanceof StructuredActivityNode
        val childKernel = singleChild.generateStage(true)
        val thisKernel = stage.rootAction.generateReturn()
        '''
        «childKernel.toString.trim()»«IF !isBlock».then(function(«singleChild.alias») {
            «dump(thisKernel)»
            «thisKernel»
        })«ENDIF»'''
    }
    
    def generatePipelineFrom(Stage rootStage) {
        val rootStageVariables = context.findVariables
        val specification = context.activity.specification
        val generated = context.rootStage.generateStage(false)
        val preconditions = if (specification instanceof Operation) specification.preconditions else #[]
        '''
        «IF !rootStageVariables.empty»
        «generateVariableBlock(rootStageVariables)»
        «ENDIF» 
        «IF !context.activity.activityStatic && !context.activity.isConstraintBehavior»
        var me = this;
        «ENDIF»
        «IF !preconditions.empty»
        return Q()«(context.activity.specification as Operation).preconditions.map[ constraint |
            '''
            .then(«generatePredicate(constraint).toString.trim»).then(function(pass) {
                if (!pass) {
                    var error = new Error("Precondition violated: «(if (!constraint.description.nullOrEmpty) constraint.description else constraint.name).escapeString('"')» (on '«specification.qualifiedName.escapeString('"')»')");
                    error.context = '«specification.qualifiedName»';
                    error.constraint = '«constraint.name»';
                    «IF !constraint.description.nullOrEmpty»
                    error.description = «constraint.description.generateSingleQuoteString»;
                    «ENDIF»
                    throw error;
                }    
            })'''.toString.trim
        ].join()».then(function(/*noargs*/) {
            «generated»
        });
        «ELSE»
        «generated»
        «ENDIF»
        '''
    }
    
    override generateVariables(StructuredActivityNode node) {
        // do not declare variables in async blocks/actions, as those need to be in the 
        // shared closure for all blocks
        if (application.isAsynchronous(node.actionActivity)) '' else super.generateVariables(node)
    }

    def generatePipeline() {
        if (context.activity != null && !application.isAsynchronous(context.activity))
            throw new IllegalStateException
        if (context.stagedActions.empty)
            return ''
        if (context.rootStage.substages.size == 1)
            return generatePipelineFrom(context.rootStage.substages.get(0))
        generatePipelineFrom(context.rootStage)        
    }

    def void addActionPrologue(Operation action) {
    }

    def void addActionEpilogue(Operation action) {
    }
    
    override generateSelfReference() {
        if (!this.application.asynchronousContext)
            ''' «super.generateSelfReference»'''
        else
            'me'
    }
        
    override generateSendSignalAction(SendSignalAction action) {
        super.generateSendSignalAction(action)
    }

    override generateActionProper(Action toGenerate) {
        val stage = context.findStage(toGenerate)
        if (application.isAsynchronous(context.activity)) {
            if (stage.rootAction == toGenerate) {
                if (stage.generated) {
                    val previouslyGeneratedStage = context.findStage(toGenerate)
                    return if (previouslyGeneratedStage.producer) previouslyGeneratedStage.alias else '/*sink*/'
                }
                stage.markGenerated
            }
        }
        '''«super.generateActionProper(toGenerate)»'''
    }
}
