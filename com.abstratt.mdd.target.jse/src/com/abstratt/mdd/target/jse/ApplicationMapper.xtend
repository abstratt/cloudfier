package com.abstratt.mdd.target.jse

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.spi.TargetUtils
import java.util.LinkedHashMap
import java.util.List
import org.eclipse.uml2.uml.Class
import java.io.InputStream

class ApplicationMapper implements ITopLevelMapper<Class> {
    
    def InputStream getTemplateContents(String path) {
        getClass().getResourceAsStream('''/templates/«path»''')
    } 
    
    override mapFileName(Class element) {
        throw new UnsupportedOperationException
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
    
    override mapAll(IRepository repository) {
        val applicationPackages = KirraHelper.getApplicationPackages(repository.getTopLevelPackages(null))
        val applicationDescription = KirraHelper.getApplicationName(repository, applicationPackages)
        val replacements = newLinkedHashMap(
            'applicationName' -> applicationPackages.head.name,
            'applicationDescription' -> applicationDescription,
            'groupId' -> (applicationPackages.head?.namespace?.qualifiedName ?: 'undefined'),
            'artifactId' -> applicationPackages.head.name
        )
        repository.properties.forEach[key, value| replacements.put(key.toString(), value.toString)]
        val result = new LinkedHashMap<String, CharSequence>()
        result.putAll(#['pom.xml'].toInvertedMap[ name | TargetUtils.merge(getTemplateContents(name), replacements)])
        return result
    }    
}
