package com.abstratt.kirra.auth;

import java.util.HashMap;
import java.util.Map;

public class TransientAuthenticationService implements AuthenticationService {
	
	private Map<String, String> users = new HashMap<String, String>();

	@Override
	public boolean authenticate(String username, String password) {
		return password != null && password.equals(users.get(username));
	}

	@Override
	public boolean createUser(String username, String password) {
		if (users.containsKey(username))
			return false;
		users.put(username, password);
		return true;
	}

	@Override
	public boolean resetPassword(String username) {
		return false;
	}

}
