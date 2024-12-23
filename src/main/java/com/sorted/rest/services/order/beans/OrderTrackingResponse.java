package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import lombok.Data;

/**
 * Bean to be returned with Contains Orders
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class OrderTrackingResponse implements Serializable {

	private static final long serialVersionUID = 3277966018852198461L;

	@ApiModelProperty(value = " Order Id.", accessMode = AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "estimated Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double estimatedBillAmount;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderStatus status;

	@ApiModelProperty(value = "Order Submitted Date", allowEmptyValue = true)
	private Date submittedAt;

	@ApiModelProperty(value = "Payment Details", allowEmptyValue = true)
	private PaymentDetail paymentDetail;

	public static OrderTrackingResponse newInstance() {
		return new OrderTrackingResponse();
	}

}
