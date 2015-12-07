package com.abstratt.nodestore.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.tests.runtime.AbstractRuntimeTests;
import com.abstratt.nodestore.INode;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.INodeStore;

public abstract class AbstractNodeStoreTests extends AbstractRuntimeTests {
    private final static String library = "package datatypes;\n" + "primitive Integer;\n" + "primitive String;\n" + "primitive Boolean;\n"
            + "primitive Date;\n" + "end.";

    public AbstractNodeStoreTests(String name) {
        super(name);
    }

    // 1:many references no updateable on the one side
    public void _testCreateAndRetrieveNodeWithChildren() {
        // INodeStore store = catalog.createStore("mypackage.MyClass1");
        // INode node = catalog.newNode();
        // INode child = catalog.newNode();
        // child.setProperties(Collections.<String, Object>
        // singletonMap("childProperty", "childValue"));
        // node.setChildren(Collections.<String, Collection<INode>>
        // singletonMap("children", Collections.singleton(child)));
        // INodeKey key = store.createNode(node);
        // assertNotNull(key);
        // INode loaded = store.getNode(key);
        // Collection<INode> children = loaded.getChildren().get("children");
        // assertEquals(1, children.size());
        // INode loadedChild = children.iterator().next();
        // assertEquals("childValue",
        // loadedChild.getProperties().get("childProperty"));
        // assertNotNull(loadedChild.getKey());
        // assertEquals(1, store.getNodes().size());
    }

    public void _testCreateAndRetrieveNodeWithRelated() {
        // INodeStore store = catalog.createStore("mypackage.MyClass1");
        // INode node = catalog.newNode();
        //
        // NodeReference ref1 = new NodeReference("mypackage.MyClass2", new
        // IntegerKey(999));
        // NodeReference ref2 = new NodeReference("mypackage.MyClass3", new
        // IntegerKey(1000));
        // node.setRelated(Collections.<String, Collection<NodeReference>>
        // singletonMap("related", Arrays.asList(ref1, ref2)));
        // INodeKey key = store.createNode(node);
        // assertNotNull(key);
        // INode loaded = store.getNode(key);
        // Collection<NodeReference> related =
        // loaded.getRelated().get("related");
        // assertEquals(2, related.size());
        // assertEquals(new HashSet<NodeReference>(Arrays.asList(ref1, ref2)),
        // new HashSet<NodeReference>(related));
    }

