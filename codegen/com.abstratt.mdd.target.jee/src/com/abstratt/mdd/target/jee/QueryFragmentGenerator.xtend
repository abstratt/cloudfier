package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import static extension com.abstratt.mdd.core.util.ActivityUtils.*

class QueryFragmentGenerator extends AbstractJavaBehaviorGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    override generateReadSelfAction(ReadSelfAction action) {
        "context"
    }
    
    def generateReadPropertyActionViaDerivation(ReadStructuralFeatureAction action) {
        val property = action.structuralFeature as Property
        val derivation = property.defaultValue.resolveBehaviorReference as Activity
        derivation.generateDerivation(action.object.source as OutputPin)
    }
    
    def generateDerivation(Activity derivation, OutputPin self) {
    	ActivityContext.generateInNewContext(derivation, self, [
			generateAction(derivation.findSingleStatement)
		])
    }
    
}