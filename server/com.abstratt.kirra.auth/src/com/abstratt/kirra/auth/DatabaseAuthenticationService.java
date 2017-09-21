package com.abstratt.kirra.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.resman.ResourceManager;

/**
 * An authentication service that authenticates users via 
 * a database lookup in the current application.
 */
public class DatabaseAuthenticationService implements AuthenticationService {

	@Override
	public boolean authenticate(String username, String password) {
		Instance found = findUser(username, password);
		return found != null;
	}

	private Instance findUser(String username, String password) {
		Repository repository = getRepository();
		Entity userEntity = getUserEntity(repository);
		if (userEntity == null)
			return null;
		List<Instance> found = repository.filterInstances(Collections.singletonMap("username", Collections.singletonList(username)), "userprofile", "UserProfile", false);
		if (!found.isEmpty() && password.equals(found.get(0).getValue("password")))
			return found.get(0);
		return null;
	}
	
	@Override
	public List<String> getRoleNames(String username) {
		Repository repository = getRepository();
		Entity userEntity = getUserEntity(repository);
		if (userEntity == null)
			return null;
		List<Instance> found = repository.filterInstances(Collections.singletonMap("username", Collections.singletonList(username)), "userprofile", "UserProfile", false);
		if (found.isEmpty())
			return Collections.emptyList();
		Instance profile = found.get(0);
		Collection<Entity> roleEntities = repository.getRoleEntities();
		return roleEntities.stream()
				.filter(role -> profile.getValue("roleAs" + role.getName()) != null)
				.map(role -> role.getTypeRef().getFullName())
				.collect(Collectors.toList());
	}

	private Entity getUserEntity(Repository repository) {
		Entity userEntity = repository.getEntity(new TypeRef("userprofile", "UserProfile", TypeKind.Entity));
		return userEntity;
	}

	private Repository getRepository() {
		return ResourceManager.getCurrentResourceManager().getCurrentResource().getFeature(Repository.class);
	}

	@Override
	public boolean createUser(String username, String password) {
		Repository repository = getRepository();
		Entity userEntity = getUserEntity(repository);
		Instance newUser = new Instance(userEntity.getTypeRef(), null);
		newUser.setValue("username", username);
		newUser.setValue("password", password);
		Instance createdUser = repository.createInstance(newUser);
		return true;
	}

	@Override
	public boolean resetPassword(String username) {
		return false;
	}

}
