package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Property

class JAXRSResourceGenerator extends BehaviorlessClassGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateResource(Class entity) {
        '''
        package resource.«entity.packagePrefix»;
        
        import «entity.packagePrefix».*;
        
        import java.util.*;
        import java.util.stream.*;
        import javax.ws.rs.GET;
        import javax.ws.rs.PUT;
        import javax.ws.rs.POST;
        import javax.ws.rs.DELETE;
        import javax.ws.rs.Path;
        import javax.ws.rs.PathParam;
        import javax.ws.rs.Produces;
        import javax.ws.rs.core.MediaType;
        import javax.ws.rs.core.Response;
        import javax.ws.rs.core.Response.Status;
        
        
        «entity.generateImports»
        
        @Path("«entity.name.toLowerCase»")
        @Produces("application/json")
        public class «entity.name»Resource {
                private «entity.name»Service service = new «entity.name»Service();
                @GET
                public Response getSingle(@PathParam("id") Long id) {
                    «entity.name» found = service.find(id);
                    if (found == null)
                        return Response.status(Response.Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                    «entity.name»Element result = new «entity.name»Element();
                    «entity.properties.map[
                        '''result.«name» = «getModelValue(it, 'found')»;'''
                    ].join('\n')» 
                    return Response.ok(result, MediaType.APPLICATION_JSON).build();
                }
                @GET
                public Response getList() {
                    Collection<«entity.name»> models = service.findAll();
                    
                    Collection<«entity.name»Element> items = models.stream().map(toMap -> {
                        «entity.name»Element item = new «entity.name»Element();
                        «entity.properties.map[
                            '''item.«name» = «getModelValue(it, 'toMap')»;'''
                        ].join('\n')»
                        return item;
                    }).collect(Collectors.toList());
                    
                    Map<String, Object> result = new LinkedHashMap<String, Object>();
                    result.put("items", items);  
                    return Response.ok(result, MediaType.APPLICATION_JSON).build();
                }
        }
        '''
    }
    
    def getModelValue(Property property, String varName) {
         '''«varName».«property.generateAccessorName»()«IF property.type.enumeration».name()«ENDIF»'''
    }
    
}