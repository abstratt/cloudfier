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
        throw new UnsupportedOperationException("This generator cannot generate behavior")
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        throw new UnsupportedOperationException("This generator cannot generate behavior")
    }
    
    override generateActivityAsExpression(Activity toGenerate) {
        throw new UnsupportedOperationException("This generator cannot generate behavior")
    }

    override generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        throw new UnsupportedOperationException("This generator cannot generate behavior")
    }
    
}