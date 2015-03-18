package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;
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

/**
 * Servlet Filter to ensure user has authorized us for OAuth.
 */
@WebFilter(filterName="ensureOAuthFilter", urlPatterns={"/api/*"})
public class EnsureOAuthFilter extends BaseFilter {
	// logger
	private static final Logger logger = Logger.getLogger(EnsureOAuthFilter.class.getPackage().getName());
	
	// declarations
	@EJB
	private IOAuthFacade oauth = null;
	
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// cast
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		// get username of authenticated user
		String username = this.getUsername(req);
		
		// make sure user has oauth enabled us 
		boolean rc = this.oauth.hasOAuthAccessToken();
		logger.finer("OAuth check for username <" + username + "> was <" + rc + ">");
		if (rc) {
			// user has OAuth enabled us - send along filter chain
			chain.doFilter(request, response);
		} else {
			// return error to caller with information about how authorize us for OAuth
			ResponseUtil.sendError(resp, HttpServletResponse.SC_OK, "OAuthMissing", "User has not authorized the application for OAuth", null, 
					"OAuth", 
					"URL", this.oauth.getOAuthAuthorizeURL());
		}
	}
}
