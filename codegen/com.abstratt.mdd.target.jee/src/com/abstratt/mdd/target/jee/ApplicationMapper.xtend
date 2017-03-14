package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.spi.TargetUtils

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import com.abstratt.kirra.mdd.core.KirraMDDConstants

class ApplicationMapper extends com.abstratt.mdd.target.jse.ApplicationMapper {
    override mapAll(IRepository repository) {
        val result = super.mapAll(repository)
        val properties = repository.properties
        val applicationName = repository.applicationName
        val jdbcProductionUsername = properties.getProperty("mdd.generator.jpa.jdbc.production.username", "cloudfier")
        val jdbcProductionPassword = properties.getProperty("mdd.generator.jpa.jdbc.production.password", "password")
        val jdbcTestUsername = properties.getProperty("mdd.generator.jpa.jdbc.test.username", jdbcProductionUsername)
        val jdbcTestPassword = properties.getProperty("mdd.generator.jpa.jdbc.test.password", jdbcProductionPassword)
        val defaultJdbcProductionUrl = '''jdbc:hsqldb:mem:«applicationName»;user=«jdbcProductionUsername»;password=«jdbcProductionPassword»'''
        val defaultJdbcTestUrl = '''jdbc:hsqldb:mem:«applicationName»;user=«jdbcTestUsername»;password=«jdbcTestPassword»'''
        
        val loginRequired = Boolean.parseBoolean(repository.properties.getOrDefault(KirraMDDConstants.LOGIN_REQUIRED, Boolean.toString(false)) as String)
        
        val explicitJdbcProductionUrl = properties.getProperty("mdd.generator.jpa.jdbc.production.url")
        val explicitJdbcTestUrl = properties.getProperty("mdd.generator.jpa.jdbc.test.url")
		val jdbcProductionUrl = explicitJdbcProductionUrl ?: defaultJdbcProductionUrl
		val jdbcTestUrl = explicitJdbcTestUrl ?: defaultJdbcTestUrl
		val jpaPreserveSchema = properties.getProperty("mdd.generator.jpa.preserveSchema", Boolean.toString(explicitJdbcProductionUrl != null))
		val jpaPreserveData = properties.getProperty("mdd.generator.jpa.preserveData", Boolean.toString(explicitJdbcProductionUrl != null))
        val preserveSchema = Boolean.valueOf(jpaPreserveSchema)
        val preserveData = Boolean.valueOf(jpaPreserveData)
        val templates = #{
            'src/main/resources/META-INF/persistence.xml' -> null, 
            'src/main/resources/META-INF/orm.xml' -> null,
            'src/main/resources/log4j.properties' -> null,
            'src/main/assemble/assembly.xml' -> null,
            'src/main/java/util/PersistenceHelper.java' -> null,
            'src/main/java/resource/util/StandaloneRequestResponseFilter.java' -> null,
            'src/main/java/resource/util/LoginLogoutResource.java' -> null,
            'src/main/java/resource/util/Authenticator.java' -> null,
            'src/main/java/resource/util/ContainerRequestResponseFilter.java' -> null,            
            'src/main/java/resource/util/EntityManagerProvider.java' -> null,
            'src/main/java/resource/util/EntityResourceHelper.java' -> null,
        	'src/main/java/resource/applicationName/RESTServer.java' -> '''src/main/java/resource/«applicationName»/RESTServer.java''',
        	'src/main/resources/META-INF/sql/create.sql' -> null,
	        'src/main/resources/META-INF/sql/drop.sql' -> null
        }
		val replacements = newLinkedHashMap(
            'applicationName' -> applicationName,
            'jdbc.production.url' -> jdbcProductionUrl,
            'jdbc.production.user' -> jdbcProductionUsername,
            'jdbc.production.password' -> jdbcProductionPassword,
            'jdbc.test.url' -> jdbcTestUrl,
            'jdbc.test.user' -> jdbcTestUsername,
            'jdbc.test.password' -> jdbcTestPassword,
            'jpa.preserveSchema' -> Boolean.toString(preserveSchema),
            'jpa.preserveData' -> Boolean.toString(preserveData),
            'loginRequired' -> Boolean.toString(loginRequired)
        )
        
        templates.forEach[templatePath, outputPath | 
        	result.put(outputPath ?: templatePath, TargetUtils.merge(getTemplateContents(templatePath), replacements))
        ]
        return result
    }
}
