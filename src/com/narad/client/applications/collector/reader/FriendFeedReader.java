package com.narad.client.applications.collector.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.util.CommunicationResponse;
import com.narad.client.util.CommunicationUtil;

public class FriendFeedReader extends NetworkReader {

	private static final Logger logger = LoggerFactory.getLogger(FriendFeedReader.class);
	private static final Object ID = "id";
	private static final Object SUBSCRIBERS = "subscribers";
	private static final Object SUBSCRIPTIONS = "subscriptions";

	public FriendFeedReader(String proxyHost, int proxyPort) {
		super(logger, proxyHost, proxyPort);
	}

	@Override
	public String getNetworkName() {
		return "friendfeed";
	}

	@Override
	public String getBaseUrl() {
		return "http://friendfeed-api.com/v2/";
	}

	@Override
	public String getName() {
		return "friendfeed";
	}

	@Override
	public Map<String, Object> fetchUser(Object id) {
		CommunicationResponse response = makeCall("feedinfo/" + id, null);
		if (isErrorResponse(response)) {
			response.setError("Could not fetch " + getNetworkName() + " users. Response: " + getLastResponse());
		} else if (response.getParsedJson() == null) {
			logger.info("Friend feed is null for user: {}", id);
		}
		return response;
	}

	@Override
	public List<Object> fetchFriends(Object userId, Map<String, Object> userMap) {
		// Map<String, Object> addedUserMap = (Map)addedUserObj;
		if (userMap == null) {
			userMap = fetchUser(userId);
		}
		lastResponse = null;
		List<Object> friendIds = new ArrayList<Object>();
		Object subscriptionsObject = userMap.get(SUBSCRIPTIONS);
		if (subscriptionsObject != null && subscriptionsObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscriptionsObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					/*
					 * if (friendIds.size() > MAX_FRIENDS_PER_USER) {
					 * logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserMap.get(ID)); return
					 * friendIds; }
					 */
				}
			}
		}

		Object subscribersObject = userMap.get(SUBSCRIBERS);
		if (subscribersObject != null && subscribersObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscribersObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					/*
					 * if (friendIds.size() > MAX_FRIENDS_PER_USER) {
					 * logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserMap.get(ID)); return
					 * friendIds; }
					 */
				}
			}
		}

		return friendIds;
	}

	@Override
	protected boolean isRateError(CommunicationResponse response) {
		if (response.getStatus() == 403) {
			return true;
		}
		return false;
	}
}
