package com.narad.client.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.NaradJsonClient;
import com.narad.command.CommandResult.CommandResultType;
import com.narad.command.NaradCommandConstants;

public class TwitterGraphBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(TwitterGraphBuilder.class);

	private static final int MAX_FRIEND_QUEUE_SIZE = 5000;//Max total friends to store
	private static final int MAX_FRIENDS_PER_USER = 1000;//Max friends per user to store
	private static final int RATE_LIMIT_SLEEP_TIME = 10 * 60 * 1000;//sleep if rate limit error encountered
	private static final int SEARCH_SLEEP_TIME = 500;//Sleep after every search


	private static final String TWITTER_URL = "http://api.twitter.com/1/";
	private static final String SCREEN_NAME = "screen_name";
	private static final String ID = "id";

	public static final int BUFFER_SIZE = 1024;

	private JAXRSClientFactoryBean sf;
	private Queue<TwitterFriend> twitterIds;
	private Set<Object> allTwitterIds;// to ensure we don't query twice for any user
	private NaradJsonClient naradJsonClient;

	public TwitterGraphBuilder() {
		super();
		sf = getFactoryBean(TWITTER_URL);
		twitterIds = new LinkedList<TwitterFriend>();
		naradJsonClient = new NaradJsonClient();
		allTwitterIds = new HashSet<Object>();
		String name = logger.getName();
		System.out.println(name);
	}

	private Map<String, Object> getUser(Object id) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("invalid id type for user: {}", id);
			return Collections.EMPTY_MAP;
		}
		Map<String, Object> callReturn = executeCall("users/show.json", map, null);
		if (callReturn == null) {
			logger.info("No return from call for user. Id: {}", id);
			return Collections.EMPTY_MAP;
		}
		return callReturn;
	}

	private List<Long> getFriendIds(Object id) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (id instanceof Long) {
			map.put(ID, id);
		} else if (id instanceof String) {
			map.put(SCREEN_NAME, id);
		} else {
			logger.info("Invalid id type. Id: {}", id);
			return Collections.EMPTY_LIST;
		}
		Map<String, Object> callReturn = executeCall("friends/ids.json", map, null);
		if (callReturn == null) {
			logger.info("No return from call. Id: {}");
			return Collections.EMPTY_LIST;
		}
		List<Long> idList = (List<Long>) callReturn.get("ids");
		return idList;
	}

	private Map<String, Object> executeCall(String path, Map<String, Object> queryParameters, String responseFilePath) {
		WebClient wc = sf.createWebClient();
		wc.path(path);
		for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
			wc.query(entry.getKey(), entry.getValue());
		}
		wc.accept(MediaType.APPLICATION_JSON);

		Response response = wc.get();
		Object entity = response.getEntity();
		int status = response.getStatus();
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
			} else {
				logger.info("Error executing call: {} return: {}", new Object[] { path, responseStr });
				if (error instanceof String && ((String) error).startsWith("Rate limit exceeded")) {
					try {
						logger.info("Rate limit error, retrying after 10 minutes. Current Date: {}", new Date());
						Thread.sleep(RATE_LIMIT_SLEEP_TIME);
						return executeCall(path, queryParameters, responseFilePath);
					} catch (InterruptedException e) {
						logger.error("Error while sleeping to wait for rate limit. ", e);
					}
				}
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
	private String addUser(Object id) throws IOException {
		Map<String, Object> userProps = getUser(id);
		String emailId = null;
		if (!userProps.isEmpty()) {
			String screenName = (String) userProps.get(SCREEN_NAME);
			if (screenName != null) {
				emailId = screenName + "@twitter.com";
				// Add node
				userProps.put("network", "twitter");
				ArrayList<Object> profiles = new ArrayList<Object>();
				profiles.add(userProps);
				Map<String, Object> nodeMap = new HashMap<String, Object>();
				Object name = userProps.get("name");
				if (name != null) {
					nodeMap.put("name", name);
				}
				nodeMap.put(NaradCommandConstants.COMMAND_PROFILES, profiles);
				JSONObject addNode = naradJsonClient.addNode(emailId, nodeMap);
				Map result = (Map) addNode.get("result");
				if (!(CommandResultType.SUCCESS.toString().equals(result.get("status")))) {
					emailId = null;
					logger.info(addNode.toJSONString());
				}
			}
		}
		return emailId;
	}

	public void runSearch(int maxDepth) throws IOException {
		long startTime = System.currentTimeMillis();
		int totalNodes = 0;
		logger.info("Start time {}", startTime);
		TwitterFriend poll = twitterIds.poll();
		while (poll != null) {
			try {
				allTwitterIds.add(poll.getId());
				if (allTwitterIds.size() % 1000 == 0) {
					logger.info("Number of twitter ids: " + allTwitterIds.size());
				}
				if (poll.getDepth() >= maxDepth) {
					logger.info("Stopping search as max depth reached: {} account: {}", maxDepth, poll);
					return;
				}

				String addedUserEmail = addUser(poll.getId());
				if (addedUserEmail == null) {
					// Could not add user. Not adding friends
					logger.info("Could not add user for id: {}", poll);
					continue;
				}
				logger.info("Adding user: {} poll: {} index: {}", new Object[] { addedUserEmail, poll, totalNodes });
				totalNodes++;

				// add relation
				String sourceEmail = poll.getSourceEmail();
				if (sourceEmail != null) {
					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put("network", "twitter");
					naradJsonClient.addRelationship(sourceEmail, addedUserEmail, properties);
				}

				if (twitterIds.size() < MAX_FRIEND_QUEUE_SIZE) {
					List<Long> friendIds = getFriendIds(poll.getId());
					if (friendIds == null) {
						continue;
					}
					int newFriends = 0;
					for (Long friendId : friendIds) {
						TwitterFriend twitterFriend = new TwitterFriend(addedUserEmail, friendId, poll.getDepth() + 1);
						if (!allTwitterIds.contains(friendId)) {
							twitterIds.add(twitterFriend);
							newFriends++;
						}
					}
					logger.info("Adding new friends: ", newFriends);
				} else {
					logger.info("More than :{} friends in queue. not fetching more.", MAX_FRIEND_QUEUE_SIZE);
				}
			} catch (ClientWebApplicationException e) {
				naradJsonClient = new NaradJsonClient();
			} catch (Throwable t) {
				logger.error("Unexpected error while executing loop: ", t);
			}
			poll = twitterIds.poll();
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

	public void addStartPoint(Object id) {
		twitterIds.add(new TwitterFriend(null, id, 0));
		allTwitterIds.add(id);
	}

	public static void main(String[] args) throws IOException {
		TwitterGraphBuilder twitterClient = new TwitterGraphBuilder();
		twitterClient.addStartPoint("aditya_instacol");
		twitterClient.runSearch(100);
	}

	private class TwitterFriend {
		private Object id;
		private int depth;
		private String sourceEmail;

		public TwitterFriend(String sourceEmail, Object id, int depth) {
			super();
			this.sourceEmail = sourceEmail;
			this.id = id;
			this.depth = depth;
		}

		public Object getId() {
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
