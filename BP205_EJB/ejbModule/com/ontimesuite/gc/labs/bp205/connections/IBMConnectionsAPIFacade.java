package com.ontimesuite.gc.labs.bp205.connections;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import com.ontimesuite.gc.labs.bp205.Community;
import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.HTTP;
import com.ontimesuite.gc.labs.bp205.IBMConnectionsInfo;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade;
import com.ontimesuite.gc.labs.bp205.IIBMConnectionsFacade;
import com.ontimesuite.gc.labs.bp205.IOAuthFacade;
import com.ontimesuite.gc.labs.bp205.SERVICE;
import com.ontimesuite.gc.labs.bp205.StringUtil;
import com.ontimesuite.gc.labs.bp205.User;
import com.ontimesuite.gc.labs.bp205.HTTP.HTTPResult;

/**
 * Session Bean implementation class IBMConnectionsAPIFacade
 */
@Stateless(mappedName = "ic")
@LocalBean
@Local(IIBMConnectionsFacade.class)
public class IBMConnectionsAPIFacade implements IIBMConnectionsFacade {
	// logger
	private static final Logger logger = Logger.getLogger(IBMConnectionsAPIFacade.class.getPackage().getName());
	
	// declarations
	@Resource
	private SessionContext sessionContext;
	@EJB
	private ICommunityCalendarFacade facade = null;
	@EJB
	private IOAuthFacade oauth = null;
	
	@Override
	public User getUserObject() {
		// get ic info
		IBMConnectionsInfo icInfo = this.facade.getIBMConnectionsInfo();
		
		// get service document URL
		URL urlServiceDoc = icInfo.getProfilesServiceDocumentUrl();
		
		// request service document through the OAuth facade
		HTTP.HTTPResult result = this.oauth.get(urlServiceDoc);
		if (!result.isOK()) {
			throw new RuntimeException("Unable to get Profiles service document for user");
		}
		final String userId = StringUtil.getSubstring(result.contents, "<snx:userid>", "</snx:userid>");
		if (StringUtil.isEmpty(userId)) {
			throw new RuntimeException("Unable to find userid in Profiles service document for user");
		}
		
		// load user profile
		URL urlProfile = icInfo.getProfileUrl(userId);
		result = this.oauth.get(urlProfile);
		if (!result.isOK()) {
			throw new RuntimeException("Unable to load full Profiles profile for user id <" + userId + ">");
		}
		
		final String userKey = StringUtil.getSubstring(result.contents, "<div class=\"x-profile-key\">", "</div>");
		final String userUid = StringUtil.getSubstring(result.contents, "<div class=\"x-profile-uid\">", "</div>");
		final String userEmail = StringUtil.getSubstring(result.contents, "<email>", "</email>");
		final String strExternal = StringUtil.getSubstring(result.contents, "<snx:isExternal>", "</snx:isExternal>");
		boolean userExternal = false;
		if (!StringUtil.isEmpty(strExternal)) {
			userExternal = Boolean.parseBoolean(strExternal);
		} else {
			userExternal = false;
		}
		
		// create user object
		User user = new User(userKey, userId, userUid, userEmail, userExternal);
		
		// return
		return user;
	}

	@Override
	public Community[] getCommunitiesForUser(User user) {
		// get ic info
		IBMConnectionsInfo icInfo = this.facade.getIBMConnectionsInfo();
		
		// get communtiies
		URL url = icInfo.getMyCommunitiesUrl();
		HTTP.HTTPResult result = this.oauth.get(url);
		if (!result.isOK()) {
			throw new RuntimeException("Unable to get Communities feed for user <" + user + ">");
		}
		List<Community> comms = new LinkedList<Community>();
		int idx1 = 0;
		int idx2 = 0;
		while (true) {
			idx1 = result.contents.indexOf("<snx:communityUuid>", idx2);
			if (idx1 < 0) break;
			idx2 = result.contents.indexOf("</snx:communityUuid>", idx1+19);
			String comm_id = result.contents.substring(idx1+19, idx2);
			idx1 = result.contents.indexOf("<title type=\"text\">", idx2);
			idx2 = result.contents.indexOf("</title>", idx1+19);
			String comm_name = result.contents.substring(idx1+19, idx2);
			comms.add(new Community(comm_id, comm_name));
		}
		
		// return
		return comms.toArray(new Community[comms.size()]);
	}

	public void attendEvent(Event event) {
		// compose base url
		String baseUrl = this.facade.getConfiguration().getURL(SERVICE.COMMUNITIES, true);
		
		
	}
}
