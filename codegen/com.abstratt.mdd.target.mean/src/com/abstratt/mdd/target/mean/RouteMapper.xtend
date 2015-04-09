package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

class RouteMapper implements ITopLevelMapper<Class> {
    
    final String ROUTES_JS = "routes.js"
    
    override mapFileName(Class element) {
        ""
    }
    
    override mapAll(IRepository repo) {
        #{
            (ROUTES_JS) -> 
            new RouteGenerator(repo).generateRoutes
        }
    }
    
    override map(Class toMap) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(List<Class> toMap) {
        throw new UnsupportedOperationException
    }
    
    override canMap(Class element) {
        throw new UnsupportedOperationException
    }
}
