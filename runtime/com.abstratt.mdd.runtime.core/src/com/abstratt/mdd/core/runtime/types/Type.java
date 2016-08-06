package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public interface Type {
	BooleanType equals(ExecutionContext context, Type another);
	BooleanType notEquals(ExecutionContext context, Type another);
}
