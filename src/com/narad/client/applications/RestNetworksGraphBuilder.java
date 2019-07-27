package com.narad.client.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import com.narad.client.NaradJsonClient;
import com.narad.client.util.FileUtil;
import com.narad.command.CommandResult.CommandResultType;

public abstract class RestNetworksGraphBuilder {

	private final Logger logger;

	private static final String HTTP_PROXY_PORT = "http.proxyPort";
	private static final String HTTP_PROXY_HOST = "http.proxyHost";
	private static final String BASE_RESPONSE_PATH = "baseResponsePath";

	public static final int BUFFER_SIZE = 1024;
	public static final int MAX_FRIEND_QUEUE_SIZE = 5000;// Max total friends to store
	public static final int MAX_FRIENDS_PER_USER = 1000;// Max friends per user to store
	public static final int RATE_LIMIT_SLEEP_TIME = 10 * 60 * 1000;// sleep if rate limit error encountered
	public static final int SEARCH_SLEEP_TIME = 500;// Sleep after every search
	private static final String DEFAULT_PROXY_HOST = "192.168.0.99";
	private static final int DEFAULT_PROXY_PORT = 9945;

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String NETWORK = "network";

	private JAXRSClientFactoryBean sf;
	private Queue<Friend> queueFriendIds;
	// private Set<Object> allFriendIds;// to ensure we don't query twice for any user, all completed users stored here
	private int totalFriends;
	private NaradJsonClient naradJsonClient;
	private String baseFilePath;
	private String friendsListFilePath;

	public RestNetworksGraphBuilder(Logger logger) {
		super();
		// if (System.getProperty(HTTP_PROXY_HOST) == null) {
		// System.setProperty(HTTP_PROXY_HOST, "192.168.0.99");
		// System.setProperty(HTTP_PROXY_PORT, "9945");
		// }
		this.logger = logger;
		sf = getFactoryBean(getUrl());
		queueFriendIds = new LinkedList<Friend>();
		naradJsonClient = new NaradJsonClient();
		// allFriendIds = new HashSet<Object>();
		String basePath = "D:\\development\\narad\\response_dump";
		if (System.getProperty(BASE_RESPONSE_PATH) != null) {
			basePath = System.getProperty(BASE_RESPONSE_PATH);
		}
		baseFilePath = basePath + File.separator + getNetwork();
		friendsListFilePath = basePath + File.separator + "friends_" + getNetwork();

		String name = logger.getName();
		System.out.println(name);
		FileUtil.createFile(baseFilePath, true, true);
		FileUtil.createFile(friendsListFilePath, false, false);
	}

	public String getEmail(Object id) {
		return id + "@" + getNetwork() + ".com";
	}

	protected abstract String getUrl();

	protected abstract String getNetwork();

	protected abstract Map<String, Object> getUserById(Object id) throws IOException;

	protected abstract boolean isErrorResponse(int status, Map<String, Object> parsedResponse);

	public Map<String, Object> executeJsonCall(String callUrl, Map<String, Object> queryParameters,
			String responseFileDumpName) {
		// WebClient wc = sf.createWebClient();
		// wc.path(callUrl);
		// for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
		// wc.query(entry.getKey(), entry.getValue());
		// }
		// wc.accept(MediaType.APPLICATION_JSON);
		//
		// Response response = wc.get();
		// Object entity = response.getEntity();
		// int status = response.getStatus();
		//
		// MultivaluedMap<String, Object> metadata = response.getMetadata();

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(DEFAULT_PROXY_HOST, DEFAULT_PROXY_PORT));
		HttpURLConnection urlConnection = null;
		String responseStr = null;
		int status = -1;
		try {
			String urlStr = getUrl() + callUrl;
			for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
				urlStr += "?" + entry.getKey() + "=" + entry.getValue();
			}
			URL url = new URL(urlStr);
			urlConnection = (HttpURLConnection) url.openConnection(proxy);
			urlConnection.connect();

			status = urlConnection.getResponseCode();

