package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.entity.OrderItemEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Order Object for Offer Client
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Order Object for Offer Client")
@Data
public class OfferClientOrderBean implements Serializable {
	public OfferClientOrderBean(OrderEntity orderObject){
		this.storeId = orderObject.getStoreId();
		this.channel = orderObject.getChannel();
		this.amountReceived = orderObject.getAmountReceived();
		List<OfferClientOrderItemBean> orderList  = new java.util.ArrayList<>(Collections.emptyList());
		for(OrderItemEntity item : orderObject.getOrderItems()){
			orderList.add(new OfferClientOrderItemBean(item)); //= orderObject.getOrderItems();
		}
		this.orderItems = orderList;
		this.totalMrpGrossAmount = orderObject.getTotalMrpGrossAmount();
		this.totalSpGrossAmount = orderObject.getTotalSpGrossAmount();
		this.finalBillAmount = orderObject.getFinalBillAmount();
		this.amountReceived = orderObject.getAmountReceived();
		this.paymentMethod = orderObject.getPaymentMethod();
		this.shippingMethod = orderObject.getShippingMethod();
	}

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	private String storeId;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	private Double finalBillAmount;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	private Double amountReceived;

	@ApiModelProperty(value = "item Count", allowEmptyValue = false)
	private Integer itemCount;

	@ApiModelProperty(value = "order count", allowEmptyValue = true)
	private Long orderCount;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	private String channel;

	@ApiModelProperty(value = "Order Shipping Method", allowEmptyValue = false)
	private ShippingMethod shippingMethod;

	@ApiModelProperty(value = "Order Payment Method", allowEmptyValue = false)
	private PaymentMethod paymentMethod;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	private List<OfferClientOrderItemBean> orderItems;
}
