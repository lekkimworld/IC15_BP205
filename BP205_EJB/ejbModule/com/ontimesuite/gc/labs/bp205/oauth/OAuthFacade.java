package com.ontimesuite.gc.labs.bp205.oauth;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import com.ibm.websphere.security.auth.WSSubject;
import com.ontimesuite.gc.labs.bp205.ArrayUtil;
import com.ontimesuite.gc.labs.bp205.Configuration;
import com.ontimesuite.gc.labs.bp205.HTTP;
import com.ontimesuite.gc.labs.bp205.HTTP.HTTPResult;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade;
import com.ontimesuite.gc.labs.bp205.IOAuthFacade;
import com.ontimesuite.gc.labs.bp205.SERVICE;
import com.ontimesuite.gc.labs.bp205.StringUtil;
import com.ontimesuite.gc.labs.bp205.dao.DAO;

import dk.intravision.json.Json;

/**
 * Session Bean implementation class IBMConnectionsAPIFacade
 */
@Stateless(mappedName = "oauth")
@LocalBean
@Local(IOAuthFacade.class)
public class OAuthFacade implements IOAuthFacade {
	// logger
	private static final Logger logger = Logger.getLogger(OAuthFacade.class.getPackage().getName());
	
	// declarations
	@Resource
	private SessionContext sessionContext;
	@EJB
	private DAO dao = null;
	@EJB
	private ICommunityCalendarFacade facade = null;

	@Override
	public void exchangeOAuthAuthorizationCode(String authCode) {
		// validate
		if (StringUtil.isEmpty(authCode)) {
			throw new RuntimeException("No authorization code supplied");
		}
		
		// get url and body
		String authUrl = this.getOAuthTokenURL();
		String oauthBodyValue = this.getOAuthAccessTokenBody(authCode);
		if (logger.isLoggable(Level.FINER)) logger.finer("OAuth token body < " + oauthBodyValue + ">");
		
		HTTPResult result = null;
		try {
			HTTP http = new HTTP();
			http.setContentType(HTTP.CONTENTTYPE_FORM);
			result = http.post(authUrl, oauthBodyValue);
			if (200 == result.rc) {
				String content = result.contents;
				if (logger.isLoggable(Level.FINER)) logger.finer("Result body <" + result.contents + ">");
				
				// get token
				Json json = new Json(content);
				String access_token = json.get("access_token").getStr();
				String refresh_token = json.get("refresh_token").getStr();
				if (logger.isLoggable(Level.FINE)) logger.finer("Exchanged authcode to access token <" + access_token + "> and refresh token <" + refresh_token + ">");
				
				// add to database
				boolean insertResult = this.dao.addOAuthTokensForUser(this.getUsername(), access_token, refresh_token);
				if (logger.isLoggable(Level.FINER)) logger.finer("Did insert of tokens <" + insertResult + ">");
				
			} else {
				// throw exception to go to error page
				throw new RuntimeException("Non code 200 returned <" + result.rc + ">");
			}
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to exchange authorization code for access/refresh token", t);
		}
	}

	
	
	@Override
	public HTTPResult get(String username, URL url) {
		return this.url(username, "GET", url, null, 0, null);
	}

	@Override
	public HTTPResult get(URL url) {
		return this.get(WSSubject.getCallerPrincipal(), url);
	}
	
	@Override
	public HTTPResult post(String username, URL url, String postData) {
		return this.url(username, "POST", url, postData, 0, null);
	}
	
	@Override
	public HTTPResult post(URL url, String postData) {
		return this.post(WSSubject.getCallerPrincipal(),  url, postData);
	}

	@Override
	public String[] getOAuthTokensForUser() {
		String username = this.getUsername();
		logger.finest("Getting OAuth tokens for user <" + username + ">");
		return this.getOAuthTokensForUser(username);
	}

	@Override
	public String[] getOAuthTokensForUser(String username) {
		if (StringUtil.isEmpty(username)) return this.getOAuthTokensForUser();
		String[] tokens = this.dao.getOAuthTokensForUser(username);
		return tokens;
	}

