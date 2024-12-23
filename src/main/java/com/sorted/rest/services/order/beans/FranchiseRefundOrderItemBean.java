package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(description = "Franchise Refund Order Item Response Bean")
@Data
public class FranchiseRefundOrderItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	@NotEmpty
	private String skuCode;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Double refundQuantity;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Integer whId;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Double moq;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	private Boolean warehouseReturnCheck;

	@ApiModelProperty(value = "Refund Item Remarks", allowEmptyValue = false)
	private String refundRemarks;
}