			StringBuilder sbr = new StringBuilder();
			BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) urlConnection.getInputStream()));
			String s;
			s = br.readLine();
			while (s != null) {
				sbr.append(s.trim());
				s = br.readLine();
			}
			responseStr = sbr.toString();
		} catch (IOException e1) {
			logger.error("Error while reading response for path: {}", callUrl);
			return null;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}

		try {
			Map<String, Object> parse = (Map<String, Object>) new JSONParser().parse(responseStr);
			if (isErrorResponse(status, parse)) {
				try {
					logger.info("Rate limit error, retrying after 10 minutes. Current Date: {}", new Date());
					Thread.sleep(RATE_LIMIT_SLEEP_TIME);
					return executeJsonCall(callUrl, queryParameters, responseFileDumpName);
				} catch (InterruptedException e) {
					logger.error("Error while sleeping to wait for rate limit. ", e);
					return null;
				}
			}
			dumpResponse(responseFileDumpName, responseStr);
			logger.debug("Json response for path: {} Response: {}", new Object[] { callUrl, responseStr });
			return parse;
		} catch (ParseException e) {
			logger.error("Error while parsing json. Exception: {}, Json: {} ", e.getMessage(), responseStr);
		}

		return null;
	}

	private void dumpResponse(String responseFileDumpName, String response) {
		File createFile = FileUtil.createNewFile(baseFilePath + File.separator + responseFileDumpName, false);
		try {
			FileWriter fileWriter = new FileWriter(createFile);
			fileWriter.write(response);
			fileWriter.close();
		} catch (IOException e) {
			logger.error("Error while dumping response to file: {}", e, response);
		}
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

	protected abstract Map<String, Object> addUser(Object id) throws IOException;

	public void runSearchInNewThread(final int maxDepth) throws IOException {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				runSearch(maxDepth);
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
	}

	public void runSearch(int maxDepth) {
		long startTime = System.currentTimeMillis();
		int totalNodes = 0;
		logger.info("Start time {}", startTime);
		while (true) {
			try {
				Thread.sleep(SEARCH_SLEEP_TIME);
				Friend poll = queueFriendIds.poll();// putting this at start for using continue statement (for less
													// indentation)
				if (poll == null) {
					break;
				}
				// allFriendIds.add(poll.getId());
				totalFriends++;
				if (totalFriends % 1000 == 0) {
					logger.info("Number of friend ids: " + totalFriends);
				}
				if (queueFriendIds.size() % 1000 == 0) {
					logger.info("Number of friend ids in queue: " + queueFriendIds.size());
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
					continue;
				}

				String addedUserEmail = getEmail(addedUserObj.get(ID));
				logger.info("Adding user: {} poll: {} index: {}", new Object[] { addedUserEmail, poll, totalNodes });
				totalNodes++;

				// add relation
				String sourceEmail = poll.getSourceEmail();
				if (sourceEmail != null) {
					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put("network", getNetwork());
					naradJsonClient.addRelationship(sourceEmail, addedUserEmail, properties);
				}

				if (queueFriendIds.size() > MAX_FRIEND_QUEUE_SIZE) {
					logger.info("More than :{} friends in queue. not fetching more.", MAX_FRIEND_QUEUE_SIZE);
					continue;
				}

				List<Object> friendIds = getFriendIds(addedUserObj);
				if (friendIds == null || friendIds.isEmpty()) {
					logger.info("No friends: {} for user: {}", addedUserEmail);
					continue;
				}
				int newFriends = 0;
				for (Object friendId : friendIds) {
					Friend friend = new Friend(addedUserEmail, friendId, poll.getDepth() + 1);
					if ((null == friendId) || (friendId instanceof String && ((String) friendId).trim().isEmpty())) {
						logger.info("Friend has empty id, Not adding. FriendId: {}, Source: {}", friendId, poll);
					} else if (!userExists(getEmail(friendId))) {
						// if (!allFriendIds.contains(friendId)) {
						addFriend(friend);
						newFriends++;
					} else {
						// TODO - add relationship information
					}
				}
				logger.info("Adding new friends: {} for user: {}", newFriends, addedUserEmail);
			} catch (ClientWebApplicationException e) {
				naradJsonClient = new NaradJsonClient();
			} catch (Throwable t) {
				logger.info("Unexpected error while running loop: ", t);
			}
		}
		long endTime = System.currentTimeMillis();
		logger.info("End time {}, total time: {}, totalNodes: {}", new Object[] { endTime, (endTime - startTime),
				totalNodes });
	}

	private void addFriend(Friend friend) {
		queueFriendIds.add(friend);
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(friendsListFilePath, true);
			fileWriter.append(friend.toJsonString() + "\n");
			fileWriter.close();
		} catch (IOException e) {
			logger.error("Error while writing to friends list. Friend: {}", e, friend);
		} finally {
			try {
				fileWriter.close();
			} catch (IOException e) {
				logger.error("Error while closing friends list writer. ", e);
			}
		}
	}

	private boolean userExists(String emailId) {
		JSONObject findPerson = naradJsonClient.findPerson(emailId, null);
		JSONObject result = (JSONObject) findPerson.get("result");
		Object data = result.get("data");
		if (data != null) {
			return true;
		}
		return false;
	}

	protected abstract List<Object> getFriendIds(Map<String, Object> addedUserMap);

	public void addStartPoint(Object id) {
		queueFriendIds.add(new Friend(null, id, 0));
		// allFriendIds.add(id);
	}

	protected boolean addNodeInServer(String emailId, Map<String, Object> nodeMap) {
		JSONObject addNode = naradJsonClient.addNode(emailId, nodeMap);
		Map result = (Map) addNode.get("result");
		if (!(CommandResultType.SUCCESS.toString().equals(result.get("status")))) {
			logger.info(addNode.toJSONString());
		} else {
			return true;
		}
		return false;
	}

	public class Friend {
		private Object id;
		private int depth;
		private String sourceEmail;

		public Friend(String sourceEmail, Object id, int depth) {
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

		public String toJsonString() {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("depth", depth);
			jsonObject.put("sourceEmail", sourceEmail);
			return jsonObject.toJSONString();
		}
	}

	public static void main(String[] args) throws IOException {
		RestNetworksGraphBuilder friendFeedClient = new FriendFeedGraphBuilder2();
		friendFeedClient.addStartPoint("blindguy");
		friendFeedClient.runSearchInNewThread(100);

		RestNetworksGraphBuilder twitterClient = new TwitterGraphBuilder2();
		twitterClient.addStartPoint(422610112L);
		twitterClient.runSearchInNewThread(100);
	}
}
