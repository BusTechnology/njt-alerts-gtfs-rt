package net.mtabuscis.alerts.scraping;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.joestelmach.natty.*;

import net.mtabuscis.alerts.gtfs.GTFSLib;

import org.w3c.dom.Element;

import com.google.transit.realtime.GtfsRealtime.TranslatedString;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.Alert.Effect;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;

@Component
public class AlertBuilder {
	
	public static Map<String, String> RailLongNameToRouteId = new ConcurrentHashMap();
	
	private final String [] NJTAcronyms = {"BNTN", "MNBN", "MNE", "PASC", "RARV", "NEC", "NJCL"};
	
	@Autowired
	private GTFSLib gtfsLib;
	
	private Element eElement;
	private Alert alert;
	private String endpoint;
	
	public Alert buildAlert() {
		return alert;
	}
		
	public AlertBuilder init (Element elmt) {
		this.eElement = elmt;
		
		this.alert = new Alert();
		Alert.effectToKeywords.put(Effect.NO_SERVICE,	Alert.noService);
		Alert.effectToKeywords.put(Effect.SIGNIFICANT_DELAYS, Alert.significantDelays);
		Alert.effectToKeywords.put(Effect.DETOUR, Alert.detour);
		Alert.effectToKeywords.put(Effect.MODIFIED_SERVICE, Alert.modified);
		Alert.effectToKeywords.put(Effect.STOP_MOVED, Alert.stopMoved);	
		
/*
 * LIST MAY NOT BE FULL YET!! NO PRACTICAL WAY TO FINISH THE MAP WITHOUT SEEING ALERTS FOR THAT LINE
 * LOOK AT THE LOGS WHICH WILL INDICATE A MISSING ID AND 
 * USE THE selLine PARAMETER TO ASSOCIATE A LINE WITH ITS ROUTE ID	
 */
		//RailLongNameToRouteId.put("Atlantic City Rail Line", "NJT_1");
		RailLongNameToRouteId.put("MNBN", "NJT_2");
		RailLongNameToRouteId.put("BNTN", "NJT_3");
//		RailLongNameToRouteId.put("Hudson-Bergen Light Rail", "NJT_4");
//		RailLongNameToRouteId.put("Main/Bergen County Line", "NJT_5");
//		RailLongNameToRouteId.put("Port Jervis Line", "NJT_6");
		RailLongNameToRouteId.put("MNE", "NJT_7");
//		RailLongNameToRouteId.put("Gladstone Branch", "NJT_8");
		RailLongNameToRouteId.put("NEC", "NJT_9");
		RailLongNameToRouteId.put("NJCL", "NJT_10");
		//RailLongNameToRouteId.put("North Jersey Coast Line", "NJT_11");
//		RailLongNameToRouteId.put("Newark Light Rail", "NJT_12");
		RailLongNameToRouteId.put("PASC", "NJT_13");
//		RailLongNameToRouteId.put("Princeton Shuttle", "NJT_14");
		RailLongNameToRouteId.put("RARV", "NJT_15");
//		RailLongNameToRouteId.put("Riverline Light Rail", "NJT_16");
//		RailLongNameToRouteId.put("Meadowlands Rail Line", "NJT_17");
		
		
		return this;
	}
	
	/*
	 * ASSIGN ID TO AN ALERT:
	 * ID SHAPE: 
	 * For BUS: NJB:hashCodeOfXmlElementDefiningAlert
	 * For RAIL: NJT:hashCodeOfXmlElementDefiningAlert
	 */
	public AlertBuilder assignId(String endpoint) {
		this.endpoint = endpoint;
		
		if (endpoint != null) {
			if (endpoint.contains("Bus"))
				alert.setId("NJB:" + Integer.toString(this.eElement.hashCode()));
			else if (endpoint.contains("Rail")) {
				String link = getTagTextForElement("link");
				for (String acronym : NJTAcronyms)
					if(link.contains(acronym)) {
						alert.setRouteId(RailLongNameToRouteId.get(acronym));
						break;
					}
				if(alert.getRouteId() != null)
					alert.setId(alert.getRouteId().replace('_', ':') + Integer.toString(this.eElement.hashCode()));
				else 
					// Indicates a missing acronym in the map!!!! To be updated as new alerts come in!!!!
					System.out.println("No acronym corresponds to the selLine parameter in the link " + 
							link);
			}
		}
		
		return this;
	}
	
