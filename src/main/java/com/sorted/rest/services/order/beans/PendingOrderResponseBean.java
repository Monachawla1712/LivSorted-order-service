package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Bean to be returned with Contains Orders
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class PendingOrderResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = " Order Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "Order Shipping Method", allowEmptyValue = false)
	@NotNull
	private OrderConstants.ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Method", allowEmptyValue = false)
	@NotNull
	private OrderConstants.PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private OrderConstants.OrderStatus status;

	@ApiModelProperty(value = "Order MetaData", allowEmptyValue = true)
	private OrderMetadata metadata;

	@ApiModelProperty(value = "Order Submitted Date", allowEmptyValue = true)
	private Date submittedAt;

	public static PendingOrderResponseBean newInstance() {
		return new PendingOrderResponseBean();
	}

}
