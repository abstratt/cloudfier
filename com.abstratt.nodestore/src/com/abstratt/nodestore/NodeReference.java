package com.abstratt.nodestore;

import java.io.Serializable;

/**
 * A reference to a node.
 */
public class NodeReference implements Serializable {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private INodeKey key;

    public NodeReference(String storeName, INodeKey key) {
        this.storeName = storeName;
        this.key = key;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeReference other = (NodeReference) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (storeName == null) {
            if (other.storeName != null)
                return false;
        } else if (!storeName.equals(other.storeName))
            return false;
        return true;
    }

    public INodeKey getKey() {
        return key;
    }

    public String getStoreName() {
        return storeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (storeName == null ? 0 : storeName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return storeName + ":" + key;
    }
}