package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderTotalInfo {

	private String storeId;

	private Long orderCount;

	private Double totalSale;
}
