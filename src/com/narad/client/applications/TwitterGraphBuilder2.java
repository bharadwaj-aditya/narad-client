package com.narad.client.applications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.command.NaradCommandConstants;

public class TwitterGraphBuilder2 extends RestNetworksGraphBuilder {

	private static final Logger logger = LoggerFactory.getLogger(TwitterGraphBuilder2.class);

	private static final String SCREEN_NAME = "screen_name";

	public TwitterGraphBuilder2() {
		super(logger);
	}

	@Override
	public String getUrl() {
		return "http://api.twitter.com/1/";
	}

	@Override
	public String getNetwork() {
		return "twitter";
	}

	@Override
	protected boolean isErrorResponse(int status, Map<String, Object> parsedResponse) {
		Object error = parsedResponse.get("error");
		if (error instanceof String && ((String) error).startsWith("Rate limit exceeded")) {
			return true;
		}
		return false;
	}

	@Override
	protected Map<String, Object> getUserById(Object id) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("invalid id type for user: {}", id);
			return Collections.EMPTY_MAP;
		}
		Map<String, Object> callReturn = executeJsonCall("users/show.json", map, id.toString() + ".json");
		if (callReturn == null) {
			logger.info("No return from call for user. Id: {}", id);
			return Collections.EMPTY_MAP;
		}
		return callReturn;
	}

	@Override
	protected List<Object> getFriendIds(Map<String, Object> addedUserMap) {
		Object id = addedUserMap.get(ID);
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("Invalid id type. Id: {}", id);
			return Collections.EMPTY_LIST;
		}
		Map<String, Object> callReturn = executeJsonCall("friends/ids.json", map, id + "_friends.json");
		if (callReturn == null) {
			logger.info("No return from call. Id: {}");
			return Collections.EMPTY_LIST;
		}
		return (List) callReturn.get("ids");
	}

	@Override
	protected Map<String, Object> addUser(Object id) throws IOException {
		boolean success = false;
		Map<String, Object> userProps = getUserById(id);
		String emailId = null;
		if (!userProps.isEmpty()) {
			String screenName = (String) userProps.get(SCREEN_NAME);
			if (screenName != null) {
				emailId = getEmail(screenName);
				// Add node
				userProps.put(NETWORK, getNetwork());
				ArrayList<Object> profiles = new ArrayList<Object>();
				profiles.add(userProps);
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
}
