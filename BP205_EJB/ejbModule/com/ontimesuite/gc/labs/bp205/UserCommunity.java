package com.ontimesuite.gc.labs.bp205;

/**
 * Represents the settings a {@link User} have done for a {@link Community}.
 *  
 * @author lekkim
 */
public class UserCommunity {
	// declarations
	private User user = null;
	private Community comm = null;
	private ACTION action = null;
	
	public UserCommunity(User user, Community comm, ACTION action) {
		this.user = user;
		this.comm = comm;
		this.action = action;
	}

	public User getUser() {
		return user;
	}

	public Community getCommunity() {
		return comm;
	}

	public ACTION getAction() {
		return action;
	}

}
