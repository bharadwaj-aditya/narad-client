package com.narad.client.applications.collector.writer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileWriter;
import com.narad.client.util.FileUtil;

public class FileProfileWriter implements ProfileWriter {
	private final Logger logger;
	private String name;
	private String baseFolderPath;

	public FileProfileWriter(String name, String baseFolderPath) {
		super();
		if (name != null && !name.isEmpty()) {
			this.name = name;
		} else {
			name = "BasicProfileWriter";
		}
		this.baseFolderPath = baseFolderPath;
		FileUtil.createFile(baseFolderPath, true, true);
		logger = LoggerFactory.getLogger(FileProfileWriter.class.getName() + "name");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean savesRaw() {
		return true;
	}

	@Override
	public boolean userExists(Object userId) {
		File file = new File(getUserFilePath(userId));
		if (file.exists()) {
			return true;
		}
		return false;
	}

	protected String getUserFilePath(Object userId) {
		return baseFolderPath + File.separator + name + File.separator + userId + ".json";
	}

	protected String getFriendsFilePath(Object userId) {
		return baseFolderPath + File.separator + name + File.separator + userId + "_friends.json";
	}

	@Override
	public boolean friendsExist(Object userId) {
		File file = new File(getFriendsFilePath(userId));
		if (file.exists()) {
			return true;
		}
		return false;
	}

	@Override
	public Map<String, Object> saveUserRaw(Object userId, String userResponse) {
		if (userResponse == null) {
			return null;
		}
		writeToNewFile(getUserFilePath(userId), userResponse);
		return Collections.EMPTY_MAP;
	}

	@Override
	public Map<String, Object> saveFriendsRaw(Object userId, String friendsResponse) {
		if (friendsResponse == null) {
			return null;
		}
		writeToNewFile(getFriendsFilePath(userId), friendsResponse);
		return Collections.EMPTY_MAP;
	}

	private boolean writeToNewFile(String filePath, String data) {
		File createNewFile = FileUtil.createNewFile(filePath, false);
		return FileUtil.writeToFile(createNewFile, data);
	}

	@Override
	public Map<String, Object> saveUser(Object userId, Map<String, Object> userMap) {
		if (userMap == null || userMap.isEmpty()) {
			return null;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.putAll(userMap);
		String userJsonStr = jsonObject.toJSONString();
		return saveUserRaw(userId, userJsonStr);
	}

	@Override
	public Map<String, Object> saveFriends(Object userId, List<Object> friends) {
		if (friends == null || friends.isEmpty()) {
			return null;
		}
		JSONArray jsonArr = new JSONArray();
		jsonArr.addAll(friends);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("ids", jsonArr);
		String friendsJsonStr = jsonObject.toJSONString();
		return saveFriendsRaw(userId, friendsJsonStr);
	}

}
