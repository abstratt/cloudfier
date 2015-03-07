package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.ReadVariableAction
import java.util.Set
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.Action

class JPABehaviorGenerator extends PlainJavaBehaviorGenerator {
    PlainJavaBehaviorGenerator plainJavaBehaviorGenerator
    
    new(IRepository repository) {
        super(repository)
        this.plainJavaBehaviorGenerator = new PlainJavaBehaviorGenerator(repository)
    }

    override generateProviderReference(Classifier context, Classifier provider) {
        '''new «provider.name.toFirstUpper»Service()'''
    }
    
        
    override generateReadExtentAction(ReadExtentAction action) {
        val classifier = action.classifier
        val providerReference = generateProviderReference(action.actionActivity.behaviorContext, classifier)
        '''«providerReference».findAll()'''
    }
    
    override generateCreateObjectAction(CreateObjectAction action) {
        val classifier = action.classifier
        val core = super.generateCreateObjectAction(action)
//        if (classifier.entity)
//            '''«generateProviderReference(action.actionActivity.behaviorContext, classifier)».create(«core»)'''
//        else
            core
    }

    override generateTraverseRelationshipAction(InputPin target, Property property) {
        if (property.navigable)
            return super.generateTraverseRelationshipAction(target, property)
        else
        // use service to get related instances
        '''«generateProviderReference(target.owningAction.actionActivity.behaviorContext, property.type as Classifier)».find«property.name.toFirstUpper»By«property.otherEnd.name.toFirstUpper»(«target.generateAction»)'''        
    }
    
    override generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        val core = super.generateStructuredActivityNodeAsBlock(node)
        
        if (node == node.owningActivity.rootAction || node.shouldIsolate) {
            val currentEntity = node.owningActivity.behaviorContext
            val creationVars = node.findMatchingActions(UMLPackage.Literals.CREATE_OBJECT_ACTION)
                .map[it as CreateObjectAction]
                .filter[
                    classifier.entity && 
                    targetAction instanceof AddVariableValueAction && 
                    !targetAction.returnAction
                ]
                .map[(targetAction as AddVariableValueAction).variable]
                
            val workingSet = creationVars.map[type -> name]
            '''
            «core»
            «workingSet.generateMany[pair | '''«generateProviderReference(currentEntity, pair.key as Classifier)».create(«pair.value»);''']»
            «IF node.shouldIsolate»
            util.PersistenceHelper.flush(true);
            «ENDIF»
            «IF node == node.owningActivity.rootAction && node.owningActivity.operation != null && !node.owningActivity.operation.query && node.findStatements.last?.returnAction»
                «super.generateStatement(node.findStatements.last)»
            «ENDIF»
            '''
        } else 
            core
    }

    override generateStatement(Action statementAction) {
        if (statementAction.returnAction && statementAction.owningActivity.operation != null && !statementAction.owningActivity.operation.query)
            // generate the return action later, after we ensure we persisted any created objects 
            ''
        else
            super.generateStatement(statementAction)
    }
    
}
