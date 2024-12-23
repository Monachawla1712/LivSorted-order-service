package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Rejection Order Bean")
@Data
public class RejectionOrderResponseBean extends RejectionOrderListBean {

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total item count", allowEmptyValue = false)
	@NotNull
	private Integer itemCount;

	@ApiModelProperty(value = "total final bill amount", allowEmptyValue = false)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "total discount amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "total tax amount", allowEmptyValue = false)
	@NotNull
	private Double totalTaxAmount;

	@ApiModelProperty(value = "amount received ", allowEmptyValue = false)
	@NotNull
	private Double amountReceived;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	private List<RejectionOrderItemResponseBean> orderItems;

	@ApiModelProperty(value = "estimated amount", allowEmptyValue = true)
	private Double estimatedBillAmount;

	public static RejectionOrderResponseBean newInstance() {
		return new RejectionOrderResponseBean();
	}
}
