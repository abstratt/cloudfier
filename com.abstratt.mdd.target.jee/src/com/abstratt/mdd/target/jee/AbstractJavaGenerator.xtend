package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension org.apache.commons.lang3.text.WordUtils.*

abstract class AbstractJavaGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel]
    }

    def generateComment(Element element) {
        if(!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).split('<br>').map[ '''* «it»''' ].join('\n')
            '''
            /**
             «reformattedParagraphs»
             */
            '''
        }
    }
    
    
    def String packageSuffix(Class contextual) {
        contextual.nearestPackage.toJavaPackage
    }
    
    def String toJavaPackage(Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }
    
        
    def static <I> CharSequence generateMany(Iterable<I> items, (I) => CharSequence mapper) {
        return items.generateMany(mapper, '\n')
    }
    
    def static <I> CharSequence generateMany(Iterable<I> items, (I) => CharSequence mapper, String separator) {
        return items.map[mapper.apply(it)].join(separator)
    }
    
}