	@Override
	public boolean hasOAuthAccessToken() {
		String[] tokens = this.getOAuthTokensForUser();
		return !ArrayUtil.isEmpty(tokens);
	}

	@Override
	public String getOAuthRefreshToken() {
		String username = this.getUsername();
		String[] tokens = this.dao.getOAuthTokensForUser(username);
		if (ArrayUtil.isEmpty(tokens) || StringUtil.isEmpty(tokens[1])) {
			throw new RuntimeException("No OAuth refresh token found for user");
		}
		return tokens[1];
	}

	@Override
	public String getOAuthAuthorizeURL() {
		Configuration config = this.dao.getConfiguration();
		return config.getURL(SERVICE.OAUTH2, true) 
				+ "/endpoint/connectionsProvider/authorize?response_type=code&client_id=" 
				+ config.getOAuthClientId() + "&callback_uri=" + config.getOAuthCallback();
	}
	
	@Override
	public String getOAuthTokenURL() {
		Configuration config = this.dao.getConfiguration();
		return config.getURL(SERVICE.OAUTH2, true) 
				+ "/endpoint/connectionsProvider/token";
	}

	@Override
	public String[] deleteOAuthAccessToken(String username) {
		String[] tokens = this.getOAuthTokensForUser(username);
		tokens[0] = "dummy_access_token";
		this.dao.updateOAuthTokensForUser(username, tokens[0], tokens[1]);
		return tokens;
	}

	@Override
	public String[] deleteOAuthRefreshToken(String username) {
		String[] tokens = this.getOAuthTokensForUser(username);
		tokens[1] = "";
		this.dao.updateOAuthTokensForUser(username, tokens[0], tokens[1]);
		return tokens;
	}

	@Override
	public void deleteOAuthTokens(String username) {
		this.dao.deleteOAuthTokensForUser(username);
	}

	/**
	 * Get the username of the currently authenticated user.
	 * 
	 * @return
	 */
	protected String getUsername() {
		return this.sessionContext.getCallerPrincipal().getName();
	}
	
	/**
	 * Builds the OAuth token body.
	 * 
	 * @param authCode authorization code to use
	 * @return
	 */
	protected String getOAuthAccessTokenBody(String authCode) {
		Configuration config = this.facade.getConfiguration();
		String callback = config.getOAuthCallback();
		
		// build url
		StringBuilder b = new StringBuilder();
		b.append("client_secret=").append(URLEncoder.encode(config.getOAuthClientSecret())) 
			.append("&client_id=").append(URLEncoder.encode(config.getOAuthClientId())) 
			.append("&grant_type=authorization_code")
			.append("&code=").append(URLEncoder.encode(authCode))
			.append("&callback_uri=").append(URLEncoder.encode(callback));
		return b.toString();
	}
	
	/**
	 * Builds the OAuth token body for refresh.
	 * 
	 * @return
	 */
	protected String getOAuthRefreshTokenBody() {
		Configuration config = this.facade.getConfiguration();
		
		// build url
		StringBuilder b = new StringBuilder();
		b.append("client_secret=").append(config.getOAuthClientSecret()) 
			.append("&client_id=").append(config.getOAuthClientId()) 
			.append("&grant_type=refresh_token")
			.append("&refresh_token=").append(this.getOAuthRefreshToken());
		return b.toString();
	}
	
