package com.abstratt.kirra.auth;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Repository;
import com.abstratt.resman.ResourceManager;

/**
 * An authentication service that authenticates users via 
 * a database lookup in the current application.
 */
public class DatabaseAuthenticationService implements AuthenticationService {

	@Override
	public boolean authenticate(String username, String password) {
		Repository repository = ResourceManager.getCurrentResourceManager().getCurrentResource().getFeature(Repository.class);
		List<Entity> userDataEntities = repository.getAllEntities().stream().filter(e -> e.isUser()).collect(Collectors.toList());
		List<Operation> finders = userDataEntities.stream().map(e -> e.getOperation("findByUsernameAndPassword")).filter(it -> it != null).collect(Collectors.toList());
		List<Instance> results = new LinkedList<>();
		finders.forEach(finder -> repository.executeOperation(finder, null, Arrays.asList(username, password)).forEach(match -> results.add((Instance) match)));
		if (results.size() != 1)	
			return false;
		return true;
	}

	@Override
	public boolean createUser(String username, String password) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resetPassword(String username) {
		// TODO Auto-generated method stub
		return false;
	}

}
