package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.UUID;

@Data
public class FranchiseOrderMetadataUpdateRequest implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	@NotEmpty
	private UUID orderId;

	private FranchiseOrderDeliveryBean deliveryDetails;
}

