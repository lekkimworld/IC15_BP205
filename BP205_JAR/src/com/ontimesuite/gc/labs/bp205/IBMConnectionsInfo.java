package com.ontimesuite.gc.labs.bp205;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class to obtain service information from IBM Connections while still 
 * allowing overriding for testing.
 * 
 * @author lekkim
 *
 */
public class IBMConnectionsInfo {
	// constants
	protected static final String ACTIVITY_STREAM_POST = "/opensocial/oauth/rest/activitystreams/@me/@all/@all";
	protected static final String COMMUNITIES_MINE_FEED = "/service/atom/oauth/communities/my?ps=1000";
	protected static final String PROFILE_SERVICE_DOCUMENT = "/oauth/atom/profileService.do";
	protected static final String PROFILE_USER = "/oauth/atom/profile.do?format=full&userid=";
	public static final String COMMUNITY_SERVICE_DOCUMENT = "/oauth/calendar/atom/calendar/service?calendarUuid={0}";
	public static final String COMMUNITY_HTML_URL = "/service/html/communitystart?communityUuid=";
	public static final String ATOM_TITLE_COMMUNTIES_EVENT_COLL = "Community Events";
	public static final String COMMUNITY_CALENDAR_EVENTS = "/oauth/calendar/atom/calendar/event?calendarUuid={0}&startDate={1}&endDate={2}&lang=en_us";
	public static final String ATOM_REL_ATTEND_COMMUNITY_CALENDAR_EVENT = "http://www.ibm.com/xmlns/prod/sn/calendar/event/attend";
	public static final String ATOM_REL_FOLLOW_COMMUNITY_CALENDAR_EVENT = "http://www.ibm.com/xmlns/prod/sn/calendar/event/follow";
	
	// declarations
	private Configuration config = null;
	private URL communitiesMyUrl = null;
	private URL profilesServiceURL = null;
	
	/**
	 * Default constructor.
	 * 
	 */
	public IBMConnectionsInfo(Configuration config) {
		this.config = config;
		try {
			this.communitiesMyUrl = new URL(config.getURL(SERVICE.COMMUNITIES, true) + COMMUNITIES_MINE_FEED);
			this.profilesServiceURL = new URL(config.getURL(SERVICE.PROFILES, true) + PROFILE_SERVICE_DOCUMENT);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unable to build URL", e);
		}
	}

	public URL getMyCommunitiesUrl() {
		return communitiesMyUrl;
	}

	public URL getProfilesServiceDocumentUrl() {
		return this.profilesServiceURL;
	}
	
	public URL getCommunityURL(String communityId) {
		try {
			return new URL(this.config.getURL(SERVICE.COMMUNITIES, true) + COMMUNITY_HTML_URL + communityId);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unable to build URL", e);
		}
	}
	
	public URL getProfileUrl(String userId) {
		try {
			return new URL(this.config.getURL(SERVICE.PROFILES, true) + PROFILE_USER + userId);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unable to build URL", e);
		}
	}
	
	public URL getActivityStreamPostUrl() {
		try {
			return new URL(this.config.getURL(SERVICE.CONNECTIONS, true) + ACTIVITY_STREAM_POST);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unable to build URL", e);
		}
	}
}
