package resource.util;

import userprofile.*;

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
		Profile user = new ProfileService().findByUsername(username);
		if (user == null)
			return false;
		if (!user.getPassword().equals(password))
			return false;
		return true;
	}
}
