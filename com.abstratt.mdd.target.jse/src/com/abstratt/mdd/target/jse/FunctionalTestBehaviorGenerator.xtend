package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Classifier

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.jse.TestUtils.*

class FunctionalTestBehaviorGenerator extends PlainJavaBehaviorGenerator {

    new(IRepository repository) {
        super(repository)
    }

    override generateProviderReference(Classifier context, Classifier provider) {
        '''new «provider.toJavaType»Service()'''
    }
    
    override CharSequence generateBasicTypeOperationCall(CallOperationAction action) {
        if (action.assertion)
            TestUtils.generateAssertOperationCall(action, [generateAction])
        else
            super.generateBasicTypeOperationCall(action)
    }
    
    override generateStatement(Action statementAction) {
        TestUtils.generateStatement(statementAction, [super.generateStatement(statementAction)])
    }
}
