package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet Filter implementation class EnsureAuthenticatedFilter
 */
@WebFilter(filterName="ensureUserObjectFilter", urlPatterns={"/api/*"})
public class EnsureUserObjectFilter extends BaseFilter {
	// logger
	private static final Logger logger = Logger.getLogger(EnsureUserObjectFilter.class.getPackage().getName());
	
	// constants
	private static final String ATTRIBUTE_SESSION_OTGCUSER = "OnTimeGC_CommCal_User";
	public static final String ATTRIBUTE_USER = "User";
	
	// declarations
	@EJB
	private IIBMConnectionsFacade ic = null;
	
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// cast
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		// get username
		String username = this.getUsername(req);
		
		// look in session
		HttpSession session = this.getSession(req, false);
		User user = null;
		if (null != session && !session.isNew()) {
			// we already have a session with the server - look for info here
			user = (User)session.getAttribute(ATTRIBUTE_SESSION_OTGCUSER);
			if (null != user) {
				// we have it - set in request
				req.setAttribute(ATTRIBUTE_USER, user);
				
				// pass the request along the filter chain
				chain.doFilter(request, response);
				return;
			}
		}
		session = this.getSession(req, true);
		
		try {
			// talk to IBM Connections and get user info
			user = this.ic.getUserObject();
			if (logger.isLoggable(Level.FINE)) logger.fine("Constructed user object <" + user + ">");
			
		} catch (Throwable t) {
			// unable to get user
			ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NoUserObject", "Unable to create user object for username <" + username + ">", t);
			logger.log(Level.WARNING, "Unable to create user object for username <" + username + ">", t);
			return;
		}
		
		// store user object in session and request
		req.setAttribute(ATTRIBUTE_USER, user);
		session.setAttribute(ATTRIBUTE_SESSION_OTGCUSER, user);
		
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}
	
	protected HttpSession getSession(HttpServletRequest req, boolean create) {
		return req.getSession(create);
	}
}
