package com.ontimesuite.gc.labs.bp205.connections;

import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.ontimesuite.gc.labs.bp205.ACTION;
import com.ontimesuite.gc.labs.bp205.ArrayUtil;
import com.ontimesuite.gc.labs.bp205.Community;
import com.ontimesuite.gc.labs.bp205.Configuration;
import com.ontimesuite.gc.labs.bp205.EVENT_NAME;
import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.IBMConnectionsInfo;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade;
import com.ontimesuite.gc.labs.bp205.IIBMConnectionsFacade;
import com.ontimesuite.gc.labs.bp205.StringUtil;
import com.ontimesuite.gc.labs.bp205.User;
import com.ontimesuite.gc.labs.bp205.UserEvent;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade.STATUS;
import com.ontimesuite.gc.labs.bp205.dao.DAO;
import com.ontimesuite.gc.labs.bp205.ontime.OnTimeGroupCalendarFacade;

/**
 * Session Bean implementation class CommunityCalendarFacade
 */
@Stateless(mappedName = "facade")
@TransactionManagement(TransactionManagementType.CONTAINER)
@LocalBean
@Local(ICommunityCalendarFacade.class)
public class CommunityCalendarFacade implements ICommunityCalendarFacade {
	// declarations
	@EJB
	private DAO dao = null;
	@EJB
	private OnTimeGroupCalendarFacade otgc = null;
	@EJB
	private IIBMConnectionsFacade ic = null;
	@EJB
	private ActivityStreamFacade as = null;
	private Configuration config = null;
	private IBMConnectionsInfo icInfo = null;
	
	public CommunityCalendarFacade() {
		
	}
	
	@Override
	public Configuration getConfiguration() {
		if (null == this.config) {
			this.config = this.dao.getConfiguration();
		}
		return this.config;
	}

	@Override
	public IBMConnectionsInfo getIBMConnectionsInfo() {
		if (null == this.icInfo) {
			Configuration config = this.getConfiguration();
			this.icInfo = new IBMConnectionsInfo(config);
		}
		return this.icInfo;
	}

	@Override
    public Event[] getEventsForUser(User user, STATUS status) {
    	Event[] events = this.dao.getEvents(user, status);
    	if (ArrayUtil.isEmpty(events)) {
    		return new Event[]{};
    	}
    	
    	// get communities the user has access to
    	Community[] comms = this.ic.getCommunitiesForUser(user);
    	
    	// filter
    	List<Event> result = new LinkedList<Event>();
    	for (Event event : events) {
    		if (ArrayUtil.containsEquals(comms, event.getCommunity())) {
    			// user has access
    			result.add(event);
    		}
    	}
    	
    	// return
    	return result.toArray(new Event[result.size()]);
    }

	@Override
	public void handleCommunityCalendarEvent(EVENT_NAME event, Event calevent) {
		// get community id
		String comm_id = calevent.getCommunity().getId();
		
		// make sure we have the community registered
		Community comm = dao.getCommunity(comm_id);
		if (null == comm) {
			// we do not have the community - add it
			comm = new Community(comm_id, calevent.getCommunity().getName());
			dao.addCommunity(comm);
		}
		
		// update database event wise
		if (EVENT_NAME.COMMUNITY_EVENT_ADDED == event) {
			// save the event
			dao.addEvent(calevent);
			
			// get any users configured for auto action
			//TODO implement auto-action handling
			
		} else if (EVENT_NAME.COMMUNITY_EVENT_UPDATED == event) {
			// update the event
			dao.updateEvent(calevent);
			
			// update calendars for users who accepted the event
			UserEvent[] ues = dao.getUserEventMappings(calevent);
			for (UserEvent ue : ues) {
				// update calendar if there is a unid
				if (!StringUtil.isEmpty(ue.getUnID())) {
					this.otgc.updateAppointment(ue, calevent);
				}
			}
				
		} else if (EVENT_NAME.COMMUNITY_EVENT_REMOVED == event) {
			// get the calendar mapping data for users who added the event before
			// removing
			UserEvent[] ues = dao.getUserEventMappings(calevent);
			for (UserEvent ue : ues) {
				// remove mapping from database
				dao.removeUserEventMapping(ue);
				
				// delete from calendar if there is a unid
				if (!StringUtil.isEmpty(ue.getUnID())) {
					this.otgc.removeAppointment(ue);
				}
			}
			
			// remove the event
			dao.removeEvent(calevent);
		}
		 
	}

