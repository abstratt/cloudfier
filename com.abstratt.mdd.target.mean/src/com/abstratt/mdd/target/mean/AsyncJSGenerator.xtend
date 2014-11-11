package com.abstratt.mdd.target.mean

import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Operation

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import com.abstratt.mdd.target.mean.ActivityContext.Stage
import org.eclipse.uml2.uml.StructuredActivityNode

class AsyncJSGenerator extends JSGenerator {
    
    final protected ApplicationContext application = new ApplicationContext


    override generateActivityRootAction(Activity activity) {
        val action = if(activity.specification instanceof Operation && !activity.specification.static &&
                (activity.specification as Operation).action) (activity.specification as Operation) else null
        val isInstanceActionOperation = action != null
        application.newActivityContext(activity)
        try {
            if (!application.isAsynchronous(activity)) {
                return super.generateActivityRootAction(activity)
            }
            application.activityContext.buildPipeline(activity.rootAction)

            //            if (isInstanceActionOperation) 
            //                addActionPrologue(action)
            //            context.stages += '''
            //            function () {
            //                «super.generateActivityRootAction(activity)»
            //            }
            //            '''
            //            if (isInstanceActionOperation) 
            //                addActionEpilogue(action)
            generatePipeline()
        } finally {
            application.dropActivityContext
        }
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
        if (expression)
            return kernel
        val optionalReturn = if (stage.rootAction.outputs.empty) '' else 'return '
        val optionalSemicolon = if (kernel.toString.trim.endsWith(';')) '' else ';'
        '''«optionalReturn»«kernel»«optionalSemicolon»'''
    }
    
    def generateReturn(Action rootAction) {
        val kernel = rootAction.generateAction
        if (rootAction.outputs.empty)
            return kernel
        '''return «kernel»;'''
    }
    
    def generateLeafStage(Stage stage) {
        '''
        q().then(function() {
            «stage.rootAction.generateReturn»
        })'''
    }
    
    def generateStageMultipleChildren(Stage stage) {
        '''
        q().all([«stage.substages.map[generateStage(true)].join(', ')»]).spread(function(«stage.substages.map[alias].join(', ')») {
            «stage.rootAction.generateReturn»
        })'''
    }
    
    def generateStageSingleChild(Stage stage) {
        '''
        «stage.substages.head.generateStage(true)».then(function(«stage.substages.head.alias») {
            «stage.rootAction.generateReturn()»
        })'''
    }
    
    def generatePipelineFrom(Stage rootStage) {
        // always generate a return, caller may be interested in promise
        '''return «context.rootStage.generateStage(false)»'''
    }

    def generatePipeline() {
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

    override generateActionProper(Action toGenerate) {

        //        if (context.isStaged(toGenerate)) {
        //            return context.findStage(toGenerate).alias  
        //        }
        //        val asyncProducers = action.inputs.map[ it.sourceAction ].filter[it.asynchronous]
        //        asyncProducers.forEach [ this.context.stage(it, it.eClass.name.toFirstLower) ]
        val stage = context.findStage(toGenerate)
        if (application.isAsynchronous(context.activity)) {
            if (stage.rootAction == toGenerate) {
                if (stage.generated)
                    return context.findStage(toGenerate).alias
                stage.markGenerated
            }
        }
        '''«super.generateActionProper(toGenerate)»'''
    }
}
