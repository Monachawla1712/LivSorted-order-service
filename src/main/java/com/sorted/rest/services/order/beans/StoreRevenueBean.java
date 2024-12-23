package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreRevenueBean {

	private String storeId;

	private Double revenue;

}
