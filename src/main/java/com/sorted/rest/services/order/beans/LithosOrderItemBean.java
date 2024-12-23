package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Bean to be returned Containing Order Item
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Item Response Bean")
@Data
public class LithosOrderItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	private String skuCode;

	@ApiModelProperty(value = "Order Item uom", allowEmptyValue = false)
	private String uom;

	@ApiModelProperty(value = "Order Item final quantity", allowEmptyValue = false)
	@NotNull
	private Double finalQuantity;

	@ApiModelProperty(value = "Order Item sale price per unit", allowEmptyValue = false)
	@NotNull
	private BigDecimal salePrice;

	@NotNull
	private Double spGrossAmount;

	@NotNull
	private Double finalAmount;

	private OrderItemMetadata metadata;

	@ApiModelProperty(value = "Order Item discount", allowEmptyValue = true)
	private Double discountAmount;
}