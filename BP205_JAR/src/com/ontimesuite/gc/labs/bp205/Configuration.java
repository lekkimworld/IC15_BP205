package com.ontimesuite.gc.labs.bp205;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
	// declarations
	private String oauthClientSecret = null;
	private String oauthClientId = null;
	private String oauthCallback = null;
	private String baseURL = null;
	private Map<SERVICE, String> services = new HashMap<SERVICE, String>(4);

	public Configuration(ResultSet rs) {
		try {
			while (rs.next()) {
				String key = rs.getString("key");
				String value = rs.getString("value");
				if (key.equals("BASE_URL")) {
					this.baseURL = value;
				} else if (key.startsWith("SERVICE_")) {
					String strService = key.substring(8);
					SERVICE service = SERVICE.getService(strService);
					if (null != service) {
						this.services.put(service, value);
					}
				} else if (key.equals("OAUTH_CLIENT_SECRET")) {
					this.oauthClientSecret = value;
				} else if (key.equals("OAUTH_CLIENT_ID")) {
					this.oauthClientId = value;
				} else if  (key.equals("OAUTH_CALLBACK")) {
					this.oauthCallback = value;
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Unable to build Configuration object", e);
		}
	}
	
	public String getOAuthClientId() {
		return this.oauthClientId;
	}
	
	public String getOAuthClientSecret() {
		return this.oauthClientSecret;
	}
	
	public String getOAuthCallback() {
		return this.oauthCallback;
	}
	
	public String getBaseURL(boolean secure) {
		return this.getURL(null, secure);
	}
	
	public String getURL(SERVICE service, boolean secure) {
		StringBuilder b = new StringBuilder();
		b.append("http");
		if (secure) b.append('s');
		b.append("://").append(this.baseURL);
		if (null != service) {
			String servicePart = this.services.get(service);
			b.append(servicePart);
		}
		return b.toString();
	}

}
