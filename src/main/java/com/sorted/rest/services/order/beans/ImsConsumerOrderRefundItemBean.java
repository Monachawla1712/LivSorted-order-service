package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ImsConsumerOrderRefundItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@NotEmpty
	private String skuCode;

	@NotNull
	private Double refundQuantity;
}