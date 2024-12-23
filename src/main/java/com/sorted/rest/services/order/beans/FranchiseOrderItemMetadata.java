package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FranchiseOrderItemMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private Integer deliveryNumber = null;

	private Integer startOrderQty;

	private List<PartnerContent> partnerContents;

	public static FranchiseOrderItemMetadata newInstance() {
		return new FranchiseOrderItemMetadata();
	}
}