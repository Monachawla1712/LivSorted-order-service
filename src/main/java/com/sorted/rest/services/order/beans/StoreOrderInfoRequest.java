package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class StoreOrderInfoRequest {

	private List<String> storeIds;
}
