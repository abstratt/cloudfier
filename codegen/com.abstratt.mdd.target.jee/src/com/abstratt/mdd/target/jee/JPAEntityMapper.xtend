package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Classifier
import java.util.List
import org.eclipse.uml2.uml.Class
import com.abstratt.mdd.core.target.spi.TargetUtils
import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper

class JPAEntityMapper extends com.abstratt.mdd.target.jse.EntityMapper {
    
    override JPAEntityGenerator createEntityGenerator(IRepository repository) {
        new JPAEntityGenerator(repository)
    }
    
    override JPAServiceGenerator createServiceGenerator(IRepository repository) {
        new JPAServiceGenerator(repository)
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val entities = appPackages.entities.filter[!abstract]
        val applicationName = entities.head.package.name
        val applicationLabel = KirraHelper.getLabel(entities.head.package)
        val entityNames = entities.map [ TypeRef.sanitize(qualifiedName) ]
        val crudTestGenerator = new CRUDTestGenerator(repository)
        val jaxRsResourceGenerator = new JAXRSResourceGenerator(repository)
        val jaxbSerializationGenerator = new JAXBSerializationGenerator(repository)
        val apiSchemaGenerator = new KirraAPIResourceGenerator(repository)
        val mappings = super.mapAll(repository)
        mappings.putAll(entities.toMap[generateCRUDTestFileName].mapValues[crudTestGenerator.generateCRUDTestClass(it)])
        mappings.putAll(entities.toMap[generateJAXRSResourceFileName].mapValues[jaxRsResourceGenerator.generateResource(it)])
        mappings.putAll(entities.toMap[generateJAXBSerializationFileName].mapValues[jaxbSerializationGenerator.generateHelpers(it)])
        mappings.put(generateJAXRSApplicationFileName(applicationName), new JAXRSApplicationGenerator(repository).generate())
        mappings.put(generateJAXRSServerFileName(applicationName), new JAXRSServerGenerator(repository).generate())
        mappings.put('src/test/resources/META-INF/sql/data.sql', new DataSnapshotGenerator(repository).generate())
        mappings.putAll(entities.toMap[generateSchemaRepresentationFileName(it)].mapValues[apiSchemaGenerator.generateEntityRepresentation(it)])
        
        val templates = #{
        	'''src/main/java/resource/«applicationName»/EntityResource.java'''.toString -> "/templates/src/main/java/resource/EntityResource.java",
        	'''src/main/java/resource/«applicationName»/ConversionException.java'''.toString -> "/templates/src/main/java/resource/ConversionException.java",
        	'''src/main/java/resource/«applicationName»/ConstraintViolationExceptionMapper.java'''.toString -> "/templates/src/main/java/resource/ConstraintViolationExceptionMapper.java",
        	'''src/main/java/resource/«applicationName»/ThrowableMapper.java'''.toString -> "/templates/src/main/java/resource/ThrowableMapper.java",
        	'''src/main/java/resource/«applicationName»/RestEasyFailureMapper.java'''.toString -> "/templates/src/main/java/resource/RestEasyFailureMapper.java"
    	}
    	templates.forEach[targetPath, sourcePath|
    		mappings.put(
				targetPath,
					TargetUtils.merge(JPAEntityMapper.getResourceAsStream(sourcePath), 
	                #{ 
	                    "entityNameList" -> entityNames.map['''"«it»"'''].join(', '),
	                    "applicationName" -> applicationName,
	                    "applicationLabel" -> applicationLabel
	                }
	            )
    		)	
    	]
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
    
    def generateJAXBSerializationFileName(Classifier entityClass) {
        '''src/main/java/resource/«entityClass.namespace.name»/«entityClass.name»JAXBSerialization.java'''.toString
    }
    
    def generateJAXRSApplicationFileName(String applicationName) {
        '''src/main/java/resource/«applicationName»/Application.java'''.toString
    }
    
    def generateJAXRSServerFileName(String applicationName) {
        '''src/main/java/resource/«applicationName»/RESTServer.java'''.toString
    }
}
