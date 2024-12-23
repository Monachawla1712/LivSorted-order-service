package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Back Office Order Request Bean
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Refund Order Request Bean")
@Data
public class RefundOrderBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID parentOrderId;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@Valid
	private List<RefundOrderItemBean> refundOrderItems;

	public static RefundOrderBean newInstance() {
		return new RefundOrderBean();
	}

}