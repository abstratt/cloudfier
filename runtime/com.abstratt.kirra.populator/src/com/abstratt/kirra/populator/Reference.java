package com.abstratt.kirra.populator;

public class Reference {
    public static Reference parse(String currentNamespace, String referenceString) {
        int addressSeparatorIndex = referenceString.indexOf('@');
        if (addressSeparatorIndex <= 0 || addressSeparatorIndex == referenceString.length() - 1)
            return null;
        int position;
        try {
            position = Integer.parseInt(referenceString.substring(addressSeparatorIndex + 1)) - 1;
        } catch (NumberFormatException e) {
            return null;
        }
        String entityName = referenceString.substring(0, addressSeparatorIndex);
        int namespaceSeparatorIndex = entityName.indexOf('.');
        if (namespaceSeparatorIndex == 0 || namespaceSeparatorIndex == entityName.length() - 1)
            return null;
        String namespaceName;
        if (namespaceSeparatorIndex == -1)
            namespaceName = currentNamespace;
        else {
            namespaceName = entityName.substring(0, namespaceSeparatorIndex);
            entityName = entityName.substring(namespaceSeparatorIndex + 1);
        }
        return new Reference(namespaceName, entityName, position);
    }

    private String namespace;
    private String entity;

    private int index;

    public Reference(String namespace, String entity, int index) {
        super();
        this.namespace = namespace;
        this.entity = entity;
        this.index = index;
    }

    public String getEntity() {
        return entity;
    }

    public int getIndex() {
        return index;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String currentNamespace) {
        this.namespace = currentNamespace;
    }

    @Override
    public String toString() {
        return (namespace == null ? "" : namespace + '.') + entity + '@' + index;
    }
}