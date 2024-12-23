package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sorted.rest.services.order.constants.OrderConstants.DiscountType;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * SKU Level Offer
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "SKU Level Offer")
@Data
public class SkuLevelOffer implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("skuCode")
	public String skuCode;

	@JsonProperty("discountType")
	public DiscountType discountType;

	@JsonProperty("discountAmount")
	public int discountValue;
}
