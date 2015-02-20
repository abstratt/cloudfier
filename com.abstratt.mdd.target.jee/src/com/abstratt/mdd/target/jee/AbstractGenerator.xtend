package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.google.common.base.Function
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.NamedElement

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class AbstractGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }
    
    def String packageSuffix(Class contextual) {
        contextual.nearestPackage.toJavaPackage
    }
    
    def String toJavaPackage(org.eclipse.uml2.uml.Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }
    
        
    def static <I> CharSequence generateMany(Iterable<I> items, Function<I, CharSequence> mapper) {
        return items.map[mapper.apply(it)].join('\n')
    }
}