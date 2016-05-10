package com.abstratt.mdd.target.jse

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.spi.TargetUtils
import java.io.InputStream
import java.util.LinkedHashMap
import org.eclipse.uml2.uml.Class

import static com.abstratt.kirra.mdd.core.KirraHelper.*

import static extension com.abstratt.mdd.target.base.MapperHelper.*

class ApplicationMapper implements ITopLevelMapper<Class> {
    
    def InputStream getTemplateContents(String path) {
        return getClass().getTemplateContents(path)
    } 
    
    override mapAll(IRepository repository) {
        val applicationName = getApplicationName(repository)
        val applicationDescription = getApplicationLabel(repository)
        val replacements = newLinkedHashMap(
            'applicationName' -> KirraHelper.getApplicationName(repository),
            'applicationDescription' -> applicationDescription,
            'groupId' -> applicationName,
            'groupPath' -> applicationName,
            'artifactId' -> applicationName,
            'version' -> '1.0'
        )
        repository.properties.forEach[key, value| replacements.put(key.toString(), value.toString)]
        val result = new LinkedHashMap<String, CharSequence>()
        result.putAll(#['index.html'].toInvertedMap[ name | TargetUtils.merge(getTemplateContents(name), replacements)])
        return result
    }    
}
