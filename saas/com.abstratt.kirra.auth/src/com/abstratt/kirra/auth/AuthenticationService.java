package com.abstratt.kirra.auth;

public interface AuthenticationService {
    /**
     * Validates the given username/password combination.
     * 
     * @param username
     * @param password
     * @return
     */
    public boolean authenticate(String username, String password);

    /**
     * Provisions the given user. Fails if the user already exists.
     * 
     * @param username
     * @param password
     * @return whether a user was created
     */
    public boolean createUser(String username, String password);

    public boolean resetPassword(String username);
}
