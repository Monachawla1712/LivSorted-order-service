package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Back Office Order Request Bean
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Back Office Order Request Bean")
@Data
public class BackofficeOrderBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	private String channel;

	private Date deliveryDate;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Order Delivery Address", allowEmptyValue = true)
	@NotNull
	private Long deliveryAddress;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	@ApiModelProperty(value = "Order MetaData", allowEmptyValue = true)
	@Valid
	private OrderMetadata metadata;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@Valid
	private List<BackofficeOrderItemBean> orderItems;

	@ApiModelProperty(value = "total Discount Amount", allowEmptyValue = false)
	private OfferData offerData;

	@ApiModelProperty(value = "slot Id", allowEmptyValue = false)
	private Integer slotId;

	public static BackofficeOrderBean newInstance() {
		return new BackofficeOrderBean();
	}

}
