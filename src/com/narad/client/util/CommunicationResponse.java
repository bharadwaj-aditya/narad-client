package com.narad.client.util;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunicationResponse extends HashMap<String, Object> {
	private static final Logger logger = LoggerFactory.getLogger(CommunicationResponse.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -2759813796715837972L;
	
	public static final String RAW_RESPONSE = "rawResponse";
	public static final String STATUS = "status";
	private int status;
	private String responseStr;
	private JSONObject parsedJson;
	private String error;
	private boolean isParsed = false;

	public CommunicationResponse(int status, String responseStr) {
		super();
		this.status = status;
		this.responseStr = responseStr;
	}

	public int getStatus() {
		put(STATUS, status);
		return status;
	}

	public String getResponseStr() {
		put(RAW_RESPONSE, responseStr);
		return responseStr;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
		put("error", "Error while parsing response json");
	}

	public JSONObject getParsedJson() {
		if (!isParsed && responseStr != null) {
			try {
				parsedJson = (JSONObject) new JSONParser().parse(responseStr);
				putAll(parsedJson);
			} catch (ParseException e) {
				logger.error("Error while parsing json. Exception: {}, Json: {} ", e.getMessage(), responseStr);
				put("error", "Error while parsing response json");
			} catch (ClassCastException e) {
				logger.error("Error while parsing json. Exception: {}, Json: {} ", e.getMessage(), responseStr);
				put("error", "Error while parsing response json");
			}
		}
		if (parsedJson == null) {
			parsedJson = new JSONObject();
		}
		isParsed = true;
		return parsedJson;
	}

	public Object get(Object key) {
		if (!isParsed) {
			getParsedJson();
		}
		return super.get(key);
	}
}