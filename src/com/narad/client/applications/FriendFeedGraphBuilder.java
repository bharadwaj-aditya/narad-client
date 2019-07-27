package com.narad.client.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.NaradJsonClient;
import com.narad.command.CommandResult.CommandResultType;
import com.narad.command.NaradCommandConstants;

public class FriendFeedGraphBuilder {

	private static final Logger logger = LoggerFactory.getLogger(FriendFeedGraphBuilder.class);

	private static final int MAX_FRIEND_QUEUE_SIZE = 5000;// Max total friends to store
	private static final int MAX_FRIENDS_PER_USER = 1000;// Max friends per user to store
	private static final int RATE_LIMIT_SLEEP_TIME = 10 * 60 * 1000;// sleep if rate limit error encountered
	private static final int SEARCH_SLEEP_TIME = 500;// Sleep after every search

	private static final String NETWORK_NAME = "friendfeed";
	private static final String URL = "http://friendfeed-api.com/v2/";
	private static final String MAIL_DOMAIN = "@" + NETWORK_NAME + ".com";

	private static final String ID = "id";
	private static final String SUBSCRIBERS = "subscribers";
	private static final String SUBSCRIPTIONS = "subscriptions";

	public static final int BUFFER_SIZE = 1024;

	private JAXRSClientFactoryBean sf;
	private Queue<Friend> friendFeedIds;
	private Set<Object> allFriendFeedIds;// to ensure we don't query twice for any user
	private NaradJsonClient naradJsonClient;

	public FriendFeedGraphBuilder() {
		super();
		sf = getFactoryBean(URL);
		friendFeedIds = new LinkedList<Friend>();
		naradJsonClient = new NaradJsonClient();
		allFriendFeedIds = new HashSet<Object>();
		String name = logger.getName();
		System.out.println(name);
	}

