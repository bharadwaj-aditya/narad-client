package com.narad.client.applications;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FacebookScraper {

	private HttpClient httpClient;
	private List<String> allUserUrls;
	private Queue<String> userUrls;

	public FacebookScraper() {
		super();
		allUserUrls = new ArrayList<String>();
		userUrls = new LinkedList<String>();
		httpClient = new HttpClient();
	}

	public void crawl(String urlStr) throws HttpException, IOException, ParserConfigurationException, SAXException,
			XMLStreamException, XPathExpressionException {
		URL url = new URL(urlStr);
		GetMethod getMethod = new GetMethod(urlStr);
		int status = httpClient.executeMethod(getMethod);

		if (status == HttpStatus.SC_OK) {
			String responseBodyAsString = getMethod.getResponseBodyAsString();
			System.out.println(responseBodyAsString);

			xpathParse(responseBodyAsString);

			// XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// // Setup a new eventReader
			// InputStream in = getMethod.getResponseBodyAsStream();
			// XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// // Read the XML document
			// //Item item = null;
			//
			// while (eventReader.hasNext()) {
			// XMLEvent event = eventReader.nextEvent();
			// if(event.isStartElement()) {
			// StartElement asStartElement = event.asStartElement();
			// QName name = asStartElement.getName();
			// System.out.println(name.getLocalPart());
			// }
			//
			// }

		} else {
			System.out.println("Could not access url: " + url + " status: " + status);
		}
	}

	private void xpathParse(String responseBodyAsString) throws ParserConfigurationException, SAXException,
			IOException, XPathExpressionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document parse = builder.parse(responseBodyAsString);

		XPath path = XPathFactory.newInstance().newXPath();
		XPathExpression compile = path.compile("//img");

		Object result = compile.evaluate(parse, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());
		}
	}

	public static void main(String[] args) throws Exception {
		FacebookScraper facebookScraper = new FacebookScraper();
		facebookScraper.crawl("http://www.facebook.com/ajay.k.soni.90");
	}
}
