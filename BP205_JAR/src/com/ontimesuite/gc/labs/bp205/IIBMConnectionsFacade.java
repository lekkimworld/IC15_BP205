package com.ontimesuite.gc.labs.bp205;

import javax.ejb.Local;

/**
 * Service interface for the IBM Connections API facade.
 * 
 * @author lekkim
 */
@Local
public interface IIBMConnectionsFacade {

	/**
	 * Using the OAuth information obtains a user object for the current user.
	 * 
	 * @return
	 */
	public User getUserObject();
	
	/**
	 * Using the OAuth information for the supplied user obtains a 
	 * list of {@link Community communities} for that user. If no 
	 * OAuth information is present we throw a RuntimeException.
	 * 
	 * @param user
	 * @return
	 */
	public Community[] getCommunitiesForUser(User user);
}
