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

@Data
@ApiModel(description = "Backoffice Rejection Order Request Bean")
public class BackofficeRejectionOrderPricingBean implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@ApiModelProperty(value = "Order Id order", allowEmptyValue = false)
	@NotNull
	private UUID orderId;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotEmpty
	@Valid
	private List<BackofficeRejectionOrderPricingRequest> skuPriceList;

	public static BackofficeFranchiseOrderBean newInstance() {
		return new BackofficeFranchiseOrderBean();
	}
}
