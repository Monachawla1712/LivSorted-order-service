package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class PosStoreReport {
	private List<StoreOrderTotalInfoResponse> storeOrderInfo;
	private List<SkuOrderTotalInfoResponse> skuOrderInfo;
	private Double monthlySales;
	private Double weeklySales;
	private Double todaySales;
}
