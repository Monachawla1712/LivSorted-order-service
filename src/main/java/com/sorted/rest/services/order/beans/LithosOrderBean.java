package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Lithos Order Request Bean
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Lithos Order Request Bean")
@Data
public class LithosOrderBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = " Order Id.")
	private UUID id;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Lithos Order ID", allowEmptyValue = false)
	private Long lithosOrderId;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "Order Shipping Menthod", allowEmptyValue = false)
	@NotNull
	private ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Menthod", allowEmptyValue = false)
	@NotNull
	private PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderStatus status;

	@ApiModelProperty(value = "Order Delivery Address", allowEmptyValue = true)
	private Long deliveryAddress;

	@ApiModelProperty(value = "Order Submitted Date", allowEmptyValue = true)
	private Date submittedAt;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total Discount Amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	@NotNull
	private String channel;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	@Valid
	private List<LithosOrderItemBean> orderItems;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	@ApiModelProperty(value = "Order MetaData", allowEmptyValue = true)
	private OrderMetadata metadata;

	@ApiModelProperty(value = "Offer Data", allowEmptyValue = true)
	private OfferData offerData;

	@ApiModelProperty(value = "Payment Data", allowEmptyValue = true)
	private PaymentNotifyBean payment;

	public static LithosOrderBean newInstance() {
		return new LithosOrderBean();
	}

}
