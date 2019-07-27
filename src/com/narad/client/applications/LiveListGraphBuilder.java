package com.narad.client.applications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.narad.client.NaradJsonClient;

public class LiveListGraphBuilder {
	private NaradJsonClient naradJsonClient;

	public static void main(String[] args) {
		String allRows = "firstname_x1a46a99	last_name_7hkkro50	email_78k2vut2	linkedin_page_q4fd8n09	facebook_page_7qh8277k	twitter_page_c0ku0bea\r\n" + 
				"Xyz	abc	a@b.c.com	NULL	http://www.facebook.com/abc	http://www.twitter.com/abc\r\n" + 
				"Rose	Gonzalez	rose@edge.com	http://www.linkedin.com/rose	NULL	NULL\r\n" + 
				"Sean	Forbes	sean@edge.com	http://www.linked.com/seanforbes	NULL	http://www.twitter.com/seanforbes\r\n" + 
				"Jack	Rogers	jrogers@burlington.com	NULL	NULL	http://www.twitter.com/jackrogers\r\n" + 
				"Pat	Stumuller	pat@pyramid.net	NULL	NULL	http://www.twitter.com/patstumuller\r\n" + 
				"Andy	Young	a_young@dickenson.com	NULL	http://www.facebook.com/andyyoung	http://www.twitter.com/andyyoung\r\n" + 
				"Tim	Barr	barr_tim@grandhotels.com	http://www.linkedin.com/timbarr	NULL	NULL\r\n" + 
				"NULL	Roger Test	0boy@friendfeed.com	http://www.linkedin.com/rogertest	NULL	NULL\r\n" + 
				"NULL	Robinson	1marc@friendfeed.com	NULL	http://www.facebook.com/marcrobinson	NULL\r\n" + 
				"Michael	Wilson	1michaelwilson@friendfeed.com	NULL	NULL	http://www.twitter.com/michaelwilson";
		LiveListGraphBuilder liveListGraphBuilder = new LiveListGraphBuilder();
		//liveListGraphBuilder.runBuilderFromString(allRows);
		
		liveListGraphBuilder.getPath("1michaelwilson@friendfeed.com", "a_young@dickenson.com");
		liveListGraphBuilder.getPath("1michaelwilson@friendfeed.com", "a@b.c.com");
		liveListGraphBuilder.getPath("1marc@friendfeed.com", "a@b.c.com");
		liveListGraphBuilder.getPath("1marc@friendfeed.com", "1michaelwilson@friendfeed.com");
		liveListGraphBuilder.getPath("rose@edge.com", "1michaelwilson@friendfeed.com");
		liveListGraphBuilder.getPath("rose@edge.com", "1marc@friendfeed.com");
	}

	public LiveListGraphBuilder() {
		super();
		naradJsonClient = new NaradJsonClient();
	}

	public void runBuilderFromString(String builderStr) {

		StringTokenizer tokenizer = new StringTokenizer(builderStr, "\r\n");
		String[] columnNames = null;
		List<Map<String, Object>> rowList = new ArrayList<Map<String, Object>>();
		while (tokenizer.hasMoreTokens()) {
			String row = tokenizer.nextToken();
			String[] split = row.split("\t");
			if (columnNames == null) {
				columnNames = split;
			} else {
				HashMap<String, Object> rowMap = new HashMap<String, Object>();
				for (int j = 0; j < split.length; j++) {
					String val = split[j];
					if (!val.equals("NULL")) {
						rowMap.put(columnNames[j], val);
					}
				}
				rowList.add(rowMap);
			}
		}

		for (Map<String, Object> row : rowList) {
			String fromEmail = (String) row.get("email_78k2vut2");
			for (Map.Entry<String, Object> entry : row.entrySet()) {
				String key = entry.getKey();
				if (key == null) {
				} else if (key.contains("twitter")) {
					addRelationshipForNetwork(rowList, naradJsonClient, fromEmail, key, "twitter");
				} else if (key.contains("facebook")) {
					addRelationshipForNetwork(rowList, naradJsonClient, fromEmail, key, "facebook");
				} else if (key.contains("linkedin")) {
					addRelationshipForNetwork(rowList, naradJsonClient, fromEmail, key, "linkedin");
				}
			}
		}
	}

	private void addRelationshipForNetwork(List<Map<String, Object>> rowList, NaradJsonClient naradJsonClient,
			String fromEmail, String key, String networkName) {
		for (Map<String, Object> row2 : rowList) {
			Object object = row2.get(key);
			if (object != null) {
				String toEmail = (String) row2.get("email_78k2vut2");
				if (fromEmail != toEmail) {
					//naradJsonClient.
					naradJsonClient.addRelationship(fromEmail, toEmail, networkName);
				}
			}
		}
	}

	public String[] getPath(String fromEmail, String toEmail) {
		List<String> path = new ArrayList<String>();
		JSONObject findPath = naradJsonClient.findPath(fromEmail, toEmail, null);
		JSONObject result = (JSONObject)findPath.get("result");
		JSONArray data = (JSONArray)result.get("data");
		if(data!=null) {
			for(Object dat: data) {
				JSONObject obj = (JSONObject)dat;
				JSONObject object = (JSONObject)obj.get("profiles");
				Object[] array = object.keySet().toArray(new String[object.keySet().size()]);
				if(array.length>0) {
					path.add((String)array[0]);
				}
			}
		}
		//System.out.println(path);
		return null;
	}
}
