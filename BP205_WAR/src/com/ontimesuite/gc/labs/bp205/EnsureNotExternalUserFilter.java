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
@WebFilter(filterName="ensureNotExternalFilter", urlPatterns={"/api/*"})
public class EnsureNotExternalUserFilter extends BaseFilter {
	
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// cast
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		// get user
		User user = (User)req.getAttribute(EnsureUserObjectFilter.ATTRIBUTE_USER);
		
		// we do not want external users
		if (user.isExternal()) {
			ResponseUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "ExternalUser", "External users not supported");
			return;
		}		
		
		// send along filter chain
		chain.doFilter(request, response);
	}

}
