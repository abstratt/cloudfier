package com.abstratt.nodestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Basic implementation for {@link INode}.
 */
public class BasicNode implements INode, Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Collection<INode>> children;
    private INodeKey key;
    private Map<String, Object> properties;
    private Map<String, Collection<NodeReference>> related;
    private INode parent;

    public BasicNode(INode source) {
        setKey(source.getKey());
        setProperties(source.getProperties());
        setChildren(source.getChildren());
        setRelated(source.getRelated());
    }

    public BasicNode(INodeKey key) {
        this();
        setKey(key);
    }

    public BasicNode(INodeKey key, Map<String, Object> map) {
        this(key);
        setProperties(map);
    }

    public BasicNode(Long key) {
        this();
        setKey(new IntegerKey(key));
    }

    private BasicNode() {
        children = new HashMap<String, Collection<INode>>();
        properties = new HashMap<String, Object>();
        related = new HashMap<String, Collection<NodeReference>>();
    }

    @Override
    public BasicNode clone() {
        BasicNode clone = new BasicNode(this);
        clone.key = key;
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicNode other = (BasicNode) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    @Override
    public Map<String, Collection<INode>> getChildren() {
        return copyNodes(children);
    }

    @Override
    public INodeKey getKey() {
        return key;
    }

    @Override
    public INode getParent() {
        return parent;
    }

    @Override
    public Map<String, Object> getProperties() {
        return new HashMap<String, Object>(properties);
    }

    @Override
    public Map<String, Object> getProperties(boolean readonly) {
        return readonly ? Collections.unmodifiableMap(properties) : getProperties();
    }

    @Override
    public Map<String, Collection<NodeReference>> getRelated() {
        return copyReferences(this.related);
    }

    public Map<String, Collection<INode>> getSharedChildren() {
        return children;
    }

    public Map<String, Object> getSharedProperties() {
        return properties;
    }

    public Map<String, Collection<NodeReference>> getSharedRelated() {
        return this.related;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean isPropertySet(String nodeProperty) {
        return this.properties.containsKey(nodeProperty);
    }

    @Override
    public boolean isTopLevel() {
        return this.parent == null;
    }

    @Override
    public void setChildren(Map<String, Collection<INode>> sourceChildren) {
        this.children = copyNodes(sourceChildren);
        Collection<Collection<INode>> values = this.children.values();
        for (Collection<INode> collection : values)
            for (INode child : collection)
                ((BasicNode) child).parent = this;
    }

    public void setKey(INodeKey key) {
        this.key = key;
    }

    @Override
    public void setProperties(Map<String, Object> values) {
        this.properties = new HashMap<String, Object>(values);
    }

    @Override
    public void setRelated(Map<String, Collection<NodeReference>> related) {
        this.related = copyReferences(related);
    }

    @Override
    public String toString() {
        return "values: " + this.properties + " - children: " + this.children + " - related: " + this.related;
    }

    /**
     * Returns a deep copy of the given collection of nodes.
     * 
     * @param source
     *            collection to copy
     * @return a deep copy of the given source collection of nodes
     */
    private Map<String, Collection<INode>> copyNodes(Map<String, Collection<INode>> source) {
        Map<String, Collection<INode>> copy = new HashMap<String, Collection<INode>>(source.size());
        for (Entry<String, Collection<INode>> entry : source.entrySet()) {
            Collection<INode> roleNodes = entry.getValue();
            List<INode> roleChildren = new ArrayList<INode>(roleNodes.size());
            for (INode sourceChild : roleNodes)
                roleChildren.add(new BasicNode(sourceChild));
            copy.put(entry.getKey(), roleChildren);
        }
        return copy;
    }

    /**
     * Returns a deep copy of the given collection of node references.
     * 
     * @param source
     *            collection to copy
     * @return a deep copy of the given source collection of node references
     */
    private Map<String, Collection<NodeReference>> copyReferences(Map<String, Collection<NodeReference>> source) {
        Map<String, Collection<NodeReference>> copy = new HashMap<String, Collection<NodeReference>>(source.size());
        for (Entry<String, Collection<NodeReference>> entry : source.entrySet())
            copy.put(entry.getKey(), new ArrayList<NodeReference>(entry.getValue()));
        return copy;
    }
}