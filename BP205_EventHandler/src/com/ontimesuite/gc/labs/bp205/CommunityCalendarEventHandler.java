package com.ontimesuite.gc.labs.bp205;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import com.ibm.connections.spi.events.EventHandler;
import com.ibm.connections.spi.events.EventHandlerException;
import com.ibm.connections.spi.events.EventHandlerInitException;
import com.ibm.connections.spi.events.object.Event;

import dk.intravision.json.JsonBuilder;

public class CommunityCalendarEventHandler implements EventHandler {
	// logger 
	private static final Logger logger = Logger.getLogger(CommunityCalendarEventHandler.class.getPackage().getName());
	
	// declarations
	protected String username = null;
	protected String password = null;
	protected InitialContext ctx = null;
	protected QueueConnectionFactory fact = null;
	protected QueueConnection connect = null;
	protected QueueSession session = null;
	protected Queue queue = null;
	
	@Override
	public void handleEvent(Event event) throws EventHandlerException {
		logger.fine("Received event name <" + event.getName() +">, id <" + event.getID() + ">");
		final EVENT_NAME eventName = EVENT_NAME.getEventFromName(event.getName());
		if (null == eventName) {
			// unknown event - ignore
			return;
		}
		
		// prepare json builder
		JsonBuilder json = new JsonBuilder();
		json.add("EventName", eventName.getStringName())
			.add("EventID", event.getID());
		
		// add actor info
		json.beginObject("Actor")
			.add("Name", event.getActor().getDisplayName())
			.add("Key", event.getActor().getExtID())
			.add("Email", event.getActor().getEmailAddress())
		.endObject();
		
		// add container info (community)
		json.beginObject("Community")
			.add("ID", event.getContainer().getID())
			.add("Name", event.getContainer().getName())
		.endObject();
		
		// begin event info
		json.beginObject("Event")
			.add("ID", event.getItem().getID());
		
		switch (eventName) {
		case COMMUNITY_EVENT_ADDED:
		case COMMUNITY_EVENT_REMOVED:
		case COMMUNITY_EVENT_UPDATED:
			boolean allday = Boolean.parseBoolean(event.getProperties().get("event.allday"));
			json.add("Subject", event.getItem().getName())
				.add("ID", event.getItem().getID())
				.add("URL", event.getItem().getHTMLPath())
				.add("Location", event.getProperties().get("event.location"))
				.add("Allday", allday)
				.add("StartDT", event.getProperties().get("event.startDate"))
				.add("EndDT", event.getProperties().get("event.endDate"));			
			break;
		default:
			// catch rest
			break;
		}
		
		// end Event object in JSON
		json.endObject();
		
		// get string
		String jsonStr = json.toString();
		logger.fine("Built JSON payload <"+ jsonStr + ">");
		
		try {
			// publish to queue
			MessageProducer producer = this.session.createProducer(this.queue);
			TextMessage msg = this.session.createTextMessage(jsonStr);
			producer.send(msg);
			producer.close();
			logger.fine("Sent message to queue");
			
		} catch (JMSException e) {
			logger.log(Level.SEVERE, "Unable to send message to queue to to JMS exception", e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Unable to send message to unknown exception", t);
		}
	}
	
	@Override
	public void destroy() {
		// release queue
		try {
			this.session.close();
			this.connect.stop();
			this.connect.close();
			this.ctx.close();
			
		} catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to release all queue resources without exception", t);
		}
	}
	
	@Override
	public void init() throws EventHandlerInitException {
		// obtain queue
		try {
			this.ctx = new InitialContext();
			this.fact = (QueueConnectionFactory)this.ctx.lookup("jms/calendarEventsQueueFactory");
			if (StringUtil.isEmpty(this.username) || StringUtil.isEmpty(this.password)) {
				this.connect = this.fact.createQueueConnection();
			} else {
				this.connect = this.fact.createQueueConnection(this.username, this.password);
			}
			this.session = this.connect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.queue = (Queue)this.ctx.lookup("jms/calendarEventsQueue");
			
		} catch (Throwable t) {
			throw new EventHandlerInitException("Unable to initialize event handler due to exception getting queue", t);
		}
	}
	
	public void setQueueUsername(String username) {
		this.username = username;
	}
	
	public String getQueueUsername() {
		return this.username;
	}
	
	public void setQueuePassword(String password) {
		this.password = password;
	}
	
	public String getQueuePassword() {
		return this.password;
	}
}
