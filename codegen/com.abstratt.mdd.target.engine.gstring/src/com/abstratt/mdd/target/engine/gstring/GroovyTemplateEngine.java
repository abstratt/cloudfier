package com.abstratt.mdd.target.engine.gstring;

import java.net.URI;

import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.spi.CustomTargetPlatform;
import com.abstratt.mdd.core.target.spi.ITransformationEngine;

public class GroovyTemplateEngine implements ITransformationEngine {

    @Override
    public ITopLevelMapper createLanguageMapper(CustomTargetPlatform platform) {
        return new GroovyLanguageMapper(platform.getProperties());
    }

}
