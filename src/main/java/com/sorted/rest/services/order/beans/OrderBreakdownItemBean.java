package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBreakdownItemBean {
	private String identifier;
	private String name;
	private Double totalSales;

	public OrderBreakdownItemBean(String identifier, PaymentMethod paymentMethod, Double totalSales) {
		this.identifier = identifier;
		this.name = paymentMethod.toString();
		this.totalSales = totalSales;
	}
}
