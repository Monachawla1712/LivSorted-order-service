package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Bean to be returned Containing Order Item
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Item Response Bean")
@Data
public class PosOrderItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	private String skuCode;

	@ApiModelProperty(value = "Order Item final quantity", allowEmptyValue = false)
	@NotNull
	private Double quantity;

	@ApiModelProperty(value = "Order Item discount", allowEmptyValue = true)
	private Double discountAmount;
}