package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDExtensionUtils.AccessCapability
import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import java.util.EnumSet
import org.eclipse.uml2.uml.AggregationKind
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.TypedElement

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ConstraintUtils.*

class JAXRSResourceGenerator extends BehaviorlessClassGenerator {
    
    PlainEntityGenerator entityGenerator
    
    new(IRepository repository) {
        super(repository)
        entityGenerator = new PlainEntityGenerator(repository)
    }
    
    def generateResource(Class entity) {
        val typeRef = entity.convertType
        val entityFullName = typeRef.fullName
        val allRoleClasses = appPackages.entities.filter[ role ]
        val accessControlGenerator = new JAXRSAccessControlGenerator()
        '''
        package resource.«entity.packagePrefix»;

        import resource.util.EntityResourceHelper;
        import javax.ws.rs.GET;
        import javax.ws.rs.Path;
        import javax.ws.rs.Produces;
        import javax.ws.rs.core.Context;
        import javax.ws.rs.core.MediaType;
        import javax.ws.rs.core.Response;
        import javax.ws.rs.core.UriInfo;
        
        import java.io.IOException;
        
        «IF entity.concrete»        
        import «entity.packagePrefix».*;
        
        import java.util.*;
        import java.util.stream.*;
        import java.text.*;
        import java.util.function.Function;
        import java.io.IOException;

        import org.apache.commons.lang3.time.DateUtils;
        import org.apache.commons.lang3.StringUtils;
        
        import javax.ws.rs.PUT;
        import javax.ws.rs.POST;
        import javax.ws.rs.DELETE;
        import javax.ws.rs.OPTIONS;
        import javax.ws.rs.PathParam;
        import javax.ws.rs.Consumes;        
        import javax.ws.rs.core.MediaType;
        import javax.ws.rs.core.Response;
        import javax.ws.rs.core.Response.ResponseBuilder;
        import javax.ws.rs.core.Response.Status;
        import javax.annotation.security.RolesAllowed;
        
        import java.net.URI;
        
        «entity.generateImports»
        «ENDIF»
        
        @Path("entities/«entityFullName»/")
        @Produces(MediaType.APPLICATION_JSON)
        «accessControlGenerator.generateEndpointAnnotation(EnumSet.noneOf(AccessCapability), allRoleClasses, #[entity])»
        public class «entity.name»Resource {
        «IF entity.concrete»
            private static final String[] DATE_FORMATS = { "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd", "yyyy/MM/dd" };
            private «entity.name»Service service = new «entity.name»Service();
        «ENDIF»
            @Context
            UriInfo uri;
            @GET
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.noneOf(AccessCapability), #[], #[])»
            public Response getEntity() {
                try {
                    String contents = EntityResourceHelper.getEntityRepresentation("«entityFullName»", uri.getRequestUri().resolve("..").toString());
                    if (contents == null) {
                        return Response.status(404).build();
                    }
                    return Response.ok(contents, MediaType.APPLICATION_JSON).build();
                } catch (IOException e) {
                    return Response.status(500).build();
                }
            }
            
            «IF entity.concrete»
            @GET
            @Path("instances/{id}")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Read), allRoleClasses, #[entity])»
            public Response getSingle(@PathParam("id") String idString) {
                if ("_template".equals(idString)) {
                    «entity.name» template = new «entity.name»(); 
                    return status(Status.OK).entity(toExternalRepresentation(template, uri.getRequestUri().resolve(""))).build();
                }
                Long id = Long.parseLong(idString);
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity(Collections.singletonMap("message", "«entity.name» not found: " + id)).build();
                return status(Status.OK).entity(toFullExternalRepresentation(found, uri.getRequestUri().resolve(""))).build();
            }
            
            @PUT
            @Path("instances/{id}")
            @Consumes(MediaType.APPLICATION_JSON)
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Update), allRoleClasses, #[entity])»
            public Response put(@PathParam("id") Long id, Map<String, Object> representation) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                try {    
                    updateFromExternalRepresentation(found, representation);
                } catch (RuntimeException e) {
                    return errorStatus(Status.BAD_REQUEST, e.getMessage()).build();
                }    
                service.update(found);
                return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(""))).build();
            }
            
            @POST
            @Path("instances")
            @Consumes(MediaType.APPLICATION_JSON)
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Create), allRoleClasses, #[entity])»
            public Response post(Map<String, Object> representation) {
                «entity.name» newInstance = new «entity.name»();
                try {    
                    updateFromExternalRepresentation(newInstance, representation);
                } catch (RuntimeException e) {
                    return errorStatus(Status.BAD_REQUEST, e.getMessage()).build();
                }    
                service.create(newInstance);
                return status(Status.CREATED).entity(toExternalRepresentation(newInstance, uri.getRequestUri().resolve(newInstance.getId().toString()))).build();
            }
            
            @DELETE
            @Path("instances/{id}")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Delete), allRoleClasses, #[entity])»
            public Response delete(@PathParam("id") Long id) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                service.delete(id);    
                return Response.noContent().build();
            }
                            
            @GET
            @Path("instances")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Read), allRoleClasses, #[entity])»
            public Response getList() {
                Collection<«entity.name»> models = service.findAll();
                return toExternalList(uri, models).build();
            }
            
            «FOR relationship : entity.entityRelationships.filter[multiple && navigable && userVisible]»
            @GET
            @Path("instances/{id}/relationships/«relationship.name»")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Read), allRoleClasses, #[entity, relationship])»
            public Response list«relationship.name.toFirstUpper»(@PathParam("id") Long id) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                Collection<«relationship.type.name»> related = found.«relationship.generateAccessorName»();
                return «relationship.type.name»Resource.toExternalList(uri, related).build();
            }
            «IF !relationship.readOnly»
            @PUT
            @Path("instances/{id}/relationships/«relationship.name»/{toAttach}")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Update), allRoleClasses, #[entity, relationship])»
            public Response attach«relationship.name.toFirstUpper»(@PathParam("id") Long id, @PathParam("toAttach") String toAttachIdStr) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                
                Long toAttachId = parseId(toAttachIdStr);
                «relationship.type.name» toAttach = new «relationship.type.name»Service().find(toAttachId);
                if (toAttach == null)
                    return status(Status.BAD_REQUEST).entity("«relationship.type.name» not found: " + toAttachId).build();

                Collection<«relationship.type.name»> related = found.«relationship.generateAccessorName»();                    
                related.add(toAttach);
                service.update(found);
                return «relationship.type.name»Resource.toExternalList(uri, related).build();
            }
            @DELETE
            @Path("instances/{id}/relationships/«relationship.name»/{toDetach}")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Update), allRoleClasses, #[entity, relationship])»            
            public Response detach«relationship.name.toFirstUpper»(@PathParam("id") Long id, @PathParam("toDetach") String toDetachIdStr) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                
                Long toDetachId = parseId(toDetachIdStr);
                «relationship.type.name» toDetach = new «relationship.type.name»Service().find(toDetachId);
                if (toDetach == null)
                    return status(Status.BAD_REQUEST).entity("«relationship.type.name» not found: " + toDetachId).build();    
                    
                found.«relationship.generateAccessorName»().remove(toDetach);
                service.update(found);
                return status(Status.NO_CONTENT).build();
            }
            «ENDIF»
            «ENDFOR»
            «FOR relationship : entity.entityRelationships.filter[!derived && navigable && aggregation != AggregationKind.COMPOSITE_LITERAL && userVisible]»
            @GET
            @Path("instances/{id}/relationships/«relationship.name»/domain")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Read), allRoleClasses, #[entity, relationship])»
            public Response listDomainFor«relationship.name.toFirstUpper»(@PathParam("id") Long id) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                Collection<«relationship.type.name»> domain = new «entity.name»Service().getDomainFor«relationship.name.toFirstUpper»(found);
                return «relationship.type.name»Resource.toExternalList(uri, domain).build();
            }
            «ENDFOR»
            
            «FOR action : entity.instanceActions»
            @POST
            @Consumes(MediaType.APPLICATION_JSON)
            @Path("instances/{id}/actions/«action.name»")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Call), allRoleClasses, #[entity, action])»
            public Response execute«action.name.toFirstUpper»(@PathParam("id") Long id, Map<String, Object> representation) {
                «action.generateArgumentMatching»
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                found.«action.name»(«action.parameters.map[name].join(', ')»);
                // save 
                service.update(found);
                return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(".."))).build();
            }
            
            «FOR parameter : action.parameters.filter[type.entity]»
            @GET
            @Path("instances/{id}/actions/«action.name»/parameters/«parameter.name»/domain")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Call), allRoleClasses, #[entity, action])»
            public Response list«action.name.toFirstUpper»_«parameter.name»Domain(@PathParam("id") Long id) {
                «entity.name» found = service.find(id);
                if (found == null)
                    return status(Status.NOT_FOUND).entity("«entity.name» not found: " + id).build();
                Collection<«parameter.type.name»> domain = «if (parameter.hasParameterConstraints)
                	'''service.getParameterDomainFor«parameter.name.toFirstUpper»To«action.name.toFirstUpper»(found)'''
                else
                	'''new «parameter.type.name»Service().findAll()'''»;
                return «parameter.type.name»Resource.toExternalList(uri, domain).build();
            }
            «ENDFOR»
            «ENDFOR»
            
            «FOR action : entity.entityActions»
            @POST
            @Consumes(MediaType.APPLICATION_JSON)
            @Path("actions/«action.name»")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Call), allRoleClasses, #[entity, action])»
            public Response execute«action.name.toFirstUpper»(Map<String, Object> representation) {
                «action.generateArgumentMatching»
                service.«action.name»(«action.parameters.map[name].join(', ')»);
                return status(Status.OK).entity(Collections.emptyMap()).build();
            }
            
            «FOR parameter : action.parameters.filter[type.entity]»
            @GET
            @Path("instances/undefined/actions/«action.name»/parameters/«parameter.name»/domain")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Call), allRoleClasses, #[entity, action])»
            public Response list«action.name.toFirstUpper»_«parameter.name»Domain() {
                Collection<«parameter.type.name»> domain = «if (parameter.hasParameterConstraints)
                	'''service.getParameterDomainFor«parameter.name.toFirstUpper»To«action.name.toFirstUpper»()'''
                else
                	'''new «parameter.type.name»Service().findAll()'''»;
                return «parameter.type.name»Resource.toExternalList(uri, domain).build();
            }
            «ENDFOR»
            «ENDFOR»
            
            «FOR query : entity.queries.filter[getReturnResult().multiple && getReturnResult().type == entity]»
            @POST
            @Consumes(MediaType.APPLICATION_JSON)
            @Path("finders/«query.name»")
            «accessControlGenerator.generateEndpointAnnotation(EnumSet.of(AccessCapability.Call), allRoleClasses, #[entity, query])»
            public Response execute«query.name.toFirstUpper»(Map<String, Object> representation) {
                «query.generateArgumentMatching»
                Collection<«entity.name»> models = service.«query.name»(«query.parameters.map[name].join(', ')»);
                return toExternalList(uri, models).build();
            }
            «ENDFOR»
            
            
        
            private static ResponseBuilder status(Status status) {
                return Response.status(status).type(MediaType.APPLICATION_JSON);
            }
            
            private static ResponseBuilder errorStatus(Status status, String message) {
                return Response.status(status).type(MediaType.APPLICATION_JSON).entity(Collections.singletonMap("message", message));
            }
            
            private static Long parseId(String idStr) {
                String[] components = StringUtils.split(idStr, '@');
                return Long.parseLong(components[components.length - 1]);
            }
            
            static ResponseBuilder toExternalList(UriInfo uriInfo, Collection<«entity.name»> models) {
                URI extentURI = uriInfo.getRequestUri();
                Collection<Map<String, Object>> items = models.stream().map(toMap -> {
                    return toExternalRepresentation(toMap, extentURI);
                }).collect(Collectors.toList());
                
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("contents", items);
                result.put("offset", 0);
                result.put("length", items.size());  
                return status(Status.OK).entity(result);
            }
            
            private static Map<String, Object> toExternalRepresentation(«entity.name» toRender, URI instancesURI) {
            	return «entity.name»JAXBSerialization.toExternalRepresentation(toRender, instancesURI, «entity.name»JAXBSerialization.Feature.Values, «entity.name»JAXBSerialization.Feature.Links);
        	}
            
            private static Map<String, Object> toFullExternalRepresentation(«entity.name» toRender, URI instancesURI, «entity.name»JAXBSerialization.Feature... featureOptions) {
            	return «entity.name»JAXBSerialization.toExternalRepresentation(toRender, instancesURI, «entity.name»JAXBSerialization.Feature.values());
            }
            
            private static void updateFromExternalRepresentation(«entity.name» toUpdate, Map<String, Object> external) {
            	«entity.name»JAXBSerialization.updateFromExternalRepresentation(toUpdate, external);
            }
            «ENDIF»
        }
        '''
    }
    
