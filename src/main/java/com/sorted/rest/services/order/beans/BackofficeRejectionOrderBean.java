package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.RejectionOrderConstants.RejectionOrderType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@ApiModel(description = "Backoffice Rejection Order Request Bean")
public class BackofficeRejectionOrderBean implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@ApiModelProperty(value = "Store Id order", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "Order Type", allowEmptyValue = false)
	@NotNull
	private RejectionOrderType orderType;

	@ApiModelProperty(value = "Store Id order", allowEmptyValue = false)
	@NotNull
	private Integer whId;

	@ApiModelProperty(value = "Store Id order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@Valid
	private List<BackofficeRejectionOrderRequest> orderItems;

	public static BackofficeFranchiseOrderBean newInstance() {
		return new BackofficeFranchiseOrderBean();
	}
}
