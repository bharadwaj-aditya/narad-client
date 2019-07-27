package com.narad.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunicationUtil {
	private static final Logger logger = LoggerFactory.getLogger(CommunicationUtil.class);

	public static CommunicationResponse basicHttpRequest(String proxyHost, int proxyPort, String urlStr,
			Map<String, Object> queryParameters) {

		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		HttpURLConnection urlConnection = null;
		String responseStr = null;
		int status = -1;
		try {
			for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
				urlStr += "?" + entry.getKey() + "=" + entry.getValue();
			}
			URL url = new URL(urlStr);
			urlConnection = (HttpURLConnection) url.openConnection(proxy);
			urlConnection.connect();

			status = urlConnection.getResponseCode();
			InputStream inputStream = (InputStream) urlConnection.getInputStream();

			responseStr = inputStreamToString(inputStream);
			return new CommunicationResponse(status, responseStr);
		} catch (IOException e1) {
			logger.error("Error while reading response for path: {}", urlStr);
			return null;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	public static String inputStreamToString(InputStream inputStream) throws IOException {

		StringBuilder sbr = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		String s;
		s = br.readLine();
		while (s != null) {
			sbr.append(s.trim());
			s = br.readLine();
		}
		return sbr.toString();
	}

	public static CommunicationResponse apacheHttpRequest(String proxyHost, int proxyPort, String urlStr,
			Map<String, Object> queryParameters) {

		HttpClient client = new HttpClient();
		if (proxyHost != null && proxyPort > -1) {
			client.getHostConfiguration().setProxy(proxyHost, proxyPort);
		}
		GetMethod getMethod = new GetMethod(urlStr);
		NameValuePair[] nameValues = new NameValuePair[queryParameters.size()];
		int i = 0;
		for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
			Object value = entry.getValue();
			nameValues[i] = new NameValuePair(entry.getKey(), String.valueOf(value));
		}
		getMethod.setQueryString(nameValues);

		try {
			client.executeMethod(getMethod);
			int status = getMethod.getStatusCode();
			String responseStr = getMethod.getResponseBodyAsString();
			return new CommunicationResponse(status, responseStr);
		} catch (IOException e) {
			logger.error("Error while reading response for path: {}", e, urlStr);
			return new CommunicationResponse(-1, "Error while reading response for path " + urlStr);
		}
	}
}
