package com.narad.client.applications.collector.reader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.narad.client.applications.collector.api.ProfileReader;
import com.narad.client.util.FileUtil;
import com.narad.dataaccess.dao.DaoConstants;
import com.narad.util.NaradMapUtils;
import com.narad.util.XmlNode;
import com.narad.util.XmlParser;

public class LiveInboxReader implements ProfileReader {
	public static final String FACEBOOK = "facebook";
	public static final String LINKEDIN = "linkedin";

	private static final Logger logger = LoggerFactory.getLogger(LiveInboxReader.class);

	private String baseFilePath;
	private String lastResponse;

	public LiveInboxReader(String baseFilePath) {
		super();
		this.baseFilePath = baseFilePath;
	}

	@Override
	public String getName() {
		return "liveInbox";
	}

	@Override
	public boolean userExists(Object userId) {
		// Check this base on structure of file names
		return true;
	}

	@Override
	public boolean friendsExist(Object userId) {
		// Check facebook/twitter etc
		return false;
	}

	@Override
	public Map<String, Object> fetchUser(Object id) {
		Map<String, Object> userObject = new HashMap<String, Object>();
		populateUserMapFromFb(id, userObject);
		// populateUserMapFromTwitter(id, userObject);
		populateUserMapFromLinkedin(id, userObject);

		/*
		 * Map<String, Object> linkedinMap = (Map) FileUtil.readJsonFromFile(baseFilePath + File.separator + "linkedin"
		 * + "_" + id + ".json");
		 */

		// Build person in structure
		return userObject;
	}

	private void populateUserMapFromFb(Object id, Map<String, Object> userObj) {
		// Map<String, Object> facebookMap = (Map) FileUtil.readJsonFromFile(baseFilePath + File.separator + "fb"
		// + "_" + id + ".json");
		String facebookFilePath = baseFilePath + File.separator + getName() + File.separator + id + File.separator
				+ "fb" + File.separator + "me" + File.separator + "person.dat.txt";
		Map<String, Object> facebookMap = (Map) FileUtil.readJsonFromFile(facebookFilePath);
		if (facebookMap != null) {
			populateUserMapFromFb(userObj, facebookMap);
		} else {
			logger.info("Could not read user map for fb: {}", facebookFilePath);
		}
	}

	private void populateUserMapFromFb(Map<String, Object> userObj, Map<String, Object> facebookMap) {
		userObj.put(DaoConstants.FIRST_NAME, facebookMap.get("first_name"));
		userObj.put(DaoConstants.LAST_NAME, facebookMap.get("last_name"));
		userObj.put(DaoConstants.FULL_NAME, facebookMap.get("name"));
		userObj.put(DaoConstants.BIRTHDAY, facebookMap.get("birthday"));
		userObj.put(DaoConstants.FIRST_NAME, facebookMap.get("first_name"));

		List<Map<String, Object>> networks = (List) userObj.get(DaoConstants.NETWORKS);
		if (networks == null) {
			networks = new ArrayList<Map<String, Object>>();
			userObj.put(DaoConstants.NETWORKS, networks);
		}
		HashMap<String, Object> fbMap = new HashMap<String, Object>();
		fbMap.put(DaoConstants.NETWORK_USER_URL, facebookMap.get("link"));
		fbMap.put(DaoConstants.NETWORK_ID, FACEBOOK);
		fbMap.put(DaoConstants.NETWORK_NAME, "Facebook");
		HashMap<String, Object> fbNetworkProps = new HashMap<String, Object>();
		fbNetworkProps.putAll(facebookMap);
		fbMap.put("properties", fbNetworkProps);
		networks.add(fbMap);

		List<String> emailIds = (List) userObj.get(DaoConstants.EMAIL_IDS);
		if (emailIds == null) {
			emailIds = new ArrayList<String>();
			userObj.put(DaoConstants.EMAIL_IDS, emailIds);
		}
		String email = (String)facebookMap.get("email");
		if (email != null) {
			emailIds.add(email);
		}

		List<String> websites = (List) userObj.get(DaoConstants.WEBSITES);
		if (websites == null) {
			websites = new ArrayList<String>();
			userObj.put(DaoConstants.WEBSITES, websites);
		}
		String website = (String)facebookMap.get("website");
		if (website != null) {
			websites.add(website);
		}

		List<Map<String, Object>> jobs = (List) userObj.get(DaoConstants.JOBS);
		if (jobs == null) {
			jobs = new ArrayList<Map<String, Object>>();
			userObj.put(DaoConstants.JOBS, jobs);
		}

		List<Map<String, Object>> fbJobs = (List) facebookMap.get("work");
		if (fbJobs != null) {
			for (Map<String, Object> fbJob : fbJobs) {
				HashMap<String, Object> job = new HashMap<String, Object>();
				jobs.add(job);

				Object object = fbJob.get("employer");
				if (object != null) {
					job.put(DaoConstants.INST_NAME, ((Map) object).get("name"));
				}
				object = fbJob.get("position");
				if (object != null) {
					job.put(DaoConstants.INST_TITLE, ((Map) object).get("name"));
				}
				Map<String, Object> properties = (Map) fbJob.get(DaoConstants.PROPERTIES);
				if (properties == null) {
					properties = new HashMap<String, Object>();
					job.put(DaoConstants.PROPERTIES, properties);
				}
				object = fbJob.get("location");
				if (object != null) {
					properties.put("location", ((Map) object).get("name"));
				}
				object = fbJob.get("description");
				if (object != null) {
					properties.put("description", object);
				}
			}
		}

		List<Map<String, Object>> locations = (List) userObj.get(DaoConstants.LOCATIONS);
		Map<String, Object> fbLocation = (Map) facebookMap.get("location");
		if (locations == null) {
			locations = new ArrayList<Map<String, Object>>();
			userObj.put("location", locations);
		}

		if (fbLocation != null) {
			HashMap<String, Object> location = new HashMap<String, Object>();
			Object locationObject = fbLocation.get("name");
			if (locationObject != null) {
				location.put(DaoConstants.LOC_ADDRESS, locationObject);
			}
			if (!location.isEmpty()) {
				locations.add(location);
			}
		}

		List<Map<String, Object>> education = (List) userObj.get(DaoConstants.EDUCATION);
		List<Map<String, Object>> fbEducation = (List) facebookMap.get("education");
		if (education == null) {
			education = new ArrayList<Map<String, Object>>();
			userObj.put(DaoConstants.EDUCATION, education);
		}

		if (fbEducation != null) {
			for (Map<String, Object> fbEduInst : fbEducation) {
				Map<String, Object> eduInst = new HashMap<String, Object>();
				Object object = fbEduInst.get("school");
				if (object != null) {
					eduInst.put(DaoConstants.INST_NAME, ((Map) object).get("name"));
				}
				object = fbEduInst.get("type");
				if (object != null) {
					eduInst.put(DaoConstants.INST_TYPE, object);
				}
				if (eduInst != null) {
					education.add(eduInst);
				}
			}
		}
	}

