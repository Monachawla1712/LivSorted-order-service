package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeliveryDetails implements Serializable {

	private static final long serialVersionUID = 7644595743589360269L;

	private String arrivedAt;

	private String deliveredAt;
}
