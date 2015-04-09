package com.abstratt.nodestore;

public class IntegerKey implements INodeKey {
    private long innerKey;

    public IntegerKey(long innerKey) {
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
        IntegerKey other = (IntegerKey) obj;
        return innerKey == other.innerKey;
    }

    public long getInnerKey() {
        return innerKey;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(innerKey).hashCode();
    }

    @Override
    public String toString() {
        return Long.toString(innerKey);
    }
}
