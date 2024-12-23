package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Bean to be Containing Order Products update request
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Product Update Request Bean")
@Data
public class OrderItemUpdateBean implements Serializable {

	private static final long serialVersionUID = 2780508087326367416L;

	private UUID orderItemId;

	@NotNull
	private String skuCode;

	@NotNull
	@Min(value = 0)
	private Double quantity;

	public OrderItemUpdateBean(String skuCode, Double quantity) {
		this.setSkuCode(skuCode);
		this.setQuantity(quantity);
	}
}
