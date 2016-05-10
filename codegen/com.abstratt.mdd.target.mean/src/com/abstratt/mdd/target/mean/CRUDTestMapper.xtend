package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

class CRUDTestMapper implements ITopLevelMapper<Class> {
    
    override mapAll(IRepository repo) {
        #{ 'test/CRUD.js' -> new CRUDTestGenerator(repo).generateTests }
    }
}
