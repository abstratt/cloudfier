package com.abstratt.mdd.target.jee

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.spi.TargetUtils
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class JPAEntityMapper extends com.abstratt.mdd.target.jse.EntityMapper {
    
    override JPAEntityGenerator createEntityGenerator(IRepository repository) {
        new JPAEntityGenerator(repository)
    }
    
    override JPAServiceGenerator createServiceGenerator(IRepository repository) {
        new JPAServiceGenerator(repository)
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val entities = appPackages.entities
        val allResourceEntities = entities.filter[userVisible]
        val allRepresentationEntities = entities.filter[userVisible && concrete]
        val persistentEntities = entities
        val roleEntities = entities.filter[role && concrete]
        val applicationName = KirraHelper.getApplicationName(repository)
        val applicationLabel = KirraHelper.getApplicationLabel(repository)
        val crudTestGenerator = new CRUDTestGenerator(repository)
        val jaxRsResourceGenerator = new JAXRSResourceGenerator(repository)
        val jaxbSerializationGenerator = new JAXBSerializationGenerator(repository)
        val apiSchemaGenerator = new KirraAPIResourceGenerator(repository)
        val mappings = super.mapAll(repository)
        val entityNames = persistentEntities.map[ TypeRef.sanitize(qualifiedName) ]
        mappings.putAll(persistentEntities.filter[concrete].toMap[generateCRUDTestFileName].mapValues[crudTestGenerator.generateCRUDTestClass(it)])
        mappings.putAll(allResourceEntities.toMap[generateJAXRSResourceFileName].mapValues[jaxRsResourceGenerator.generateResource(it)])
        mappings.putAll(allRepresentationEntities.toMap[generateJAXBSerializationFileName].mapValues[jaxbSerializationGenerator.generateHelpers(it)])
        mappings.put(generateJAXRSApplicationFileName(applicationName), new JAXRSApplicationGenerator(repository).generate())
        mappings.put(generateUserLoginServiceFileName(applicationName), new UserLoginServiceGenerator(repository).generate())
        mappings.put(generateSecurityHelperFileName(applicationName), new SecurityHelperGenerator(repository).generate())
        mappings.put('src/main/webapp/WEB-INF/web.xml', new WebXmlGenerator(repository).generateWebXml())
        mappings.put('src/test/resources/META-INF/sql/data.sql', new HSQLDataSnapshotGenerator(repository).generate())
        mappings.putAll(allResourceEntities.toMap[generateSchemaRepresentationFileName(it)].mapValues[apiSchemaGenerator.generateEntityRepresentation(it)])
        
        val templates = #{
        	'''src/main/java/resource/«applicationName»/RESTServer.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/IndexResource.java'''.toString -> null,
            '''src/main/java/resource/«applicationName»/EntityResource.java'''.toString -> null,        	
        	'''src/main/java/resource/«applicationName»/ConversionException.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/ConstraintViolationExceptionMapper.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/ThrowableMapper.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/RestEasyFailureMapper.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/WebApplicationExceptionMapper.java'''.toString -> null,
        	'''src/main/java/resource/«applicationName»/ContextListener.java'''.toString -> null
    	}
    	templates.forEach[targetPath, sourcePath |
    		mappings.put(
				targetPath,
					TargetUtils.merge(JPAEntityMapper.getResourceAsStream(sourcePath ?: '''/templates/«targetPath.replaceFirst(applicationName, 'applicationName')»'''), 
	                #{ 
	                    "entityNameList" -> entityNames.map['''"«it»"'''].join(', '),
	                    "applicationName" -> applicationName,
	                    "applicationLabel" -> applicationLabel,
	                    "userRoleNames" -> roleEntities.map['''«name».ROLE_ID'''].join(", ")
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
    
	def generateUserLoginServiceFileName(String applicationName) {
		'''src/main/java/resource/«applicationName»/UserLoginService.java'''.toString
	}
    def generateSecurityHelperFileName(String applicationName) {
		'''src/main/java/util/SecurityHelper.java'''.toString
	}
    
}
