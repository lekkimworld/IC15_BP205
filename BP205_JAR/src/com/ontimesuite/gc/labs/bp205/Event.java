package com.ontimesuite.gc.labs.bp205;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade.STATUS;

import dk.intravision.json.Json.JValue;

public class Event extends BaseJsonObject {
	// declarations
	private Community comm = null;
	private String id = null;
	private String subject = null;
	private String location = null;
	private boolean allday = false;
	private Date startDt = null;
	private Date endDt = null;
	private String url = null;
	private STATUS status = STATUS.DONT_CARE;
	
	/**
	 * Build object from JSON.
	 * 
	 * @param comm
	 * @param obj
	 */
	public Event(Community comm, JValue obj) {
		try {
			if (null == comm) throw new IllegalArgumentException("Missing Community object");
			this.comm  = comm;
			this.id = obj.get("ID").getStr();
			this.subject = this.getValueIfPresent(obj, "Subject");
			this.location = this.getValueIfPresent(obj, "Location");
			this.url = this.getValueIfPresent(obj, "URL");
			this.allday = this.getValueIfPresentBool(obj, "Allday", false);
			
			// get dates
			this.startDt = this.getValueIfPresentDate(obj, "StartDT");
			this.endDt = this.getValueIfPresentDate(obj, "EndDT");
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to construct Event object from JSON <" + (null == obj ? "null" : obj.toString() + ">"), t);
		}
	}
	
	/**
	 * Build object from SQL {@link ResultSet} where the cursor is at.
	 * 
	 * @param comm
	 * @param rs
	 * @throws SQLException
	 */
	public Event(Community comm, ResultSet rs) throws SQLException {
		this.comm = comm;
		this.id = rs.getString("event_id");
		this.subject = rs.getString("subject");
		this.location = rs.getString("location");
		this.url = rs.getString("url");
		this.allday = rs.getBoolean("allday");
		this.startDt = rs.getDate("start_dt");
		this.endDt = rs.getDate("end_dt");
		try {
			int clmn = rs.findColumn("action");
			this.status = STATUS.getStatus(rs.getString(clmn));
		} catch (SQLException e) {
			this.status = STATUS.DONT_CARE;
		}
	}
	
	public Event(String id) {
		this.id = id;
	}

	public Community getCommunity() {
		return comm;
	}

	public String getID() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public String getLocation() {
		return location;
	}

	public boolean isAllday() {
		return allday;
	}

	public Date getStartDt() {
		return startDt;
	}

	public Date getEndDt() {
		return endDt;
	}

	public String getUrl() {
		return url;
	}
	
	public STATUS getStatus() {
		return this.status;
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || !(obj instanceof Event)) return false;
		return ((Event)obj).getID().equals(this.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return "[EVENT - id<" + id + "> subject<" + subject + "> location<" + location + "> startdt <" + (null != startDt ? format.format(startDt) : null) + 
				"> enddt <" + (null != endDt ? format.format(endDt) : null) + "> comm<" + comm + ">]";
	}
	
	

}
