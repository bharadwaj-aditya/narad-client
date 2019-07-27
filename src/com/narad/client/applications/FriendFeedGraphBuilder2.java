package com.narad.client.applications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.command.NaradCommandConstants;

public class FriendFeedGraphBuilder2 extends RestNetworksGraphBuilder {

	private static final Logger logger = LoggerFactory.getLogger(FriendFeedGraphBuilder2.class);

	private static final String SUBSCRIBERS = "subscribers";
	private static final String SUBSCRIPTIONS = "subscriptions";

	public FriendFeedGraphBuilder2() {
		super(logger);
	}

	@Override
	public String getUrl() {
		return "http://friendfeed-api.com/v2/";
	}

	@Override
	public String getNetwork() {
		return "friendfeed";
	}

	@Override
	protected boolean isErrorResponse(int status, Map<String, Object> parsedResponse) {
		if (status == 403) {
			return true;
		}
		return false;
	}

	@Override
	public Map<String, Object> getUserById(Object id) throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		// map.put("id", id);
		Map<String, Object> callReturn = executeJsonCall("feedinfo/" + id, map, id.toString() + ".json");
		if (callReturn == null) {
			logger.info("Friend feed is null for user: {}", id);
			return Collections.EMPTY_MAP;
		}
		return callReturn;
	}

	@Override
	protected Map<String, Object> addUser(Object id) throws IOException {
		boolean success = false;
		Map<String, Object> userProps = getUserById(id);
		String emailId = null;
		if (!userProps.isEmpty()) {
			//String screenName = (String) userProps.get(ID);
			if (id != null) {
				emailId = getEmail(id);
				// Add node
				userProps.put(NETWORK, getNetwork());
				ArrayList<Object> profiles = new ArrayList<Object>();
				Map<String, Object> profileProperties = new HashMap<String, Object>();
				for (Map.Entry<String, Object> entry : userProps.entrySet()) {
					String key = entry.getKey();
					if (key.equals(SUBSCRIPTIONS) || key.equals(SUBSCRIBERS)) {
					} else {
						profileProperties.put(entry.getKey(), entry.getValue());
					}
				}
				profiles.add(profileProperties);
				Map<String, Object> nodeMap = new HashMap<String, Object>();
				Object name = userProps.get(NAME);
				if (name != null) {
					nodeMap.put(NAME, name);
				}
				nodeMap.put(NaradCommandConstants.COMMAND_PROFILES, profiles);
				success = addNodeInServer(emailId, nodeMap);
			}
		}
		if (success) {
			return userProps;
		}
		return null;
	}

	@Override
	protected List<Object> getFriendIds(Map<String, Object> addedUserMap) {
		// Map<String, Object> addedUserMap = (Map)addedUserObj;
		List<Object> friendIds = new ArrayList<Object>();
		Object subscriptionsObject = addedUserMap.get(SUBSCRIPTIONS);
		if (subscriptionsObject != null && subscriptionsObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscriptionsObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					if (friendIds.size() > MAX_FRIENDS_PER_USER) {
						logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserMap.get(ID));
						return friendIds;
					}
				}
			}
		}

		Object subscribersObject = addedUserMap.get(SUBSCRIBERS);
		if (subscribersObject != null && subscribersObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscribersObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					if (friendIds.size() > MAX_FRIENDS_PER_USER) {
						logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserMap.get(ID));
						return friendIds;
					}
				}
			}
		}

		return friendIds;
	}
}
