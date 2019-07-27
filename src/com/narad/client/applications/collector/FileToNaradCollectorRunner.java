package com.narad.client.applications.collector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileQueue;
import com.narad.client.applications.collector.api.ProfileReader;
import com.narad.client.applications.collector.api.ProfileWriter;
import com.narad.client.applications.collector.queue.BasicProfileQueue;
import com.narad.client.applications.collector.queue.FileListProfileQueue;
import com.narad.client.applications.collector.reader.FileProfileReader;
import com.narad.client.applications.collector.reader.FriendFeedFileReader;
import com.narad.client.applications.collector.writer.FriendFeedToNaradProfileWriter;
import com.narad.client.applications.collector.writer.TwitterToNaradProfileWriter;

public class FileToNaradCollectorRunner {
	private static final Logger logger = LoggerFactory.getLogger(FileToNaradCollectorRunner.class);
	private String network;
	private String baseFilePath;
	private ProfileReader reader;
	private ProfileWriter writer;
	private ProfileQueue profileQueue;

	public static void main(String[] args) throws Exception {
		FileToNaradCollectorRunner friendFeedRunner = new FileToNaradCollectorRunner(
				"D:\\development\\narad\\response_dump", "friendfeed", "D:\\development\\narad\\response_dump");
		friendFeedRunner.runCollectorAsync();
	}

	public FileToNaradCollectorRunner(String baseFilePath, String network, String filePath) throws Exception {
		super();
		this.network = network;
		this.baseFilePath = baseFilePath;
		String naradUrl = "http://localhost:8180/services/v1/";
		if (network.equals("twitter")) {
			this.reader = new FileProfileReader(null, baseFilePath, network);
			this.writer = new TwitterToNaradProfileWriter(naradUrl);
		} else if (network.equals("friendfeed")) {
			this.reader = new FriendFeedFileReader(baseFilePath);
			this.writer = new FriendFeedToNaradProfileWriter(naradUrl);
		} else {
			throw new Exception("bad configuration");
		}
		if (filePath == null) {
			ArrayList<Object> arrayList = new ArrayList<Object>();
			arrayList.add("1michaelwilson");
			//arrayList.add("1marc");
			//arrayList.add("bret");
			profileQueue = new BasicProfileQueue(arrayList);
		} else {
			profileQueue = new FileListProfileQueue(filePath + File.separator + network);
		}
	}

	public void runCollectorAsync() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				runCollector();
			}
		};
		new Thread(runnable).start();
	}

	public void runCollector() {
		Object id = null;
		while ((id = getNextFriend()) != null) {
			try {
				boolean userExists = writer.userExists(id);
				boolean friendExists = writer.friendsExist(id);
				if (userExists && friendExists) {
					logger.info("Nothing to do on network: {} for userId: {}", network, id);
					continue;
				}
				Map<String, Object> fetchUser = null;
				if (!userExists) {
					logger.info("Fetching user on network: {}, userId: {} ", network, id);
					fetchUser = reader.fetchUser(id);
					if (fetchUser != null) {
						Object errorObject = fetchUser.get("error");
						if (errorObject != null) {
							logger.info("Error while fetching user: {}", errorObject);
						} else {
							writer.saveUser(id, fetchUser);
						}
					}
				}
				if (!friendExists && (userExists || fetchUser != null)) {
					logger.info("Fetching friends on network: {}, userId: {} ", network, id);
					List<Object> fetchFriends = reader.fetchFriends(id, fetchUser);
					if (fetchFriends != null) {
						writer.saveFriends(id, fetchFriends);
						addFriends(fetchFriends);
					}
				}
				Thread.sleep(100);
			} catch (Exception e) {
				logger.error("Error while running loop for id: " + id + " in network: " + network, e);
			}
		}
		logger.info("No more friends to process, exiting");
	}

	private Object getNextFriend() {
		return profileQueue.getNextId();
	}
	
	private void addFriends(List<Object> friendIds) {
		profileQueue.addIds(friendIds);
	}
}
