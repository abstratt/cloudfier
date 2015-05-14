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
        val typeRef = entity.convertType
        val entityFullName = typeRef.fullName
        '''
        package resource.«entity.packagePrefix»;
        
        import «entity.packagePrefix».*;
        
        import java.util.*;
        import java.util.stream.*;
        import java.text.*;
        
        import javax.ws.rs.core.Context;
        import javax.ws.rs.core.UriInfo;
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
        
        import java.net.URI;
        
        «entity.generateImports»
        
        @Path("entities/«entityFullName»/instances")
        @Produces("application/json")
        public class «entity.name»Resource {
                @Context
                UriInfo uri;
            
                private «entity.name»Service service = new «entity.name»Service();
                @GET
                @Path("{id}")
                public Response getSingle(@PathParam("id") Long id) {
                    «entity.name» found = service.find(id);
                    if (found == null)
                        return Response.status(Response.Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                    return Response.ok(toExternalRepresentation(found, uri.getRequestUri().resolve(""), true), MediaType.APPLICATION_JSON).build();
                }
                @GET
                public Response getList() {
                    Collection<«entity.name»> models = service.findAll();
                    URI extentURI = uri.getRequestUri();
                    Collection<Map<String, Object>> items = models.stream().map(toMap -> {
                        return toExternalRepresentation(toMap, extentURI, true);
                    }).collect(Collectors.toList());
                    
                    Map<String, Object> result = new LinkedHashMap<String, Object>();
                    result.put("contents", items);
                    result.put("offset", 0);
                    result.put("length", items.size());  
                    return Response.ok(result, MediaType.APPLICATION_JSON).build();
                }
                
                private Map<String, Object> toExternalRepresentation(«entity.name» toRender, URI instancesURI, boolean full) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    Map<String, Object> values = new LinkedHashMap<>();
                    «IF (entity.properties.exists[type.name == 'Date'])»
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                    «ENDIF»
                    «entity.properties.map[
                        '''values.put("«name»", «getModelValue(it, 'toRender')»);'''
                    ].join('\n')»
                    result.put("values", values);
                    Map<String, Object> links = new LinkedHashMap<>();
                    result.put("links", links);
                    result.put("uri", instancesURI.resolve(toRender.getId().toString()).toString());
                    result.put("entityUri", instancesURI.resolve("../..").resolve("«entityFullName»").toString());
                    result.put("objectId", toRender.getId().toString());
                    result.put("shorthand", «getModelValue(entity.properties.head, 'toRender')»);
                    result.put("full", full);
                    result.put("disabledActions", Collections.emptyMap());
                    result.put("scopeName", "«typeRef.typeName»");
                    result.put("scopeNamespace", "«typeRef.entityNamespace»");
                    Map<String, Object> typeRef = new LinkedHashMap<>();
                    typeRef.put("entityNamespace", "«typeRef.entityNamespace»");
                    typeRef.put("kind", "«typeRef.kind»");
                    typeRef.put("typeName", "«typeRef.typeName»");
                    typeRef.put("fullName", "«typeRef.fullName»");
                    result.put("typeRef", typeRef);   
                    return result;                    
                }    
        }
        '''
    }
    
    def getModelValue(Property property, String varName) {
        val core = '''«varName».«property.generateAccessorName»()'''
        if (property.type.enumeration) 
            '''«core».name()'''
        else if (property.type.name == 'Date') 
            '''«core» == null ? null : dateFormat.format(«core»)'''            
        else
            core
    }
    
}