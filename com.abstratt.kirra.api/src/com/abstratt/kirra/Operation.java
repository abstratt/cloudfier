package com.abstratt.kirra;

import java.util.List;

public class Operation extends TypedElement<BehaviorScope> {
	public enum OperationKind {
		Action,
		Finder,
		Retriever,
		Event
	}
	
	private static final long serialVersionUID = 1L;
	private boolean instanceOperation;
	private OperationKind kind;
	private List<Parameter> parameters;
	
	public Operation() {
		
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public boolean isInstanceOperation() {
		return instanceOperation;
	}

	public void setInstanceOperation(boolean instanceOperation) {
		this.instanceOperation = instanceOperation;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (instanceOperation ? 1231 : 1237);
		result = prime * result
				+ ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		Operation other = (Operation) obj;
		if (instanceOperation != other.instanceOperation)
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		return true;
	}
	
	public OperationKind getKind() {
		return kind;
	}
	public void setKind(OperationKind kind) {
		this.kind = kind;
	}

	public Parameter getParameter(String parameterName) {
		for (Parameter current : parameters)
			if (parameterName.equals(current.getName()))
				return current;
		return null;
	}
}
