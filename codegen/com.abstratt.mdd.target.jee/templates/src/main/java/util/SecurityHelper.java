package util;

import userprofile.Profile;
import userprofile.ProfileService;

public class SecurityHelper {
    public static ThreadLocal<String> currentUser = new ThreadLocal<>();
    
    public static Profile getCurrentUser() {
        return new ProfileService().findByUsername(getCurrentUsername());
    }

    public static String getCurrentUsername() {
        return currentUser.get();
    }

    public static void setCurrentUsername(String username) {
    	currentUser.set(username);
    }
}