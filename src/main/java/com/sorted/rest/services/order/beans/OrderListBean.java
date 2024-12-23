package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sorted.rest.common.websupport.base.BaseBean;
import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;

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
@ApiModel(description = "Order Response Bean")
@Data
public class OrderListBean extends BaseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = " Order Id.", accessMode = AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "estimated Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double estimatedBillAmount;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	@NotNull
	private Double amountReceived;

	@ApiModelProperty(value = "item Count", allowEmptyValue = false)
	@NotNull
	private Integer itemCount;

	@ApiModelProperty(value = "Order Shipping Menthod", allowEmptyValue = false)
	@NotNull
	private ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Menthod", allowEmptyValue = false)
	@NotNull
	private PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderStatus status;

	@NotNull
	private PaymentDetail paymentDetail;

	@ApiModelProperty(value = "Order Delivery Address", allowEmptyValue = true)
	private Long deliveryAddress;

	@ApiModelProperty(value = "Order Submitted Date", allowEmptyValue = true)
	private Date submittedAt;

	@ApiModelProperty(value = "Order MetaData", allowEmptyValue = true)
	private OrderMetadata metadata = new OrderMetadata();

	public static OrderListBean newInstance() {
		return new OrderListBean();
	}

}