	private void populateUserMapFromTwitter(Object id, Map<String, Object> userObj) {
		String twitterFilePath = baseFilePath + File.separator + getName() + File.separator + "twitter" + "_" + id
				+ ".json";
		Map<String, Object> twitterMap = (Map) FileUtil.readJsonFromFile(twitterFilePath);
		String fullXml = (String) twitterMap.get("xml");
		if (fullXml == null) {
			logger.info("Could not populate twitter data for user: {}", twitterFilePath);
			return;
		}
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fullXml.getBytes());
		try {
			Document read = new SAXReader().read(byteArrayInputStream);
			Iterator nodeIterator = read.nodeIterator();
			for (Iterator<Element> iterator = read.nodeIterator(); iterator.hasNext();) {
				Element type = iterator.next();
				System.out.println(type.getName() + " " + type.getData());
			}
		} catch (DocumentException e) {
			logger.error("Error reading twitter xml");
		}
	}

	private void populateUserMapFromLinkedin(Object id, Map<String, Object> userObj) {
	}

	@Override
	public List<Object> fetchFriends(Object id, Map<String, Object> userMap) {
		List<Map<String, Object>> fbFriendsList = new ArrayList<Map<String, Object>>();
		populateFriendsListFromFacebook(id, fbFriendsList);
		for (Map<String, Object> friend : fbFriendsList) {
			// add relationship to current node
		}
		List<Map<String, Object>> linkedinConnectionList = new ArrayList<Map<String, Object>>();
		populateFriendsListFromLinkedin(id, linkedinConnectionList);
		for (Map<String, Object> friend : linkedinConnectionList) {
			// add relationship to current node
		}
		ArrayList<Object> allFriends = new ArrayList<Object>();
		allFriends.addAll(fbFriendsList);
		allFriends.addAll(linkedinConnectionList);
		return allFriends;
	}

	private void populateFriendsListFromFacebook(Object id, List<Map<String, Object>> friendsList) {
		// Map<String, Object> facebookFriendsMap = (Map) FileUtil.readJsonFromFile(baseFilePath + File.separator
		// + "facebook" + "_" + id + "_friends.json");
		String fbFriendsFilePath = baseFilePath + File.separator + getName() + File.separator + id + File.separator
				+ "fb" + File.separator + "me" + File.separator + "friends.dat.txt";
		Map<String, Object> facebookFriendsMap = (Map) FileUtil.readJsonFromFile(fbFriendsFilePath);

		if (facebookFriendsMap == null) {
			logger.info("Could not fetch friends for user from file: {}", fbFriendsFilePath);
			return;
		}
		List<Map<String, Object>> friendsJsonList = (List) facebookFriendsMap.get("data");
		for (Map<String, Object> friendJson : friendsJsonList) {
			HashMap<String, Object> userObj = new HashMap<String, Object>();
			populateUserMapFromFb(userObj, friendJson);
			friendsList.add(userObj);
		}
	}

	private void populateFriendsListFromLinkedin(Object id, List<Map<String, Object>> friendsList) {
		String linkedinFilePath = baseFilePath + File.separator + getName() + File.separator + id + File.separator
				+ "lin" + File.separator + "connections" + File.separator + "me.dat.txt";
		XmlNode parseFile = XmlParser.parseFile(linkedinFilePath);
		if (parseFile == null) {
			logger.info("Could not populate linked in data for user: {}", linkedinFilePath);
			return;
		}
		List<XmlNode> personList = (List) parseFile.getElement("person");
		if (personList == null) {
			logger.info("Not adding any linked in connections for: {}", id);
			return;
		}
		for (XmlNode person : personList) {
			HashMap<String, Object> personMap = populateUserMapFromLinkedIn(person);
			friendsList.add(personMap);
		}
	}

	private HashMap<String, Object> populateUserMapFromLinkedIn(XmlNode person) {
		HashMap<String, Object> personMap = new HashMap<String, Object>();

		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		// person.get();
		NaradMapUtils.putInMapIfNotNull(personMap, DaoConstants.FIRST_NAME, getFirstElementValue(person, "first-name"));
		NaradMapUtils.putInMapIfNotNull(personMap, DaoConstants.LAST_NAME, getFirstElementValue(person, "last-name"));
		NaradMapUtils.putInMapIfNotNull(personMap, DaoConstants.INDUSTRY, getFirstElementValue(person, "industry"));
		List<XmlNode> positions = (List) person.get("positions");
		if (positions != null) {
			ArrayList<Map<String, Object>> jobList = new ArrayList<Map<String, Object>>();
			for (XmlNode position : positions) {
				HashMap<String, Object> jobMap = new HashMap<String, Object>();
				HashMap<String, Object> positionProperties = new HashMap<String, Object>();
				positionProperties.putAll(position.getMap(false, false, false));
				jobMap.put(DaoConstants.PROPERTIES, positionProperties);
				List<XmlNode> elementList = (List) position.getElementList();
				jobMap.put(DaoConstants.INST_TYPE, "company");
				for (XmlNode element : elementList) {
					List<XmlNode> elementList2 = element.getElementList();
					for (XmlNode element2 : elementList2) {
						String elementName = element2.getName();
						if (elementName.equals("company")) {
							jobMap.put(DaoConstants.INST_NAME, getFirstElementValue(element2, "name"));
						} else if (elementName.equals("start-date")) {
							jobMap.put(DaoConstants.INST_FROM_YEAR, getFirstElementValue(element2, "year"));
						} else if (elementName.equals("end-date")) {
							jobMap.put(DaoConstants.INST_TO_YEAR, getFirstElementValue(element2, "year"));
						} else if (elementName.equals("title")) {
							jobMap.put(DaoConstants.INST_TITLE, getFirstElementValue(element2, "title"));
						}
					}
				}
				if (jobMap.size() > 2) {
					jobList.add(jobMap);
				}
			}
			if (!jobList.isEmpty()) {
				personMap.put(DaoConstants.JOBS, jobList);
			} else {
				logger.info("No positions registered for user: {} {}",
						new Object[] { personMap.get(DaoConstants.FIRST_NAME), personMap.get(DaoConstants.LAST_NAME) });
				System.out.println("How i say!!");
			}
		}

		XmlNode profile = (XmlNode) person.getFirstElement("site-standard-profile-request");
		if (profile != null) {
			Map<String, Object> fullMap = person.getMap();
			HashMap<String, Object> networkMap = new HashMap<String, Object>();
			networkMap.put(DaoConstants.NETWORK_ID, LINKEDIN);
			networkMap.put(DaoConstants.NETWORK_NAME, "LinkedIn");
			networkMap.put(DaoConstants.NETWORK_USER_URL, getFirstElementValue(profile, "url"));
			networkMap.put(DaoConstants.PROPERTIES, fullMap);
			personMap.put(DaoConstants.NETWORKS, Arrays.asList(new Object[] { networkMap }));
		} else {
			logger.info("There is not profile for user: {} {}", personMap.get(DaoConstants.FIRST_NAME),
					personMap.get(DaoConstants.LAST_NAME));
			System.out.println("ANARTH!!!");
		}

		XmlNode location = (XmlNode) person.getFirstElement("location");
		if (location != null) {
			HashMap<String, Object> locationMap = new HashMap<String, Object>();
			NaradMapUtils.putInMapIfNotNull(locationMap, DaoConstants.LOC_ADDRESS,
					getFirstElementValue(location, "name"));
			if (!locationMap.isEmpty()) {
				personMap.put(DaoConstants.LOCATIONS, Arrays.asList(new Object[] { locationMap }));
			}
		}
		return personMap;
	}

	private String getFirstElementValue(XmlNode xmlNode, String name) {
		Object firstElement = xmlNode.getFirstElement(name);
		if (firstElement == null) {
			return null;
		} else if (firstElement instanceof XmlNode) {
			XmlNode node = (XmlNode) firstElement;
			return node.getText();
		}
		return null;
	}

	@Override
	public String getLastResponse() {
		return lastResponse;
	}

	public static void main(String[] args) {
		LiveInboxReader liveInboxReader = new LiveInboxReader("D:\\development\\narad\\data_sources\\live_inbox");
		// Map<String, Object> fetchUser = liveInboxReader.fetchUser("adarsh");
		liveInboxReader.fetchFriends("adarsh", null);

	}

}
