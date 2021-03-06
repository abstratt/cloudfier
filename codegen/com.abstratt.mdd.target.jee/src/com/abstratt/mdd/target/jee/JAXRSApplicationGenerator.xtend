package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*


class JAXRSApplicationGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generate() {
        val entities = appPackages.entities.filter[userVisible]
        val entityPackages = entities.map[package.name].toSet
        '''
        package resource.«applicationName»;
        
        import java.util.HashSet;
        import java.util.Set;
        
        import resource.userprofile.UserProfileResource;
        import resource.util.LoginLogoutResource;
        
        «entityPackages.map[ appPackage |
        	'''
        	import resource.«appPackage».*;
        	'''
        ].join»
        
        public class Application extends javax.ws.rs.core.Application {
            private Set<Object> services = new HashSet<>();
        
            public Application() {
                «entities.map [ entity | '''
                services.add(new «entity.name»Resource());
                '''].join()»
                services.add(new LoginLogoutResource());
                services.add(new resource.util.StandaloneRequestResponseFilter());
                services.add(new IndexResource());
                services.add(new EntityResource());
                services.add(new ConstraintViolationExceptionMapper());
                services.add(new RestEasyFailureMapper());
                services.add(new WebApplicationExceptionMapper());
                services.add(new ThrowableMapper());
            }
        
            @Override
            public Set<Object> getSingletons() {
                return services;
            }
        }
        '''
    }    
}