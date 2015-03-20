package com.abstratt.mdd.target.jee

import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.UMLPackage
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.TypedElement

class JPAHelper {
    def static boolean isQueryPerformingActivity(Activity activity) {
        if (activity.activityReturnParameter == null)
            return false
        !activity.bodyNode.findMatchingActions(UMLPackage.Literals.READ_EXTENT_ACTION).empty
    }
        
    def static CharSequence generateQueryExecutionMethod(MultiplicityElement element) {
        if (element != null) {
            if (element.multivalued) 'getResultList()' else 'getResultList().stream().findAny().orElse(null)'
        } else {
            'executeUpdate()'
        }
    } 
}