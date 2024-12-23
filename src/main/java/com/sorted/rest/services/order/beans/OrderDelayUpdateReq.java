package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderDelayUpdateReq {

	List<UUID> orderIds;

	String deliveryDelayTime;

	String deliveryDelayReason;
}

