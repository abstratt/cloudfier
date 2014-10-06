package com.abstratt.mdd.core.target.spi;

import com.abstratt.mdd.core.target.ITopLevelMapper;

/**
 * A transformation engine.
 */
public interface ITransformationEngine {
    public ITopLevelMapper createLanguageMapper(CustomTargetPlatform customTargetPlatform);
}
