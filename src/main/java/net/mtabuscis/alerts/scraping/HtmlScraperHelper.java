package net.mtabuscis.alerts.scraping;
import java.io.IOException;
import java.util.logging.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * This class encapsulates a JSoup Java HTML Parser
 */
public class HtmlScraperHelper {
	
	Logger _log = Logger.getLogger("HtmlScraperHelper");
	
	public String getElementByClass(URL url, String className) { 
		Document doc = null;
		try {
			doc = Jsoup.parse(url, 9000);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			_log.log(Level.SEVERE, "URL given is to retrieve the " + className + " class from html is not valid. Closing...");
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			_log.log(Level.SEVERE, "There was a problem reading from the URL. Closing...");
			return null;
		}
		Element em = doc.getElementsByClass(className).get(0);
		return em.text();
	}
}
