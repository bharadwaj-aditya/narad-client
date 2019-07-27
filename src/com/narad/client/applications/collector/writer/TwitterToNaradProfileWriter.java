package com.narad.client.applications.collector.writer;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitterToNaradProfileWriter extends NaradProfileWriter {

	private static final Logger logger = LoggerFactory.getLogger(TwitterToNaradProfileWriter.class);

	public TwitterToNaradProfileWriter(String naradUrl) {
		super(logger, naradUrl);
	}

	@Override
	public String getName() {
		return "friendfeed";
	}

	@Override
	public Map<String, Object> saveUser(Object id, Map<String, Object> userMap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> saveFriends(Object userId, List<Object> friends) {
		throw new UnsupportedOperationException();
	}

}
