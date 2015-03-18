package com.ontimesuite.gc.labs.bp205;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.intravision.json.JsonBuilder;

/**
 * Servlet implementation class OAuth20DebugServlet
 */
@WebServlet("/oauthdebug/*")
public class OAuth20DebugServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	@EJB
	private IOAuthFacade oauth = null;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String result = "{\"Status\": \"ERROR\", \"ErrorCode\": \"UnknownCommand\"}";
		if (this.matches(req, "/oauthdebug/view/user/")) {
			// utility to show codes for a specific user - not for prod :)
			final String reqURI = req.getRequestURI().toLowerCase();
			String username = reqURI.substring(req.getContextPath().length() + 22);
			String tokens[] = this.oauth.getOAuthTokensForUser(username);
			
			// rewrite response
			JsonBuilder b = new JsonBuilder()
				.add("access_token", tokens[0])
				.add("refresh_token", tokens[1])
				.add("username", username);
			result = b.toString();
			
		} else if (this.matches(req, "/oauthdebug/resetaccess/user/")) {
			// utility to delete codes for a specific user - not for prod :)
			final String reqURI = req.getRequestURI().toLowerCase();
			String username = reqURI.substring(req.getContextPath().length() + 29);
			String tokens[] = this.oauth.deleteOAuthAccessToken(username);
			
			// rewrite response
			JsonBuilder b = new JsonBuilder()
				.add("access_token", tokens[0])
				.add("refresh_token", tokens[1])
				.add("username", username);
			result = b.toString();
			
		} else if (this.matches(req, "/oauthdebug/resetrefresh/user/")) {
			// utility to delete codes for a specific user - not for prod :)
			final String reqURI = req.getRequestURI().toLowerCase();
			String username = reqURI.substring(req.getContextPath().length() + 30);
			String tokens[] = this.oauth.deleteOAuthRefreshToken(username);
			
			// rewrite response
			JsonBuilder b = new JsonBuilder()
				.add("access_token", tokens[0])
				.add("refresh_token", tokens[1])
				.add("username", username);
			result = b.toString();
			
		} else if (this.matches(req, "/oauthdebug/delete/user/")) {
			// utility to delete codes for a specific user - not for prod :)
			final String reqURI = req.getRequestURI().toLowerCase();
			String username = reqURI.substring(req.getContextPath().length() + 24);
			this.oauth.deleteOAuthTokens(username);
			
			// rewrite response
			JsonBuilder b = new JsonBuilder()
				.add("username", username);
			result = b.toString();
		}
		
		// response
		resp.setContentType("application/json");
		PrintWriter pw = resp.getWriter();
		pw.write(result);
		pw.flush();
		pw.close();
	}
	
	private boolean matches(HttpServletRequest req, String cmd) {
		final String reqURI = req.getRequestURI().toLowerCase();
		final String ctx = req.getContextPath();
		return reqURI.startsWith(cmd, ctx.length());
	}
}