	public Map<String, Object> getUserById(String id) throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		// map.put("id", id);
		Map<String, Object> callReturn = executeCall("feedinfo/" + id, map);
		return callReturn;
	}

	private Map<String, Object> executeCall(String path, Map<String, Object> queryParameters) {
		WebClient wc = sf.createWebClient();
		wc.path(path);
		for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
			wc.query(entry.getKey(), entry.getValue());
		}
		wc.accept(MediaType.APPLICATION_JSON);

		Response response = wc.get();
		Object entity = response.getEntity();
		int status = response.getStatus();

		// logger.info("Error executing call: {} return: {}", new Object[] { path, responseStr });
		if (status == 403) {
			try {
				logger.info("Rate limit error, retrying after 10 minutes. Current Date: {}", new Date());
				Thread.sleep(RATE_LIMIT_SLEEP_TIME);
				return executeCall(path, queryParameters);
			} catch (InterruptedException e) {
				logger.error("Error while sleeping to wait for rate limit. ", e);
				return null;
			}
		}

		MultivaluedMap<String, Object> metadata = response.getMetadata();

		StringBuilder sbr = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) entity));
		String s;
		try {
			s = br.readLine();
			while (s != null) {
				sbr.append(s.trim());
				s = br.readLine();
			}
		} catch (IOException e1) {
			logger.error("Error while reading response for path: {}", path);
			return null;
		}
		String responseStr = sbr.toString();

		try {
			Map<String, Object> parse = (Map<String, Object>) new JSONParser().parse(responseStr);
			Object error = parse.get("error");
			if (error == null) {
				logger.debug("Json response for path: {} Response: {}", new Object[] { path, responseStr });
				return parse;
			}
		} catch (ParseException e) {
			logger.error("Error while parsing json. Exception: {}, Json: {} ", e.getMessage(), responseStr);
		}

		return null;
	}

	private JAXRSClientFactoryBean getFactoryBean(String url) {
		JAXRSClientFactoryBean sf = new JAXRSClientFactoryBean();
		sf.setAddress(url);
		BindingFactoryManager manager = sf.getBus().getExtension(BindingFactoryManager.class);
		JAXRSBindingFactory factory = new JAXRSBindingFactory();
		factory.setBus(sf.getBus());
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, factory);
		return sf;
	}

	// Make rest call to add friend
	private Map<String, Object> addUser(String id) throws IOException {
		boolean success = false;
		Map<String, Object> userProps = getUserById(id);
		String emailId = null;
		if (!userProps.isEmpty()) {
			String screenName = (String) userProps.get(ID);
			if (screenName != null) {
				emailId = screenName + MAIL_DOMAIN;
				// Add node
				userProps.put("network", NETWORK_NAME);
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
				Object name = userProps.get("name");
				if (name != null) {
					nodeMap.put("name", name);
				}
				nodeMap.put(NaradCommandConstants.COMMAND_PROFILES, profiles);
				JSONObject addNode = naradJsonClient.addNode(emailId, nodeMap);
				Map result = (Map) addNode.get("result");
				if (!(CommandResultType.SUCCESS.toString().equals(result.get("status")))) {
					logger.info(addNode.toJSONString());
				} else {
					success = true;
				}
			}
		}
		if (!success) {
			userProps = null;
		}
		return userProps;
	}

	public void runSearch(int maxDepth) throws IOException {
		long startTime = System.currentTimeMillis();
		int totalNodes = 0;
		logger.info("Start time {}", startTime);
		Friend poll = friendFeedIds.poll();
		while (poll != null) {
			try {
				allFriendFeedIds.add(poll.getId());
				if (allFriendFeedIds.size() % 1000 == 0) {
					logger.info("Number of friend feed ids: " + allFriendFeedIds.size());
				}
				if (friendFeedIds.size() % 1000 == 0) {
					logger.info("Number of friend feed ids in queue: " + friendFeedIds.size());
				}
				if (poll.getDepth() >= maxDepth) {
					logger.info("Stopping search as max depth reached: {} account: {}", maxDepth, poll);
					return;
				}

				Map<String, Object> addedUserObj = null;
				try {
					addedUserObj = addUser(poll.getId());
				} catch (Throwable t) {
					logger.error("Unexpected error while adding user: ", t);
				}
				if (addedUserObj == null || addedUserObj.isEmpty()) {
					// Could not add user. Not adding friends
					logger.info("Could not add user for id: {}", poll);
				} else {
					String addedUserEmail = addedUserObj.get(ID) + MAIL_DOMAIN;
					logger.info("Adding user: {} poll: {} index: {}", new Object[] { addedUserEmail, poll, totalNodes });
					totalNodes++;

					// add relation
					String sourceEmail = poll.getSourceEmail();
					if (sourceEmail != null) {
						Map<String, Object> properties = new HashMap<String, Object>();
						properties.put("network", NETWORK_NAME);
						naradJsonClient.addRelationship(sourceEmail, addedUserEmail, properties);
					}

					if (friendFeedIds.size() < MAX_FRIEND_QUEUE_SIZE) {
						List<String> friendIds = getFriendIds(addedUserObj);
						if (friendIds == null || friendIds.isEmpty()) {
							continue;
						}
						int newFriends = 0;
						for (String friendId : friendIds) {
							Friend friend = new Friend(addedUserEmail, friendId, poll.getDepth() + 1);
							if (!allFriendFeedIds.contains(friendId)) {
								friendFeedIds.add(friend);
								newFriends++;
							}
						}
						logger.info("Adding new friends: ", newFriends);
					} else {
						logger.info("More than :{} friends in queue. not fetching more.", MAX_FRIEND_QUEUE_SIZE);
					}
				}
			} catch (ClientWebApplicationException e) {
				naradJsonClient = new NaradJsonClient();
			} catch (Throwable t) {
				logger.info("Unexpected error while running loop: ", t);
			}
			poll = friendFeedIds.poll();
			try {
				Thread.sleep(SEARCH_SLEEP_TIME);
			} catch (InterruptedException e) {
				logger.error("Unexpected error while executing loop: ", e);
			}
		}
		long endTime = System.currentTimeMillis();
		logger.info("End time {}, total time: {}, totalNodes: {}", new Object[] { endTime, (endTime - startTime),
				totalNodes });
	}

	private List<String> getFriendIds(Map<String, Object> addedUserObj) {
		List<String> friendIds = new ArrayList<String>();
		Object subscriptionsObject = addedUserObj.get(SUBSCRIPTIONS);
		if (subscriptionsObject != null && subscriptionsObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscriptionsObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					if (friendIds.size() > MAX_FRIENDS_PER_USER) {
						logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserObj.get(ID));
						return friendIds;
					}
				}
			}
		}

		Object subscribersObject = addedUserObj.get(SUBSCRIBERS);
		if (subscribersObject != null && subscribersObject instanceof JSONArray) {
			JSONArray array = (JSONArray) subscribersObject;
			for (int i = 0; i < array.size(); i++) {
				JSONObject innerObj = (JSONObject) array.get(i);
				Object id = innerObj.get(ID);
				if (id instanceof String && !friendIds.contains(id)) {
					friendIds.add((String) id);
					if (friendIds.size() > MAX_FRIENDS_PER_USER) {
						logger.info("More than 1000 friends for user. Stopping at 1000: " + addedUserObj.get(ID));
						return friendIds;
					}
				}
			}
		}

		return friendIds;
	}

	public void addStartPoint(String id) {
		friendFeedIds.add(new Friend(null, id, 0));
		allFriendFeedIds.add(id);
	}

	public static void main(String[] args) throws IOException {
		FriendFeedGraphBuilder friendFeedClient = new FriendFeedGraphBuilder();
		friendFeedClient.addStartPoint("bret");
		friendFeedClient.runSearch(100);
	}

	private class Friend {
		private String id;
		private int depth;
		private String sourceEmail;

		public Friend(String sourceEmail, String id, int depth) {
			super();
			this.sourceEmail = sourceEmail;
			this.id = id;
			this.depth = depth;
		}

		public String getId() {
			return id;
		}

		public int getDepth() {
			return depth;
		}

		public String getSourceEmail() {
			return sourceEmail;
		}

		public String toString() {
			return "source: " + sourceEmail + " id: " + id + " depth: " + depth;
		}
	}
}
