package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Property
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator

class JAXRSResourceGenerator extends BehaviorlessClassGenerator {
    
    PlainEntityGenerator entityGenerator
    
    new(IRepository repository) {
        super(repository)
        entityGenerator = new PlainEntityGenerator(repository)
    }
    
    def generateResource(Class entity) {
        val typeRef = entity.convertType
        val entityFullName = typeRef.fullName
        val properties = entity.properties
        val dataProperties = properties.filter[!derived]
        val derivedProperties = properties.filter[derived]        
        '''
        package resource.«entity.packagePrefix»;
        
        import «entity.packagePrefix».*;
        
        import java.util.*;
        import java.util.stream.*;
        import java.text.*;
        
        import javax.ws.rs.core.Context;
        import javax.ws.rs.core.MediaType;
        import javax.ws.rs.core.UriInfo;
        import javax.ws.rs.GET;
        import javax.ws.rs.PUT;
        import javax.ws.rs.POST;
        import javax.ws.rs.DELETE;
        import javax.ws.rs.Path;
        import javax.ws.rs.PathParam;
        import javax.ws.rs.Produces;
        import javax.ws.rs.Consumes;        
        import javax.ws.rs.core.MediaType;
        import javax.ws.rs.core.Response;
        import javax.ws.rs.core.Response.ResponseBuilder;
        import javax.ws.rs.core.Response.Status;
        
        import java.net.URI;
        
        «entity.generateImports»
        
        @Path("entities/«entityFullName»/instances")
        @Produces(MediaType.APPLICATION_JSON)
        public class «entity.name»Resource {
            
                private static ResponseBuilder status(Status status) {
                    return Response.status(status).type(MediaType.APPLICATION_JSON);
                }
            
                @Context
                UriInfo uri;
            
                private «entity.name»Service service = new «entity.name»Service();
                
                @GET
                @Path("{id}")
                public Response getSingle(@PathParam("id") String idString) {
                    if ("_template".equals(idString)) {
                        «entity.name» template = new «entity.name»(); 
                        return status(Status.OK).entity(toExternalRepresentation(template, uri.getRequestUri().resolve(""), true)).build();
                    }
                    Long id = Long.parseLong(idString);
                    «entity.name» found = service.find(id);
                    if (found == null)
                        return status(Status.NOT_FOUND).entity(Collections.singletonMap("message", "«entity.name» not found: " + id)).build();
                    return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(""), true)).build();
                }
                
                @PUT
                @Path("{id}")
                @Consumes(MediaType.APPLICATION_JSON)
                public Response put(@PathParam("id") Long id, Map<String, Object> representation) {
                    «entity.name» found = service.find(id);
                    if (found == null)
                        return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                    try {    
                        updateFromExternalRepresentation(found, representation);
                    } catch (RuntimeException e) {
                        return status(Status.BAD_REQUEST).entity(Collections.singletonMap("message", e.getMessage())).build();
                    }    
                    service.update(found);
                    return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(""), true)).build();
                }
                
                @POST
                @Consumes(MediaType.APPLICATION_JSON)
                public Response post(Map<String, Object> representation) {
                    «entity.name» newInstance = new «entity.name»();
                    try {    
                        updateFromExternalRepresentation(newInstance, representation);
                    } catch (RuntimeException e) {
                        return status(Status.BAD_REQUEST).entity(Collections.singletonMap("message", e.getMessage())).build();
                    }    
                    service.create(newInstance);
                    return status(Status.CREATED).entity(toExternalRepresentation(newInstance, uri.getRequestUri().resolve(newInstance.getId().toString()), true)).build();
                }
                
                @DELETE
                @Path("{id}")
                public Response delete(@PathParam("id") Long id) {
                    «entity.name» found = service.find(id);
                    if (found == null)
                        return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                    service.delete(id);    
                    return Response.noContent().build();
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
                    return status(Status.OK).entity(result).build();
                }
                
                private Map<String, Object> toExternalRepresentation(«entity.name» toRender, URI instancesURI, boolean full) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    Map<String, Object> values = new LinkedHashMap<>();
                    boolean persisted = toRender.getId() != null;
                    «IF (properties.exists[type.name == 'Date'])»
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                    «ENDIF»
                    «dataProperties.map[
                        '''values.put("«name»", «getModelValue(it, 'toRender')»);'''
                    ].join('\n')»
                    «if (!derivedProperties.empty)
                    '''
                    if (persisted) {
                        «derivedProperties.map[
                            '''values.put("«name»", «getModelValue(it, 'toRender')»);'''
                        ].join('\n')»
                    } else {
                        «derivedProperties.map[
                            '''values.put("«name»", «it.type.generateDefaultValue»);'''
                        ].join('\n')»
                    }
                    '''»
                    result.put("values", values);
                    Map<String, Object> links = new LinkedHashMap<>();
                    result.put("links", links);
                    result.put("uri", instancesURI.resolve(persisted ? toRender.getId().toString() : "_template").toString());
                    result.put("entityUri", instancesURI.resolve("../..").resolve("«entityFullName»").toString());
                    if (persisted) {
                        result.put("objectId", toRender.getId().toString());
                        result.put("shorthand", «getModelValue(entity.properties.head, 'toRender')»);
                    }
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
                
                private void updateFromExternalRepresentation(«entity.name» toUpdate, Map<String, Object> external) {
                    Map<String, Object> values = (Map<String, Object>) external.get("values");
                    «IF (entity.properties.exists[type.name == 'Date'])»
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                    «ENDIF»
                    «entity.properties.filter[!KirraHelper.isReadOnly(it)].map[
                        '''«setModelValue(it, "values", "toUpdate")»;'''
                    ].join('\n')»
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
    
    def convertToInternal(Property property, CharSequence expression) {
        switch (property.type.name) {
            case 'Double' : '''Double.parseDouble((String) «expression»)'''
            case 'Integer' : '''Long.parseLong((String) «expression»)'''
            default: '''(«property.toJavaType(true)») «expression»'''
        }
    }
    
    def setModelValue(Property property, String sourceVarName, String targetVarName) {
        val generateStatement = [ CharSequence value | '''«targetVarName».«property.generateSetterName»(«value»)''' ]
        val valueRetrieval = '''«sourceVarName».get("«property.name»")'''
        val core = generateStatement.apply(convertToInternal(property, valueRetrieval))
        return if (property.required)
            '''
            if («valueRetrieval» != null)
                «core»;
            else
                «generateStatement.apply('''«entityGenerator.generateAttributeDefaultValue(property)»''')»
            '''.toString.trim 
        else
            core
    }
    
}