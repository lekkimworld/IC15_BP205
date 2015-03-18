package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Callback for OAuth 2.
 */
@WebServlet("/oauth20_cb")
public class OAuth20CallbackServlet extends HttpServlet {
	// logger
	private static final Logger logger = Logger.getLogger(OAuth20CallbackServlet.class.getPackage().getName());
	
	// declarations
	@EJB
	private ICommunityCalendarFacade facade = null;
	@EJB
	private IIBMConnectionsFacade ic = null;
	@EJB
	private IOAuthFacade oauth = null;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String authError = req.getParameter("oauth_error");
		String authCode = req.getParameter("code");
		if (StringUtil.isEmpty(authError) && StringUtil.isEmpty(authCode)) {
			try {
				// get user object based on current user
				User user = this.ic.getUserObject();
				req.setAttribute(EnsureUserObjectFilter.ATTRIBUTE_USER, user);
				
				// just redirect to JSP
				req.getRequestDispatcher("/oauth2success.jsp").include(req, resp);
				
			} catch (Throwable t) {
				this.showErrorPage(req, resp, "Unable to obtain user object based on OAuth info", t);
			}
			return;
		}
		
		// see we received an oauth error
		if (!StringUtil.isEmpty(authError)) {
			this.showErrorPage(req, resp, "Error: " + authError, null);
			return;
		}
		
		// see if we received an oauth code to exchange
		if (!StringUtil.isEmpty(authCode)) {
			try {
				// we did - ask OAuth facade to exchange it for an access_token and refresh_token
				this.oauth.exchangeOAuthAuthorizationCode(authCode);
				
				// coming here means that the exchange worked - redirect back here without 
				// parameters to show result to user
				resp.sendRedirect(req.getContextPath() + "/oauth20_cb");
				
			} catch (Throwable t) {
				this.showErrorPage(req, resp, "Unable to exchange OAuth authorization code for access_token and refresh_token", t);
			}
		}
	}
	
	private void showErrorPage(HttpServletRequest req, HttpServletResponse resp, String error, Throwable t) throws IOException {
		resp.setContentType("text/plain");
		PrintWriter w = new PrintWriter(resp.getWriter());
		if (!StringUtil.isEmpty(error)) {
			w.print(error);
			w.print('\n');
		}
		t.printStackTrace(w);
		w.flush();
		w.close();
	}
	
	
}
