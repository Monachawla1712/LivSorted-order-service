package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuOrderTotalInfo {

	private String skuCode;

	private Long orderCount;

	private Double totalSale;
}
