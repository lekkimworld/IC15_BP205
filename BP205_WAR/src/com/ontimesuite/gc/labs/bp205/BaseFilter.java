package com.ontimesuite.gc.labs.bp205;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public abstract class BaseFilter implements Filter {

	@Override
	public void destroy() {
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}
	
	protected String getUsername(HttpServletRequest req) {
		return req.getRemoteUser();
	}

}
