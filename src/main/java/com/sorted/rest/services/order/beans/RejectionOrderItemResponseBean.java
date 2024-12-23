package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.beans.ErrorBean;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@ApiModel(description = "Rejection Order Item Bean")
@Data
public class RejectionOrderItemResponseBean {

	@ApiModelProperty(value = "Order Item Id", allowEmptyValue = false)
	@NotNull
	private UUID id;

	@ApiModelProperty(value = "Order Item's Order Id", allowEmptyValue = false)
	@NotNull
	private UUID orderId;

	@ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
	@NotNull
	private String skuCode;

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

	@ApiModelProperty(value = "Uom", allowEmptyValue = false)
	private String uom;

	@ApiModelProperty(value = "hsn", allowEmptyValue = true)
	private String hsn;

	@ApiModelProperty(value = "Order Item error message", allowEmptyValue = false)
	@NotNull
	private ErrorBean error;

}
