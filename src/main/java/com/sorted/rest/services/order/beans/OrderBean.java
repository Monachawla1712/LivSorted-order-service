package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

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
@ApiModel(description = "Order Bean")
@Data
public class OrderBean extends BaseBean implements Serializable {
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

	@ApiModelProperty(value = "Store Device ID", allowEmptyValue = false)
	@NotNull
	private String storeDeviceId;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total Discount Amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "total Tax Amount", allowEmptyValue = false)
	@NotNull
	private Double totalTaxAmount;

	@ApiModelProperty(value = "total Extra Fee Amount", allowEmptyValue = false)
	@NotNull
	private Double totalExtraFeeAmount;

	@ApiModelProperty(value = "refund Amount", allowEmptyValue = false)
	@NotNull
	private Double refundAmount;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	@NotNull
	private Double amountReceived;

	@ApiModelProperty(value = "Tax Details", allowEmptyValue = false)
	@NotNull
	private TaxDetails taxDetails;

	@ApiModelProperty(value = "total Additional Discount", allowEmptyValue = false)
	@NotNull
	private AdditionalDiscount totalAdditionalDiscount;

	@ApiModelProperty(value = "extra Fee Details", allowEmptyValue = false)
	@NotNull
	private ExtraFeeDetail extraFeeDetails;

	@ApiModelProperty(value = "item Count", allowEmptyValue = false)
	@NotNull
	private Integer itemCount;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	@NotNull
	private String channel;

	@ApiModelProperty(value = "Order Shipping Menthod", allowEmptyValue = false)
	@NotNull
	private ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Menthod", allowEmptyValue = false)
	@NotNull
	private PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	private List<OrderItemBean> orderItems;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderStatus status;

	@ApiModelProperty(value = "Order Delivery Address", allowEmptyValue = true)
	private Long deliveryAddress;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	public static OrderBean newInstance() {
		return new OrderBean();
	}
}
