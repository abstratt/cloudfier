package com.abstratt.mdd.target.mean.tests

import com.abstratt.kirra.mdd.core.KirraMDDCore
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests
import java.util.Properties

abstract class AbstractGeneratorTest extends AbstractRepositoryBuildingTests {
    
    new(String name) {
        super(name)
    }
    
    override Properties createDefaultSettings() {
        val defaultSettings = super.createDefaultSettings()
        // so the kirra profile is available as a system package (no need to
        // load)
        defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString())
        // so kirra stereotypes are automatically applied
        defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER)
        // so classes extend Object by default (or else weaver ignores them)
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString())
        return defaultSettings
    }
}
