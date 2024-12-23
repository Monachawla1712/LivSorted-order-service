package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class NotificationServiceEmailRequest {
	Map<String, String> fillers;

	private UUID userId;

	private String templateName;

	private String emailId;
}
