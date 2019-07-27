package com.narad.client.applications.collector.reader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.util.CommunicationResponse;

public class TwitterReader extends NetworkReader {

	private static final Logger logger = LoggerFactory.getLogger(TwitterReader.class);
	private static final String ID = "id";
	private static final String SCREEN_NAME = "screen_name";

	public TwitterReader(String proxyHost, int proxyPort) {
		super(logger, proxyHost, proxyPort);
	}

	@Override
	public String getNetworkName() {
		return "twitter";
	}

	@Override
	public String getBaseUrl() {
		return "http://api.twitter.com/1/";
	}

	@Override
	public String getName() {
		return "twitter";
	}
	
	@Override
	public Map<String, Object> fetchUser(Object id) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("invalid id type for user: {}", id);
			return Collections.EMPTY_MAP;
		}
		CommunicationResponse response = makeCall("users/show.json", map);
		if (isErrorResponse(response)) {
			response.setError("Could not fetch " + getNetworkName() + " users");
		} else if (response.getParsedJson() == null) {
			logger.info("Twitter profile is null for user: {}", id);
		}
		return response;
	}

	@Override
	public List<Object> fetchFriends(Object userId, Map<String, Object> userMap) {
		Object id = userId;
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("Invalid id type. Id: {}", id);
			return Collections.EMPTY_LIST;
		}
		CommunicationResponse response = makeCall("friends/ids.json", map);
		//lastResponse = response.getResponseStr();
		return (List) response.getParsedJson().get("ids");
	}
	
	@Override
	protected boolean isRateError(CommunicationResponse response) {
		JSONObject parsedJson = response.getParsedJson();
		if (parsedJson == null) {
			return true;
		}
		Object error = parsedJson.get("error");
		if (error instanceof String && ((String) error).startsWith("Rate limit exceeded")) {
			return true;
		}
		return false;
	}
}
