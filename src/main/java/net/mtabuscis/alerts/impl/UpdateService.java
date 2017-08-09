package net.mtabuscis.alerts.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeConstants;

import net.mtabuscis.alerts.gtfs.GTFSLib;
import net.mtabuscis.alerts.gtfs.GtfsRtBuilder;
import net.mtabuscis.alerts.scraping.Alert;
import net.mtabuscis.alerts.scraping.AlertBuilder;
import net.mtabuscis.alerts.scraping.LanguageCodes;


@Component
public class UpdateService {

	private static Logger _log = LoggerFactory.getLogger(UpdateService.class);

	@Autowired
	private GTFSLib _gtfsLib;
	
	@Autowired
	private StorageService cache;
	
	@Autowired
	private AlertBuilder alertBuilder;
	
	private FeedMessage alertBusFeed = null;
	private FeedMessage alertRailFeed = null;
	
	//path to the folder with alerts.ini in it (and an otp folder to get the gtfs zip file)
	@Value("${S3BUCKET}")
	private String _S3BUCKET;

	@Value("${S3DIR}")
	private String _S3DIR;

	//needs access to S3 and Route53
	@Value("${AWS_ACCESS_KEY_ID}")
	private String AWS_ACCESS_KEY_ID;

	@Value("${AWS_SECRET_ACCESS_KEY}")
	private String AWS_SECRET_ACCESS_KEY;

	private HashMap<String, String> _feeds = new HashMap<String, String>(50);
	private HashMap<String, String> _gtfsFiles = new HashMap<String, String>(50);
	private HashMap<String, Long> _modificationDates = new HashMap<String, Long>(51);

	private boolean _ready = false;
	
	@Value("${NJB_URL}")
	private String njb_endpoint;
	
	@Value("${NJT_URL}")
	private String njt_endpoint;
	
	@PostConstruct
	public  void init() {
		System.setProperty("AWS_ACCESS_KEY_ID", AWS_ACCESS_KEY_ID);
		System.setProperty("AWS_SECRET_ACCESS_KEY", AWS_SECRET_ACCESS_KEY);
	}

	@Scheduled(initialDelay=0, fixedDelay=900000)//start immediately then delay to 15 minutes
	public void update(){
		
		AmazonS3 s3 = AmazonS3Client.builder().withRegion("us-east-1").build();
		
		//check for alerts.ini file and update if necessary
		pullINI(s3);

		for(Entry<String, String> ge : _gtfsFiles.entrySet()){
			String[] files = ge.getValue().split(",");

			if(files.length < 1){
				_log.info("No files defined for feed "+String.valueOf(ge.getKey()));
				continue;
			}

			boolean upd = false;
			ArrayList<String> updateFiles = new ArrayList<String>();//handle many files , separated for same feed id
			for(String file : files){
				S3Object s3gtfsfile = s3.getObject(_S3BUCKET, _S3DIR+"/otp/"+file);
				if(s3gtfsfile == null){
					_log.error(String.valueOf(ge.getValue())+" wasn't found in bucket "+String.valueOf(_S3BUCKET)+" and dir: "+String.valueOf(_S3DIR)+"/otp/");
					continue;
				}

				updateFiles.add(file);//just incase a different file needs to be updated in the same feed id

				long s3gtfsfilelastmod = s3gtfsfile.getObjectMetadata().getLastModified().getTime();
				try {
					_log.warn("You may safely ignore the following message, this is normal. 'Not all bytes were read from the S3ObjectInputStream'... ");
					s3gtfsfile.close();//This will result in a not all bytes read from the stream, which is fine
				} catch (IOException e) {
					_log.error("Error closing s3 file "+file);
					e.printStackTrace();
				}
				if(_modificationDates.get(file) != null){
					if(_modificationDates.get(file) >= s3gtfsfilelastmod){
						_log.info("No modification required for file "+String.valueOf(file));
						continue;
					}
				}
				_modificationDates.put(file, s3gtfsfilelastmod);//always update the file if we're going to run an update here because it's hard to do this further downstream
				upd = true;
			}
			if(updateFiles.size() > 0 && upd){
				//update any files required
				try {
					_gtfsLib.load(ge.getKey(), updateFiles, _S3BUCKET, _S3DIR+"/otp/", s3);
				} catch (IOException e) {
					_log.error("Unable to load feed "+ge.getKey());
					e.printStackTrace();
					System.gc();//close any files that are open
				}
			}
		}
		
		_ready = true;
		

	}

	private void pullINI(AmazonS3 s3){
		
		S3Object o = s3.getObject(_S3BUCKET, _S3DIR+"/"+"alerts.ini");
		if(o == null){
			_log.error("alerts.ini wasn't found in bucket "+String.valueOf(_S3BUCKET)+" and dir: "+String.valueOf(_S3DIR));
			return;
		}
		long lastModified = o.getObjectMetadata().getLastModified().getTime();
		if(_modificationDates.get("alerts.ini") == null){
			updateINI(o);
		}else{
			if(lastModified <= _modificationDates.get("alerts.ini")){
				_log.info("RTPosApp.ini updater found no changes on AWS S3, no updates necessary.  Currently Loaded date:"+
						String.valueOf(_modificationDates.get("alerts.ini"))+" File on S3 Date:"+String.valueOf(lastModified));
			}else{
				updateINI(o);
			}
		}
		try {
			o.close();
		} catch (IOException e1) {
			_log.error("Error closing s3 file alerts.ini");
			e1.printStackTrace();
		}
	}

