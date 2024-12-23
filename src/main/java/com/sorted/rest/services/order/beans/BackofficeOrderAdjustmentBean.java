package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "Backoffice Franchise Store Cart Request Bean")
public class BackofficeOrderAdjustmentBean implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@ApiModelProperty(value = "Adjustments List", allowEmptyValue = false)
	@Valid
	private List<BackofficeOrderAdjustmentRequest> adjustments;

	public static BackofficeOrderAdjustmentBean newInstance() {
		return new BackofficeOrderAdjustmentBean();
	}

}
