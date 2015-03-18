package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter to ensure user is authenticated to the container.
 * 
 */
@WebFilter(filterName="ensureAuthFilter", urlPatterns={"/api/*", "/oauth2_cb"})
public class EnsureAuthenticatedFilter extends BaseFilter {
	
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// cast
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		// make sure user is authenticated
		String username = this.getUsername(req);
		if (StringUtil.isEmpty(username)) {
			// user is not authenticated
			ResponseUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "NotLoggedIn", "User must be authenticated", null);
			return;
		}
		
		// send along filter chain
		chain.doFilter(request, response);
	}

}
