package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Activity
import java.util.List
import org.eclipse.uml2.uml.Parameter

class BehaviorlessClassGenerator extends PlainJavaGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    override generateActivity(Activity a) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
}