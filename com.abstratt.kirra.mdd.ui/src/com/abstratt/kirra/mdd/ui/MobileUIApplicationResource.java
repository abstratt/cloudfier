package com.abstratt.kirra.mdd.ui;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITopLevelMapper;


/**
 * Provides access to an application session.
 */
public class MobileUIApplicationResource extends AbstractUIResource<org.eclipse.uml2.uml.Package> {
	@Override
	protected String getTemplateFileName() {
		return "main-mobile-js.gt";
	}
	
	@Override
	protected String map(IRepository coreRepository, ITopLevelMapper<org.eclipse.uml2.uml.Package> mapper) {
		return mapNamespaces(coreRepository, mapper);
	}

}