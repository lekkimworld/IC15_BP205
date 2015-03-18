package com.ontimesuite.gc.labs.bp205;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTP utility class instead of having dependency on a HTTP library.
 *  
 */
public class HTTP {
	// logger
	private static final Logger logger = Logger.getLogger(HTTP.class.getPackage().getName());
	
	// constants
	public static final String CONTENTTYPE_ATOMXML = "application/atom+xml";
	public static final String CONTENTTYPE_JSON = "application/json";
	public static final String CONTENTTYPE_XML = "text/xml";
	public static final String CONTENTTYPE_TXT = "text/plain";
	public static final String CONTENTTYPE_FORM = "application/x-www-form-urlencoded";
	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String OAUTH_BEARER_TOKEN = "Bearer ";
	
	// declarations
	private final String charset = "UTF-8";
	private String contentType = "application/json";
	private int okCode = 200;
	protected boolean followRedirects = false;
	protected String baseUrl = null;
	protected Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> cookies = new HashMap<String, String>(4);
	
	public HTTP() {
		super();
	}
	
	public HTTP addCookie(String name, String value) {
		this.cookies.put(name, value);
		return this;
	}
	
	public HTTP addHeader(String h, String v) {
		this.headers.put(h,  v);
		return this;
	}
	
	public HTTP addOAuthAccessTokenHeader(String token) {
		return this.addHeader(HEADER_AUTHORIZATION, OAUTH_BEARER_TOKEN + token);
	}
	
	public HTTP setContentType(String type) {
		if (null != type) {
			this.contentType = type;
		}
		return this;
	}
	
	public HTTP setOKCode(int code) {
		if (code <= 0) this.okCode = 200;
		this.okCode = code;
		return this;
	}
	
	public HTTP setFollowRedirects(boolean flag) {
		this.followRedirects = flag;
		return this;
	}
	
	public HTTPResult get(String requestUrl, Map<String, String> params) {
		// abort early
		if (null == requestUrl) {
			return null;
		}
		
		// implode
		String impl = this.implode(params);
		String newRequestUrl = requestUrl;
		if (null != impl) {
			if (requestUrl.indexOf('?') < 0) {
				newRequestUrl = requestUrl + "?" + impl;
			} else {
				newRequestUrl = requestUrl + "&" + impl;
			}
		}
		return this.url("GET", newRequestUrl, null);
	}
	
	public HTTPResult get(String requestUrl) {
		return this.get(requestUrl, null);
	}
	
	public HTTPResult post(String requestUrl, Map<String, String> params) {
		// implode
		String impl = this.implode(params);
		return this.post(requestUrl, impl);
	}
	
	public HTTPResult post(String requestUrl, String requestBody) {
		// abort early
		if (null == requestUrl)	return null;
		
		// do request
		return this.url("POST", requestUrl, requestBody);
	}
	
	public HTTPResult put(String requestUrl, String requestBody) {
		// abort early
		if (null == requestUrl) return null;
		
		// do request
		return this.url("PUT", requestUrl, requestBody);
	}
	
	public HTTPResult delete(String requestUrl) {
		return this.url("DELETE", requestUrl, null);
	}
	
	private HttpURLConnection openConnection(String url) throws Exception {
		return (HttpURLConnection)(new URL(url)).openConnection();
	}
	
