package com.narad.client.applications.collector.reader;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.narad.client.applications.collector.api.ProfileNetwork;
import com.narad.client.applications.collector.api.ProfileReader;
import com.narad.client.util.CommunicationResponse;
import com.narad.client.util.CommunicationUtil;

public abstract class NetworkReader implements ProfileNetwork, ProfileReader {

	public static final int RATE_LIMIT_SLEEP_TIME = 10 * 60 * 1000;
	private Logger logger;
	private String proxyHost;
	private int proxyPort;
	protected String lastResponse;

	public NetworkReader(Logger logger, String proxyHost, int proxyPort) {
		super();
		this.logger = logger;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	protected CommunicationResponse makeCall(String url, Map<String, Object> queryParameters) {
		if(queryParameters==null) {
			queryParameters = new HashMap<String, Object>();
		}
		CommunicationResponse response = CommunicationUtil.apacheHttpRequest(proxyHost, proxyPort, getBaseUrl() + url,
				queryParameters);
		lastResponse = response.getResponseStr();
		if (isRateError(response)) {
			logger.info("Rate limit error, retrying after 10 minutes. Current Date: {}", new Date());
			try {
				Thread.sleep(RATE_LIMIT_SLEEP_TIME);
			} catch (InterruptedException e) {
				logger.error("Error while sleeping thread", e);
			}
		}
		return response;
	}
	
	public String getLastResponse() {
		return lastResponse;
	}

	public void setLastResponse(String lastResponse) {
		this.lastResponse = lastResponse;
	}

	protected abstract boolean isRateError(CommunicationResponse response);
	
	protected boolean isErrorResponse(CommunicationResponse response) {
		if (response.getStatus() != 200) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean userExists(Object userId) {
		Map<String, Object> fetchUser = fetchUser(userId);
		if (((CommunicationResponse) fetchUser).getError() != null) {
			return false;
		}
		return true;
	}

	@Override
	public boolean friendsExist(Object userId) {
		List<Object> fetchFriends = fetchFriends(userId, null);
		if (((CommunicationResponse) fetchFriends).getError() != null) {
			return false;
		}
		return true;
	}

}
