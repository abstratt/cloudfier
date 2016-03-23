package com.abstratt.mdd.target.jee

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.spi.TargetUtils

class ApplicationMapper extends com.abstratt.mdd.target.jse.ApplicationMapper {
    override mapAll(IRepository repository) {
        val result = super.mapAll(repository)
        val entityPackages = getEntityPackages(repository.getTopLevelPackages(null))
        val applicationName = getEntities(entityPackages).filter[userVisible].head.package.name
        val templates = #[
            'src/main/resources/META-INF/persistence.xml', 
            'src/main/resources/META-INF/orm.xml',
            'src/main/resources/META-INF/sql/create.sql',
            'src/main/resources/META-INF/sql/drop.sql',
            'src/main/resources/log4j.properties',
            'src/main/assemble/assembly.xml',
            'src/main/java/util/PersistenceHelper.java',
            'src/main/java/resource/util/StandaloneRequestResponseFilter.java',
            'src/main/java/resource/util/Authenticator.java',
            'src/main/java/resource/util/ContainerRequestResponseFilter.java',            
            'src/main/java/resource/util/EntityManagerProvider.java',
            'src/main/java/resource/util/EntityResourceHelper.java'            
        ]
		val replacements = newLinkedHashMap(
            'applicationName' -> applicationName,
            //'jdbc.url' -> 'jdbc:postgresql://127.0.0.1:5432/cloudfier',
            'jdbc.url' -> '''jdbc:hsqldb:mem:«applicationName»;user=cloudfier;password=password''',
            'jdbc.user' -> 'cloudfier',
            'jdbc.password' -> 'password'
        )
        
        result.putAll(templates.toInvertedMap[name|TargetUtils.merge(getTemplateContents(name), replacements)])
        return result
    }
}
