package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import org.eclipse.uml2.uml.ReadExtentAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.ReadSelfAction

class QueryFragmentGenerator extends AbstractJavaBehaviorGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    override generateReadSelfAction(ReadSelfAction action) {
        "context"
    }
}