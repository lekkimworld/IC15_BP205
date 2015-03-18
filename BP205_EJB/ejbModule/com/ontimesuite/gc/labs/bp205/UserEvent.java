package com.ontimesuite.gc.labs.bp205;


/**
 * Represents the action a {@link User} have done for an {@link Event}.
 *  
 * @author lekkim
 */
public class UserEvent {
	// declarations
	private User user = null;
	private Event event = null;
	private ACTION action = null;
	private String unid = null;
	
	public UserEvent(User user, Event event) {
		this.user = user;
		this.event = event;
	}
	
	public UserEvent(User user, Event event, ACTION action, String unid) {
		this(user, event);
		this.action = action;
		this.unid = unid;
	}

	public User getUser() {
		return user;
	}

	public Event getEvent() {
		return event;
	}

	public ACTION getAction() {
		return action;
	}

	public String getUnID() {
		return unid;
	}
}
