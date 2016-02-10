package com.abstratt.mdd.target.jee

import java.util.Collection
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.UMLPackage.Literals

import static extension com.abstratt.mdd.core.util.ActivityUtils.*

class JPAHelper {
    def static boolean isQueryPerformingActivity(Activity activity) {
        if (activity.activityReturnParameter == null)
            return false
        !activity.bodyNode.findMatchingActions(UMLPackage.Literals.READ_EXTENT_ACTION).empty
    }
        
    def static CharSequence generateQueryExecutionMethod(MultiplicityElement element) {
        if (element != null) {
            if (element.multivalued) 'getResultList()' else 'setMaxResults(1).getResultList().stream().findAny().orElse(null)'
        } else {
            'executeUpdate()'
        }
    } 
    
    @Deprecated
    def static CharSequence getAlias(Type classifier) {
    	// this is too naive, may need to refer to two objects of the same type, each need their own alias.
        '''«classifier.name.toFirstLower»_'''
    }
    def dispatch static CharSequence alias(OutputPin pin) {
    	val source = new DataFlowAnalyzer().findSource(pin)
		return generateAlias(if (source == null) pin else source)
    }
	
    def dispatch static CharSequence alias(InputPin pin) {
    	(pin.source as OutputPin).alias
    }
    
	def static CharSequence generateAlias(OutputPin pin) {
		generateAlias(pin.owningAction, pin )	
	}

	def dispatch static CharSequence generateAlias(ReadVariableAction action, OutputPin pin) {
		'''«action.variable.name»_'''	
	}
	def dispatch static CharSequence generateAlias(ReadExtentAction action, OutputPin pin) {
		'''«action.classifier.name.toFirstLower»_'''	
	}
	def dispatch static CharSequence generateAlias(ReadLinkAction action, OutputPin pin) {
		'''«action.endData.head.end.name»'''	
	}
	def dispatch static CharSequence generateAlias(ReadStructuralFeatureAction action, OutputPin pin) {
		action.structuralFeature.name	
	}
	def dispatch static CharSequence generateAlias(ReadSelfAction action, OutputPin pin) {
		return ActivityContext.generateSelf
	}
	
	def static Collection<Action> findInstanceProducingActions(Activity activity) {
		activity.rootAction.findMatchingActions(Literals.READ_EXTENT_ACTION, Literals.READ_LINK_ACTION, Literals.READ_STRUCTURAL_FEATURE_ACTION);
	} 
}