package resource.util;

import kirra_user_profile.*;

public class Authenticator {
	private static Authenticator authenticator = null;

	public static Authenticator getInstance() {
		if (authenticator == null) {
			authenticator = new Authenticator();
		}
		return authenticator;
	}

	public boolean authenticate(String username, String password) {
		if (username == null)
			return false;
		if (password == null)
			return false;
		UserProfile user = new UserProfileService().findByUsername(username);
		if (user == null)
			return false;
		if (!user.getPassword().equals(password))
			return false;
		return true;
	}
}
