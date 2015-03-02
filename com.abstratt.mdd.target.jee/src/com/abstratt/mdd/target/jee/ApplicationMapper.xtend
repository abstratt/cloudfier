package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.spi.TargetUtils

class ApplicationMapper extends com.abstratt.mdd.target.jse.ApplicationMapper {
    override mapAll(IRepository repository) {
        val result = super.mapAll(repository)
        result.putAll(#['src/main/webapp/WEB-INF/web.xml'].toInvertedMap[ name | TargetUtils.renderStaticResource(getTemplateContents(name))])
        return result
    }
}
