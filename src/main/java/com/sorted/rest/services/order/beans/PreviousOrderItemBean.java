package com.sorted.rest.services.order.beans;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviousOrderItemBean {

	private String skuCode;

	private String productName;

	private String uom;

	private String imageUrl;

	private BigDecimal finalQuantity;

	private Integer pieces;

	private List<CartRequest.OrderItemGradeBean> grades;

	private Date deliveryDate;

	private String categoryName;

	private Boolean isOzoneWashedItem;
}
