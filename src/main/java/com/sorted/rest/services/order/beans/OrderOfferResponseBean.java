package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.persistence.Column;
import java.util.List;

@Data
public class OrderOfferResponseBean {

	private String skuCode;

	private String productName;

	private String image;

	private String uom;

	private Double quantity;

	private Double amount;

	private Double activeAmount;

	private Double actualAmount;

	private Double actualProductSalePrice;

	private OrderOfferMetadata metadata;

	private Boolean isItemInCart = false;

}
