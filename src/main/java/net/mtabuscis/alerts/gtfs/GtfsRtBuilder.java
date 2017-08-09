package net.mtabuscis.alerts.gtfs;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;

import net.mtabuscis.alerts.scraping.Alert;

/*
 * This class creates a feed entity from the corresponding fields of Alert type
 *
 * Null will be returned IF and ONLY IF the alert object does not have an ID
 */

public class GtfsRtBuilder {
	
	FeedEntity feedEnt = null;
	public FeedEntity buildGTFSRTAlert(Alert alert){
		
		com.google.transit.realtime.GtfsRealtime.Alert.Builder alertBuilder = 
								com.google.transit.realtime.GtfsRealtime.Alert.newBuilder();

		FeedEntity.Builder entity = FeedEntity.newBuilder();
		
		if(alert.getId() != null) {
			entity.setId(alert.getId());
		}
		if(alert.getAlertDescriptionText() != null) 
			alertBuilder.setDescriptionText(alert.getAlertDescriptionTextTranslations());
		
		if(alert.getAlertHeaderTextTranslations() != null) 
			alertBuilder.setHeaderText(alert.getAlertHeaderTextTranslations());
			
		if(alert.getActivePeriod() != null)
			alertBuilder.addActivePeriod(alert.getActivePeriod());
			
		if(alert.getInformedEntity() != null) 
			alertBuilder.addInformedEntity(alert.getInformedEntity());
			
		if(alert.getEffect() != null) 
			alertBuilder.setEffect(alert.getEffect());
			
		if(alert.getAlertUrl() != null) 
			alertBuilder.setUrl(alert.getAlertUrl());
		
		entity.setAlert(alertBuilder.build());
		return entity.build();
	}
}
