package net.mtabuscis.alerts.spring;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.mtabuscis.alerts.scraping.Alert;

@Configuration
@ComponentScan(basePackages = {
		"net.mtabuscis.alerts.impl",		
		"net.mtabuscis.alerts.gtfs",
		"net.mtabuscis.alerts.spring",
		"net.mtabuscis.alerts.scraping"
		})
@EnableAutoConfiguration
@EnableCaching
@EnableScheduling

public class Application {
	
	public static void main(String [] args)
	{
		//TODO move this out to a properties file or configuration server
    	System.setProperty("user.timezone", "America/New_York");
    	System.setProperty("spring.profiles.active", "default");
    	
    	//configure logging, once again, move out to file / central server
    	System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "info" );
    	System.setProperty( "org.slf4j.simpleLogger.logFile", "System.out" );
    	System.setProperty( "org.slf4j.simpleLogger.showShortLogName", "true" );
    	System.setProperty( "org.slf4j.simpleLogger.showDateTime", "true" );
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SZ" );
    	System.setProperty( "org.slf4j.simpleLogger.dateTimeFormat", simpleDateFormat.toPattern() );
    	
    	SpringApplication.run(Application.class, args);		
	}
	
}

