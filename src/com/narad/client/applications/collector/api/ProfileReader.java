package com.narad.client.applications.collector.api;

import java.util.List;
import java.util.Map;

/**
 * Read profiles from a given source
 * @author Aditya
 *
 */
public interface ProfileReader {
	
	public String getName();
	
	public boolean userExists(Object userId);
	
	public boolean friendsExist(Object userId);
	
	public Map<String, Object> fetchUser(Object id);
	
	public List<Object> fetchFriends(Object id, Map<String, Object> userMap);
	
	public String getLastResponse();
	
}
