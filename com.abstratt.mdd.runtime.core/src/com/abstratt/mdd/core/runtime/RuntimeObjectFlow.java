package com.abstratt.mdd.core.runtime;

public class RuntimeObjectFlow {
	private RuntimeObjectNode source;
	private RuntimeObjectNode target;

	public RuntimeObjectFlow(RuntimeObjectNode source, RuntimeObjectNode target) {
		super();
		this.source = source;
		this.source.addOutgoing(this);
		this.target = target;
		this.target.addIncoming(this);
	}

	public RuntimeObjectNode getSource() {
		return source;
	}

	public RuntimeObjectNode getTarget() {
		return target;
	}
	public void tryToTransfer() {
		try {
			target.basicAddValue(source.consumeValue());
		} catch (NoDataAvailableException e) {
			// no data available
		}
	}
}
