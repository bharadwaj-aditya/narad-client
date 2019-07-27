package com.narad.client.applications.collector.api;

import java.util.List;
import java.util.Map;

/**
 * Write profiles to a destination
 * @author Aditya
 *
 */
public interface ProfileWriter {
	
	public String getName();
	
	public boolean savesRaw();
	
	public boolean userExists(Object userId);
	
	public boolean friendsExist(Object userId);
	
	public Map<String, Object> saveUserRaw(Object userId, String userResponse);
	
	public Map<String, Object> saveFriendsRaw(Object userId, String friendsResponse);
	
	public Map<String, Object> saveUser(Object userId, Map<String, Object> userMap);
	
	public Map<String, Object> saveFriends(Object userId, List<Object> friends);

}
