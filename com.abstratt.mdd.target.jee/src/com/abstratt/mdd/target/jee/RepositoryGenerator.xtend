package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.NamedElement

import static extension org.apache.commons.lang3.text.WordUtils.*

class RepositoryGenerator extends AbstractGenerator {

    protected IRepository repository

    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }

    def generateComment(Element element) {
        if (!element.ownedComments.empty) {
            val reformattedParagraphs = element.ownedComments.head.body.replaceAll('\\s+', ' ').wrap(120, '<br>', false).
                split('<br>').map['''* «it»'''].join('\n')
            '''
                /**
                 «reformattedParagraphs»
                 */
            '''
        }
    }

    def generateRepository(Class entity) {
        val packageSuffix = entity.namespace.qualifiedName.replace(NamedElement.SEPARATOR, ".")
        '''
            package repository.«entity.packageSuffix»;
            
            import entity.«packageSuffix».*;
            
            import java.util.*;
            import javax.persistence.*;
            import javax.ejb.*;
            import javax.ejb.*;
                
            /** Repository for «entity.name». */
            public class «entity.name»Repository {
            }
        '''
    }
}