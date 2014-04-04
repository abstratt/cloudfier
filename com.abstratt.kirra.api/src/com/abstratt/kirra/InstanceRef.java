package com.abstratt.kirra;

public class InstanceRef {
	private String entityName;
	private String entityNamespace;
	private String objectId;

	public String getEntityName() {
		return entityName;
	}

	public String getEntityNamespace() {
		return entityNamespace;
	}

	public String getObjectId() {
		return objectId;
	}

	public InstanceRef(String entityNamespace, String entityName, String objectId) {
		super();
		this.entityNamespace = entityNamespace;
		this.entityName = entityName;
		this.objectId = objectId;
	}
	
	@Override
	public String toString() {
		return TypeRef.toString(entityNamespace, entityName) + '@' + objectId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
		result = prime * result + ((entityNamespace == null) ? 0 : entityNamespace.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceRef other = (InstanceRef) obj;
		if (entityName == null) {
			if (other.entityName != null)
				return false;
		} else if (!entityName.equals(other.entityName))
			return false;
		if (entityNamespace == null) {
			if (other.entityNamespace != null)
				return false;
		} else if (!entityNamespace.equals(other.entityNamespace))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		return true;
	}
}
