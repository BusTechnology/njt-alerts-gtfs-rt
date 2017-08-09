package net.mtabuscis.alerts.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import net.mtabuscis.alerts.impl.UpdateService;

/*
 * This class defines 2 endpoints for alert feeds: for the NJ Buses and NJ Rails(NJT)
 */
@Controller
public class DataServer {
	
	@Autowired
	private UpdateService data;
	
	@RequestMapping(value = "/alerts/njb")
	public void getNjbAlertFeed(HttpServletResponse response) throws IOException {
		data.getAlertBusFeed().writeTo(response.getOutputStream());
	}
	
	@RequestMapping(value = "/alerts/njt")
	public void getNjtAlertFeed(HttpServletResponse response) throws IOException {
		data.getAlertRailFeed().writeTo(response.getOutputStream());
	}
}
