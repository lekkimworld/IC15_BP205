package com.ontimesuite.gc.labs.bp205.connections;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import com.ontimesuite.gc.labs.bp205.ACTION;
import com.ontimesuite.gc.labs.bp205.ArrayUtil;
import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.HTTP;
import com.ontimesuite.gc.labs.bp205.IBMConnectionsInfo;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade;
import com.ontimesuite.gc.labs.bp205.IOAuthFacade;
import com.ontimesuite.gc.labs.bp205.StringUtil;
import com.ontimesuite.gc.labs.bp205.User;
import com.ontimesuite.gc.labs.bp205.HTTP.HTTPResult;

import dk.intravision.json.JsonBuilder;

/**
 * Session Bean EJB that acts as a facade to the activity stream.
 */
@Stateless(mappedName = "as")
@LocalBean
public class ActivityStreamFacade {
	// logger
	private static final Logger logger = Logger.getLogger(ActivityStreamFacade.class.getPackage().getName());

	// declarations
	@EJB
	private ICommunityCalendarFacade facade = null;
	@EJB
	private IOAuthFacade oauth = null;
		
	/**
	 * Post to the activity steam
	 * @param user
	 * @param event
	 * @param action
	 * @param title
	 * @param summary
	 * @param url
	 * @param gadgetUrl
	 */
	public void post(User user, Event event, ACTION action, String title, String summary, String url, String gadgetUrl) {
		this.doPost(user.getUid(), event.getCommunity().getId(), true, user.getGUID(), action, event.getID(), title, summary, url, gadgetUrl, null);
	}
	
	/**
	 * Utility method.
	 * 
	 * @param uid
	 * @param recipientId
	 * @param recipientCommunity
	 * @param userGUID
	 * @param action
	 * @param eventId
	 * @param title
	 * @param summary
	 * @param url
	 * @param gadgetUrl
	 * @param ltpatoken
	 */
	private void doPost(String uid, String recipientId, boolean recipientCommunity, String userGUID, ACTION action, String eventId, String title, String summary, String url, String gadgetUrl, String ltpatoken) {
		// define date format
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// make sure we have oauth tokens for the user
		String[] tokens = this.oauth.getOAuthTokensForUser(uid);
		if (ArrayUtil.isEmpty(tokens)) throw new RuntimeException("Unable to post to activity stream due to missing OAuth tokens for user");
		
		// build JSON
		JsonBuilder b = new JsonBuilder()
			.beginObject("generator")
				.add("id", "otgc_commcal")
			.endObject()
			.beginObject("actor")
				.add("id", "@me")
			.endObject();
		if (!StringUtil.isEmpty(recipientId)) {
			b.beginArray("to")
				.beginArrayObject()
					.add("objectType", recipientCommunity ? "community" : "person")
					.add("id", recipientCommunity ? "urn:lsid:lconn.ibm.com:communities.community:" + recipientId : recipientId)
				.endArrayObject()
				.endArray();
		}
		String verb = null;
		if (null != action) {
			switch (action) {
			case ACCEPTED:
				verb = "accept";
				break;
			case REJECTED:
				verb = "reject";
				break;
			}
		} else {
			verb = "unhandle";
		}
		b.add("verb", verb)
			.add("title", StringUtil.isEmpty(title) ? "${create}" : title)
			.add("updated", format.format(new Date()))
			.beginObject("object")
				.add("summary", summary)
				.add("objectType", "community meeting")
				.add("id", eventId)
				.add("url", url)
			.endObject()
			.beginObject("connections")
				.add("rollupid", eventId)
			.endObject();
		if (!StringUtil.isEmpty(gadgetUrl)) {
			b.beginObject("openSocial")
				.beginObject("embed")
					.add("gadget", gadgetUrl)
					.beginObject("context")
						.add("EventID", eventId)
						.add("UserID", userGUID)
					.endObject()
				.endObject()
			.endObject();
		}
		String json = b.toString();
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructed JSON for ActivityStream post <" + json + ">");
		
		// compose url
		IBMConnectionsInfo icInfo = this.facade.getIBMConnectionsInfo();
		URL postUrl = icInfo.getActivityStreamPostUrl();
		
		// post to activity stream
		HTTPResult result = this.oauth.post(postUrl, json);
		
	}
	
}
