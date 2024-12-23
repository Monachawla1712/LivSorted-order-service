package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sorted.rest.services.order.constants.OrderConstants.DiscountType;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Order Level Offer
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Order Level Offer")
@Data
public class OrderLevelOffer implements Serializable {
	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("discountType")
	public DiscountType discountType;

	@JsonProperty("discountValue")
	public Double discountValue;

	@JsonProperty("maxDiscountAmount")
	public Double maxDiscountAmount;
}
