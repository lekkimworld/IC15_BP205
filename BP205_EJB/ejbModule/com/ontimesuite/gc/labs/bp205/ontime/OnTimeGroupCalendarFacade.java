package com.ontimesuite.gc.labs.bp205.ontime;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.User;
import com.ontimesuite.gc.labs.bp205.UserEvent;

/**
 * Session Bean implementation class OnTimeGroupCalendarFacade
 */
@Stateless(mappedName = "otgc")
@LocalBean
public class OnTimeGroupCalendarFacade {
	// logger
	private static final Logger logger = Logger.getLogger(OnTimeGroupCalendarFacade.class.getPackage().getName());

	// declarations
	
	
	public String createAppointment(User user, Event event) {
		String uuid = UUID.randomUUID().toString().substring(0, 32);
		logger.info("Create appointment - user<" + user + "> unid <" + event + "> uuid<" + uuid + ">");
		return uuid;
	}
	
	public void updateAppointment(UserEvent ue, Event event) {
		logger.info("Update appointment - user<" + ue.getUser() + "> old event<" + ue.getEvent() + "> unid<" + ue.getUnID() + "> new event<" + event + ">");
	}
	
	public void removeAppointment(UserEvent ue) {
		logger.info("Remove appointment - user<" + ue.getUser() + "> unid <" + ue.getUnID() + ">");
	}
}
