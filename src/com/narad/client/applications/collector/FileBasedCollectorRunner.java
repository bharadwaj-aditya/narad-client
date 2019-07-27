package com.narad.client.applications.collector;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileQueue;
import com.narad.client.applications.collector.api.ProfileReader;
import com.narad.client.applications.collector.api.ProfileWriter;
import com.narad.client.applications.collector.queue.FileListProfileQueue;
import com.narad.client.applications.collector.reader.FileProfileReader;
import com.narad.client.applications.collector.reader.FriendFeedFileReader;
import com.narad.client.applications.collector.reader.FriendFeedReader;
import com.narad.client.applications.collector.reader.TwitterReader;
import com.narad.client.applications.collector.writer.FileProfileWriter;

public class FileBasedCollectorRunner {
	private static final Logger logger = LoggerFactory.getLogger(FileBasedCollectorRunner.class);
	private String network;
	private String baseFilePath;
	private ProfileReader externalReader;
	private ProfileReader fileReader;
	private ProfileWriter fileWriter;
	private ProfileQueue profileQueue;

	public static void main(String[] args) throws Exception {
		FileBasedCollectorRunner twitterRunner = new FileBasedCollectorRunner("192.168.0.99", 9945,
				"D:\\development\\narad\\response_dump", "twitter");
		twitterRunner.runCollectorAsync();
		FileBasedCollectorRunner friendFeedRunner = new FileBasedCollectorRunner("192.168.0.99", 9945,
				"D:\\development\\narad\\response_dump", "friendfeed");
		friendFeedRunner.runCollectorAsync();
	}

	public FileBasedCollectorRunner(String proxyHost, int proxyPort, String baseFilePath, String network) throws Exception {
		super();
		this.network = network;
		this.baseFilePath = baseFilePath;
		if (network.equals("twitter")) {
			this.externalReader = new TwitterReader(proxyHost, proxyPort);
			this.fileReader = new FileProfileReader(null, baseFilePath, network);
			this.fileWriter = new FileProfileWriter(network, baseFilePath);
		} else if (network.equals("friendfeed")) {
			this.externalReader = new FriendFeedReader(proxyHost, proxyPort);
			this.fileReader = new FriendFeedFileReader(baseFilePath);
			this.fileWriter = new FileProfileWriter(network, baseFilePath);
		} else {
			throw new Exception("bad configuration");
		}
		profileQueue = new FileListProfileQueue(baseFilePath + File.separator + network);
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
				boolean userExists = fileReader.userExists(id);
				boolean friendExists = fileReader.friendsExist(id);
				if (userExists && friendExists) {
					logger.info("Nothing to do on network: {} for userId: {}", network, id);
					continue;
				}
				Map<String, Object> fetchUser = null;
				if (!userExists) {
					logger.info("Fetching user on network: {}, userId: {} ", network, id);
					fetchUser = externalReader.fetchUser(id);
					if (fetchUser != null) {
						Object errorObject = fetchUser.get("error");
						if (errorObject != null) {
							logger.info("Error while fetching user: {}", errorObject);
						} else {
							fileWriter.saveUser(id, fetchUser);
						}
					}
				}
				if (!friendExists && (userExists || fetchUser != null)) {
					logger.info("Fetching friends on network: {}, userId: {} ", network, id);
					List<Object> fetchFriends = externalReader.fetchFriends(id, fetchUser);
					if (fetchFriends != null) {
						fileWriter.saveFriends(id, fetchFriends);
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
