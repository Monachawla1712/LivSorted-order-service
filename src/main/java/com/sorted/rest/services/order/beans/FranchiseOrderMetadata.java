package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FranchiseOrderMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private BigDecimal orderPlacedAmount = null;

	private String remarks;

	private List<FranchiseOrderDeliveryBean> deliveryDetails;

	private Boolean isEligiblePreAutoRemoval;
}