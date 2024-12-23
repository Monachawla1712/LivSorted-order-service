package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.UUID;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;

import lombok.Data;

@Data
public class UpdateOrderStatusBean implements Serializable {

	private static final long serialVersionUID = -4770623927816940840L;

	private UUID orderId;

	private OrderStatus status;

	private String arrivedAt;

	private String deliveredAt;

	private String remarks;
}
