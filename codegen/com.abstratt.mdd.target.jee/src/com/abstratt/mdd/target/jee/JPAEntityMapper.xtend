package com.abstratt.mdd.target.jee

 import com.abstratt.mdd.core.IRepository
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Classifier
import java.util.List
import org.eclipse.uml2.uml.Class
import com.abstratt.mdd.core.target.spi.TargetUtils
import com.abstratt.kirra.TypeRef

class JPAEntityMapper extends com.abstratt.mdd.target.jse.EntityMapper {
    
    override JPAEntityGenerator createEntityGenerator(IRepository repository) {
        new JPAEntityGenerator(repository)
    }
    
    override JPAServiceGenerator createServiceGenerator(IRepository repository) {
        new JPAServiceGenerator(repository)
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val applicationName = appPackages.head.name
        val entities = appPackages.entities
        val entityNames = entities.map [ TypeRef.sanitize(qualifiedName) ]
        val crudTestGenerator = new CRUDTestGenerator(repository)
        val jaxRsResourceGenerator = new JAXRSResourceGenerator(repository)
        val jaxbElementGenerator = new JAXBElementGenerator(repository)
        val apiSchemaGenerator = new KirraAPIResourceGenerator(repository)
        val mappings = super.mapAll(repository)
        mappings.putAll(entities.toMap[generateCRUDTestFileName].mapValues[crudTestGenerator.generateCRUDTestClass(it)])
        mappings.putAll(entities.toMap[generateJAXRSResourceFileName].mapValues[jaxRsResourceGenerator.generateResource(it)])
        mappings.putAll(entities.toMap[generateJAXBElementFileName].mapValues[jaxbElementGenerator.generateElement(it)])
        mappings.put(generateJAXRSApplicationFileName(applicationName), new JAXRSApplicationGenerator(repository).generate())
        mappings.put(generateJAXRSServerFileName(applicationName), new JAXRSServerGenerator(repository).generate())
        mappings.put('src/main/resources/META-INF/sql/data.sql', new DataSnapshotGenerator(repository).generate())
        // no data snapshot for testing
        mappings.put('src/test/resources/META-INF/sql/data.sql', '')
        mappings.putAll(entities.toMap[generateSchemaRepresentationFileName(it)].mapValues[apiSchemaGenerator.generateEntityRepresentation(it)])
        mappings.put(
            '''src/main/java/resource/«applicationName»/EntityResource.java'''.toString, 
            TargetUtils.merge(
                class.getResourceAsStream("/templates/src/main/java/resource/EntityResource.java"), 
                #{ 
                    "entityNameList" -> entityNames.map['''"«it»"'''].join(', '),
                    "applicationName" -> applicationName
                }
            )
        )        
        return mappings 
    }
    
    def generateSchemaRepresentationFileName(Class entityClass) {
        '''src/main/resources/schema/entities/«entityClass.namespace.name».«entityClass.name».json'''.toString
    }
    
    override findEntitiesWithServices(List<Class> entities) {
        // entities double as repositories as well, so all entities have one
        entities
    }
    
    def generateCRUDTestFileName(Classifier entityClass) {
        '''src/test/java/«entityClass.namespace.name»/«entityClass.name»CRUDTest.java'''.toString
    }
    
    def generateJAXRSResourceFileName(Classifier entityClass) {
        '''src/main/java/resource/«entityClass.namespace.name»/«entityClass.name»Resource.java'''.toString
    }
    
    def generateJAXRSApplicationFileName(String applicationName) {
        '''src/main/java/resource/«applicationName»/Application.java'''.toString
    }
    
    def generateJAXRSServerFileName(String applicationName) {
        '''src/main/java/resource/«applicationName»/RESTServer.java'''.toString
    }
    
    def generateJAXBElementFileName(Classifier entityClass) {
        '''src/main/java/resource/«entityClass.namespace.name»/«entityClass.name»Element.java'''.toString
    }
}