	private HTTP.HTTPResult url(final String username, final String method, final URL url, final String postData, final int callCount, final String[] useTokens) {
		// get tokens for user
		if (logger.isLoggable(Level.FINEST)) logger.finest("OAuth URL request - method <" + method + "> url <" + url + "> postData <" + postData + "> callCount <" + callCount + "> useTokens <" + Arrays.toString(useTokens) + ">");
		String[] tokens = (ArrayUtil.isEmpty(useTokens) ? this.getOAuthTokensForUser(username) : useTokens);
		if (logger.isLoggable(Level.FINEST)) logger.finest("Did lookup of tokens / use from request - tokens now <" + Arrays.toString(tokens) + ">");
		if (ArrayUtil.isEmpty(tokens)) {
			throw new OAuthRuntimeException(OAuthRuntimeException.NO_TOKENS);
		}
		
		// perform operation using access_token
		HTTP http = new HTTP();
		http.addOAuthAccessTokenHeader(tokens[0]);
		if (logger.isLoggable(Level.FINEST)) logger.finest("Added access token to header <" + tokens[0] + ">");
				
		// do operation
		HTTP.HTTPResult result = http.url(method.toUpperCase(), url.toString(), postData);
		if (logger.isLoggable(Level.FINEST)) logger.finest("HTTP result - <" + result + ">");
		if (null != result.exception && result.isCode(401)) {
			// access token has expired - check call count
			if (logger.isLoggable(Level.FINEST)) logger.finest("Received 401 or exception back - check call count");
			if (callCount > 0) {
				// we already tried this - throw exception
				if (logger.isLoggable(Level.FINEST)) logger.finest("Too many calls - throw <" + OAuthRuntimeException.REFRESHED_TOKEN_DOESNT_WORK  + ">");
				throw new OAuthRuntimeException(OAuthRuntimeException.REFRESHED_TOKEN_DOESNT_WORK);
			}
			
			// try and refresh
			String refreshBody = this.getOAuthRefreshTokenBody();
			String refreshUrl = this.getOAuthTokenURL();
			if (logger.isLoggable(Level.FINEST)) logger.finest("Composed refresh body <" + refreshBody + ">, refresh url <" + refreshUrl + ">");
			
			// do request
			http = new HTTP();
			http.setContentType(HTTP.CONTENTTYPE_FORM);
			http.addHeader("Origin", this.facade.getConfiguration().getBaseURL(true));
			result = http.post(refreshUrl, refreshBody);
			if (null == result.exception && result.isCode(200)) {
				// success - we received new access token back
				try {
					if (logger.isLoggable(Level.FINEST)) logger.finest("Received code 200 back - contents <" + result.contents + "> - parse to JSON");
					Json json = new Json(result.contents);
					String access_token = json.get("access_token").getStr();
					String refresh_token = json.get("refresh_token").getStr();
					if (logger.isLoggable(Level.FINEST)) logger.finest("Parsed new OAuth tokens - access_token <" + access_token + "> refresh_token <" + refresh_token + ">");
					
					// update database
					this.dao.updateOAuthTokensForUser(username, access_token, refresh_token);
					if (logger.isLoggable(Level.FINEST)) logger.finest("Updated database - recalling url...");
					
					// reissue call
					return this.url(username, method, url, postData, callCount+1, new String[]{access_token, refresh_token});
					
				} catch (Throwable t) {
					throw new OAuthRuntimeException(OAuthRuntimeException.JSON_PARSE_ERROR, result.contents, t);
				}
			} else if (null != result.exception && (result.rc - (result.rc % 100)) == 400) {
				// refresh token has expired - clear tokens from database
				if (logger.isLoggable(Level.FINEST)) logger.finest("Caught exception from call and code is 4xx - refresh token expired - throw <" + OAuthRuntimeException.REFRESH_TOKEN_EXPIRED + ">");
				this.dao.deleteOAuthTokensForUser(username);
				
				// throw exception
				throw new OAuthRuntimeException(OAuthRuntimeException.REFRESH_TOKEN_EXPIRED);
			} else {
				// unknown error
				if (logger.isLoggable(Level.FINEST)) logger.finest("Unknown error from call - throw <" + OAuthRuntimeException.UNKNOWN_ERROR + ">");
				throw new OAuthRuntimeException(OAuthRuntimeException.UNKNOWN_ERROR, result.contents, result.exception);
			}
		} else if (null == result.exception && result.isCode(200)) {
			// call went fine - return result
			if (logger.isLoggable(Level.FINEST)) logger.finest("Received code 200 back - all is fine - return result");
			return result;
		} else {
			// unknown return code
			throw new OAuthRuntimeException(OAuthRuntimeException.UNKNOWN_ERROR, "Unhandled return code <" + result.rc + "> returned", result.exception);
		}
		
	}
}
