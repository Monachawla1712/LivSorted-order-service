package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.math.BigDecimal;

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
public class OfferClientOrderItemBean implements Serializable {

	public OfferClientOrderItemBean(OrderItemEntity orderItemObject) {
		this.totalMrpGrossAmount = orderItemObject.getMrpGrossAmount();
		this.skuCode = orderItemObject.getSkuCode();
		this.productName = orderItemObject.getProductName();
		this.orderedQty = orderItemObject.getOrderedQty();
		this.categoryName = orderItemObject.getCategoryName();
		this.categoryId = orderItemObject.getCategoryId();
		this.finalQuantity = orderItemObject.getFinalQuantity();
		this.salePrice = orderItemObject.getSalePrice().doubleValue();
		this.markedPrice = orderItemObject.getMarkedPrice();
		this.mrpGrossAmount = orderItemObject.getMrpGrossAmount();
		this.spGrossAmount = orderItemObject.getSpGrossAmount();
		this.finalAmount = orderItemObject.getFinalAmount();
	}

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "total mrp gross amount")
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "Order Item sku code")
	private String skuCode;

	@ApiModelProperty(value = "Order Item product name")
	private String productName;

	@ApiModelProperty(value = "Order Item category Id")
	private Integer categoryId;

	@ApiModelProperty(value = "Order Item category name")
	private String categoryName;

	@ApiModelProperty(value = "Order Item ordered quantity")
	private Double orderedQty;

	@ApiModelProperty(value = "Order Item final quantity")
	private Double finalQuantity;

	@ApiModelProperty(value = "Order Item sale price per unit")
	private Double salePrice;

	@ApiModelProperty(value = "Order Item marked price per unit")
	private BigDecimal markedPrice;

	@ApiModelProperty(value = "Order Item mrp gross amount")
	private Double mrpGrossAmount;

	@ApiModelProperty(value = "Order Item sp gross amount")
	private Double spGrossAmount;

	@ApiModelProperty(value = "Order Item final amount")
	private Double finalAmount;

}
