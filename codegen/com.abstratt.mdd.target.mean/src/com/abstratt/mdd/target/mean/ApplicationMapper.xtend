package com.abstratt.mdd.target.mean

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.spi.TargetUtils
import java.util.LinkedHashMap
import java.util.List
import org.eclipse.uml2.uml.Class

class ApplicationMapper implements ITopLevelMapper<Class> {
    override mapAll(IRepository repository) {
        val applicationLabel = KirraHelper.getApplicationLabel(repository)
        val applicationName = KirraHelper.getApplicationName(repository)
        val replacements = #{
            'applicationName' -> applicationName, 
            'applicationDescription' -> applicationLabel
        }
        val result = new LinkedHashMap<String, CharSequence>()
        val getContents = [ String path | ApplicationMapper.getResourceAsStream('''/templates/«path»''') ]
        result.putAll(#['package.json'].toInvertedMap[ name | TargetUtils.merge(getContents.apply(name), replacements)])
        result.putAll(#['server.js', 'helpers.js', 'http-client.js'].toInvertedMap[ name | TargetUtils.renderStaticResource(getContents.apply(name))  ])
        return result
    }    
}
