package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.Enumeration

class JAXBElementGenerator extends BehaviorlessClassGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateElement(Class entity) {
        '''
        package resource.«entity.packagePrefix»;
        
        import java.util.*;
            import javax.xml.bind.annotation.*;
        
        @XmlRootElement
        public class «entity.name»Element {
            public String uri;
            «entity.properties.map['''
                public «it.type.toJaxbType» «it.name»;
            '''].join()»
        }
        '''
    }
    
    def String toJaxbType(Type type) {
        if (type.enumeration)
            'String'
        else
            type.toJavaType
    }
    
}