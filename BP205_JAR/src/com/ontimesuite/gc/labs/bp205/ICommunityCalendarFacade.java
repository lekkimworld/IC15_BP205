package com.ontimesuite.gc.labs.bp205;

import javax.ejb.Local;

/**
 * Service interface for the community calendar facade.
 * 
 * @author lekkim
 */
@Local
public interface ICommunityCalendarFacade {
	public enum STATUS {
		ACCEPTED("A"), REJECTED("R"), UNHANDLED("U"), DONT_CARE("D");
		private String value = null;
		private STATUS(String v) {
			this.value = v;
		}
		public String getStringValue() {
			return this.value;
		}
		public static STATUS getStatus(String s) {
			for (STATUS status : STATUS.values()) {
				if (status.getStringValue().equals(s)) return status;
			}
			return STATUS.DONT_CARE;
		}
	}
	
	/**
	 * Returns the configuration of the system.
	 * 
	 * @return
	 */
	public Configuration getConfiguration();
	
	/**
	 * Returns the IBM Connections configuration of the system.
	 * 
	 * @return
	 */
	public IBMConnectionsInfo getIBMConnectionsInfo();
	
	/**
	 * Returns the {@link Event}s for the supplied user honoring the 
	 * supplied status. May be empty - never null.
	 * 
	 * @param user
	 * @param status
	 * @return
	 */
	public Event[] getEventsForUser(User user, STATUS status);
	
	/**
	 * Should be called to add, remove, update an event in the system.
	 * 
	 * @param event
	 * @param calevent
	 */
	public void handleCommunityCalendarEvent(EVENT_NAME event, Event calevent);
	
	public void handleRsvpEvent(User user, Event event);
	
	public void handleUnrsvpEvent(User user, Event event);
	
	/**
	 * Call to update a community in the system. If the community 
	 * does not exist it is added.
	 * 
	 * @param comm
	 */
	public void communityUpdated(Community comm);
	
	/**
	 * Called to remove a community from the system.
	 * 
	 * @param comm
	 */
	public void communityRemoved(Community comm);
	
	/**
	 * Retrieves a commuity by id.
	 * 
	 * @param id
	 * @return
	 */
	public Community getCommunity(String id);
	
	/**
	 * Rejects an {@link Event} on behalf of a {@link User}. Rejecting an {@link Event} means
	 * that it is removed to the {@link User}s calendar if the user previously 
	 * accepted the event and an event is posted to the activity stream 
	 * of the community.
	 * 
	 * @param user
	 * @param eventId
	 */
	public void rejectUserEvent(User user, String eventId);
	
	/**
	 * Accepts an {@link Event} on behalf of a {@link User}. Accepting an {@link Event} means
	 * that it is added to the {@link User}s calendar and an event is posted to the activity stream 
	 * of the community.
	 * 
	 * @param user
	 * @param eventId
	 */
	public void acceptUserEvent(User user, String eventId);
	
	/**
	 * Marks an {@link Event} as unhandled on behalf of a {@link User}. Doing this means
	 * that it is removed from the {@link User}s calendar (if previously added). If it was 
	 * added an event is also posted to the activity stream of the community.
	 * 
	 * @param user
	 * @param eventId
	 */
	public void unhandleUserEvent(User user, String eventId);
}
