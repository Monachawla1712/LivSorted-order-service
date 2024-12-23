package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Refund Order Response Bean")
@Data
public class FranchiseOrderRefundBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = "Parent Order Id", allowEmptyValue = false)
	@NotNull
	private String returnIssue;

	@ApiModelProperty(value = "Parent Order Id", allowEmptyValue = false)
	@NotNull
	private UUID parentOrderId;

	@ApiModelProperty(value = "Franchise Order Items", allowEmptyValue = false)
	@NotEmpty
	@Valid
	private List<FranchiseRefundOrderItemBean> refundOrderItems;

	public static FranchiseOrderRefundBean newInstance() {
		return new FranchiseOrderRefundBean();
	}
}