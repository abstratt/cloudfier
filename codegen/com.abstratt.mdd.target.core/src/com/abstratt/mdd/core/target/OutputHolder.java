package com.abstratt.mdd.core.target;
public abstract class OutputHolder<T> {
	private T held;
    public OutputHolder(T held) {
		super();
		this.held = held;
	}

    
	public T get() {
		return held;
	}


	public abstract byte[] getBytes();
	public abstract CharSequence getChars();
}