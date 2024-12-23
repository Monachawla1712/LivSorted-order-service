package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CustomerCartInfo {

	private UUID customerId;

	private Double currentCartAmount;

	private Integer active;

	public CustomerCartInfo(UUID customerId, Double currentCartAmount, Integer active) {
		this.customerId = customerId;
		this.currentCartAmount = currentCartAmount;
		this.active = active;
	}

}
