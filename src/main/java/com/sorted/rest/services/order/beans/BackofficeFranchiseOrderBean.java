package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Back Office Franchise Order Request Bean")
@Data
public class BackofficeFranchiseOrderBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = "Store Id order", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "Sr Create ", allowEmptyValue = false)
	@NotNull
	private Integer isSrRequired;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@Valid
	private List<BackofficeFranchiseCartRequest> orderItems;

	public static BackofficeFranchiseOrderBean newInstance() {
		return new BackofficeFranchiseOrderBean();
	}

}
