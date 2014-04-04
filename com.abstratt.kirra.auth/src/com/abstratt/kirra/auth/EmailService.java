package com.abstratt.kirra.auth;

public interface EmailService {
	/**
	 * Sends an email to the user.
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	boolean send(String addresseeEmail, String addresseeName, String sender,
			String subject, String content);
}
