package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class
import com.abstratt.kirra.mdd.core.KirraHelper

class RouteMapper implements ITopLevelMapper<Class> {
    
    override mapFileName(Class element) {
        '''routes/routesFor«element.name».js'''
    }
    
    override map(Class toMap) {
        new RouteGenerator().generateRoute(toMap)
    }
    
    override mapAll(List<Class> toMap) {
        toMap.map[map].join('\n')
    }
    
    override canMap(Class element) {
        KirraHelper.isEntity(element) || KirraHelper.isService(element)
    }
    
}
