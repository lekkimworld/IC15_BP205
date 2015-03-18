package com.ontimesuite.gc.labs.bp205;

public enum EVENT_NAME {
	COMMUNITY_REMOVED("community.removed"), COMMUNITY_UPDATED("community.updated"), 
	COMMUNITY_CAL_REMOVED("community.calendar.deleted"), 
	COMMUNITY_EVENT_ADDED("community.calendar.event.entry.created"), 
	COMMUNITY_EVENT_UPDATED("community.calendar.event.entry.updated"), 
	COMMUNITY_EVENT_REMOVED("community.calendar.event.entry.deleted"),
	COMMUNITY_EVENT_RSVPED("community.calendar.event.entry.rsvped"),
	COMMUNITY_EVENT_UNRSVPED("community.calendar.event.entry.unrsvped");
	
	private String eventName = null;
	private EVENT_NAME(String name) {
		this.eventName = name;
	}
	
	public String getStringName() {
		return this.eventName;
	}
	
	public static EVENT_NAME getEventFromName(String name) {
		for (EVENT_NAME e : EVENT_NAME.values()) {
			if (e.getStringName().equals(name)) return e;
		}
		return null;
	}
}