    public void buildModel() throws CoreException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "import datatypes;\n";
        model += "[Entity]class MyClass1\n";
        model += "attribute attr1 : Integer[0,1];\n";
        model += "attribute attr2 : String[0,1];\n";
        model += "attribute attr3 : Date[0,1];\n";
        model += "end;\n";
        model += "end.";
        parseAndCheck(model, AbstractNodeStoreTests.library);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // INodeStoreFactory factory =
        // NodeStores.get().getFactory(getFactoryName());
        // assertNotNull(factory);
        // catalog = factory.createCatalog("mypackage", getKirraSchema());
        //
        // runInRuntime(new Runnable() {
        // public void run() {
        // catalog.prime();
        // catalogPrimed = true;
        // }
        // });
        // catalog.beginTransaction();
        // this.transactionStarted = true;
    }

    public void testCreateAndRetrieveNode() {
        INodeStore store = getCatalog().createStore("mypackage.MyClass1");
        INode node = getCatalog().newNode("mypackage.MyClass1");
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("attr2", "bar");
        values.put("attr3", today());
        node.setProperties(values);
        INodeKey key = store.createNode(node);
        TestCase.assertNotNull(key);
        INode loaded = store.getNode(key);
        TestCase.assertNotNull(loaded);
        TestCase.assertEquals(values, loaded.getProperties());
    }

    public void testDeleteNode() {
        INodeStore store = getCatalog().createStore("mypackage.MyClass1");
        INode node = getCatalog().newNode("mypackage.MyClass1");
        INodeKey key = store.createNode(node);
        TestCase.assertNotNull(store.getNode(key));
        store.deleteNode(key);
        TestCase.assertNull(store.getNode(key));
    }

    public void testFactory() {
        INodeStore created = getCatalog().createStore("mypackage.MyClass1");
        TestCase.assertNotNull(created);
        TestCase.assertEquals("mypackage.MyClass1", created.getName());
        TestCase.assertNotNull(getCatalog().getStore("mypackage.MyClass1"));
        Object key = created.createNode(getCatalog().newNode("mypackage.MyClass1"));
        TestCase.assertTrue("" + key, created.getNodeKeys().contains(key));
        INodeStore recreated = getCatalog().createStore("mypackage.MyClass1");
        TestCase.assertTrue(recreated.getNodeKeys().contains(key));
        TestCase.assertEquals("mypackage.MyClass1", created.getName());
        getCatalog().deleteStore("mypackage.MyClass1");
        getCatalog().createStore("mypackage.MyClass1");
        TestCase.assertNotNull(getCatalog().getStore("mypackage.MyClass1"));
    }

    public void testListNodes() {
        INodeStore store = getCatalog().createStore("mypackage.MyClass1");
        INode node1 = getCatalog().newNode("mypackage.MyClass1");
        INode node2 = getCatalog().newNode("mypackage.MyClass1");
        Collection<INode> loaded = store.getNodes();
        // nothing persisted yet
        TestCase.assertEquals(0, loaded.size());
        // create 1st node
        INodeKey key1 = store.createNode(node1);
        loaded = store.getNodes();
        TestCase.assertEquals(1, loaded.size());
        TestCase.assertEquals(key1, loaded.iterator().next().getKey());
        // create 2nd node
        Object key2 = store.createNode(node2);
        loaded = store.getNodes();
        TestCase.assertEquals(2, loaded.size());
        Set<Object> expected = new HashSet<Object>(Arrays.asList(key1, key2));
        for (INode node : loaded)
            TestCase.assertTrue(expected.remove(node.getKey()));
        TestCase.assertTrue(expected.toString(), expected.isEmpty());
        // delete 1st node
        store.deleteNode(key1);
        loaded = store.getNodes();
        TestCase.assertEquals(1, loaded.size());
        TestCase.assertEquals(key2, loaded.iterator().next().getKey());
    }

    public void testNodeIsolation() {
        INode node = getCatalog().newNode("mypackage.MyClass1");
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("name", "bar");
        node.setProperties(values);
        // changing the original map directly should not affect the node state
        values.put("name", "foo");
        TestCase.assertEquals("bar", node.getProperties().get("name"));
        // changing the map returned by the node should not affect the node
        // state
        node.getProperties().put("name", "foo");
        TestCase.assertEquals("bar", node.getProperties().get("name"));
    }

    public void testUpdateNode() {
        INodeStore store = getCatalog().createStore("mypackage.MyClass1");
        INode node = getCatalog().newNode("mypackage.MyClass1");
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("attr2", "foo");
        node.setProperties(values);
        INodeKey key = store.createNode(node);
        node = store.getNode(key);
        // update property value
        values.put("attr2", "bar");
        node.setProperties(values);
        store.updateNode(node);
        node = store.getNode(key);
        TestCase.assertEquals(values, node.getProperties());
        // add new property
        values.put("attr3", today());
        node.setProperties(values);
        store.updateNode(node);
        node = store.getNode(key);
        TestCase.assertEquals(values, node.getProperties());
        // remove existing property
        values.remove("attr2");
        node.setProperties(values);
        store.updateNode(node);
        node = store.getNode(key);
        TestCase.assertEquals(values, node.getProperties());
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        // so the kirra profile is available as a system package (no need to
        // load)
        defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString());
        return defaultSettings;
    }

    protected abstract String getFactoryName();

    @Override
    protected void originalRunTest() throws Throwable {
        buildModel();
        super.originalRunTest();
    }

    @Override
    protected void tearDown() throws Exception {
        // if (transactionStarted)
        // catalog.abortTransaction();
        // if (catalogPrimed)
        // catalog.zap();
        super.tearDown();
    }

    private Date today() {
        Date today = new Date(0);
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);
        return today;
    }
}
