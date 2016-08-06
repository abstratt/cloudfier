package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public interface ComparableType extends Type{
	BooleanType greaterThan(ExecutionContext context, ComparableType other);
	BooleanType lowerThan(ExecutionContext context, ComparableType other);
    BooleanType greaterOrEquals(ExecutionContext context, ComparableType other);
	BooleanType lowerOrEquals(ExecutionContext context, ComparableType other);
}
