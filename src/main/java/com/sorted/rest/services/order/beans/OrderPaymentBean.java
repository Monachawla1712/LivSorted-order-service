package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * Bean to be returned with Contains Orders
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Response Bean")
@Data
public class OrderPaymentBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = " Order Id.", accessMode = AccessMode.READ_ONLY)
	@NotNull
	private UUID id;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	@NotNull
	private Double amountReceived;

	@NotNull
	private PaymentDetail paymentDetail;


	public static OrderPaymentBean newInstance() {
		return new OrderPaymentBean();
	}

}
