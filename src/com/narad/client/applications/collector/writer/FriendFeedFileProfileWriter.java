package com.narad.client.applications.collector.writer;



public class FriendFeedFileProfileWriter extends FileProfileWriter {
	public FriendFeedFileProfileWriter(String baseFolderPath) {
		super("FriendFeed", baseFolderPath);
	}
	
	protected String getFriendsFilePath(Object userId) {
		return getUserFilePath(userId);
	}
	
	public boolean friendsExist(Object userId) {
		return userExists(userId);
	}

}
