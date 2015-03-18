package com.ontimesuite.gc.labs.bp205;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade.STATUS;

import dk.intravision.json.Json;
import dk.intravision.json.JsonBuilder;

/**
 * Servlet implementation class ApiServlet
 */
@WebServlet(urlPatterns={"/api/*"})
@ServletSecurity(@HttpConstraint(rolesAllowed={"Default"}))
@DeclareRoles("Default")
public class ApiServlet extends HttpServlet {
	// logger
	private static final Logger logger = Logger.getLogger(ApiServlet.class.getPackage().getName());
	
	// constants
	private static final long serialVersionUID = 1L;
	
	// declarations
	@EJB
	private ICommunityCalendarFacade facade;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// get user from request - filter should set it
		User user = (User)req.getAttribute(EnsureUserObjectFilter.ATTRIBUTE_USER);
		
		// get request uri
		STATUS status = null;
		if (this.matches(req, "/api/events/unhandled")) {
			status = STATUS.UNHANDLED;
		} else if (this.matches(req, "/api/events/accepted")) {
			status = STATUS.ACCEPTED;
		} else if (this.matches(req, "/api/events/rejected")) {
			status = STATUS.REJECTED;
		} else if (this.matches(req, "/api/events")) {
			status = STATUS.DONT_CARE;
		}
		
		if (null == status) {
			ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "InvalidCommand", "Invalid API command sent");
			return;
		}
		
		try {
			// get events
			Event[] events = this.facade.getEventsForUser(user, status);
			
			// get communities base url
			Configuration config = this.facade.getConfiguration();
			IBMConnectionsInfo ic = this.facade.getIBMConnectionsInfo();
			String baseUrl =  config.getURL(SERVICE.COMMUNITIES, req.isSecure());
			
			// serialize to json
			JsonBuilder json = new JsonBuilder();
			json.beginArray("Events");
			for (Event event : events) {
				json.beginArrayObject()
					.add("ID", event.getID())
					.add("Subject", event.getSubject())
					.add("Location", event.getLocation())
					.add("StartDT", event.getStartDt())
					.add("EndDT", event.getEndDt())
					.add("URL", baseUrl + event.getUrl())
					.add("Status", event.getStatus().getStringValue())
					.beginObject("Community")
						.add("ID", event.getCommunity().getId())
						.add("Name", event.getCommunity().getName())
						.add("URL", ic.getCommunityURL(event.getCommunity().getId()).toString())
					.endObject()
					.endArrayObject();
			}
			json.endArray();
			
			// serialize back to user
			resp.setContentType("application/json");
			PrintWriter pw = resp.getWriter();
			pw.write(json.toString());
			pw.flush();
			pw.close();
			
		} catch (Throwable t) {
			ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "UnableToGetEvents", "Unable to load events for command <" + req.getPathInfo() + ">", t);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// get user from request - filter should set it
		User user = (User)req.getAttribute(EnsureUserObjectFilter.ATTRIBUTE_USER);
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Received POST request from <" + user + "> for <" + req.getPathInfo() + ">");
		}
		
		// read content into json and get event id
		Json json = null;
		String eventId = null;
		String postedData = null;
		try {
			BufferedReader r = new BufferedReader(req.getReader());
			StringBuilder b = new StringBuilder();
			String line = null;
			while (null != (line=r.readLine())) {
				b.append(line).append('\n');
			}
			r.close();
			postedData = b.toString();
			if (StringUtil.isEmpty(postedData)) {
				ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "MissingContent", "Missing content");
				return;
			}
			json = new Json(postedData);
			if (json.get("EventID").isNothing()) {
				ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "MissingEventID",  "Missing EventID in JSON");
				return;
			}
			eventId = json.get("EventID").getStr();
			if (StringUtil.isEmpty(eventId)) {
				ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "EmptyEventID", "Empty EventID in JSON");
				return;
			}
			
		} catch (Throwable t) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request received - ignoring");
			return;
		}
		
		try {
			if (this.matches(req, "/api/event/accept")) {
				// accept an event
				if (logger.isLoggable(Level.FINER)) {
					logger.finer("User <" + user + "> is accepting event <" + eventId + ">");
				}
				this.facade.acceptUserEvent(user, eventId);
				
			} else if (this.matches(req, "/api/event/reject")) {
				// reject an event
				if (logger.isLoggable(Level.FINER)) {
					logger.finer("User <" + user + "> is rejecting event <" + eventId + ">");
				}
				this.facade.rejectUserEvent(user, eventId);
			} else if (this.matches(req, "/api/event/unhandle")) {
				// reject an event
				if (logger.isLoggable(Level.FINER)) {
					logger.finer("User <" + user + "> is unhandling event <" + eventId + ">");
				}
				this.facade.unhandleUserEvent(user, eventId);
			} else {
				ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "InvalidCommand", "Invalid API command sent");
				return;
			}
			
		} catch (Throwable t) {
			ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "UnableToGetEvents", "Unable to perform command <" + req.getPathInfo() + "> payload <" + postedData + ">", t);
		}
	}
	
	private boolean matches(HttpServletRequest req, String cmd) {
		final String reqURI = req.getRequestURI().toLowerCase();
		final String ctx = req.getContextPath();
		return reqURI.startsWith(cmd, ctx.length());
	}

}
