package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.osgi.framework.ServiceRegistration;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.kirra.rest.common.Paths;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.WebFrontEnd;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import junit.framework.TestCase;

/**
 * Tests for the Kirra-MDD-based REST API.
 */
public class KirraMDDRuntimeRest2Tests extends AbstractKirraRestTests {

    Map<String, String> authorized = new LinkedHashMap<String, String>();
    
    private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    protected ServiceRegistration<AuthenticationService> authenticatorRegistration;
    
    private static final String globalModel;
    static {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String[0,1];\n";
        model += "    attribute attr2 : String[0,1];\n";
        model += "    attribute myClass3 : MyClass3[0,1];\n";
        model += "    attribute myClass2 : MyClass2[0,1];\n";
        model += "    composition myClass4s : MyClass4[*] opposite parent;\n";
        model += "end;\n";
        model += "class MyClass2\n";
        model += "    attribute attr2 : String[0,1];\n";
        model += "end;\n";
        model += "abstract class MyClass3\n";
        model += "    attribute attr3 : String[0,1];\n";
        model += "end;\n";
        model += "class MyClass3a specializes MyClass3\n";
        model += "end;\n";
        model += "class MyClass3b specializes MyClass3\n";
        model += "end;\n";
        model += "class MyClass4\n";
        model += "    attribute attr4 : String[0,1];\n";
        model += "    attribute parent : MyClass1;\n";
        model += "end;\n";
        model += "class MyClass5\n";
        model += "    attribute attr5 : String[0,1];\n";
        model += "    attribute requiredClass1 : MyClass1;\n";
        model += "end;\n";
        model += "class MyClass6\n";
        model += "    attribute attr6 : String[0,1];\n";
        model += "    attribute optionalClass1 : MyClass1[0,1];\n";
        model += "end;\n";
        
        model += "end.";
        globalModel = model;
    }
    
    
    public KirraMDDRuntimeRest2Tests(String name) {
        super(name);
    }
    
    @Override
    protected URI getWorkspaceBaseURI() throws IOException, HttpException {
		URI workspaceURI = URI.create("http://localhost" + WebFrontEnd.APP_API2_PATH + "/");
		return workspaceURI;
    }
    

