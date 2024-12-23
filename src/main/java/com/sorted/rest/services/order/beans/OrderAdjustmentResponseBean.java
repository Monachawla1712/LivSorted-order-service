package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "Franchise Order Response Bean extending the List Bean")
@Data
public class OrderAdjustmentResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Long id;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "total item count", allowEmptyValue = false)
	@NotNull
	private Double amount;

	@ApiModelProperty(value = "total final bill amount", allowEmptyValue = false)
	@NotNull
	private FranchiseOrderConstants.OrderAdjustmentTransactionMode txnMode;

	@ApiModelProperty(value = "total discount amount", allowEmptyValue = false)
	@NotNull
	private String txnType;

	@ApiModelProperty(value = "total tax amount", allowEmptyValue = false)
	@NotNull
	private String remarks;

	@ApiModelProperty(value = "total extra fee amount", allowEmptyValue = false)
	@NotNull
	private FranchiseOrderConstants.OrderAdjustmentStatus status;

	@ApiModelProperty(value = "requester id", allowEmptyValue = false)
	private UUID requestedBy;

	@ApiModelProperty(value = "approver id", allowEmptyValue = false)
	private UUID approvedBy;

	@ApiModelProperty(value = "Order Adjustment Details", allowEmptyValue = false)
	private OrderAdjustmentDetails adjustmentDetails = new OrderAdjustmentDetails();

	public static OrderAdjustmentResponseBean newInstance() {
		return new OrderAdjustmentResponseBean();
	}
}
