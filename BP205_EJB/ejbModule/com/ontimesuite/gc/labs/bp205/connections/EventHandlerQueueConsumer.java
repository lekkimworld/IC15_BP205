package com.ontimesuite.gc.labs.bp205.connections;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ontimesuite.gc.labs.bp205.Community;
import com.ontimesuite.gc.labs.bp205.EVENT_NAME;
import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade;
import com.ontimesuite.gc.labs.bp205.User;

import dk.intravision.json.Json;

/**
 * Message-Driven Bean implementation class for: EventHandlerQueueConsumer
 */
@MessageDriven(
		activationConfig = {
				@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"), 
				@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
		}, 
		mappedName = "jms/calendarEventsQueue")
public class EventHandlerQueueConsumer implements MessageListener {
	// logger
	private static final Logger logger = Logger.getLogger(EventHandlerQueueConsumer.class.getPackage().getName());
	
	// declarations
	@EJB
	private ICommunityCalendarFacade facade;
	
	/**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message msg) {
    	try {
	    	if (null == msg || !(msg instanceof TextMessage)) {
	    		logger.severe("Unable to process message as it is not a TextMessage - aborting");
	    	}
	    	
	    	// cast and extract data
	    	TextMessage txtMsg = (TextMessage)msg;
	    	String txt = txtMsg.getText();
	    	if (logger.isLoggable(Level.FINEST)) logger.finest("Received message with text <" + txt + ">");
	    	
	    	// parse JSON and inspect
	    	Json json = new Json(txt);
	    	final String icEventId = json.get("EventID").getStr();
	    	final String icEventName = json.get("EventName").getStr();
	    	if (logger.isLoggable(Level.FINEST)) logger.finest("Message is with event name <" + icEventName + "> id <" + icEventId +">");
	    	
	    	// decide what to do
	    	EVENT_NAME event = EVENT_NAME.getEventFromName(icEventName);
	    	Community comm = null;
	    	switch (event) {
			case COMMUNITY_EVENT_ADDED:
			case COMMUNITY_EVENT_REMOVED:
			case COMMUNITY_EVENT_UPDATED:
				comm = new Community(json.get("Community"));
				Event calevent = new Event(comm, json.get("Event"));
				if (logger.isLoggable(Level.FINEST)) logger.finest("Built event <" + calevent + ">");
				this.facade.handleCommunityCalendarEvent(event, calevent);
				break;
			case COMMUNITY_CAL_REMOVED:
				// same as removing a community so let fall through
			case COMMUNITY_REMOVED:
				comm = new Community(json.get("Community"));
				if (logger.isLoggable(Level.FINEST)) logger.finest("Built community <" + comm + ">");
				this.facade.communityRemoved(comm);
				break;
			case COMMUNITY_UPDATED:
				comm = new Community(json.get("Community"));
				if (logger.isLoggable(Level.FINEST)) logger.finest("Built community <" + comm + ">");
				this.facade.communityUpdated(comm);
				break;
			case COMMUNITY_EVENT_RSVPED:
			case COMMUNITY_EVENT_UNRSVPED:
				// build objects from info
				User user = new User(json.get("Actor"));
				comm = new Community(json.get("Community"));
				calevent = new Event(comm, json.get("Event"));
				
				// ask facade to help us
				if (EVENT_NAME.COMMUNITY_EVENT_RSVPED == event) {
					this.facade.handleRsvpEvent(user, calevent);
				} else {
					this.facade.handleUnrsvpEvent(user, calevent);
				}
				
				break;
	    	}
	    	
    	} catch (Throwable t) {
    		logger.log(Level.WARNING, "Unable to process message", t);
    	}
    }

}