	/*
	 * ASSIGN HEADER TEXT TO AN ALERT
	 */
	public AlertBuilder assignHeaderText(String titleXmlTagName, TagContentParser parser) {
		if (this.endpoint != null && this.endpoint.contains("Rail")) {
			List<GtfsRelationalDaoImpl> daos = gtfsLib.getDao("NJT");
			if(daos != null)
				for(GtfsRelationalDaoImpl dao : daos) 
					for (Agency agency : dao.getAllAgencies()) 
						for (Route route: dao.getRoutesForAgency(agency)) 
							if(alert.getRouteId() != null && alert.getRouteId().equals(route.getId())) 
								alert.setAlertHeaderText(route.getLongName());
		}
		
		else {
			String title = getTagTextForElement(titleXmlTagName);
			String header = parser.parse(title);
			if(header != null) {
				alert.setAlertHeaderText(header);
				alert.setCacheId(title);
				alert.setRouteShortname(parser.parse(title).substring(parser.parse(title).indexOf(" ")+1));
			}
		}
	
		return this;
	}
	
	/*
	 * ASSIGN A TranslatedString from HeaderText to an alert(needed for GTFS-RT builder)
	 * Takes array of LanguageCodes. Add fields to LanguageCodes enum to extend support for other languages
	 */
	public AlertBuilder assignHeaderTextTranslations(LanguageCodes [] codes) {
		if(alert.getAlertHeaderText() != null) {
			TranslatedString.Builder trsBuilder = TranslatedString.newBuilder();
			Translation.Builder trBuilder = Translation.newBuilder();
		
			for (LanguageCodes code: codes) {
				trBuilder.setText(alert.getAlertHeaderText());
				trBuilder.setLanguage(code.getCode());
				trsBuilder.addTranslation(trBuilder.build());
				trBuilder.clear();
			}
			alert.setAlertHeaderTextTranslations(trsBuilder.build());
		}
		return this;
	}
	
	/*
	 * ASSIGN DESCRIPTION TEXT TO ALERT
	 * If there is a url that has more information about an alert, follow it and assign description from there
	 * Otherwise, take the description from the "description" tag
	 */
	public AlertBuilder assignDescriptionText(String descriptionXmlTagName, TagContentParser parser, String classOfAlertBodyHtml) {
		String link = alert.getUrlText();
		String description = null;
		if(link != null) {
			URL alertBodyLink = null;
			try {
				alertBodyLink = new URL (link);
			} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			HtmlScraperHelper htmlScraper = new HtmlScraperHelper();
			description = htmlScraper.getElementByClass(alertBodyLink, classOfAlertBodyHtml);
		}
		// if link does not bring new information, use the brief one from xml
		if (description == null) {
			description = getTagTextForElement(descriptionXmlTagName);
		}
		// assign description to alert
		if (description != null)
			alert.setAlertDescriptionText(parser.parse(description));
		return this;
		
	}
	/*
	 * ASSIGN A TranslatedString from DescriptionText to an alert(needed for GTFS-RT builder)
	 * Takes array of LanguageCodes. Add fields to LanguageCodes enum to extend support for other languages
	 */
	public AlertBuilder assignDescriptionTextTranslations(LanguageCodes [] codes) {
		if(alert.getAlertDescriptionText() != null) {
			TranslatedString.Builder trsBuilder = TranslatedString.newBuilder();
			Translation.Builder trBuilder = Translation.newBuilder();
			
			for (LanguageCodes code: codes) {
				trBuilder.setText(alert.getAlertDescriptionText());
				trBuilder.setLanguage(code.getCode());
				trsBuilder.addTranslation(trBuilder.build());
				trBuilder.clear();
			}
			alert.setAlertDescriptionTextTranslations(trsBuilder.build());
		}
		return this;
	}
	
