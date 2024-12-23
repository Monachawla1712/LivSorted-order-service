package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "Update Franchise Store Cart Request Bean")
public class UpdateFranchiseCartRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String storeId;

	private Double walletAmount;

}
