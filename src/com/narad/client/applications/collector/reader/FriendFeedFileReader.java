package com.narad.client.applications.collector.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FriendFeedFileReader extends FileProfileReader {
	private static final Logger logger = LoggerFactory.getLogger(FriendFeedFileReader.class);
	private static final Object ID = "id";
	private static final Object SUBSCRIBERS = "subscribers";
	private static final Object SUBSCRIPTIONS = "subscriptions";

	public FriendFeedFileReader(String baseFilePath) {
		super(logger, baseFilePath, "friendfeed");
	}
	
	public List<Object> fetchFriends(Object userId, Map<String, Object> userMap) {
		userMap = super.fetchUser(userId);
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
				}
			}
		}

		return friendIds;
	}

}
