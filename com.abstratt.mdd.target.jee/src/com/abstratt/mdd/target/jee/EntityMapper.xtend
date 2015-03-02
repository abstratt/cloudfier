package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Classifier

class EntityMapper extends com.abstratt.mdd.target.jse.EntityMapper {
    override EntityGenerator createEntityGenerator(IRepository repository) {
        new EntityGenerator(repository)
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val mappings = super.mapAll(repository)
        
        
        val entities = appPackages.entities
        
        val crudTestGenerator = new CRUDTestGenerator(repository)
        mappings.putAll(entities.toMap[generateCRUDTestFileName].mapValues[crudTestGenerator.generateCRUDTestClass(it)])

        val repositoryGenerator = new RepositoryGenerator(repository)
        mappings.putAll(entities.toMap[generateRepositoryFileName].mapValues[repositoryGenerator.generateRepository(it)])
        
        return mappings 
    }
    
    def generateCRUDTestFileName(Classifier entityClass) {
        '''src/test/java/«entityClass.namespace.name»/«entityClass.name»CRUDTest.java'''.toString
    }

    def generateRepositoryFileName(Classifier entityClass) {
        '''src/main/java/«entityClass.namespace.name»/«entityClass.name»Repository.java'''.toString
    }
}