    public void testCreateInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer := 5;\n";
        model += "    attribute attr3 : Integer := 90;\n";
        model += "    attribute attr4 : Integer[0,1] := 80;\n";
        model += "    attribute attr5 : Integer[0,1];\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        String templateUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", "mypackage.MyClass1"));
		GetMethod getTemplateInstance = new GetMethod(templateUri);
        ObjectNode template = (ObjectNode) executeJsonMethod(200, getTemplateInstance);
        ((ObjectNode) template.get("values")).put("attr1", "foo");
        ((ObjectNode) template.get("values")).put("attr3", 100);
        ((ObjectNode) template.get("values")).put("attr4", (String) null);
        PostMethod createMethod = new PostMethod(resolveApplicationURI(Paths.INSTANCES_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName())));
        createMethod.setRequestEntity(new StringRequestEntity(template.toString(), "application/json", "UTF-8"));

        ObjectNode created = (ObjectNode) executeJsonMethod(201, createMethod);
        TestCase.assertEquals("foo", created.get("values").get("attr1").asText());
        TestCase.assertEquals(5, created.get("values").get("attr2").asLong());
        TestCase.assertEquals(100, created.get("values").get("attr3").asLong());
        TestCase.assertNull(created.get("values").get("attr4"));
        TestCase.assertNull(created.get("values").get("attr5"));
    }


    public void testGetEntity() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer;\n";
        model += "    operation op1();\n";
        model += "    operation op2();\n";
        model += "    static query op3() : MyClass1;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        String entityUri = resolveApplicationURI(Paths.ENTITY_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));
        GetMethod getEntity = new GetMethod(entityUri.toString());
        executeMethod(200, getEntity);

        ObjectNode jsonEntity = (ObjectNode) JsonHelper.parse(new InputStreamReader(getEntity.getResponseBodyAsStream()));

        TestCase.assertEquals("MyClass1", jsonEntity.get("name").textValue());
        TestCase.assertEquals("mypackage", jsonEntity.get("namespace").textValue());
        TestCase.assertEquals(StringUtils.removeEnd(entityUri.toString(), "/"), StringUtils.removeEnd(jsonEntity.get("uri").textValue(), "/"));

        TestCase.assertNotNull(jsonEntity.get("extentUri"));
    }

    public void testGetEntityList() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1 attribute a : String; end;\n";
        model += "class MyClass2 attribute a : Integer; end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
        
        GetMethod getDomainTypes = new GetMethod(resolveApplicationURI(Paths.ENTITIES_PATH.replace("{application}", getName())));
        executeMethod(200, getDomainTypes);

        ArrayNode entities = (ArrayNode) JsonHelper.parse(new InputStreamReader(getDomainTypes.getResponseBodyAsStream()));
        TestCase.assertEquals(3, entities.size());

        
        List<Object> elementList = IteratorUtils.toList(entities.elements(), entities.size());
		List<JsonNode> myPackageEntities = elementList.stream().map(it -> ((JsonNode) it)).filter(it -> "mypackage".equals(it.get("namespace").textValue())).collect(Collectors.toList());
		assertEquals(2, myPackageEntities.size());
		TestCase.assertEquals("MyClass1", myPackageEntities.get(0).get("name").textValue());
		TestCase.assertEquals("MyClass2", myPackageEntities.get(1).get("name").textValue());
		
        for (JsonNode jsonNode : myPackageEntities) {
            TestCase.assertEquals("mypackage", jsonNode.get("namespace").textValue());
            TestCase.assertNotNull(jsonNode.get("uri"));
            executeMethod(200, new GetMethod(jsonNode.get("uri").toString()));
        }
    }

    public void testGetInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        Instance created = RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Instance>() {
            @Override
            public Instance run(Resource<?> resource) {
                Repository repository = resource.getFeature(Repository.class);
                Instance instance = repository.newInstance("mypackage", "MyClass1");
                instance.setValue("attr1", "The answer is");
                instance.setValue("attr2", "42");
                Instance created = repository.createInstance(instance);
                return created;
            }
        });
        
        ObjectNode jsonInstance = executeJsonMethod(200,
                new GetMethod(resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.getObjectId()).replace("{application}", getName()))));

        TestCase.assertNotNull(jsonInstance.get("uri"));
        executeMethod(200, new GetMethod(jsonInstance.get("uri").toString()));
        TestCase.assertNotNull(jsonInstance.get("entityUri"));
        executeMethod(200, new GetMethod(jsonInstance.get("entityUri").asText()));

        ObjectNode values = (ObjectNode) jsonInstance.get("values");
        TestCase.assertEquals("The answer is", values.get("attr1").textValue());
        TestCase.assertEquals(42L, values.get("attr2").asLong());
    }

    public void testGetInstanceList() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1 attribute a : Integer[0,1]; end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
            @Override
            public Object run(Resource<?> resource) {
                Repository repository = resource.getFeature(Repository.class);
                repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
                repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
                return null;
            }
        });

        String instanceListUri = resolveApplicationURI(Paths.INSTANCES_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));

        GetMethod getInstances = new GetMethod(instanceListUri.toString());
        executeMethod(200, getInstances);

        ArrayNode instances = (ArrayNode) JsonHelper.parse(new InputStreamReader(getInstances.getResponseBodyAsStream())).get("contents");
        TestCase.assertEquals(2, instances.size());

        for (JsonNode jsonNode : instances) {
            TestCase.assertNotNull(jsonNode.get("uri"));
            executeMethod(200, new GetMethod(jsonNode.get("uri").asText()));
            TestCase.assertNotNull(jsonNode.get("entityUri"));
            executeMethod(200, new GetMethod(jsonNode.get("entityUri").asText()));
        }
    }

    public void testDelete() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        String instanceUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        executeMethod(204, new DeleteMethod(instanceUri));
        executeMethod(404, new GetMethod(instanceUri));
    }
    
    public void testDetachChild() throws CoreException, IOException {
        Task<List<Instance>> fixture = (Resource<?> resource) -> {
            List<Instance> created = new LinkedList<>();
            Repository repository = resource.getFeature(Repository.class);
            Instance instance1 = repository.newInstance("mypackage", "MyClass1");
            instance1.setValue("attr1", "value1");
            instance1 = repository.createInstance(instance1);
            created.add(instance1);
            Instance instance4 = repository.newInstance("mypackage", "MyClass4");
            instance4.setValue("attr4", "value4");
            instance4.setRelated("parent", instance1);
            instance4 = repository.createInstance(instance4);
            created.add(instance4);
            return created;
        };
        List<Instance> created = buildAndRunInRepository(globalModel, fixture);
        String parentInstanceUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        String childInstanceAsTopUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass4").replace("{objectId}", created.get(1).getObjectId()).replace("{application}", getName()));
        String childInstanceUri = resolveApplicationURI(parentInstanceUri + "/" + Paths.RELATIONSHIPS + "/myClass4s/" + created.get(1).getReference()); 
        executeMethod(200, new GetMethod(parentInstanceUri));
        executeMethod(200, new GetMethod(childInstanceUri));
        executeMethod(200, new GetMethod(childInstanceAsTopUri));
        executeMethod(204, new DeleteMethod(childInstanceUri));
        executeMethod(404, new GetMethod(childInstanceUri));
        executeMethod(404, new GetMethod(childInstanceAsTopUri));
    }
    
    public void testDeleteParent() throws CoreException, IOException {
        Task<List<Instance>> fixture = (Resource<?> resource) -> {
            List<Instance> created = new LinkedList<>();
            Repository repository = resource.getFeature(Repository.class);
            Instance instance1 = repository.newInstance("mypackage", "MyClass1");
            instance1.setValue("attr1", "value1");
            instance1 = repository.createInstance(instance1);
            created.add(instance1);
            Instance instance4 = repository.newInstance("mypackage", "MyClass4");
            instance4.setValue("attr4", "value4");
            instance4.setRelated("parent", instance1);
            instance4 = repository.createInstance(instance4);
            created.add(instance4);
            return created;
        };
        List<Instance> created = buildAndRunInRepository(globalModel, fixture);
        String parentInstanceUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        String childInstanceAsTopUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass4").replace("{objectId}", created.get(1).getObjectId()).replace("{application}", getName()));
        String childInstanceUri = resolveApplicationURI(parentInstanceUri + "/" + Paths.RELATIONSHIPS + "/myClass4s/" + created.get(1).getReference());
        executeMethod(200, new GetMethod(parentInstanceUri));
        executeMethod(200, new GetMethod(childInstanceUri));
        executeMethod(200, new GetMethod(childInstanceAsTopUri));
        executeMethod(204, new DeleteMethod(parentInstanceUri));
        executeMethod(404, new GetMethod(parentInstanceUri));
        executeMethod(404, new GetMethod(childInstanceUri));
        executeMethod(404, new GetMethod(childInstanceAsTopUri));
    }
    
    public void testDeleteRelated_Required() throws CoreException, IOException {
        Task<List<Instance>> fixture = (Resource<?> resource) -> {
            List<Instance> created = new LinkedList<>();
            Repository repository = resource.getFeature(Repository.class);
            Instance instance1 = repository.newInstance("mypackage", "MyClass1");
            instance1 = repository.createInstance(instance1);
            created.add(instance1);
            Instance instance5 = repository.newInstance("mypackage", "MyClass5");
            instance5.setRelated("requiredClass1", instance1);
            instance5 = repository.createInstance(instance5);
            created.add(instance5);
            return created;
        };
        List<Instance> created = buildAndRunInRepository(globalModel, fixture);
        Instance relatedInstance = created.get(0);
        String relatedInstanceUri = getInstanceUri(relatedInstance);
        Instance dependantInstance = created.get(1);
        String dependantInstanceUri = getInstanceUri(dependantInstance);
        executeMethod(200, new GetMethod(relatedInstanceUri));
        executeMethod(200, new GetMethod(dependantInstanceUri));
        executeMethod(400, new DeleteMethod(relatedInstanceUri));
        executeMethod(200, new GetMethod(relatedInstanceUri));
        // not only it was not deleted, but it is still pointing to the related object
        ObjectNode jsonDependantInstance = executeJsonMethod(200, new GetMethod(dependantInstanceUri));
        assertEquals(relatedInstanceUri, JsonHelper.traverse(jsonDependantInstance, "links", "requiredClass1", "uri").asText());
        executeMethod(204, new DeleteMethod(dependantInstanceUri));
        executeMethod(204, new DeleteMethod(relatedInstanceUri));
    }
    
    public void testDeleteRelated_Optional() throws CoreException, IOException {
        Task<List<Instance>> fixture = (Resource<?> resource) -> {
            List<Instance> created = new LinkedList<>();
            Repository repository = resource.getFeature(Repository.class);
            Instance instance1 = repository.newInstance("mypackage", "MyClass1");
            instance1 = repository.createInstance(instance1);
            created.add(instance1);
            Instance instance6 = repository.newInstance("mypackage", "MyClass6");
            instance6.setRelated("optionalClass1", instance1);
            instance6 = repository.createInstance(instance6);
            created.add(instance6);
            return created;
        };
        List<Instance> created = buildAndRunInRepository(globalModel, fixture);
        Instance relatedInstance = created.get(0);
        String relatedInstanceUri = getInstanceUri(relatedInstance);
        Instance dependantInstance = created.get(1);
        String dependantInstanceUri = getInstanceUri(dependantInstance);
        assertEquals(relatedInstanceUri, getAndTraverse(dependantInstanceUri, "links", "optionalClass1", "uri").textValue());
        executeMethod(204, new DeleteMethod(relatedInstanceUri));
        executeMethod(404, new GetMethod(relatedInstanceUri));
        // the dependant object is still around, but its reference to the related object is now gone
        assertFalse(getAndTraverse(dependantInstanceUri, "links").has("optionalClass1"));
    }

    private String getInstanceUri(Instance instance) throws IOException, HttpException {
        return resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", instance.getTypeRef().toString()).replace("{objectId}", instance.getObjectId()).replace("{application}", getName()));
    }    
    
    private JsonNode getAndTraverse(String uri, String... paths) throws HttpException, IOException {
        JsonNode asJson = executeJsonMethod(200, new GetMethod(uri));
        return JsonHelper.traverse(asJson, paths);
    }


    
    public void testUpdateInstance() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()))));
        
        ((ObjectNode) jsonInstance1.get("values")).set("attr1", new TextNode("value 1a"));
        ((ObjectNode) jsonInstance1.get("values")).set("attr2", new TextNode("value 2a"));

        PutMethod putMethod = new PutMethod(jsonInstance1.get("uri").textValue());
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        TestCase.assertEquals("value 1a", updated.get("values").get("attr1").asText());
        TestCase.assertEquals("value 2a", updated.get("values").get("attr2").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        TestCase.assertEquals("value 1a", retrieved.get("values").get("attr1").asText());
        TestCase.assertEquals("value 2a", retrieved.get("values").get("attr2").asText());
    }
    
    public void testUpdateInstance_ClearValue() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();

        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", "mypackage.MyClass1").replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()))));
        
        ((ObjectNode) jsonInstance1.get("values")).set("attr1", new TextNode(""));

        PutMethod putMethod = new PutMethod(jsonInstance1.get("uri").textValue());
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        TestCase.assertEquals("", updated.get("values").get("attr1").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        TestCase.assertEquals("", retrieved.get("values").get("attr1").asText());
    }
    
    public void testUpdateInstance_SetLink() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        List<String> uris = created.stream().map(it ->
        	{
				try {
					return resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", it.getTypeRef().getFullName()).replace("{objectId}", it.getObjectId()).replace("{application}", getName()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
        ).collect(Collectors.toList());
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(uris.get(0)));
        ObjectNode links = jsonNodeFactory.objectNode();
        jsonInstance1.put("links", links);
        
        ObjectNode myClass2 = jsonNodeFactory.objectNode();
        links.put("myClass2", myClass2);
        myClass2.set("uri", new TextNode(uris.get(1)));
        
        ObjectNode myClass3a = jsonNodeFactory.objectNode();
        links.put("myClass3", myClass3a);
        myClass3a.set("uri", new TextNode(uris.get(2)));

        PutMethod putMethod = new PutMethod(uris.get(0));
        putMethod.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated = (ObjectNode) executeJsonMethod(200, putMethod);
        JsonNode updatedLinks = updated.get("links");
		assertNotNull(updatedLinks);
        assertNotNull(updatedLinks.get("myClass2"));
        assertNotNull(updatedLinks.get("myClass3"));
        TestCase.assertEquals(uris.get(1), updatedLinks.get("myClass2").get("uri").asText());
        TestCase.assertEquals(uris.get(2), updatedLinks.get("myClass3").get("uri").asText());
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        assertNotNull(retrieved.get("links"));
        assertNotNull(retrieved.get("links").get("myClass2"));
        TestCase.assertEquals(uris.get(1), retrieved.get("links").get("myClass2").get("uri").asText());
        assertNotNull(retrieved.get("links").get("myClass3"));
        TestCase.assertEquals(uris.get(2), retrieved.get("links").get("myClass3").get("uri").asText());
    }
    
    public void testUpdateInstance_UnsetLink() throws CoreException, IOException {
        List<Instance> created = testUpdateInstanceSetup();
        String uri1 = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", created.get(0).getTypeRef().getFullName()).replace("{objectId}", created.get(0).getObjectId()).replace("{application}", getName()));
        String uri2 = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{entityName}", created.get(1).getTypeRef().getFullName()).replace("{objectId}", created.get(1).getObjectId()).replace("{application}", getName()));        
        
        ObjectNode jsonInstance1 = executeJsonMethod(200,
                new GetMethod(uri1.toString()));
        ObjectNode links = jsonNodeFactory.objectNode();
        jsonInstance1.put("links", links);
        
        ObjectNode myClass2 = jsonNodeFactory.objectNode();
        links.put("myClass2", myClass2);
        myClass2.set("uri", new TextNode(uri2.toString()));

        PutMethod putMethod1 = new PutMethod(uri1.toString());
        putMethod1.setRequestEntity(new StringRequestEntity(jsonInstance1.toString(), "application/json", "UTF-8"));

        ObjectNode updated1 = (ObjectNode) executeJsonMethod(200, putMethod1);
        ((ObjectNode)updated1.get("links")).set("myClass2", jsonNodeFactory.nullNode());
        
        PutMethod putMethod2 = new PutMethod(uri1.toString());
        putMethod2.setRequestEntity(new StringRequestEntity(updated1.toString(), "application/json", "UTF-8"));
        ObjectNode updated2 = (ObjectNode) executeJsonMethod(200, putMethod2);
        
        assertNotNull(updated2.get("links"));
        assertNull(updated2.get("links").get("myClass2"));
        
        ObjectNode retrieved = executeJsonMethod(200,
                new GetMethod(jsonInstance1.get("uri").textValue()));
        assertNotNull(retrieved.get("links"));
        assertNull(retrieved.get("links").get("myClass2"));
    }    

	private List<Instance> testUpdateInstanceSetup() throws IOException, CoreException {
        Task<List<Instance>> fixture = (Resource<?> resource) -> {
    		List<Instance> created = new LinkedList<>();
    		Repository repository = resource.getFeature(Repository.class);
    		Instance instance1 = repository.newInstance("mypackage", "MyClass1");
    		instance1.setValue("attr1", "value1");
    		created.add(repository.createInstance(instance1));
    		Instance instance2 = repository.newInstance("mypackage", "MyClass2");
    		instance2.setValue("attr2", "value2");
    		created.add(repository.createInstance(instance2));
    		Instance instance3a = repository.newInstance("mypackage", "MyClass3a");
    		instance3a.setValue("attr3", "value3a");
    		created.add(repository.createInstance(instance3a));
    		return created;
        };
        return buildAndRunInRepository(globalModel, fixture);
	}
	
	private <T, S> T buildAndRunInRepository(String model, Task<S> task) throws IOException, CoreException {
	    buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);
	    return runInRepository(task);
	}
	
	private <T, S> T runInRepository(Task<S> task) {
	    T result = (T) RepositoryService.DEFAULT.runTask(getRepositoryURI(), task);
	    return result;
	}

    public void testGetTemplateInstance() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "class MyClass1\n";
        model += "    attribute attr1 : String;\n";
        model += "    attribute attr2 : Integer := 5;\n";
        model += "end;\n";
        model += "end.";

        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        String templateUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", "mypackage.MyClass1").replace("{application}", getName()));
		GetMethod getTemplateInstance = new GetMethod(templateUri.toString());
        executeMethod(200, getTemplateInstance);

        ObjectNode jsonInstance = (ObjectNode) JsonHelper.parse(new InputStreamReader(getTemplateInstance.getResponseBodyAsStream()));

        TestCase.assertNull(((ObjectNode) jsonInstance.get("values")).get("attr1"));
        TestCase.assertEquals(5, ((ObjectNode) jsonInstance.get("values")).get("attr2").asLong());
    }
    
    public void testSignup() throws CoreException, IOException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import base;\n";
        model += "role class User\n";
        model += "    readonly id attribute fullName : String;\n";
        model += "end;\n";
        model += "end.";
        buildProjectAndLoadRepository(Collections.singletonMap("test.tuml", model.getBytes()), true);

        String userEntityName = "mypackage.User";
		String templateUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", userEntityName));
		GetMethod getTemplateInstance = new GetMethod(templateUri);
        ObjectNode template = (ObjectNode) executeJsonMethod(200, getTemplateInstance);
        String fullName = getName() + " Chaves";
		((ObjectNode) template.get("values")).put("fullName", fullName);
        final String username = getName() + "@foo.com-" + UUID.randomUUID().toString();
        ObjectNode createdRole = signUp(template, userEntityName, username, "pass", 201);

        // double sign up should fail
        signUp(template, userEntityName, username, "pass", 400);
        
        ObjectNode index = executeJsonMethod(200, new GetMethod(resolveApplicationURI(Paths.ROOT_PATH)));
        assertNull(index.get("currentUser"));

        // can login
        login(username, "pass");
        
        index = executeJsonMethod(200, new GetMethod(resolveApplicationURI(Paths.ROOT_PATH)));
        JsonNode currentUserUri = index.get("currentUser");
		assertNotNull(currentUserUri);
        JsonNode roleLinkUri = index.get("currentUserRoles").get(userEntityName);
        assertNotNull(roleLinkUri);
        ObjectNode currentRole = executeJsonMethod(200, new GetMethod(roleLinkUri.asText()));
        assertEquals(fullName, currentRole.get("values").get("fullName").textValue());
        
        ObjectNode currentUser = executeJsonMethod(200, new GetMethod(currentUserUri.asText()));
        assertEquals(username, currentUser.get("values").get("username").textValue());
        assertNull(currentUser.get("values").get("password"));
    }

    protected <T extends JsonNode> T signUp(ObjectNode roleRepresentation, String roleEntityName, String username, String password, int expected) throws HttpException, IOException {
    	
    	String signUpURI = resolveApplicationURI(Paths.SIGNUP_PATH.replace("{roleEntityName}", roleEntityName));
    	PostMethod signUpMethod = new PostMethod(signUpURI.toString());
    	String encodedCredentials = Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8));
		signUpMethod.addRequestHeader("X-Kirra-Credentials", encodedCredentials);
        signUpMethod.setRequestEntity(new StringRequestEntity(roleRepresentation.toString(), "application/json", "UTF-8"));
        return executeJsonMethod(expected, signUpMethod);
    	
