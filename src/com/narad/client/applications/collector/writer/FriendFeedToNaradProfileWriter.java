package com.narad.client.applications.collector.writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.command.NaradCommandConstants;

public class FriendFeedToNaradProfileWriter extends NaradProfileWriter {

	private static final Logger logger = LoggerFactory.getLogger(FriendFeedToNaradProfileWriter.class);
	
	private static final String NAME = "name";
	private static final String SUBSCRIBERS = "subscribers";
	private static final String SUBSCRIPTIONS = "subscriptions";
	
	public FriendFeedToNaradProfileWriter(String naradUrl) {
		super(logger, naradUrl);
	}

	@Override
	public String getName() {
		return "friendfeed";
	}

	@Override
	public Map<String, Object> saveUser(Object id, Map<String, Object> userMap) {
		Map<String, Object> userProps = userMap;
		if (!userProps.isEmpty()) {
			// Add node
			userProps.put(NETWORK, getName());
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
			Map<String, Object> adduser = adduser(id, nodeMap);
			return adduser;
		}
		return null;
	}

	@Override
	public Map<String, Object> saveFriends(Object userId, List<Object> friends) {
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put(NETWORK, getName());
		for (Object friendId : friends) {
			JSONObject addRelationship = addRelationship(userId, friendId, properties);
			if (!isSuccess(addRelationship)) {
				logger.info("Could not add relationship between: {} and {}. Error: {}", new Object[] { userId,
						friendId, getDescription(addRelationship)});
			} else {
				logger.info("Added relationship between: {} and {}", new Object[] { userId, friendId });
			}
		}
		return null;
	}

}
