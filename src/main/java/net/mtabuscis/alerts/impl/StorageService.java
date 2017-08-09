package net.mtabuscis.alerts.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.mtabuscis.alerts.scraping.Alert;

/*
 * This class does the handles the caching of alerts
 */
@Component
public class StorageService {
	private static Logger _log = LoggerFactory.getLogger(StorageService.class);
	
	private static final long EVICTION_NUMBER_OF_HOURS = 2; //get rid of an alert after 2 hours  
	private static final long MAX_SIZE = 10000; // No more than 10000 alerts in cache are allowed
	
	@Autowired
	// Initialize alert cache
	private Cache<String, Alert> alertCache;
	
	@Bean
	public Cache<String, Alert> init() {
		Cache<String, Alert> _cacheBean = Caffeine.newBuilder()
				.expireAfterWrite(EVICTION_NUMBER_OF_HOURS, TimeUnit.HOURS)
				.maximumSize(MAX_SIZE)
				.build();
				
		return _cacheBean;
	}
		
	
	public void cacheInMemory (Alert alert) {
		if(alert.getCacheId() != null) {
			alertCache.put(alert.getCacheId(), alert);
			System.out.println("Alert cached for " + alert.getRouteId());
		}
	}
	
	public Alert retrieveAlert(String titleWithTimestamp) {
		Alert alert = alertCache.getIfPresent(titleWithTimestamp);
		if(alert == null) {
			System.out.println("Alert for " + titleWithTimestamp + " is not in cache");
		} else {
			System.out.println("Alert for routeId " + alert.getRouteId() + " is retrieved");
		}
		
		return alert;
	}
	
}