//    	String templateUri = resolveApplicationURI(Paths.INSTANCE_PATH.replace("{objectId}", "_template").replace("{entityName}", roleEntityName));
//    	GetMethod getTemplateInstance = new GetMethod(templateUri);
//    	
//    	ObjectNode template = (ObjectNode) executeJsonMethod(200, getTemplateInstance);
//    	ObjectNode values = (ObjectNode) template.get("values");
//    	values.put("username", username);
//    	values.put("password", password);
//    	PostMethod createMethod = new PostMethod(resolveApplicationURI(Paths.INSTANCES_PATH.replace("{entityName}", roleEntityName).replace("{application}", getName())));
//    	createMethod.setRequestEntity(new StringRequestEntity(template.toString(), "application/json", "UTF-8"));
    }
    
    @Override
    protected void login(String username, String password) throws HttpException, IOException {
        String loginURI = resolveApplicationURI(Paths.LOGIN_PATH);
		PostMethod loginMethod = new PostMethod(loginURI);
		restHelper.setCredentials(username, password, getName() + "-realm", URI.create(loginURI));
        restHelper.executeMethod(204, loginMethod);
    }

	private String resolveApplicationURI(String toResolve) throws IOException, HttpException {
		URI workspaceURI = getWorkspaceBaseURI();
		String placeholderFreeURI = workspaceURI.resolve(toResolve.replace("{application}", getName())).toString();
		Assert.assertFalse(placeholderFreeURI.contains("{"));
		return placeholderFreeURI;
	}
    
    @Override
    protected void logout() throws HttpException, IOException {
    	// TODO Auto-generated method stub
    	
    }
    


    @Override
    protected void tearDown() throws Exception {
        if (authenticatorRegistration != null)
            authenticatorRegistration.unregister();
        super.tearDown();
    }
}
