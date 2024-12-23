package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendNotificationsResponse implements Serializable {

	private static final long serialVersionUID = 5540219994460380044L;

	private Boolean success;

	private String message;

	private Object failedRequests;
}
