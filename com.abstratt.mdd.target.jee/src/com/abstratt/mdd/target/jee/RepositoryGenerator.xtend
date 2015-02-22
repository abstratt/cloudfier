package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.NamedElement

class RepositoryGenerator extends AbstractJavaGenerator {

    protected IRepository repository

    protected String applicationName

    new(IRepository repository) {
        super(repository)
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