	public HTTPResult url(String method, String requestUrl, String requestBody) {
		// create result
		HTTPResult result = new HTTPResult();
		result.okCode = this.okCode;
		this.logFinest("Doing <{0}> request to <{1}> with request body <{2}>", method, requestUrl, requestBody);
		
		HttpURLConnection con = null;
		try {
			con = this.openConnection(requestUrl);
			con.setRequestMethod(method.toUpperCase());
			con.addRequestProperty("Content-Type", this.contentType + "; charset=" + charset);
			con.addRequestProperty("Accept", "*/*");
			for (String k : this.headers.keySet()) {
				this.logFinest("Adding header to connection - key <{0}> value <{1}>", k, this.headers.get(k));
				con.addRequestProperty(k, this.headers.get(k));
			}
			for (String key : this.cookies.keySet()) {
				this.logFinest("Adding cookie header to connection - key <{0}> value <{1}>", key, this.cookies.get(key));
				con.addRequestProperty("Cookie", key + "=" + this.cookies.get(key));
			}
			con.setDoInput(true);
			this.logFinest("Using headers <{0}>", con.getRequestProperties());
			
			// handle post
			if ((method.equalsIgnoreCase("post") || method.equalsIgnoreCase("put")) && null != requestBody) {
				this.logFinest("Writing body to stream <{0}>", requestBody);
				con.setDoOutput(true);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), Charset.forName(this.charset)));
				writer.write(requestBody);
				writer.flush();
			}
			
			// get response code
			this.logFinest("Reading response code", null);
			result.rc = con.getResponseCode();
			this.logFinest("Read response code <{0}>", result.rc);
			
			if (300 == result.rc - (result.rc % 100)) {
				this.logFinest("Response code is 3xx - see if we should follow redirects", null);
				if (this.followRedirects) { 
					// redirect
					this.logFinest("Response code is 3xx and we should follow redirects - reading Location header", null);
					String location = con.getHeaderField("Location");
					this.logFinest("Location header <{0}> - redoing request", location);
					con.disconnect();
					return this.url(method, location, requestBody);
				} else {
					this.logFinest("We should not follow redirects", null);
				}
			}
			
			// read cookies
			result.readResponseHeaders(con);
			
			// decide on charset
			Charset charset = Charset.forName(null != result.getCharSet() ? result.getCharSet() : this.charset); 
			
			// read from url
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(con.getInputStream(), charset));
			StringBuilder buffer = new StringBuilder(1024);
			String line = null;
			while (null != (line = reader.readLine())) {
				buffer.append(line).append('\n');
			}
			con.disconnect();
			
			// return
			result.contents = buffer.toString();
			this.logFinest("Read response body <{0}>", result.contents);
			
		} catch (Throwable t) {
			try {
				result.rc = con.getResponseCode();
				this.logFinest("Read response code {0} even though an exception was thrown", result.rc);
			} catch (Throwable t2) {}
			result.exception = t;
		}
		
		// return
		return result;
	}
	
	public static void enableSelfSignedCerts() throws Throwable {
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		}};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}
	
	/**
	 * Implodes the supplied {@link Map} of parameters into URL format.
	 * 
	 * @param params
	 * @return The imploded parameters or null if no parameters was supplied
	 */
	private String implode(Map<String, String> params) {
		if (null == params || params.isEmpty()) return null;
		StringBuilder b = new StringBuilder(params.size() * 20);
		for (Iterator<String> ite=params.keySet().iterator(); ite.hasNext(); ) {
			String key = ite.next();
			String value = params.get(key);
			if (b.length() > 0) b.append('&');
			b.append(key);
			if (null != value) {
				try {
					b.append('=').append(URLEncoder.encode(value, this.charset));
				} catch (UnsupportedEncodingException e) {}
			}
		}
		return b.toString();
	}
	
	public static class HTTPResult {
		public int okCode = 200;
		public Throwable exception = null;
		public int rc = -1;
		public String contents = null;
		private Map<String, List<String>> headers = null;
		private String contentType = null;
		private String charSet = null;
		
		public boolean isCode(int code) {
			return this.rc == code;
		}
		
		public boolean isOK() {
			return this.isCode(this.okCode);
		}
		
		public String getContentType() {
			return this.contentType;
		}
		
		public String getCharSet() {
			return this.charSet;
		}
		
		public HTTPResult readResponseHeaders(HttpURLConnection con) {
			// get headers
			this.headers = con.getHeaderFields();
			
			// try and find a charset in the response
			String responseContentType = this.getHeader("Content-Type");
			if (responseContentType.indexOf(';') < 0) {
				// only content-type
				this.contentType = responseContentType;
				return this;
			}

			
			// parse header
			int idxDelim = -1;
			int idxPrev = -1;
			while (true) {
				idxDelim = responseContentType.indexOf(';', ++idxPrev);
				String token = responseContentType.substring(idxPrev, idxDelim < 0 ? responseContentType.length() : idxDelim);
				idxPrev = idxDelim;
				
				if (null == this.contentType) {
					// first token is content-type
					this.contentType = token.trim();
					if (idxDelim != 0) continue;
					break;
				}
				
				int idxEqual = token.indexOf('=');
				if (idxEqual < 0) continue;
				String key = token.substring(0, idxEqual).trim();
				String value = token.substring(idxEqual+1).trim();
				if (key.equalsIgnoreCase("charset")) {
					//found charset
					this.charSet = value;
				}
				
				if (idxDelim < 0) break;
			}
			
			return this;
		}
		
		public String getHeader(String name) {
			if (null == this.headers) return null;
			return this.headers.get(name).get(0);
		}
		
		public String getCookie(String cookie) {
			if (null == this.headers) return null;
			List<String> cookies = this.headers.get("Set-Cookie");
			if (null == cookies) return null;
			for (String v : cookies) {
				if (v.startsWith(cookie)) {
					int idx1 = v.indexOf('=');
					if (idx1 >= 0) {
						int idx2 = v.indexOf(';', idx1);
						if (idx2 < idx1) {
							idx2 = v.length()-1;
						}
						return v.substring(idx1+1, idx2);
					}
				}
			}
			return null;
		}
	}
	
	private void logFinest(String s, Object... objects) {
		String l = MessageFormat.format(s, objects);
		logger.finest(l);
	}

}
