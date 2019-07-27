package com.narad.client.applications.collector.writer;

import java.util.Map;

import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import com.narad.client.NaradJsonClient;
import com.narad.client.applications.collector.api.ProfileWriter;

public abstract class NaradProfileWriter implements ProfileWriter {
	private static final String CMD_DATA = "data";
	private static final String CMD_RESULT = "result";
	private static final String CMD_STATUS = "status";
	private static final String CMD_DESCRIPTION = "description";
	private static final String CMD_SUCCESS = "SUCCESS";
	
	public static final String NETWORK = "network";
	
	private final Logger logger;
	private String naradUrl;
	private NaradJsonClient naradJsonClient;

	public NaradProfileWriter(Logger logger, String naradUrl) {
		super();
		this.logger = logger;
		this.naradUrl = naradUrl;
		naradJsonClient = new NaradJsonClient(naradUrl);
	}
	
	private void reinitNaradClient () {
		naradJsonClient = new NaradJsonClient(naradUrl);
	}

	@Override
	public boolean savesRaw() {
		return false;
	}

	@Override
	public boolean userExists(Object userId) {
		try {
			JSONObject findPerson = naradJsonClient.findPerson(getUserEmailId(userId), null);
			if (isSuccess(findPerson) && getData(findPerson) != null) {
				return true;
			}
			return false;
		} catch (ClientWebApplicationException e) {
			reinitNaradClient();
			logger.error("Error while making client call", e);
			return userExists(userId);
		}
	}

	public String getUserEmailId(Object userId) {
		if (userId != null) {
			return userId + "@" + getName() + ".com";
		} else {
			return null;
		}
	}	

	@Override
	public boolean friendsExist(Object userId) {
		// No way to check currently!
		// return userExists(userId);
		return false;
	}

	@Override
	public Map<String, Object> saveUserRaw(Object userId, String userResponse) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> saveFriendsRaw(Object userId, String friendsResponse) {
		throw new UnsupportedOperationException();
	}

	public Map<String, Object> adduser(Object userId, Map<String, Object> userMap) {
		try{
		if (userMap == null || userMap.isEmpty()) {
			return null;
		}
		/*if (!userMap.containsKey("profiles")) {
			return null;
		}*/
		JSONObject addNode = naradJsonClient.addNode(getUserEmailId(userId), userMap);
		return getData(addNode);
		} catch (ClientWebApplicationException e) {
			reinitNaradClient();
			logger.error("Error while adding user", e);
			return adduser(userId, userMap);
		}
	}

	public JSONObject addRelationship(Object fromUserId, Object toUserId, Map<String, Object> properties) {
		if (fromUserId == null || toUserId == null) {
			return null;
		}
		try {
			JSONObject addRelationship = naradJsonClient.addRelationship(getUserEmailId(fromUserId),
					getUserEmailId(toUserId), properties);
			return addRelationship;
		} catch (ClientWebApplicationException e) {
			reinitNaradClient();
			logger.error("Error while adding relationship", e);
			return addRelationship(fromUserId, toUserId, properties);
		}
	}

	protected boolean isSuccess(JSONObject cmdResult) {
		JSONObject result = (JSONObject) cmdResult.get(CMD_RESULT);
		String status = (String)result.get(CMD_STATUS);
		if (CMD_SUCCESS.equals(status)) {
			return true;
		}
		return false;
	}
	
	protected Map<String, Object> getData(JSONObject cmdResult) {
		JSONObject result = (JSONObject) cmdResult.get(CMD_RESULT);
		return (Map) result.get(CMD_DATA);
	}
	
	protected String getDescription(JSONObject cmdResult) {
		JSONObject result = (JSONObject) cmdResult.get(CMD_RESULT);
		return (String)result.get(CMD_DESCRIPTION);
	}


}
