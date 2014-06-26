package com.abstratt.kirra.mdd.ui;

import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITopLevelMapper;

/**
 * Provides access to an application session.
 */
public class UIModuleResource extends AbstractUIResource<org.eclipse.uml2.uml.Class> {

    @Override
    protected String getTemplateFileName() {
        return "module-js.gt";
    }

    @Override
    protected String map(IRepository coreRepository, ITopLevelMapper<org.eclipse.uml2.uml.Class> mapper) {
        org.eclipse.uml2.uml.Class clazz = coreRepository.findNamedElement(getClassName(), UMLPackage.Literals.CLASS, null);
        return mapElement(coreRepository, mapper, clazz);
    }

    private String getClassName() {
        return (String) getRequestAttributes().get("className");
    }
}