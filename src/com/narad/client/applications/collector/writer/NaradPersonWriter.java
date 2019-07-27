package com.narad.client.applications.collector.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.NaradJsonPersonClient;
import com.narad.client.applications.collector.api.ProfileWriter;
import com.narad.client.applications.collector.reader.LiveInboxReader;
import com.narad.command.NaradCommandConstants;
import com.narad.dataaccess.dao.DaoConstants;

public class NaradPersonWriter implements ProfileWriter {
	private static final String CMD_DATA = "data";
	private static final String CMD_RESULT = "result";
	private static final String CMD_STATUS = "status";
	private static final String CMD_DESCRIPTION = "description";
	private static final String CMD_SUCCESS = "SUCCESS";

	public static final String NETWORK = "network";

	private final Logger logger;
	private String naradUrl;
	private NaradJsonPersonClient naradJsonClient;

	public NaradPersonWriter(Logger logger, String naradUrl) {
		super();
		if (logger == null) {
			this.logger = LoggerFactory.getLogger(NaradPersonWriter.class);
		} else {
			this.logger = logger;
		}
		this.naradUrl = naradUrl;
		naradJsonClient = new NaradJsonPersonClient(naradUrl);
	}

	private void reinitNaradClient() {
		naradJsonClient = new NaradJsonPersonClient(naradUrl);
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
		// return userId + "@" + getName() + ".com";
		return null;
	}

	@Override
	public boolean friendsExist(Object userId) {
		// No way to check currently!
		// return userExists(userId);
		// TODO - fix this - by getting all friends and checking
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
		logger.info("Adding user");
		try {
			if (userMap == null || userMap.isEmpty()) {
				return null;
			}
			if (!userMap.containsKey(DaoConstants.NETWORKS)) {
				logger.info("Cannot add a user with no networks");
				return null;
			}
			JSONObject addNode = naradJsonClient.addPerson(getUserEmailId(userId), userMap);
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
		String status = (String) result.get(CMD_STATUS);
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
		return (String) result.get(CMD_DESCRIPTION);
	}

	@Override
	public String getName() {
		return "NaradWriter";
	}

	@Override
	public Map<String, Object> saveUser(Object userId, Map<String, Object> userMap) {
		return adduser(userId, userMap);
	}

	@Override
	public Map<String, Object> saveFriends(Object userId, List<Object> friends) {
		if (userId instanceof Map) {
			for (Object friendObj : friends) {
				if (friendObj instanceof Map) {
					addRelationship((Map) userId, (Map) friendObj);
				} else {
					logger.info("Cant process as both users are not maps. WHY??");
				}
			}
		} else {
			for (Object friendObj : friends) {
				addRelationship(userId, friendObj, Collections.EMPTY_MAP);
			}
		}
		return null;
	}
	
	public JSONObject addRelationship(Map<String, Object> fromUser, Map<String, Object> toUser) {
		try {
			logger.info("Adding relation");
			HashMap<String,Object> properties = new HashMap<String, Object>();
			List<Map<String, Object>> relationNetworks = new ArrayList<Map<String,Object>>();
			properties.put(DaoConstants.RELATION, relationNetworks);
			//NaradCommandConstants.FROM_PROPERTIES
			//NaradCommandConstants.TO_PROPERTIES
			Object networksObject = toUser.get(DaoConstants.NETWORKS);
			if (networksObject != null && networksObject instanceof List && !((List)networksObject).isEmpty()) {
				Map<String,Object> network = (Map)((List)networksObject).get(0);
				HashMap<String, Object> relationNetwork = new HashMap<String, Object>();
				Object object = network.get(DaoConstants.NETWORK_ID);
				if (LiveInboxReader.FACEBOOK.equals(object)) {
					relationNetwork.put(DaoConstants.REL_NAME, object);
					relationNetwork.put(DaoConstants.REL_TYPE, "network");
					relationNetwork.put(DaoConstants.REL_SUB_TYPE, "friend");
				} else if (LiveInboxReader.LINKEDIN.equals(object)) {
					relationNetwork.put(DaoConstants.REL_NAME, object);
					relationNetwork.put(DaoConstants.REL_TYPE, "network");
					relationNetwork.put(DaoConstants.REL_SUB_TYPE, "connection");
				}
				if(!relationNetwork.isEmpty()) {
					relationNetworks.add(relationNetwork);
				}
			} else {
				logger.info("Cannot add relationship, go figure!");
				return new JSONObject();
			}
			
			HashMap<String,Object> relationMap = new HashMap<String, Object>();
			relationMap.put(NaradCommandConstants.FROM_PROPERTIES, properties);
			relationMap.put(NaradCommandConstants.TO_PROPERTIES, properties);//Use same as from and to,, works only for fb and lin
			
			JSONObject addRelationship = naradJsonClient.addRelationship(fromUser, toUser, relationMap);
			return addRelationship;
		} catch (ClientWebApplicationException e) {
			reinitNaradClient();
			logger.error("Error while adding relationship", e);
			return addRelationship(fromUser, toUser);
		}
	}


}
