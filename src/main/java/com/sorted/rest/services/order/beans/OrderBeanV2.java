package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class OrderBeanV2 implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private UUID customerId;

	private Integer slotId;

	public static OrderBeanV2 newInstance() {
		return new OrderBeanV2();
	}
}