	private void updateINI(S3Object o){
		_modificationDates.put("alerts.ini", o.getObjectMetadata().getLastModified().getTime());
		S3ObjectInputStream s3is = o.getObjectContent();
		Ini conf = new Ini();
		try {
			conf.load(s3is);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for(String key : conf.keySet()){
			for(String k : conf.get(key).keySet()){
				if(key.contains("GTFSFiles")){
					_log.info("Feed "+String.valueOf(k)+" will pull from file(s) "+String.valueOf(conf.get(key, k)));
					_gtfsFiles.put(k, conf.get(key, k));
				}else if(key.contains("AlertFeeds")){
					//just update the alert feeds blindly, this doesn't matter as much
					_log.info("Feed "+String.valueOf(k)+" will pull vehicle positions from feed url "+String.valueOf(conf.get(key, k)));
					_feeds.put(k, conf.get(key, k));
				}
			}
		}

		try {
			s3is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 *  Handles the update of alerts: it starts up in 2 minutes from initial server startup
	 *   updates the alert FeedMessage every 2 minutes after that
	 *   
	 *   NOTE: DO NOT CHANGE THE INITIAL DELAY TO 0. IT IS NEEDED FOR GTFSLib TO HAVE THE DATA NEEDED
	 */
	@Scheduled(initialDelay=120000, fixedDelay = 120000)
	public void updateAlerts(){

		_log.info("Starting updates...");

		if(!_ready){
			_log.error("GTFS Not ready yet continuing to wait on that first... ");
			return;
		}

		Logger log = LoggerFactory.getLogger(UpdateService.class);

		URL link;
		URLConnection conn;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		String [] endpoints = {njb_endpoint, njt_endpoint};
		
		// Go retrieve XML Feeds for Buses and Rail from
		for(String endpoint: endpoints) {
			try {
				link = new URL(endpoint);
				conn = link.openConnection();
				builder = factory.newDocumentBuilder();
				doc = builder.parse(conn.getInputStream());
			} catch (MalformedURLException e) {
				log.error("URL to access the alerts is malfourmed. Closing...");
				return ;
			} catch (IOException e) {
				log.error("URL does not lead to a valid file. Closing...");
				return;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return;
			} catch (SAXException e) {
				e.printStackTrace();
				return;
			}
			
			// All alerts lie within "item" tags, only tag we care about
			NodeList nList = doc.getElementsByTagName("item");
			
			List<Alert> alerts = new LinkedList();
			GtfsRtBuilder gtfsRtBuilder = new GtfsRtBuilder();
	
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
	
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					
					Element eElement = ((Element)nNode);
					
					// Use the title's tag content as material for cache - has a timestamp, so is unique
					String cacheIdString = eElement.getElementsByTagName("title").item(0).getTextContent();
					
					// only look for bus alerts and specific Rail Alerts(some rail alerts are meaningless and unparsable)
					if(endpoint.equals(njb_endpoint) || eElement.getElementsByTagName("link").item(0).getTextContent().contains("RailTab")) {
						Alert alert = cache.retrieveAlert(cacheIdString); // look if alert is already in cache; if yes, skip expensive processing
						// alert is not in cache, let's build it
						if (alert == null) {
							alert = 
								alertBuilder
									.init(eElement)
									.assignId(endpoint)
									// pass a lambda function of how you want to extract the title
									.assignHeaderText(
											"title", 
											title -> {
												if(endpoint.equals(njb_endpoint)) {
													int startInd = title.indexOf("Bus ") + 4;
													for (int j = startInd; j < title.length(); j++)
														if(!Character.isDigit(title.charAt(j))) 
															return "Bus " + title.substring(startInd, j);
												} 
												return null;
											})
									.assignHeaderTextTranslations(new LanguageCodes[]{LanguageCodes.English})
									// pass a lambda function of how to check for a URL that has more info for alert
									.assignUrl(
											"link", 
											url -> {
												if (endpoint.equals(njb_endpoint)) {
													if (url.contains("BusTab"))
														return null;
													return url;
												} else if (endpoint.equals(njt_endpoint)) {
													if (!url.contains("RailTab"))
														return null;
													return url;
												}
												return null;
											},
											new LanguageCodes[]{LanguageCodes.English}
									)
									.assignDescriptionText(
										"description", 
										description -> {return description;}, 
										"advBody"
									)
									.assignDescriptionTextTranslations(new LanguageCodes[]{LanguageCodes.English})
									.assignEffect()
									.assignActivePeriod()
									.assignInformedEntity()
									.buildAlert();	
							// ONLY ADD ALERTS WITH NON-NULL IDS!!!!! 
							// Look at the logs, which will say if id is missing
							if(alert.getId() != null)
								cache.cacheInMemory(alert);
						}

						alerts.add(alert);
					}
				}
			}
			// Build the FeedMessage from entities
			FeedMessage.Builder feedMessageBuilder = FeedMessage.newBuilder();
			// for every alert create a GTFS RT FeedEntity and add it to the FeedMessage
			for (Alert alert: alerts) 
				feedMessageBuilder.addEntity(gtfsRtBuilder.buildGTFSRTAlert(alert));
			// FeedMessage must have a header
			FeedHeader.Builder header = feedMessageBuilder.getHeaderBuilder();
			header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
			header.setTimestamp(System.currentTimeMillis()/ 1000);
			
			if (endpoint.equals(njb_endpoint)) {
				this.alertBusFeed = feedMessageBuilder.build();
				System.out.println(alertBusFeed);
			} else if (endpoint.equals(njt_endpoint)) {
				this.alertRailFeed = feedMessageBuilder.build();
				System.out.println(alertRailFeed);
			}
		}
	}
	
	public FeedMessage getAlertBusFeed() {
		return this.alertBusFeed;
	}
	
	public FeedMessage getAlertRailFeed() {
		return this.alertRailFeed;
	}
}
