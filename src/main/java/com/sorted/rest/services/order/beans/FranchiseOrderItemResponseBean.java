package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@ApiModel(description = "Franchise Order Item Response Bean")
@Data
public class FranchiseOrderItemResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Order Item Id", allowEmptyValue = false)
	@NotNull
	private UUID id;

	@ApiModelProperty(value = "Order Item's Order Id", allowEmptyValue = false)
	@NotNull
	private UUID orderId;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	@NotNull
	private String skuCode;

	@ApiModelProperty(value = "Order Item MOQ", allowEmptyValue = false)
	@NotNull
	private Double moq;

	@ApiModelProperty(value = "Warehouse Id", allowEmptyValue = false)
	@NotNull
	private Integer whId;

	@ApiModelProperty(value = "Order Item product name", allowEmptyValue = false)
	@NotNull
	private String productName;

	@ApiModelProperty(value = "Order Item sku image url", allowEmptyValue = false)
	@NotNull
	private String imageUrl;

	@ApiModelProperty(value = "Order Item category name", allowEmptyValue = false)
	@NotNull
	private String categoryName;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Double orderedQty;

	@ApiModelProperty(value = "Order Item final quantity", allowEmptyValue = false)
	@NotNull
	private Double finalQuantity;

	@ApiModelProperty(value = "Order Item sale price per unit", allowEmptyValue = false)
	@NotNull
	private Double salePrice;

	@ApiModelProperty(value = "Order Item marked price per unit", allowEmptyValue = false)
	@NotNull
	private BigDecimal markedPrice;

	@ApiModelProperty(value = "Order Item is refundable flag", allowEmptyValue = false)
	@NotNull
	private Integer isRefundable;

	@ApiModelProperty(value = "Order Item is returnable flag", allowEmptyValue = false)
	@NotNull
	private Integer isReturnable;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	private Double mrpGrossAmount = 0.0;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	private Double spGrossAmount = 0.0;

	@ApiModelProperty(value = "final amount", allowEmptyValue = false)
	private Double finalAmount = 0.0;

	@ApiModelProperty(value = "Order Item status", allowEmptyValue = false)
	@NotNull
	private FranchiseOrderConstants.FranchiseOrderItemStatus status;

	@ApiModelProperty(value = "Ordered Crate qty", allowEmptyValue = false)
	@NotNull
	private Integer orderedCrateQty;

	@ApiModelProperty(value = "Final Crate qty", allowEmptyValue = false)
	@NotNull
	private Integer finalCrateQty;

	@ApiModelProperty(value = "Uom", allowEmptyValue = false)
	private String uom;

	@ApiModelProperty(value = "estimated amount", allowEmptyValue = true)
	private Double marginDiscountPercent;

	@ApiModelProperty(value = "hsn", allowEmptyValue = true)
	private String hsn;

	@ApiModelProperty(value = "prorata amount", allowEmptyValue = true)
	private Double prorataAmount;

	@ApiModelProperty(value = "prorata amount", allowEmptyValue = true)
	private FranchiseRefundDetails refundDetails;

	@ApiModelProperty(value = "Order Item error message", allowEmptyValue = false)
	@NotNull
	private ErrorBean error;

	@ApiModelProperty(value = "Offer Discount Amount", allowEmptyValue = true)
	private BigDecimal offerDiscountAmount;

	private FranchiseOrderItemMetadata metadata;

}
