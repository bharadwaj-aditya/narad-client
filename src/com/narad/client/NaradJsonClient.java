package com.narad.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.narad.service.rest.JsonRequestMap;
import com.narad.service.rest.JsonRestDebugService;
import com.narad.service.rest.JsonRestService;

public class NaradJsonClient {

	private static final String SERVICE_URL = "http://localhost:8180/services/v1/";
	private JsonRestService service;
	private JsonRestDebugService debugService;
	private String serviceUrl;

	public NaradJsonClient() {
		this(SERVICE_URL);
	}

	public NaradJsonClient(String naradUrl) {
		super();
		if (naradUrl != null) {
			this.serviceUrl = naradUrl;
		} else {
			this.serviceUrl = SERVICE_URL;
		}
		service = getServiceProxy(JsonRestService.class);
		debugService = getServiceProxy(JsonRestDebugService.class);
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

	public JSONObject addNode(String emailId, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		String output = service.addNode(emailId, jsonRequestMap);
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

	public JSONObject findPerson(String email, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		String output = service.findPerson(email, jsonRequestMap);
		return toJsonObj(output);
	}

	public JSONObject findPath(String fromEmail, String toEmail, Map<String, Object> properties) {
		// JsonRestService service = getServiceProxy(JsonRestService.class);
		JsonRequestMap jsonRequestMap = new JsonRequestMap();
		if (properties != null) {
			jsonRequestMap.putAll(properties);
		}
		String output = service.findPath(fromEmail, toEmail, jsonRequestMap);
		JSONObject jsonObj = toJsonObj(output);
		printPath(fromEmail, toEmail, jsonObj);
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

	public JSONObject showGraph(int depth) {
		// JsonRestDebugService service = getServiceProxy(JsonRestDebugService.class);
		String output = debugService.showGraph(depth);
		return toJsonObj(output);
	}

	public JSONObject showGraphStats() {
		// JsonRestDebugService service = getServiceProxy(JsonRestDebugService.class);
		String output = debugService.showGraphStats();
		return toJsonObj(output);
	}

	public static void main(String[] args) {
		NaradJsonClient client = new NaradJsonClient();
		// HashMap<String, Object> hashMap = new HashMap<String, Object>();
		// hashMap.put("profiles", new ArrayList<Map<String, Object>>());
		// JSONObject jsonObj = client.addNode("asd@asd.com", (Map) hashMap);
		// JSONObject jsonObj = client.findPerson("asd@asd.com", null);
		// JSONObject jsonObj = client.showGraph(2);
		// System.out.println(jsonObj.toJSONString());
		// JSONObject jsonObj2 = client.showGraph(2);
		// System.out.println(jsonObj2.toJSONString());

		// client.findPath("zoukai14@chn.org", "phelpsmichael0@usa.org", null);
		// client.findPath("chenruolin15@chn.org", "phelpsmichael0@usa.org", null);
		// client.findPath("dujardincharlotte18@gbr.org", "phelpsmichael0@usa.org", null);
		// client.findPath("muffatcamille47@fra.org", "kibobae26@kor.org", null);
		// client.findPath("azarenkavictoria70@blr.org", "zoukai14@chn.org", null);
		// client.findPath("Norcalbaby1023@twitter.com", "JPAL_Global@twitter.com", null);
		// JSONObject findPerson = client.findPerson("JPAL_Global@twitter.com", null);
		// client.findPath("aditya_instacol@twitter.com", "MissionBlue@twitter.com", null);
		// client.findPerson("MissionBlue@twitter.com", null);
		// System.out.println(findPerson.toJSONString());

		// JSONObject findPerson = client.findPerson("465607562@twitter.com", null);
		// System.out.println(findPerson.toJSONString());
		// client.findPath("465607562@twitter.com", "196264809@twitter.com", null);
		// JSONObject showGraph = client.showGraph(5);
		// System.out.println(showGraph.toJSONString());

		// JSONObject showGraph = client.showGraph(100);
		// JSONObject showGraph = client.showGraphStats();
		// System.out.println(showGraph.toJSONString());

		// JSONObject findPerson = client.findPerson("jrogers@burlington.com", null);
		//JSONObject findPerson = client.findPerson("1marc@friendfeed.com", null);
		
//		JSONObject findPerson = client.findPerson("1michaelwilson@friendfeed.com", null);
//		JSONObject result = (JSONObject)findPerson.get("result");
//		Object data = result.get("data");
//		System.out.println(findPerson.toJSONString());
		
		
		
		/*client.addRelationship("a@b", "a_young@dickenson.com", "facebook");
		client.addRelationship("a@b", "1marc@friendfeed.com", "facebook");
		client.addRelationship("a_young@dickenson.com", "1marc@friendfeed.com", "facebook");
		
		client.addRelationship("a@b", "sean@edge.com", "twitter");
		client.addRelationship("a@b", "jrogers@burlington.com", "twitter");
		client.addRelationship("a@b", "sean@edge.com", "twitter");
		client.addRelationship("a@b", "sean@edge.com", "twitter");
		client.addRelationship("a@b", "sean@edge.com", "twitter");
		client.addRelationship("a@b", "sean@edge.com", "twitter");*/
		//new NaradJsonClient().addNode("a@asd.com", "twitter");
		//new NaradJsonClient().addRelationship("1michaelwilson@friendfeed.com", "a@asd.com", "twitter");
		 
//		 HashMap<String,Object> hashMap3 = new HashMap<String, Object>();
//		 hashMap3.put("profiles", new ArrayList<Object>());
//		 client.addNode("a@b.c", hashMap3);
		JSONObject showGraphStats = client.showGraphStats();
		System.out.println(showGraphStats.toJSONString());
		
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