    def CharSequence generateArgumentMatching(Operation operation) {
        '''
        «operation.parameters.map[
            '''
            «it.type.toJavaType» «it.name»«IF !it.required» = null«ENDIF»;
            if (representation.get("«it.name»") != null)
                «parseIntoLocalVar(it, '''representation.get("«it.name»")''')»
            «IF required»
            else
                return errorStatus(Status.BAD_REQUEST, "Missing argument for required parameter '«it.name»'").build();
            «ENDIF» 
            '''
        ].join('\n')»
        '''
    }
    
    def CharSequence parseIntoLocalVar(Parameter parameter, String string) {
        val core = '''«parameter.name» = «convertToInternal(parameter, '''representation.get("«parameter.name»")''')»;'''
        if (parameter.type.name == 'Date')
            '''
            try {
                «core»
            } catch (ParseException e) {
                throw new ConversionException("Invalid format for date in '«parameter.name»': " + representation.get("«parameter.name»"));
            }
            '''
        else if (parameter.type.entity) {
        	if (parameter.multiple) {
        	// it is an entity, and the parameter is multivalued - need to map ids to POJOs
        	'''
        	«parameter.name» = ((Collection<Map<String, Object>>) representation.get("«parameter.name»")).stream().map(it -> new «parameter.type.name»Service().find(Long.parseLong((String) it.get("objectId")))).orElse(null);
        	'''
        	} else {
        	'''
        	«parameter.name» = Optional.ofNullable(((Map<String, Object>) representation.get("«parameter.name»"))).map(it -> new «parameter.type.name»Service().find(Long.parseLong((String) it.get("objectId")))).orElse(null);
        	'''	
        	}
        	
        } else
            core
    }
    
    def convertToInternal(TypedElement typedElement, CharSequence expression) {
        switch (typedElement.type.name) {
            case 'Double' : '''Double.parseDouble(«expression».toString())'''
            case 'Integer' : '''Long.parseLong(«expression».toString())'''
            case 'Date' : '''DateUtils.parseDate((String) «expression», DATE_FORMATS)'''
            default: 
            	if (typedElement.type.entity) 
            		convertIdToInternal(typedElement, expression)
                else if (typedElement.type.enumeration) 
            		'''«typedElement.toJavaType()».valueOf((String) «expression»)'''
            	else 
            		'''(«typedElement.toJavaType(true)») «expression»'''
        }
    }
    
    def convertIdToInternal(TypedElement typedElement, CharSequence expression) {
    	'''((List<«typedElement.type.toJavaType»>) «expression»)'''
    }
}