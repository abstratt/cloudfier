package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository 
import java.util.List
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator

class JoinActionGenerator extends QueryFragmentGenerator {
    
    new(IRepository repository) {
        super(repository)
    }

    def override CharSequence generateReadStructuralFeatureAction(ReadStructuralFeatureAction action) {
        '''join("«action.structuralFeature.name»")'''
    }
    
    def override CharSequence generateReadLinkAction(ReadLinkAction action) {
        '''join("«action.endData.get(0).end.name»")'''
    }
    
    def override CharSequence generateStructuredActivityNode(StructuredActivityNode action) {
        if (action.objectInitialization) {
            val outputType = action.structuredNodeOutputs.head.type as Classifier
            val List<CharSequence> projections = newLinkedList()
            outputType.allAttributes.forEach[attribute, i |
                projections.add('''«attribute.type.alias».get("«action.structuredNodeInputs.get(i).name»")''')
            ]
            '''cq.multiselect(«projections.join(', ')»)'''
        } else if (isCast(action)) {
            action.inputs.head.sourceAction.generateAction
        } else
            unsupportedElement(action)
    }
    
    def override CharSequence generateAddVariableValueAction(AddVariableValueAction action) {
        action.value.sourceAction.generateAction
    }
    
}