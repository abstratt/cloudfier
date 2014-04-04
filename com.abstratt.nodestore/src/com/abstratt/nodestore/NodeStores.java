package com.abstratt.nodestore;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;

import com.abstratt.pluginutils.LogUtils;
import com.abstratt.pluginutils.RegistryReader;

public class NodeStores {
	private final static NodeStores instance = new NodeStores();
	public static NodeStores get() {
		return instance;
	}
	private NodeStores() {}
	private final class RegistryParser extends RegistryReader {
		private final String factoryNameToMatch;

		private RegistryParser(String name) {
			this.factoryNameToMatch = name;
		}

		@Override
		protected boolean readElement(IConfigurationElement element) {
			if (factories.containsKey(factoryNameToMatch))
				return true;
			String factoryName = element.getAttribute("name");
			if (factoryName.equals(factoryNameToMatch)) {
				INodeStoreFactory factory = null;
				try {
					factory  = (INodeStoreFactory) element.createExecutableExtension(ATT_CLASS);
				} catch (CoreException e) {
					LogUtils.log(e.getStatus());
				}
				// register even a null factory so we don't keep trying
				factories.put(factoryNameToMatch, factory);
			}
			return true;
		}

		@Override
		protected String getNamespace() {
			return ID;
		}
	}

	private static final String ID = NodeStores.class.getPackage().getName();
	private static final String FACTORIES_XP = ID + ".factories";
	private Map<String, INodeStoreFactory> factories = new HashMap<String, INodeStoreFactory>();
	
	public INodeStoreFactory getFactory(final String name) {
		if (!factories.containsKey(name))
		    loadFactory(name);	
		INodeStoreFactory selected = factories.get(name);
		Assert.isTrue(selected != null, "No factory named " + name);
		return selected;
	}
	
	public INodeStoreFactory getDefaultFactory() {
		return getFactory("jdbc");
	}

	private void loadFactory(final String name) {
		new RegistryParser(name).readRegistry(RegistryFactory.getRegistry(), FACTORIES_XP);
	}
	
	public void reset() {
		factories = new HashMap<String, INodeStoreFactory>();
	}
}
