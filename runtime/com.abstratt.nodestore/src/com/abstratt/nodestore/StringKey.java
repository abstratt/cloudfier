package com.abstratt.nodestore;

public class StringKey implements INodeKey {
    private String innerKey;

    public StringKey(String innerKey) {
        assert innerKey != null;
        this.innerKey = innerKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringKey other = (StringKey) obj;
        if (innerKey == null) {
            if (other.innerKey != null)
                return false;
        } else if (!innerKey.equals(other.innerKey))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (innerKey == null ? 0 : innerKey.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return innerKey;
    }
}
