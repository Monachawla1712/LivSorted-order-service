package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WmsOrderPayload {

	private String orderId;

	private String displayOrderId;

	private int storeId;

	private String customerId;

	private String deliveryDate;

	private String slot;

	private String notes;

	private Address address;

	private OrderContactDetail contactDetails;

	private List<WmsOrderItem> items;

	private Integer orderCount;

	private Boolean isVip;

	private String appVersion;

	private Boolean isCod;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Address {

		private String street;

		private String city;

		private String state;

		private String pincode;

		private String landmark;

		private String addressLine1;

		private String addressLine2;

		private String latitude;

		private String longitude;

		private Integer societyId;

		private String society;

		private String floor;

		private String house;

		private String sector;

		private String tower;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class WmsOrderItem {

		private String id;

		private String skuCode;

		private String uom;

		private String productName;

		private String productImage;

		private List<CartRequest.OrderItemGradeBean> grades;

		private double qty;

		private Integer pieces;

		private String suffix;

		private Double perPcWeight;

		private String notes;

		private Integer orderCount;

		private String packetDescription;

		private Boolean isOzoneWashedItem;
	}
}

