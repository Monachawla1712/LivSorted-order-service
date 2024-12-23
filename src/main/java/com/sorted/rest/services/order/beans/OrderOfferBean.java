package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class OrderOfferBean implements Serializable {

	private static final long serialVersionUID = 2286068724312037897L;

	private Integer id;

	@NotNull
	@NotEmpty
	private String skuCode;

	@NotNull
	@DecimalMin(value = "0.001")
	private Double quantity;

	@NotNull
	private OrderConstants.DiscountType discountType;

	@NotNull
	@Min(1)
	private Double discountValue;

	@NotNull
	@Valid
	private OrderOfferMetadata metadata;

	private Date validTill;

	@NotNull
	@Min(0)
	private Double thresholdAmount;

	@NotNull
	private OrderConstants.ComparisonOperator operator;

	private Integer active;

	@NotNull
	private OrderConstants.Fact fact;

}
