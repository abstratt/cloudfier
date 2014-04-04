package com.abstratt.kirra;

import java.util.List;

public interface BehaviorScope extends NameScope {
	List<Operation> getOperations();
	Operation getOperation(String name);
}
