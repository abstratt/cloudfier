package com.abstratt.kirra;

import java.util.List;
import java.util.Map;

/**
 * Represents functionality that is external to the Kirra repository.
 */
public interface ExternalService {
	/**
	 * Executes external functionality.
	 *  
	 * @param repository
	 * @param namespace the entity namespace
	 * @param name the entity name
	 * @param operation the operation to execute
	 * @param arguments the arguments to the operation
	 * @return the operation result
	 */
	public List<?> executeOperation(String namespace, String name,
			String operation, Map<String, Object> arguments);
}
