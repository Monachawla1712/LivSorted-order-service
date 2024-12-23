package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
public class FranchiseStoreInventoryResponse implements Serializable {

	private static final long serialVersionUID = -1365034136062032242L;

	private Integer whId;

	private String skuCode;

	private String name;

	private String scaleSkuCode;

	private Double moq;

	private String image;

	private String category;

	private String unit_of_measurement;

	private Double markedPrice;

	private Double salePrice;

	private Integer quantity;

	private Double sortedRetailPrice;

	private Double marginDiscount;

	private Double permissibleRefundQuantity;

	private String hsn;

	private Integer maxOrderQty;

	private List<PriceBracketsResponseBean> priceBrackets;

	private Integer startOrderQty;

	private Integer actualQty;

	@Data
	@NoArgsConstructor
	public static class PriceBracketsResponseBean {

		public Double max;

		public Double min;

		public Double salePrice;

		public Double markedPrice;

	}

}
