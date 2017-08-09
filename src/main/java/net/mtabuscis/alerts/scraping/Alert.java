package net.mtabuscis.alerts.scraping;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeLibrary;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.transit.realtime.GtfsRealtime.Alert.Effect;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TranslatedString;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;

/*
 * This class defines an Alert
 */
@Component
public class Alert {
	
	static Map<Effect, String []> effectToKeywords = new java.util.concurrent.ConcurrentHashMap();
	final static String [] noService = {"no service", "suspended"}; //possible problem with no
    final static String [] significantDelays = {"delay", "significant"};
	final static String [] detour = {"detour", "reroute"};
	final static String [] modified = {"modified", "changed"};
	final static String [] stopMoved = {"not stop", "skip", "stops"};
	
	final static String [] wordsToHelpDateParser = {"Beginning", "ending",
													"continuing", "until", 
													"through", "approximately", "early"};
	
	private String id, feed, agency, routeId, routeShortname;
	private String alertHeaderText;
	private TranslatedString alertHeaderTextTranslations;
	private String alertDescriptionText;
	private TranslatedString alertDescriptionTextTranslations;
	private String Url = null;
	private TranslatedString alertUrl = null;
	private long effectiveStartDate, effectiveEndDate;
	private TimeRange activePeriod;
	private Effect effect;
	private EntitySelector informedEntity;
	private String cacheId;
	
	/*
	 * Setters
	 */
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setFeed(String feed) {
		this.feed = feed;
	}
	
	public void setAgency(String agency) {
		this.agency = agency;
	}
	
	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}
	
	public void setRouteShortname(String shortName) {
		this.routeShortname = shortName;
	}
	
	public void setAlertHeaderText(String alertHeaderText) {
		this.alertHeaderText = alertHeaderText;
	}
	
	public void setAlertHeaderTextTranslations(TranslatedString alertHeaderTextTranslations) {
		this.alertHeaderTextTranslations = alertHeaderTextTranslations;
	}
	
	public void setAlertDescriptionText(String alertDescriptionText) {
		this.alertDescriptionText = alertDescriptionText;
	}
	
	public void setAlertDescriptionTextTranslations(TranslatedString alertDescriptionTextTranslations) {
		this.alertDescriptionTextTranslations = alertDescriptionTextTranslations;
	}

	public void setUrlText(String Url) {
		this.Url = Url;
	}
	public void setAlertUrl(TranslatedString alertUrl) {
		this.alertUrl = alertUrl;
	}
	
	public void setEffectiveStartDate(long effectiveStartDate) { 
		this.effectiveStartDate = effectiveStartDate;
	}
	
	public void setEffectiveEndDate(long effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}
	
	public void setEffect(Effect effect) {
		this.effect = effect;
	}
	
	public void setActivePeriod(TimeRange activePeriod) {
		this.activePeriod = activePeriod;
	}
	
	public void setInformedEntity(EntitySelector entity) {
		this.informedEntity = entity;
	}
	
	public void setCacheId(String id) {
		this.cacheId = id;
	}
	
	/*
	 * Getters
	 */
	
	public String getId() {
		return this.id;
	}
	
	public String getFeed() {
		return this.feed;
	}
	
	public String getAgency() {
		return this.agency;
	}
	
	public String getRouteId() {
		return this.routeId;
	}
	
	public String getRouteShortname() {
		return this.routeShortname;
	}
	
	public String getAlertHeaderText() {
		return this.alertHeaderText;
	}
	
	public TranslatedString getAlertHeaderTextTranslations() {
		return this.alertHeaderTextTranslations;
	}
	
	public String getAlertDescriptionText() {
		return this.alertDescriptionText;
	}
	
	public TranslatedString getAlertDescriptionTextTranslations() {
		return this.alertDescriptionTextTranslations;
	}

	public String getUrlText() {
		return this.Url;
	}
	
	public TranslatedString getAlertUrl() {
		return this.alertUrl;
	}
	
	public long getEffectiveStartDate() { 
		return this.effectiveStartDate;
	}
	
	public long getEffectiveEndDate() {
		return this.effectiveEndDate;
	}
	
	public Effect getEffect() {
		return this.effect;
	}
	
	public TimeRange getActivePeriod() {
		return this.activePeriod;
	}
	
	public EntitySelector getInformedEntity() {
		return this.informedEntity;
	}
	
	public String getCacheId() {
		return this.cacheId;
	}
}
