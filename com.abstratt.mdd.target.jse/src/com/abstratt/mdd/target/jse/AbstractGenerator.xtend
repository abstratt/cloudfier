package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import java.util.Collection
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Package

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

abstract class AbstractGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    protected Collection<Package> appPackages
    
    new(IRepository repository) {
        this.repository = repository
        this.appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }
    
    def boolean isCollectionOperation(Action toCheck) {
        if (toCheck instanceof CallOperationAction)
            return toCheck.target != null && toCheck.target.multivalued
        return false
    } 
    
    
}