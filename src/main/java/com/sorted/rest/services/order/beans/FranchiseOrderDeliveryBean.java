package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FranchiseOrderDeliveryBean implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String completedAt;

	private List<String> urls;
}

