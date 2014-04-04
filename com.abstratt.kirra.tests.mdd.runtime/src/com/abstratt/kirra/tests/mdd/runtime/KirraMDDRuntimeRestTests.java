package com.abstratt.kirra.tests.mdd.runtime;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.ServiceRegistration;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.resman.Resource;
import com.abstratt.resman.Task;

public class KirraMDDRuntimeRestTests extends AbstractKirraRestTests {

	Map<String, String> authorized = new HashMap<String, String>();

	protected ServiceRegistration<AuthenticationService> authenticatorRegistration;

	public KirraMDDRuntimeRestTests(String name) {
		super(name);
	}
	
	public void testNewSession() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "import base;\n";
		model += "apply kirra;\n";
		model += "[User] class User attribute single : String; end;\n";
		model += "end.";
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				assertTrue(repository.isOpen());
				assertEquals(1, repository.getEntities("mypackage").size());
				assertEquals("User", repository.getEntities("mypackage").get(0).getName());
				return null;
			}
		});
	}
	
	public void testGetEntityList() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "class MyClass1 attribute a : String; end;\n";
		model += "class MyClass2 attribute a : Integer; end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		GetMethod getDomainTypes = new GetMethod(sessionURI.resolve("entities/").toASCIIString());
		executeMethod(200, getDomainTypes);
		
		ArrayNode entities = (ArrayNode) JsonHelper.parse(new InputStreamReader(getDomainTypes.getResponseBodyAsStream()));
		assertEquals(2, entities.size());
		
		assertEquals("MyClass1", entities.get(0).get("name").getTextValue());
		assertEquals("MyClass2", entities.get(1).get("name").getTextValue());
		
        for (JsonNode jsonNode : entities) {
        	assertEquals("mypackage", jsonNode.get("namespace").getTextValue());
        	assertNotNull(jsonNode.get("uri"));
        	executeMethod(200, new GetMethod(jsonNode.get("uri").toString()));
		}
	}

	public void testGetServiceList() throws CoreException,IOException {
		String source = "";
		source += "model tests;\n";
		source += "interface Calculator\n";
		source += "    operation addNumbers(number1 : Integer, number2 : Integer) : Integer;\n";
		source += "end;\n";
		source += "external class CalculatorService implements Calculator\n";
		source += "end;\n";
		source += "component CalculatorComponent\n";
		source += "    composition calculatorService : CalculatorService;\n";
		source += "    provided port calculator : Calculator connector calculatorService;\n";
		source += "end;\n";
		source += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", source.getBytes()), true);

		URI sessionURI = getWorkspaceURI();
		GetMethod getDomainTypes = new GetMethod(sessionURI.resolve("services/").toASCIIString());
		executeMethod(200, getDomainTypes);
		
		ArrayNode entities = (ArrayNode) JsonHelper.parse(new InputStreamReader(getDomainTypes.getResponseBodyAsStream()));
		assertEquals(1, entities.size());
		
		assertEquals("CalculatorService", entities.get(0).get("name").getTextValue());
        assertEquals("tests", entities.get(0).get("namespace").getTextValue());
        assertNotNull(entities.get(0).get("uri"));
        executeMethod(200, new GetMethod(entities.get(0).get("uri").toString()));
    }
	public void testGetInstanceList() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1 attribute a : Integer[0,1]; end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);

		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
				repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
				return null;
			}
		});
		
		URI sessionURI = getWorkspaceURI();
		
		GetMethod getInstances = new GetMethod(sessionURI.resolve("instances/mypackage.MyClass1/").toASCIIString());
		executeMethod(200, getInstances);
		
		ArrayNode instances = (ArrayNode) JsonHelper.parse(new InputStreamReader(getInstances.getResponseBodyAsStream()));
		assertEquals(2, instances.size());
		
        for (JsonNode jsonNode : instances) {
        	assertNotNull(jsonNode.get("uri"));
        	executeMethod(200, new GetMethod(jsonNode.get("uri").toString()));
        	assertNotNull(jsonNode.get("type"));
        	executeMethod(200, new GetMethod(jsonNode.get("type").toString()));
		}
	}
	
	public void testQuery() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : Integer;\n";
		model += "    [Finder] static operation findAttr1GreaterThan(value : Integer) : MyClass1[*];\n";
		model += "    begin\n";
		model += "        return MyClass1 extent.select((a : MyClass1) : Boolean { a.attr1 > value });\n";
		model += "    end;\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		
		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				Instance instance1 = repository.newInstance("mypackage", "MyClass1");
				instance1.setValue("attr1", 10);
				Instance instance2 = repository.newInstance("mypackage", "MyClass1");
				instance2.setValue("attr1", 20);
				Instance instance3 = repository.newInstance("mypackage", "MyClass1");
				instance3.setValue("attr1", 30);
				repository.createInstance(instance1);
				repository.createInstance(instance2);
				repository.createInstance(instance3);
				return null;
			}
		});
		
		PostMethod queryMethod = new PostMethod(sessionURI.resolve("finders/mypackage.MyClass1/findAttr1GreaterThan").toString());
		queryMethod.setRequestEntity(new StringRequestEntity("{value: 15}","application/json", "UTF-8"));

		ArrayNode instances = (ArrayNode) executeJsonMethod(200, queryMethod);
		assertEquals(2, instances.size());
		
        for (JsonNode jsonNode : instances) {
        	assertNotNull(jsonNode.get("uri"));
        	assertNotNull(jsonNode.get("type"));
        	executeMethod(200, new GetMethod(jsonNode.get("type").toString()));
        	JsonNode instance = executeJsonMethod(200, new GetMethod(jsonNode.get("uri").toString()));
        	assertTrue(instance.get("values").get("attr1").getLongValue() > 15);
		}

		queryMethod.setRequestEntity(new StringRequestEntity("{value: 20}","application/json", "UTF-8"));

        instances = (ArrayNode) executeJsonMethod(200, queryMethod);
        assertEquals(1, instances.size());
        JsonNode instance = executeJsonMethod(200, new GetMethod(instances.get(0).get("uri").toString()));
        assertEquals(30, instance.get("values").get("attr1").getLongValue());
	}

	/**
	 * Shows how a service that provides data can be accessed.
	 */
	public void testServiceDataAccess() throws CoreException,IOException {
		String source = "";
		source += "model tests;\n";
		source += "interface Calculator\n";
		source += "    operation addNumbers(number1 : Integer, number2 : Integer) : Integer;\n";
		source += "end;\n";
		source += "class CalculatorService implements Calculator\n";
		source += "    operation addNumbers(number1 : Integer, number2 : Integer) : Integer;\n";
		source += "    begin\n";
		source += "        return number1 + number2;\n";
		source += "    end;\n";
		source += "end;\n";
		source += "component CalculatorComponent\n";
		source += "    composition calculatorService : CalculatorService;\n";
		source += "    provided port calculator : Calculator connector calculatorService;\n";
		source += "end;\n";
		source += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", source.getBytes()), true);
		URI sessionURI = getWorkspaceURI();

		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				Operation operation = repository.getService("tests", "CalculatorService").getOperation("addNumbers");
				List<?> result = repository.executeOperation(operation, null, Arrays.asList(31, 11));
				assertEquals(1, result.size());
				assertEquals(42l, result.get(0));
				return null;
			}
		});
		
		GetMethod queryMethod = new GetMethod(sessionURI.resolve("retrievers/tests.CalculatorService/addNumbers?number1=31&number2=11").toString());
		JsonNode jsonResult = executeJsonMethod(200, queryMethod);
        assertNotNull(jsonResult.get("data"));
        ArrayNode asArray = (ArrayNode) jsonResult.get("data");
        assertEquals(1, asArray.size());
        assertEquals(42L, asArray.get(0).getLongValue());
	}

	public void testAction() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : Integer;\n";
		model += "    [Action] operation add(value : Integer);\n";
		model += "    begin\n";
		model += "        self.attr1 := self.attr1 + value;\n";
		model += "    end;\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		final Instance created = RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Instance>() {
			@Override
			public Instance run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				Instance fresh = repository.newInstance("mypackage", "MyClass1");
				fresh.setValue("attr1", 10);
				return repository.createInstance(fresh);
			}
		});
		
		PostMethod post = new PostMethod(sessionURI.resolve("instances/mypackage.MyClass1/" + created.getObjectId() + "/actions/add").toString());
		post.setRequestEntity(new StringRequestEntity("{value: 15}","application/json", "UTF-8"));
		ObjectNode result = (ObjectNode) executeJsonMethod(200, post);
		
		assertEquals(25, result.path("values").path("attr1").getIntValue());

		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				Instance reloaded = repository.getInstance(created.getEntityNamespace(), created.getEntityName(), created.getObjectId(), false);
				assertEquals(25L, reloaded.getValue("attr1"));
				return null;
			}
		});
	}
	
	public void testGetInstance() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : String;\n";
		model += "    attribute attr2 : Integer;\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		
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

		ObjectNode jsonInstance = executeJsonMethod(200, new GetMethod(sessionURI.resolve("instances/mypackage.MyClass1/" + created.getObjectId()).toASCIIString()));
		
		assertNotNull(jsonInstance.get("uri"));
    	executeMethod(200, new GetMethod(jsonInstance.get("uri").toString()));
    	assertNotNull(jsonInstance.get("type"));
    	executeMethod(200, new GetMethod(jsonInstance.get("type").toString()));

    	ObjectNode values = (ObjectNode) jsonInstance.get("values");
    	assertEquals("The answer is", values.get("attr1").getTextValue());
    	assertEquals(42L, values.get("attr2").asLong());
	}
	
	public void testGetTemplateInstance() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : String;\n";
		model += "    attribute attr2 : Integer := 5;\n";
		model += "end;\n";
		model += "end.";
		
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();

		GetMethod getTemplateInstance = new GetMethod(sessionURI.resolve("instances/mypackage.MyClass1/_template").toASCIIString());
		executeMethod(200, getTemplateInstance);
		
		ObjectNode jsonInstance = (ObjectNode) JsonHelper.parse(new InputStreamReader(getTemplateInstance.getResponseBodyAsStream()));

		assertTrue(jsonInstance.get("uri").isNull());
		assertFalse(NullNode.instance.equals(jsonInstance.get("type")));
		assertNotNull(jsonInstance.get("type"));
    	executeMethod(200, new GetMethod(jsonInstance.get("type").toString()));

    	assertEquals(NullNode.instance, ((ObjectNode) jsonInstance.get("values")).get("attr1"));
    	assertEquals(5, ((ObjectNode) jsonInstance.get("values")).get("attr2").asLong());
	}
	
	/**
	 * Ensures after a profile is created we can see the the current user in the application index.
	 */
	public void testCreateProfile() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User \n";
		// this catches a stack overflow error with a derived property that causes a lookup
		model += "    static derived attribute current : User := { (System#user() as User) };\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), false);
		URI sessionURI = getWorkspaceURI();
		
		signUp(getName(), "pass");
		login(getName(), "pass");
		
		String typeURI = sessionURI.resolve("entities/mypackage.User").toString();
		String requestMarkup = "{'type': '" + typeURI + "'}";
		
		PostMethod createMethod = new PostMethod(sessionURI.resolve("profile").toString());
		createMethod.setRequestEntity(new StringRequestEntity(requestMarkup, "application/json", "UTF-8"));
		ObjectNode created = (ObjectNode) executeJsonMethod(201, createMethod);

		assertEquals(typeURI, created.get("type").asText());
		
		// the root should be pointing to the user just created 
		GetMethod getApplicationRoot = new GetMethod(sessionURI.toASCIIString());
		ObjectNode root = (ObjectNode) executeJsonMethod(200, getApplicationRoot);
		JsonNode profileURI = root.path("currentUser").path("profile").path("uri");
		assertNotNull(profileURI);
		assertEquals(created.get("uri").asText(), profileURI.asText());
	}
	
	
	public void testCreateInstance() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : String;\n";
		model += "    attribute attr2 : Integer := 5;\n";
		model += "    attribute attr3 : Integer := 90;\n";
		model += "    attribute attr4 : Integer[0,1] := 80;\n";
		model += "    attribute attr5 : Integer[0,1];\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		
		GetMethod getTemplateInstance = new GetMethod(sessionURI.resolve("instances/mypackage.MyClass1/_template").toASCIIString());
		
		
		ObjectNode template = (ObjectNode) executeJsonMethod(200, getTemplateInstance);
		
		((ObjectNode) template.get("values")).put("attr1", "foo");
		((ObjectNode) template.get("values")).put("attr3", 100);
		((ObjectNode) template.get("values")).put("attr4", (String) null);
		PostMethod createMethod = new PostMethod(sessionURI.resolve("instances/mypackage.MyClass1/").toString());
		createMethod.setRequestEntity(new StringRequestEntity(template.toString(), "application/json", "UTF-8"));
		
		ObjectNode created = (ObjectNode) executeJsonMethod(201, createMethod);
    	assertEquals("foo", created.get("values").get("attr1").asText());
    	assertEquals(5, created.get("values").get("attr2").asLong());
    	assertEquals(100, created.get("values").get("attr3").asLong());
    	assertEquals(NullNode.instance, created.get("values").get("attr4"));
    	assertEquals(NullNode.instance, created.get("values").get("attr5"));
	}

	public void testDeleteInstance() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User attribute attr1 : String[0,1]; end;\n";
		model += "[Entity] class MyClass1 attribute attr1 : String[0,1]; end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);
		URI sessionURI = getWorkspaceURI();
		
		final Instance created = RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Instance>() {
			@Override
			public Instance run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				Instance created = repository.createInstance(repository.newInstance("mypackage", "MyClass1"));
				
				assertNotNull(repository.getInstance("mypackage", "MyClass1", created.getObjectId(), false));
				return created;
			}
		});
		
		DeleteMethod delete = new DeleteMethod(sessionURI.resolve("instances/mypackage.MyClass1/" + created.getObjectId()).toASCIIString());
		executeMethod(200, delete);
		
		RepositoryService.DEFAULT.runTask(getRepositoryURI(), new Task<Object>() {
			@Override
			public Object run(Resource<?> resource) {
				Repository repository = resource.getFeature(Repository.class);
				assertNull(repository.getInstance("mypackage", "MyClass1", created.getObjectId(), false));
				return null;
			}
		});
		
		executeMethod(404, delete);
	}
	
	public void testSignup() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User attribute attr1 : String[0,1]; end;\n";
		model += "end.";
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), false);

		
		final String username = getName() + "@foo.com";

		// first sign up should work
		signUp(username, "pass", 200);
		
		// double sign up should fail
		signUp(username, "pass", 400);
		
		// can login
		login(username, "pass");
	}

	public void testBadLogin() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User attribute attr1 : String[0,1]; end;\n";
		model += "end.";
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), false);

		URI sessionURI = getWorkspaceURI();
		PostMethod login = new PostMethod(sessionURI.resolve("login").toString());
		login.setRequestEntity(new StringRequestEntity("login=unknownuser&password=pass","application/x-www-form-urlencoded", "UTF-8"));
		executeMethod(401, login);
	}
	
	public void testGetEntity() throws CoreException,IOException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		model += "[User] class User end;\n";
		model += "[Entity] class MyClass1\n";
		model += "    attribute attr1 : String;\n";
		model += "    attribute attr2 : Integer;\n";
		model += "    [Action] operation op1();\n";
		model += "    [Action] operation op2();\n";
		model += "    [Finder] static operation op3() : MyClass1;\n";
		model += "end;\n";
		model += "end.";
		
		buildProjectAndLoadRepository(singletonMap("test.tuml", model.getBytes()), true);

		URI sessionURI = getWorkspaceURI();
		String entityUri = sessionURI.resolve("entities/").resolve("mypackage.MyClass1").toASCIIString();
		GetMethod getEntity = new GetMethod(entityUri);
		executeMethod(200, getEntity);

		ObjectNode jsonEntity = (ObjectNode) JsonHelper.parse(new InputStreamReader(getEntity.getResponseBodyAsStream()));
		
		assertEquals("MyClass1", jsonEntity.get("name").getTextValue());
		assertEquals("mypackage", jsonEntity.get("namespace").getTextValue());
		assertEquals(entityUri, jsonEntity.get("uri").getTextValue());
		
		assertNotNull(jsonEntity.get("instances"));

		assertEquals(this.restHelper.getApiUri().resolve("instances/mypackage.MyClass1").toString(), jsonEntity.get("instances").asText());
		
		assertEquals(Arrays.asList("op1", "op2"), jsonEntity.get("actions").findValuesAsText("name"));
		assertEquals(Arrays.asList("op3"), jsonEntity.get("finders").findValuesAsText("name"));
		assertEquals(Arrays.asList("attr1", "attr2"), jsonEntity.get("properties").findValuesAsText("name"));
		assertEquals(Arrays.asList("String", "Integer"), jsonEntity.get("properties").findValuesAsText("type"));
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (authenticatorRegistration != null)
			authenticatorRegistration.unregister();
		super.tearDown();
	}
}
