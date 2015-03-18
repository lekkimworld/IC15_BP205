package com.ontimesuite.gc.labs.bp205.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.ontimesuite.gc.labs.bp205.ACTION;
import com.ontimesuite.gc.labs.bp205.Community;
import com.ontimesuite.gc.labs.bp205.Configuration;
import com.ontimesuite.gc.labs.bp205.Event;
import com.ontimesuite.gc.labs.bp205.ICommunityCalendarFacade.STATUS;
import com.ontimesuite.gc.labs.bp205.StringUtil;
import com.ontimesuite.gc.labs.bp205.User;
import com.ontimesuite.gc.labs.bp205.UserEvent;


/**
 * Session bean to handle data access.
 * 
 * @author lekkim
 */
@Stateless(mappedName = "dao")
@TransactionManagement(TransactionManagementType.BEAN)
public class DAO {
	// logger
	private static final Logger logger = Logger.getLogger(DAO.class.getPackage().getName());
	
	// declarations
	@Resource(name="jdbc/commcal")
	private DataSource dsCommcal = null;
	@Resource
	private EJBContext context;
	
	public DAO() {
		
	}
	
	private Connection getConnection() throws SQLException {
		Connection con = this.dsCommcal.getConnection();
		return con;
	}
	
	public String[] getOAuthTokensForUser(String uid) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			// lookup user in database
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("select access_token, refresh_token from commcal.tokens where user_key='" + uid + "'");
			if (rs.next()) {
				// found token - get it
				String access_token = rs.getString(1);
				String refresh_token = rs.getString(2);
				return new String[]{access_token, refresh_token};
			} else {
				return null;
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
			
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	public boolean addOAuthTokensForUser(String uid, String access_token, String refresh_token) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			// lookup user in database
			String sql = "insert into commcal.tokens (user_key, access_token, refresh_token) values ('" + uid + "', '" + access_token + "', '" + refresh_token + "')";
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int count = stmt.executeUpdate(sql);
			trans.commit();
			return count == 1;
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			t.printStackTrace();
			return false;
			
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public boolean deleteOAuthTokensForUser(String uid) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = null;
		
		try {
			// lookup user in database
			System.out.println("*** getting connection");
			con = this.getConnection();
			System.out.println("*** got connection " + con);
			trans = this.context.getUserTransaction();
			System.out.println("*** got tranactions " + trans);
			trans.begin();
			System.out.println("*** began trans");
			stmt = con.createStatement();
			int count = stmt.executeUpdate("delete from commcal.tokens where user_key='" + uid + "'");
			System.out.println("*** committing");
			trans.commit();
			return count == 1;
			
		} catch (Throwable t) {
			t.printStackTrace();
			try {
				System.out.println("*** rolling back " +trans);
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			
			return false;
			
		} finally {
			this.closeObjects(con, stmt,  null);
		}
	}
	
	public boolean updateOAuthTokensForUser(String uid, String access_token, String refresh_token) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			// lookup user in database
			String query = "update commcal.tokens set access_token='" + access_token + "', refresh_token='" + refresh_token + "' where user_key='" + uid + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int count = stmt.executeUpdate(query);
			trans.commit();
			return count == 1;
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			t.printStackTrace();
			return false;
			
		} finally {
			this.closeObjects(con, stmt,  null);
		}
	}
	
	public Event[] getEvents(User user, STATUS status) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			StringBuilder b = new StringBuilder();
			if (STATUS.ACCEPTED == status || STATUS.REJECTED == status) {
				b.append("select e.comm_id, c.comm_name, e.event_id, e.event_type, e.start_dt, ")
					.append("e.end_dt, e.subject, e.location, e.url, e.allday, u.action from ")
					.append("commcal.event as e, commcal.community as c, commcal.user_event as u ")
					.append("where e.comm_id=c.comm_id and e.event_id=u.event_id and u.user_key='")
					.append(user.getKey()).append("' and e.event_id in (select event_id from ")
					.append("commcal.user_event where user_key='").append(user.getKey())
					.append("' and action='");
				if (STATUS.ACCEPTED == status) {
					b.append(STATUS.ACCEPTED.getStringValue());
				} else {
					b.append(STATUS.REJECTED.getStringValue());
				}
				b.append("')");
			} else if (STATUS.UNHANDLED == status) {
				b.append("select e.comm_id, c.comm_name, e.event_id, e.event_type, e.start_dt, e.end_dt, " + 
						"e.subject, e.location, e.url, e.allday, '" + STATUS.UNHANDLED.getStringValue() + 
						"' as action from commcal.event as e, commcal.community as c " +
						"where e.comm_id=c.comm_id and e.event_id not in (select event_id from commcal.user_event " + 
						"where user_key='").append(user.getKey()).append("')");
			} else if (STATUS.DONT_CARE == status) {
				b.append("select e.comm_id, c.comm_name, e.event_id, e.event_type, e.start_dt, e.end_dt, " + 
					"e.subject, e.location, e.url, e.allday, '" + STATUS.DONT_CARE.getStringValue() + 
					"' as action from commcal.event as e, commcal.community as c where e.comm_id=c.comm_id");
			}
			
			// execute
			String query = b.toString();
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			
			// loop and build resulting events
			Map<String, Community> comms = new HashMap<String, Community>(16);
			List<Event> events = new LinkedList<Event>();
			while (rs.next()) {
				// get community
				String comm_id = rs.getString("comm_id");
				String comm_name = rs.getString("comm_name");
				Community comm = comms.get(comm_id);
				if (null == comm) {
					comm = new Community(comm_id, comm_name);
					comms.put(comm_id, comm);
				}
				
				// build event
				Event event = new Event(comm, rs);
				events.add(event);
			}
			
			// return
			if (logger.isLoggable(Level.FINE)) logger.fine("Retrieved <" + events.size() + "> events for user <" + user + ">");
			return events.toArray(new Event[events.size()]);
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrieve events user user <" + user + "> and status <" + status + ">", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	public Event getEvent(String eventId) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select e.comm_id, c.comm_name, e.event_id, e.event_type, e.start_dt, e.end_dt, " + 
					"e.subject, e.location, e.url, e.allday, '" + STATUS.DONT_CARE.getStringValue() + 
					"' as action from commcal.event as e, commcal.community as c where e.comm_id=c.comm_id and e.event_id='" + eventId + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				// get community
				String comm_id = rs.getString("comm_id");
				String comm_name = rs.getString("comm_name");
				Community comm = new Community(comm_id, comm_name);
				Event event = new Event(comm, rs);
				return event;
			} else {
				return null;
			}
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrieve event by id <" + eventId + ">", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	/**
	 * Returns all communities known.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Community[] getCommunities() throws Exception {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select comm_id, comm_name from commcal.community";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			List<Community> comms = new LinkedList<Community>();
			while (rs.next()) {
				comms.add(new Community(rs.getString("comm_id"), rs.getString("comm_name")));
			}
			
			// return
			return comms.toArray(new Community[comms.size()]);
			
		} catch (Throwable t) {
			throw new Exception("Unable to retrive communities", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}

	/**
	 * Returns the community with the supplied id.
	 * 
	 * @param id
	 * @return
	 */
	public Community getCommunity(String id) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select comm_id, comm_name from commcal.community where comm_id='" + id + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				return new Community(rs.getString("comm_id"), rs.getString("comm_name"));
			} else {
				// unknown 
				return null;
			}
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrive community by id <" + id + ">", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	/**
	 * Removes the community with the supplied id from the database.
	 * 
	 * @param comm
	 * @return
	 */
	public void removeCommunity(Community comm) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "delete from commcal.community where comm_id='" + comm.getId() + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to remove community <" + comm + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	/**
	 * Updates the name of the supplied community.
	 * 
	 * @param comm
	 * @return
	 */
	public void updateCommunity(Community comm) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "update commcal.community set comm_name='" + 
					comm.getName() + "' where comm_id='" + comm.getId() + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to update community <" + comm + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	/**
	 * Adds the name of the supplied community.
	 * 
	 * @param comm
	 * @return
	 */
	public void addCommunity(Community comm) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "insert into commcal.community (comm_id, comm_name) values ('" + 
					comm.getId() + "', '" + comm.getName() + "')"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to add community <" + comm + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public void addEvent(Event event) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "insert into commcal.event (event_id, event_type, comm_id, start_dt, end_dt, allday, location, subject, url) values ('" + event.getID() + "', 'C', '" + event.getCommunity().getId() + "', timestamp_format('" + format.format(event.getStartDt()) + "', 'YYYY-MM-DD HH24:MI')-CURRENT TIMEZONE, timestamp_format('" + format.format(event.getEndDt()) + "', 'YYYY-MM-DD HH24:MI')-CURRENT TIMEZONE, '" + (event.isAllday() ? '1' : '0') + "', " + (StringUtil.isEmpty(event.getLocation()) ? "null" : "'" + event.getLocation() + "'") + ", '" + event.getSubject() + "', '" + event.getUrl() + "')"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to add event <" + event + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public void updateEvent(Event event) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "update commcal.event set start_dt=timestamp_format('" + format.format(event.getStartDt()) + "', 'YYYY-MM-DD HH24:MI')-CURRENT TIMEZONE, end_dt=timestamp_format('" + format.format(event.getEndDt()) + "', 'YYYY-MM-DD HH24:MI')-CURRENT TIMEZONE, allday='" + (event.isAllday() ? '1' : '0') + "', location='" + event.getLocation() + "', subject='" + event.getSubject() + "', url='" + event.getUrl() + "' where event_id='" + event.getID() + "'"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to update event <" + event + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public void removeEvent(Event event) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "delete from commcal.event where event_id='" + event.getID() + "'"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to remove event <" + event + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	/**
	 * Returns the mappings from an {@link Event} to {@link User}s.
	 * 
	 * @param event
	 * @return
	 */
	public UserEvent[] getUserEventMappings(Event event) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select u.user_key, u.uid, u.email, action, unid from commcal.user_event as ue, commcal.user as u where ue.user_key=u.user_key and event_id='" + event.getID() + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			List<UserEvent> results = new LinkedList<UserEvent>();
			Map<String, User> users = new HashMap<String, User>(16);
			while (rs.next()) {
				String user_key = rs.getString("user_key");
				User user = users.get(user_key);
				if (null == user) {
					user = new User(user_key, null, rs.getString("uid"), rs.getString("email"), false);
					users.put(user_key, user);
				}
				ACTION action = ACTION.getAction(rs.getString("action"));
				String unid = rs.getString("unid");
				UserEvent ue = new UserEvent(user, event, action, unid);
				results.add(ue);
			}
			
			// return
			return results.toArray(new UserEvent[results.size()]);
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrive user/event mappings for event <" + event + ">", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	/**
	 * Returns the mappings from an {@link Event} to {@link User}s.
	 * 
	 * @param event
	 * @return
	 */
	public UserEvent getUserEventMapping(User user, String eventId) {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select action, unid from commcal.user_event where event_id='" + eventId + "' and user_key='" + user.getKey() + "'";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				ACTION action = ACTION.getAction(rs.getString("action"));
				String unid = rs.getString("unid");
				Event event = this.getEvent(eventId);
				UserEvent ue = new UserEvent(user, event, action, unid);
				return ue;
			} else {
				// not found
				return null;
			}
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrive user/event mapping for user <" + user + "> and event_id <" + eventId + ">", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	public void removeUserEventMapping(UserEvent ue) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "delete from commcal.user_event where event_id='" + ue.getEvent().getID() + "' and user_key='" + ue.getUser().getKey() + "'"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to remove user/event mapping for user<" + ue.getUser() + " and event<" + ue.getEvent() + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public void addUserEventMapping(User user, Event event, ACTION action, String unid) {
		Connection con = null;
		Statement stmt = null;
		UserTransaction trans = this.context.getUserTransaction();
		
		try {
			String query = "insert into commcal.user_event (event_id, user_key, action, unid) values ('" + 
					event.getID() + "', '" + user.getKey() + "', '" + action.name().charAt(0) + "', " + 
					(StringUtil.isEmpty(unid) ? "null" : "'" + unid + "'") + ")"; 
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			trans.begin();
			stmt = con.createStatement();
			int resultCount = stmt.executeUpdate(query);
			trans.commit();
			
		} catch (Throwable t) {
			try {
				trans.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Unable to add user/event mapping for user<" + user + " and event<" + event + ">", t);
		} finally {
			this.closeObjects(con, stmt, null);
		}
	}
	
	public Configuration getConfiguration() {
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			String query = "select * from commcal.configuration";
			if (logger.isLoggable(Level.FINEST)) logger.finest(query);
			con = this.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			return new Configuration(rs);
			
		} catch (Throwable t) {
			throw new RuntimeException("Unable to retrive configuration", t);
		} finally {
			this.closeObjects(con, stmt, rs);
		}
	}
	
	private void closeObjects(Connection con, Statement stmt, ResultSet rs) {
		try {
			if (null != rs) rs.close();
			if (null != stmt) stmt.close();
			if (null != con) con.close();
		} catch (SQLException e) {
			
		}
	}
}
