package com.abstratt.mdd.target.mean

import com.abstratt.kirra.SchemaManagement
import com.abstratt.mdd.core.RepositoryService
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.target.mean.mongoose.DomainModelGenerator
import java.util.List
import org.eclipse.uml2.uml.Class

class MEANPlatform implements ITopLevelMapper<Class> {
    
    override mapFileName(Class element) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override map(Class toMap) {
        new DomainModelGenerator().generateEntity(toMap)
    }
    
    def getCurrentSchema() {
        RepositoryService.DEFAULT.currentResource.getFeature(SchemaManagement)
    }
    
    override mapAll(List<Class> toMap) {
        toMap.map[map].join('\n')
    }
}