	/*
	 * ASSIGN EFFECT TO AN ALERT
	 * Uses a utility inferEffectFromText method to do the inferring job
	 */
	public AlertBuilder assignEffect() {
		alert.setEffect(inferEffectFromText());
		return this;
	}
	/*
	 * ASSIGN A URL TO ALERT
	 * pass explicit null if you just want the whole content of the url tag returned
 	 * in the parser make sure to check the url given is a URL that brings some new information
	 * otherwise, return null
	 */
	public AlertBuilder assignUrl(String urlXmlTagName, TagContentParser parser, LanguageCodes[] codes) {
		if(!this.endpoint.contains("Rail")) {
			String url = getTagTextForElement(urlXmlTagName);
			// if parser was passed, apply it
			if (parser != null)
				url = parser.parse(url);
			//create a block for the translationString
			if (url != null) {
				alert.setUrlText(url);
				TranslatedString.Builder trsBuilder = TranslatedString.newBuilder();
				Translation.Builder trBuilder = Translation.newBuilder();
				
				for (LanguageCodes code: codes) {
					if(alert.getUrlText() != null) {
						trBuilder.setText(alert.getUrlText());
						trBuilder.setLanguage(code.getCode());
						trsBuilder.addTranslation(trBuilder.build());
						trBuilder.clear();
					}
				}	
				alert.setAlertUrl(trsBuilder.build());
			}
		}
		
		return this;
	}
	
	/*
	 * ASSIGNS ACTIVE PERIOD TO ALERT
	 */
	public AlertBuilder assignActivePeriod() {
		String description = alert.getAlertDescriptionText();
		// parse through the description and replaces some words by synonyms that parser understands
		if(description.contains("Effective immediately"))
			description = description.replace("Effective immediately", "Now");
		
		// Only look for dates within the first 200 characters of alert
		int endIndex = (description.length() > 200) ? 200 : description.length();
		int beginIndex = 0;
		
		// If there are these key words, no need to parse before that index
		if (description.indexOf("Beginning") != -1)
			beginIndex = description.indexOf("Beginning");
		else if (description.indexOf("Now") != -1)
			beginIndex = description.indexOf("Now");
		
		// the method that does the actual parsing and returns results as start and end POSIX epoch time 
		long [] times = extractDateFromText(description.substring(beginIndex, endIndex));
		
		TimeRange.Builder timeRangeB = TimeRange.newBuilder();
		
		if(times[0] != 0)
			timeRangeB.setStart(times[0]);
		if (times[1] != 0)
			timeRangeB.setEnd(times[1]);
		
		alert.setActivePeriod(timeRangeB.build());
		
		return this;
	}
	
	/*
	 * ASSIGNS INFORMED ENTITY TO ALERT
	 * CRUCIAL: WE TRAVERSE OVER THE DAO SINCE RouteShortnames ARE DIFFERENT THAN RouteIds for NJB
	 */
	public AlertBuilder assignInformedEntity() {
		if (alert.getRouteId() == null) {
			List<GtfsRelationalDaoImpl> daos = gtfsLib.getDao("NJB");
			if(daos != null)
				for(GtfsRelationalDaoImpl dao : daos) 
					for (Agency agency : dao.getAllAgencies())
						for (Route route: dao.getRoutesForAgency(agency))
							if(alert.getRouteShortname() != null && alert.getRouteShortname().equals(route.getShortName())) 
								alert.setRouteId(route.getId().toString());
		}
		
		if (alert.getRouteId() != null)  {
			EntitySelector.Builder entityBuilder = EntitySelector.newBuilder();
			TripDescriptor.Builder tripBuilder = TripDescriptor.newBuilder();
			tripBuilder.setRouteId(alert.getRouteId());
			entityBuilder.setTrip(tripBuilder);
			alert.setInformedEntity(entityBuilder.build());	
		}
		
		return this;
	}
	
