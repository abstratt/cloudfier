package com.abstratt.mdd.target.tests;

import com.abstratt.kirra.mdd.core.KirraMDDCore;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import java.util.Properties;

@SuppressWarnings("all")
public abstract class AbstractGeneratorTest extends AbstractRepositoryBuildingTests {
  public AbstractGeneratorTest(final String name) {
    super(name);
  }
  
  public Properties createDefaultSettings() {
    final Properties defaultSettings = super.createDefaultSettings();
    String _string = Boolean.TRUE.toString();
    defaultSettings.setProperty("mdd.enableKirra", _string);
    defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER);
    String _string_1 = Boolean.TRUE.toString();
    defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, _string_1);
    return defaultSettings;
  }
}
