package com.abstratt.mdd.target.mean

import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.CreateLinkAction
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.SendSignalAction


import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.VariableAction

class ApplicationContext {
    private Map<Activity, Boolean> synchronism = new LinkedHashMap()

    final protected List<ActivityContext> activityContexts = newLinkedList

    def void newActivityContext(Activity activity) {
        activityContexts.add(new ActivityContext(this, activity))
    }

    def void dropActivityContext() {
        activityContexts.remove(activityContexts.size - 1)
    }

    def ActivityContext getActivityContext() {
        activityContexts.last
    }
    
    def isAsynchronous(Action action) {
        switch (action) {
            ReadLinkAction: {
                val openEndData = action.endData.head
                if (!openEndData.end.linkRelationship)
                    true
                else
                    openEndData.end.propertyAsynchronous
            }
            ReadStructuralFeatureAction:
                (action.structuralFeature as Property).propertyAsynchronous
            CallOperationAction:
                action.operation.methods.exists[(it as Activity).asynchronous]
            CreateObjectAction:
                false
            AddStructuralFeatureValueAction:
                // setting a value/link is cheap - obtaining the value to add may be expensive
                false
            AddVariableValueAction:
                // writing variables is cheap - obtaining the value to add may be expensive
                false
            CreateLinkAction:
                // linking is cheap - obtaining the value to link may be expensive
                false
            ReadExtentAction:
                {
                    false
//                    // only async if used as a return value 
//                    val targetAction = action.result.targetAction
//                    targetAction instanceof AddVariableValueAction && (targetAction as AddVariableValueAction).variable.name.empty
                }  
            SendSignalAction:
                // should be async at least for self-directed messages
                true
            StructuredActivityNode:
                // TODO: revisit me - blocks are important delineating stages
                false
            ReadVariableAction:
                // if we did not write to it yet in this block, it is async (needs refetching)
                // TODO: ideally we would determine whether the write is guaranteed to happen, here we just look for any write, even if path may not be executed 
                !(action.owningBlock.findFirstAccess(action.variable) instanceof AddVariableValueAction)
            default:
                false
        }
    }

    def boolean isPropertyAsynchronous(Property property) {
        if (property.isDerived)
            property.defaultValue?.isBehaviorReference &&
                (property.defaultValue.resolveBehaviorReference as Activity).isAsynchronous
        else
            property.relationship && !property.childRelationship
    }
    
    def boolean isAsynchronousContext() {
        activityContexts.exists[it.activity.asynchronous]
    }

    def boolean isAsynchronous(Activity activity) {
        if (synchronism.containsKey(activity)) {
            val cached = synchronism.get(activity)
            return if(cached == null)
                // yet TBD - assume the worse 
                true 
            else
                cached
        }
        synchronism.put(activity, null)
        val specification = activity.specification
        if (specification instanceof Operation) {
            // if an action or returns an entity instance
            if (!specification.query || specification.getReturnResult?.type?.entity) {
                synchronism.put(activity, true)
                return true
            }
            
            // async preconditions -> async operation
            val preconditions = specification.preconditions
            if (preconditions.filter[it.specification.isBehaviorReference].map[
                it.specification.resolveBehaviorReference].exists[(it as Activity).asynchronous]) {

                // one of the operation preconditions were asynchronous
                synchronism.put(activity, true)
                return true
            }
            // all preconditions are sync, go on to check the activity itself
        }
        val result = activity.allOwnedElements.filter[it instanceof Action].exists [
            isAsynchronous(it as Action)
        ]
        synchronism.put(activity, result)
        return result
    }

    

}
