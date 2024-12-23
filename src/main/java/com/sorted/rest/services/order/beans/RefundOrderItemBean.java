package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Back Office Order Item Bean
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Refund Order Item Response Bean")
@Data
public class RefundOrderItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	@NotEmpty
	private String skuCode;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Double quantity;

}