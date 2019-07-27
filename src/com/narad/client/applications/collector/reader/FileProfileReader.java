package com.narad.client.applications.collector.reader;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileReader;
import com.narad.client.util.CommunicationResponse;
import com.narad.client.util.FileUtil;

public class FileProfileReader implements ProfileReader {

	public static final int RATE_LIMIT_SLEEP_TIME = 10 * 60 * 1000;
	private Logger logger;
	private String name;
	private String baseFilePath;
	protected String lastResponse;

	public FileProfileReader(Logger logger, String baseFilePath, String name) {
		super();
		if (logger != null) {
			this.logger = logger;
		} else {
			this.logger = LoggerFactory.getLogger(FileProfileReader.class.getName() + "." + name);
		}
		this.baseFilePath = baseFilePath;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	protected CommunicationResponse readFile(String filePath) {
		File file = new File(filePath);
		CommunicationResponse response = null;
		if (file.exists()) {
			String readFromFile = FileUtil.readFromFile(filePath);
			if (readFromFile != null) {
				response = new CommunicationResponse(-1, readFromFile);
			} else {
				response = new CommunicationResponse(-1, null);
				response.setError("Error while reading file");
			}
		} else {
			logger.info("Could not read from file as file does not exist: {}", filePath);
		}

		lastResponse = response == null ? null : response.getResponseStr();
		return response;
	}

	public Map<String, Object> fetchUser(Object id) {
		String userFilePath = getUserFilePath(id);
		CommunicationResponse readFile = readFile(userFilePath);
		if (readFile != null) {
			JSONObject parsedJson = readFile.getParsedJson();
			return parsedJson;
		}
		return null;
	}

	private String getUserFilePath(Object id) {
		String userFilePath = baseFilePath + File.separator + name + File.separator + id + ".json";
		return userFilePath;
	}

	public List<Object> fetchFriends(Object userId, Map<String, Object> userMap) {
		String friendsFilePath = getFriendsFilePath(userId);
		CommunicationResponse readFile = readFile(friendsFilePath);
		if (readFile != null) {
			JSONObject parsedJson = readFile.getParsedJson();
			JSONArray jsonArray = (JSONArray) parsedJson.get("ids");
			return jsonArray;
		}
		return null;
	}

	private String getFriendsFilePath(Object userId) {
		String friendsFilePath = baseFilePath + File.separator + name + File.separator + userId + "_friends.json";
		return friendsFilePath;
	}

	public String getLastResponse() {
		return lastResponse;
	}

	@Override
	public boolean userExists(Object userId) {
		String userFilePath = getUserFilePath(userId);
		File file = new File(userFilePath);
		if (file.exists()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean friendsExist(Object userId) {
		String friendsFilePath = getFriendsFilePath(userId);
		File file = new File(friendsFilePath);
		if (file.exists()) {
			return true;
		}
		return false;
	}
}
