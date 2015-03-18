package com.ontimesuite.gc.labs.bp205;

import java.net.URL;

import javax.ejb.Local;

/**
 * Service interface for the OAuth 2.0 facade.
 * 
 * @author lekkim
 */
@Local
public interface IOAuthFacade {
	
	/**
	 * Tries to exchange the supplied authorization code for an access_token and 
	 * a refresh_token which are stored in the database. If this method returns without 
	 * error you are good to go.
	 * 
	 * @param authCode
	 */
	public void exchangeOAuthAuthorizationCode(String authCode);
	
	/**
	 * Does a GET request adding OAuth information for the supplied user 
	 * and handling refresh of the access token if required. If no OAuth 
	 * information for the user is available or a refresh fails we throw a 
	 * RuntimeException.
	 * 
	 * @param username
	 * @param url
	 * @return
	 */
	public HTTP.HTTPResult get(String username, URL url);
	
	/**
	 * Does a GET request adding OAuth information for the current user 
	 * and handling refresh of the access token if required. If no OAuth 
	 * information for the user is available or a refresh fails we throw a 
	 * RuntimeException.
	 * 
	 * @param url
	 * @return
	 */
	public HTTP.HTTPResult get(URL url);
	
	/**
	 * Does a POST request adding OAuth information for the supplied user 
	 * and handling refresh of the access token if required. If no OAuth 
	 * information for the user is available or a refresh fails we throw a 
	 * RuntimeException.
	 * 
	 * @param url
	 * @return
	 */
	public HTTP.HTTPResult post(String username, URL url, String postData);
	
	/**
	 * Does a POST request adding OAuth information for the current user 
	 * and handling refresh of the access token if required. If no OAuth 
	 * information for the user is available or a refresh fails we throw a 
	 * RuntimeException.
	 * 
	 * @param url
	 * @return
	 */
	public HTTP.HTTPResult post(URL url, String postData);
	
	/**
	 * Returns the tokens for supplied username.
	 * 
	 * @return
	 */
	public String[] getOAuthTokensForUser(String username);
	
	/**
	 * Same as {@link #getOAuthTokensForUser(String)} but for the active user.
	 * 
	 * @return
	 */
	public String[] getOAuthTokensForUser();
	
	/**
	 * Returns true if the user previous authorized us for OAuth.
	 * 
	 * @return
	 */
	public boolean hasOAuthAccessToken();
	
	/**
	 * Returns the OAuth refresh token for the current user if we have it.
	 * 
	 * @return
	 */
	public String getOAuthRefreshToken();
	
	/**
	 * Returns the URL for clients to use for OAuth authorization.
	 * 
	 * @return
	 */
	public String getOAuthAuthorizeURL();
	
	/**
	 * Returns the URL for clients to use for OAuth access token refresh / exchange.
	 * 
	 * @return
	 */
	public String getOAuthTokenURL();
	
	public String[] deleteOAuthAccessToken(String username);
	public String[] deleteOAuthRefreshToken(String username);
	public void deleteOAuthTokens(String username);
	
	/**
	 * Used to signal issues with the OAuth operation to the caller.
	 * 
	 * @author lekkim
	 */
	public class OAuthRuntimeException extends RuntimeException {
		public static final String NO_TOKENS = "NoTokens";
		public static final String REFRESH_TOKEN_EXPIRED = "RefreshTokenExpired";
		public static final String EXCEPTION_ON_REFRESH = "ExceptionOnRefresh";
		public static final String EXCEPTION_ON_REQUEST = "ExceptionOnRequest";
		public static final String JSON_PARSE_ERROR = "JsonParseError";
		public static final String UNKNOWN_ERROR = "UnknownError";
		public static final String REFRESHED_TOKEN_DOESNT_WORK = "RefreshedTokenDoesntWork";
		
		// declarations
		private String errorCode = null;
		
		public OAuthRuntimeException(String errorCode) {
			super();
			this.errorCode = errorCode;
		}
		public OAuthRuntimeException(String errorCode, String msg, Throwable t) {
			super(msg, t);
			this.errorCode = errorCode;
		}
		public OAuthRuntimeException(String errorCode, String msg) {
			super(msg);
			this.errorCode = errorCode;
		}
		public String getErrorCode() {
			return this.errorCode;
		}
	}
}
