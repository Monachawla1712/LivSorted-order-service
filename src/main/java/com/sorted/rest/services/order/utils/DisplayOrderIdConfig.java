package com.sorted.rest.services.order.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sorted.rest.services.order.services.DisplayOrderIdService;

@Configuration
public class DisplayOrderIdConfig {

	@Autowired
	private DisplayOrderIdService displayOrderIdService;

	@Bean
	public void displayOrderIdQueue() {
		displayOrderIdService.fetchBulkAndInactive();
	}
}
