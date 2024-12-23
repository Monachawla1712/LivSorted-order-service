package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Order Object for Offer Client")
@Data
public class OfferClientFranchiseOrderBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	public OfferClientFranchiseOrderBean(FranchiseOrderEntity orderObject) {
		this.storeId = orderObject.getStoreId();
		this.displayOrderId = orderObject.getDisplayOrderId();
		this.totalMrpGrossAmount = orderObject.getTotalMrpGrossAmount();
		this.totalSpGrossAmount = orderObject.getTotalSpGrossAmount();
		this.finalBillAmount = orderObject.getFinalBillAmount();
		this.itemCount = orderObject.getItemCount();
		this.status = orderObject.getStatus();
		List<OfferClientFranchiseOrderItemBean> orderList = new java.util.ArrayList<>(Collections.emptyList());
		for (FranchiseOrderItemEntity item : orderObject.getOrderItems()) {
			orderList.add(new OfferClientFranchiseOrderItemBean(item));
		}
		this.orderItems = orderList;
	}

	private String storeId;

	private String displayOrderId;

	private Double totalMrpGrossAmount;

	private Double totalSpGrossAmount;

	private Double finalBillAmount;

	private Integer itemCount;

	private FranchiseOrderConstants.FranchiseOrderStatus status;

	private Long orderCount;

	private Date firstOrderDate;

	private Double spGrossAmountWithoutBulkSkus;

	private List<OfferClientFranchiseOrderItemBean> orderItems;

	@Data
	public static class OfferClientFranchiseOrderItemBean implements Serializable {

		public OfferClientFranchiseOrderItemBean(FranchiseOrderItemEntity orderItem) {
			this.skuCode = orderItem.getSkuCode();
			this.categoryName = orderItem.getCategoryName();
			this.finalQuantity = orderItem.getFinalQuantity();
			this.salePrice = orderItem.getSalePrice();
			this.markedPrice = orderItem.getMarkedPrice();
			this.mrpGrossAmount = orderItem.getMrpGrossAmount();
			this.spGrossAmount = orderItem.getSpGrossAmount();
			this.finalAmount = orderItem.getFinalAmount();
			this.orderedCrateQty = orderItem.getOrderedCrateQty();
			this.finalCrateQty = orderItem.getFinalCrateQty();
			this.cratesPicked = orderItem.getCratesPicked();
			this.weightPicked = orderItem.getWeightPicked();
		}

		private static final long serialVersionUID = 2102504245219017738L;

		private String skuCode;

		private String categoryName;

		private Double finalQuantity;

		private BigDecimal salePrice;

		private BigDecimal markedPrice;

		private Double mrpGrossAmount;

		private Double spGrossAmount;

		private Double finalAmount;

		private Integer orderedCrateQty;

		private Integer finalCrateQty;

		private Integer cratesPicked;

		private Double weightPicked;
	}

}