	@Override
	public void handleRsvpEvent(User user, Event event) {
		
	}

	@Override
	public void handleUnrsvpEvent(User user, Event event) {
		
	}

	@Override
	public void communityUpdated(Community comm) {
		if (null == comm) throw new IllegalArgumentException("Must supply non-null community");
		this.dao.updateCommunity(comm);
	}

	@Override
	public void communityRemoved(Community comm) {
		if (null == comm || StringUtil.isEmpty(comm.getId())) throw new IllegalArgumentException("Must supply non-null community with non-empty id");
		this.dao.removeCommunity(comm);
	}

	@Override
	public Community getCommunity(String id) {
		return this.dao.getCommunity(id);
	}
	
	@Override
	public void rejectUserEvent(User user, String eventId) {
		// get user/event mapping
		UserEvent ue = this.dao.getUserEventMapping(user, eventId);
		Event event = null;
		if (null != ue) {
			// we already have it - see if status needs change
			event = ue.getEvent();
			if (ue.getAction() == ACTION.REJECTED) {
				// already rejected - return
				return;
			} else {
				// remove the mapping
				this.dao.removeUserEventMapping(ue);
			}
		} else {
			event = this.dao.getEvent(eventId);
			if (null == event) {
				throw new RuntimeException("Non-existing event id <" + eventId + "> supplied");
			}
		}
		
		// add (new) mapping
		this.dao.addUserEventMapping(user, event, ACTION.REJECTED, null);
		
		// remove appointment
		this.otgc.removeAppointment(ue);
		
		// send activity stream event
		String summary = event.getSubject();
		if (!StringUtil.isEmpty(event.getLocation())) summary += " / " + event.getLocation();
		this.as.post(user, 
				event, 
				ACTION.REJECTED, 
				"${Actor} rejected a community calendar event (subject: " + event.getSubject() + ")",
				summary, 
				event.getUrl(),
				null	// gadget url
		);
	}

	@Override
	public void acceptUserEvent(User user, String eventId) {
		// get user/event mapping
		UserEvent ue = this.dao.getUserEventMapping(user, eventId);
		Event event = null;
		if (null != ue) {
			// we already have it - see if status needs change
			event = ue.getEvent();
			if (ue.getAction() == ACTION.ACCEPTED) {
				// already accepted - return
				return;
			} else {
				// remove the mapping
				this.dao.removeUserEventMapping(ue);
			}
		} else {
			event = this.dao.getEvent(eventId);
			if (null == event) {
				throw new RuntimeException("Non-existing event id <" + eventId + "> supplied");
			}
		}
		
		// add appointment
		String unid = this.otgc.createAppointment(user, event);
		
		// add user/event mapping
		this.dao.addUserEventMapping(user, event, ACTION.ACCEPTED, unid);
		
		// send activity stream event
		String summary = event.getSubject();
		if (!StringUtil.isEmpty(event.getLocation())) summary += " / " +event.getLocation();
		this.as.post(user, 
				event, 
				ACTION.ACCEPTED, 
				"${Actor} accepted a community calendar event (subject: " + event.getSubject() + ")",
				summary, 
				event.getUrl(),
				null	// gadget url
		);
	}

	@Override
	public void unhandleUserEvent(User user, String eventId) {
		// get user/event mapping
		UserEvent ue = this.dao.getUserEventMapping(user, eventId);
		if (null == ue) {
			// no mapping for this event - return
			return;
		}
		
		// remove the mapping
		this.dao.removeUserEventMapping(ue);
		
		// remove appointment (if there is a unid)
		this.otgc.removeAppointment(ue);
		
		// send activity stream event
		String summary = ue.getEvent().getSubject();
		if (!StringUtil.isEmpty(ue.getEvent().getLocation())) summary += " / " + ue.getEvent().getLocation();
		this.as.post(user, 
				ue.getEvent(), 
				null, 	// action
				"${Actor} unhandled a community calendar event (subject: " + ue.getEvent().getSubject() + ")",
				summary, 
				ue.getEvent().getUrl(),
				null	// gadget url
		);
	}

}
