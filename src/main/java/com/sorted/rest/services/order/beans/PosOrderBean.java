package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * POS Order Request Bean
 *
 * @author Abhishek Dogra
 * @version $Id: $Id
 */
@ApiModel(description = "Pos Order Request Bean")
@Data
public class PosOrderBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = " Order Id.")
	private UUID id;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "Order Delivery Address", allowEmptyValue = true)
	private Long deliveryAddress;

	@ApiModelProperty(value = "Order Shipping Menthod", allowEmptyValue = false)
	@NotNull
	private ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Menthod", allowEmptyValue = false)
	@NotNull
	private PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderStatus status;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	@NotNull
	private String channel;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	@Valid
	private List<PosOrderItemBean> orderItems;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	@ApiModelProperty(value = "Order MetaData", allowEmptyValue = true)
	private OrderMetadata metadata;

	@ApiModelProperty(value = "Offer Data", allowEmptyValue = true)
	private OfferData offerData;

	@ApiModelProperty(value = "Payment Data", allowEmptyValue = true)
	private PaymentNotifyBean payment;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	private Double amountReceived;

	@ApiModelProperty(value = "order discount amount", allowEmptyValue = true)
	private Double orderDiscountAmount;

	public static PosOrderBean newInstance() {
		return new PosOrderBean();
	}

}
