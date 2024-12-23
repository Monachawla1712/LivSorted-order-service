package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Request Bean to PlaceOrder Customer cart
 *
 * @author mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Customer Cart Place Order Request Bean")
@Data
public class PlaceOrderRequest implements Serializable {

	private static final long serialVersionUID = -2261408883952313126L;

	private UUID customerId;

	private OrderConstants.OrderStatus orderStatus = OrderConstants.OrderStatus.ORDER_DELIVERED;

	private String channel;

	private Date deliveryDate;

	@NotNull
	private ShippingMethod shippingMethod;

	@NotNull
	private PaymentMethod paymentMethod = PaymentMethod.WALLET;

}