	/*
	 * This method does the parsing with the use of NATTY library for NJT alert active period
	 */
	public long [] extractDateFromText(String text) {
		// parser is good but not intelligent, so I am removing words that make it fail from the input
		for (String word : Alert.wordsToHelpDateParser) {
			if(text.contains(word))
				text = text.replace(word, "");
			else if (text.contains(word.toLowerCase()))
				text = text.replace(word.toLowerCase(), "");
		}
		 Parser parser = new Parser();

			 
		 // getting all the date groups, only the first group in a list is interesting to us
		 List<DateGroup> groups = parser.parse(text);
		 List<Date> dates = null;
		 
		 // if any date group was found, lets convert it to  List of java.util.Date
		 if (groups.size() > 0) {
			 dates = groups.get(0).getDates();
			 // if there are at least 2 dates, the first 1st - begin date, 2nd - end date
			 if (dates.size() > 1) {
				 Calendar calendar = Calendar.getInstance();
				 calendar.setTime(dates.get(1));
				 // in case alert starts by "Effective immediately", it puts 
				 // the "now" timestamp on both dates automatically, which is not what we want
				 // the solution would be to check if there is anything but 0 in the seconds place
				 int secCount = calendar.get(Calendar.SECOND);
				 
				 // if there is something, it was copied from the start date. Manually reassign end date to 12am
				 if (secCount != 0 || secCount != 60) {
					 dates.remove(1);
					 calendar.set(Calendar.HOUR_OF_DAY, 0);
					 calendar.set(Calendar.MINUTE, 0);
					 calendar.set(Calendar.SECOND, 0);
					 dates.add(1, calendar.getTime());
				 }
				
				 alert.setEffectiveStartDate((int)(dates.get(0).getTime()/1000));
				 alert.setEffectiveEndDate((int)(dates.get(1).getTime()/1000));
				  
			 } else if (dates.size() == 1) {		// if only one date was found, it is the start date. End date is NONE, pass 0 to alert setter
				 alert.setEffectiveStartDate((int)(dates.get(0).getTime()/1000));
				 alert.setEffectiveEndDate(0);
			 }
		 }
		 
		 return new long [] {alert.getEffectiveStartDate(), alert.getEffectiveEndDate()};
	}
	
	private String getTagTextForElement(String tag) {
		return this.eElement.getElementsByTagName(tag).item(0).getTextContent();
	}
	
	/*
	 * TRIES TO GUESS THE EFFECT OF THE ALER FROM ITS DESCRIPTION
	 * RUNNING A LITTLE CONTEST FOR EFFECT KEYWORDS
	 * THE HIGHEST KEYWORD HIT COUNT WINS
	 */
	private Effect inferEffectFromText() { 
		Map<Effect,Integer> bestEffectGuess = new java.util.concurrent.ConcurrentHashMap();
		String description = alert.getAlertDescriptionText();
		Alert.effectToKeywords.forEach((k,v) -> {
			for (String keyword : v)
				if(description.contains(keyword)) {
					bestEffectGuess.computeIfAbsent(k, par -> {	return 0; });
					bestEffectGuess.put(k, bestEffectGuess.get(k)+1);
				}
		});
		
		Effect bestEffect = null;
		int max = 0;
		
		for (Entry entry : bestEffectGuess.entrySet()) {
			if((Integer)entry.getValue() > max)
				bestEffect = (Effect) entry.getKey();
			else if ((Integer)entry.getValue() == max)
				return Effect.UNKNOWN_EFFECT;	
		}
		if (bestEffect == null)
			bestEffect = Effect.UNKNOWN_EFFECT;
		return bestEffect;
	}
}
