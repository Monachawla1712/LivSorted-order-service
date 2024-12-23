package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderAdjustmentUserDetails implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String id;

	private String name;

	private String emailAddress;

}