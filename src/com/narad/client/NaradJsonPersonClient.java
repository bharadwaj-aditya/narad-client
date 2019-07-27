package com.narad.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.narad.dataaccess.dao.DaoConstants;
import com.narad.service.rest.JsonPersonRestService;
import com.narad.service.rest.JsonRequestMap;

public class NaradJsonPersonClient {

	private static final String SERVICE_URL = "http://localhost:8180/services/v1/";
	private JsonPersonRestService service;
	private String serviceUrl;

	public NaradJsonPersonClient() {
		this(SERVICE_URL);
	}

	public NaradJsonPersonClient(String naradUrl) {
		super();
		if (naradUrl != null) {
			this.serviceUrl = naradUrl;
		} else {
			this.serviceUrl = SERVICE_URL;
		}
		service = getServiceProxy(JsonPersonRestService.class);
	}

	public JAXRSClientFactoryBean getFactoryBean(Class clazz, String serviceUrl) {
		JAXRSClientFactoryBean sf = new JAXRSClientFactoryBean();
		sf.setResourceClass(clazz);
		sf.setAddress(serviceUrl);
		BindingFactoryManager manager = sf.getBus().getExtension(BindingFactoryManager.class);
		JAXRSBindingFactory factory = new JAXRSBindingFactory();
		factory.setBus(sf.getBus());
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, factory);
		return sf;
	}

	public <T> T getServiceProxy(Class<T> t) {
		JAXRSClientFactoryBean factoryBean = getFactoryBean(t, serviceUrl);
		return factoryBean.create(t);
	}

	private JSONObject toJsonObj(String jsonStr) {
		try {
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
			return jsonObj;
		} catch (Exception e) {
			System.out.println(jsonStr);
			e.printStackTrace();
		}
		return null;
	}

	public JSONObject addPerson(String emailId, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		String output = service.addPerson(emailId, jsonRequestMap);
		return toJsonObj(output);
	}

	public JSONObject addRelationship(String fromEmail, String toEmail, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		// System.out.println("Adding relationship from: " + fromEmail + " toEmail: " + toEmail);
		String output = service.addRelation(fromEmail, toEmail, jsonRequestMap);
		return toJsonObj(output);
	}
	
	public JSONObject addRelationship(Map<String, Object> fromUserMap, Map<String, Object> toUserMap, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		JsonRequestMap fromUser = new JsonRequestMap();
		fromUser.putAll(fromUserMap);
		JsonRequestMap toUser = new JsonRequestMap();
		toUser.putAll(toUserMap);
		// System.out.println("Adding relationship from: " + fromEmail + " toEmail: " + toEmail);
		String output = service.addRelationByPerson(fromUser, toUser, jsonRequestMap);
		return toJsonObj(output);
	}


	public JSONObject findPerson(String email, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		String output = service.findPerson(email, jsonRequestMap);
		return toJsonObj(output);
	}

	public JSONObject findPath(Map<String, Object> fromEmail, Map<String, Object> toEmail,
			Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		JsonRequestMap fromUser = new JsonRequestMap();
		fromUser.putAll(fromEmail);
		JsonRequestMap toUser = new JsonRequestMap();
		toUser.putAll(toEmail);
		String output = service.findPath(fromUser, toUser, jsonRequestMap);
		JSONObject jsonObj = toJsonObj(output);
		//printPath(fromEmail, toEmail, jsonObj);
		return jsonObj;
	}

	private void printPath(String fromEmail, String toEmail, JSONObject jsonObj) {
		Object data = ((Map) jsonObj.get("result")).get("data");
		if (data != null && data instanceof JSONArray) {
			JSONArray pathArr = (JSONArray) data;
			System.out.print("Path: ");
			for (int i = 0; i < pathArr.size(); i++) {
				Map<String, Object> node = (Map<String, Object>) pathArr.get(i);
				Map<String, Object> profile = (Map<String, Object>) node.get("profiles");
				for (Map.Entry<String, Object> entry : profile.entrySet()) {
					System.out.print(entry.getKey() + " ");
					break;
				}
			}
			System.out.println();
		} else {
			System.out.println("no path from: " + fromEmail + " to " + toEmail + " Reason: " + data);
		}
	}

	public static void main(String[] args) {
		NaradJsonPersonClient client = new NaradJsonPersonClient();
//		HashMap<String, Object> hashMap = new HashMap<String, Object>();
////		String emailId = person1(hashMap);
////		client.addPerson(emailId, hashMap);
//		
//		hashMap = new HashMap<String, Object>();
//		String emailId2 = person2(hashMap);
//		client.addPerson(emailId2, hashMap);
////		JSONObject findPerson = client.findPerson("a@b.com", null);
////		System.out.println(findPerson.toJSONString());
//		JSONObject findPerson = client.findPerson("x@z.com", null);
//		System.out.println(findPerson.toJSONString());
		
//		person1(client);
//		person2(client);
//		addRelation(client);
		
		//JSONObject findPerson = client.findPerson("x@z.com", null);
//		HashMap<String,Object> hashMap = new HashMap<String, Object>();
//		hashMap.put(DaoConstants.FIRST_NAME, "Satish");
//		hashMap.put(DaoConstants.LAST_NAME, "Reddy");
//		hashMap.put(DaoConstants.FULL_NAME, "Satish Reddy");
//
//		JSONObject findPerson = client.findPerson(null, hashMap);
//		hashMap.remove(DaoConstants.FULL_NAME);
//		findPerson = client.findPerson(null, hashMap);
//		hashMap.remove(DaoConstants.FIRST_NAME);
//		findPerson = client.findPerson(null, hashMap);
//		hashMap.remove(DaoConstants.LAST_NAME);
//		findPerson = client.findPerson(null, hashMap);
		HashMap<String,Object> fromMap = new HashMap<String, Object>();
		fromMap.put(DaoConstants.FIRST_NAME, "Satish");
		fromMap.put(DaoConstants.LAST_NAME, "Reddy");
		
		HashMap<String,Object> toMap = new HashMap<String, Object>();
		toMap.put(DaoConstants.FIRST_NAME, "Ajay");
		toMap.put(DaoConstants.LAST_NAME, "Soni");
		
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("maxDistance", 2);
		
		JSONObject findPath = client.findPath(fromMap, toMap, properties);
		System.out.println(findPath.toJSONString());
	}

	private static void addRelation(NaradJsonPersonClient client) {
		HashMap<String,Object> hashMap = new HashMap<String, Object>();
		HashMap<String,Object> twitterFromRelation = new HashMap<String, Object>();
		twitterFromRelation.put(DaoConstants.REL_TYPE, "social");
		twitterFromRelation.put(DaoConstants.REL_NAME, "twitter");
		twitterFromRelation.put(DaoConstants.REL_SUB_TYPE, "following");
		twitterFromRelation.put(DaoConstants.REL_DISTANCE, 1);
		twitterFromRelation.put(DaoConstants.REL_WEIGHT, 100);
		HashMap<String,Object> fromHashMap = new HashMap<String, Object>();
		fromHashMap.put(DaoConstants.RELATION, Arrays.asList(new Object[]{twitterFromRelation}));
		
		
		HashMap<String,Object> twitterReturnRelation = new HashMap<String, Object>();
		twitterReturnRelation.put(DaoConstants.REL_TYPE, "social");
		twitterReturnRelation.put(DaoConstants.REL_NAME, "twitter");
		twitterReturnRelation.put(DaoConstants.REL_SUB_TYPE, "follower");
		twitterReturnRelation.put(DaoConstants.REL_DISTANCE, 1);
		twitterReturnRelation.put(DaoConstants.REL_WEIGHT, 100);
		HashMap<String,Object> returnHashMap = new HashMap<String, Object>();
		returnHashMap.put(DaoConstants.RELATION, Arrays.asList(new Object[]{twitterReturnRelation}));
		
		hashMap.put("fromProperties", fromHashMap);
		hashMap.put("toProperties", returnHashMap);
		
		JSONObject addRelationship = client.addRelationship("a@b.com", "x@y.com", hashMap);
		System.out.println(addRelationship.toJSONString());
	}

	private static String person1(NaradJsonPersonClient client) {
		HashMap<String,Object> hashMap = new HashMap<String, Object>();
		String emailId = "a@b.com";
		hashMap.put(DaoConstants.FIRST_NAME, "Satish");
		hashMap.put(DaoConstants.LAST_NAME, "Reddy");
		hashMap.put(DaoConstants.FULL_NAME, "Satish Reddy");
		hashMap.put(DaoConstants.EMAIL_IDS, Arrays.asList(new String[]{emailId, "a@d.com", "sat@red.com"}));
		hashMap.put(DaoConstants.PHONES, Arrays.asList(new String[]{ "21", "9999912345"}));
		
		//networks
		HashMap<String, Object> twitterMap = new HashMap<String, Object>();
		twitterMap.put(DaoConstants.NETWORK_USER_ID, "satred");
		twitterMap.put(DaoConstants.NETWORK_ID, "twitter");
		twitterMap.put(DaoConstants.NETWORK_NAME, "Twitter");
		hashMap.put(DaoConstants.NETWORKS, Arrays.asList(new Object[] { twitterMap }));
		//jobs
		//locations
		hashMap.put(DaoConstants.BIRTHDAY, "1980-01-01T00:00:00");
		hashMap.put(DaoConstants.ANNIVERSARY, "2005-01-01T00:00:00");
		hashMap.put(DaoConstants.INDUSTRY, "IT");
		//education
		hashMap.put(DaoConstants.GENDER, "Male");
		//spouse name
		//spouse last name
		//spouse full name
		hashMap.put(DaoConstants.AGE, 35);
		//hashMap.put(DaoConstants.AGE_RANGE, 35);
		hashMap.put(DaoConstants.SKILLS, Arrays.asList(new String[]{"Admin", "Pool", "Table tennis", "Telugu", "Haahaa"}));
		//Location checkin
		//properties
		JSONObject addPerson = client.addPerson(emailId, hashMap);
		System.out.println(addPerson.toJSONString());
		return emailId;
	}
	
	private static String person2(NaradJsonPersonClient client) {
		HashMap<String,Object> hashMap = new HashMap<String, Object>();
		String emailId = "x@y.com";
		hashMap.put(DaoConstants.FIRST_NAME, "Bikju");
		hashMap.put(DaoConstants.LAST_NAME, "Abraham");
		hashMap.put(DaoConstants.FULL_NAME, "Biju Abraham");
		hashMap.put(DaoConstants.EMAIL_IDS, Arrays.asList(new String[]{emailId, "x@z.com", "biju@abr.com"}));
		hashMap.put(DaoConstants.PHONES, Arrays.asList(new String[]{ "211", "8888812345"}));
		
		//networks
		HashMap<String, Object> twitterMap = new HashMap<String, Object>();
		twitterMap.put(DaoConstants.NETWORK_USER_ID, "bijabr");
		twitterMap.put(DaoConstants.NETWORK_ID, "twitter");
		twitterMap.put(DaoConstants.NETWORK_NAME, "Twitter");
		hashMap.put(DaoConstants.NETWORKS, Arrays.asList(new Object[] { twitterMap }));
		//jobs
		//locations
		hashMap.put(DaoConstants.BIRTHDAY, "1981-01-01T00:00:00");
		hashMap.put(DaoConstants.ANNIVERSARY, "2006-01-01T00:00:00");
		hashMap.put(DaoConstants.INDUSTRY, "IT");
		//education
		hashMap.put(DaoConstants.GENDER, "Male");
		//spouse name
		//spouse last name
		//spouse full name
		hashMap.put(DaoConstants.AGE, 34);
		//hashMap.put(DaoConstants.AGE_RANGE, 35);
		hashMap.put(DaoConstants.SKILLS, Arrays.asList(new String[]{"Admin", "HR", "Accounts"}));
		//Location checkin
		//properties
		JSONObject addPerson = client.addPerson(emailId, hashMap);
		System.out.println(addPerson.toJSONString());
		return emailId;
	}

	public void addRelationship(String fromEmail, String toEmail, String network){
		HashMap<String,Object> fromMap = new HashMap<String, Object>();
		HashMap<String,Object> toMap = new HashMap<String, Object>();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("network", network);
		properties.put("fromProperties", fromMap);
		properties.put("toProperties", toMap);
		
		JSONObject addRelationship = addRelationship(fromEmail, toEmail, properties);
		System.out.println(addRelationship.get("result"));
	}
}
