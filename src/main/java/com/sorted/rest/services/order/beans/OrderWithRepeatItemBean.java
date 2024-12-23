package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class OrderWithRepeatItemBean {

	private UUID id;

	private UUID customerId;

	private Double finalBillAmount;

	private OrderMetadata metadata;

	public OrderWithRepeatItemBean(UUID id, UUID customerId, Double finalBillAmount, Object metadata) {
		this.id = id;
		this.customerId = customerId;
		this.finalBillAmount = finalBillAmount;
		this.metadata = (OrderMetadata) metadata;
	}

}
