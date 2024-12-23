package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sorted.rest.common.beans.ErrorBean;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Response Bean to be returned with Customer cart related information
 *
 * @author mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Customer Cart Request Bean")
@Data
public class CartResponse implements Serializable {

	private static final long serialVersionUID = 6369074118582415131L;

	private OrderResponseBean data;

	private List<OrderOfferResponseBean> orderOffers;

	private String scratchCardItem;

	private DiscountedOrderOfferResponse discountedOrderOffer;

	private Boolean isAutoCheckoutEnabled;

	private String amountRemainingMsg;

	/**
	 * true implies successful execution
	 * 
	 */
	private boolean status;

	@JsonInclude(Include.NON_NULL)
	private String message;

	@JsonInclude(Include.NON_NULL)
	private ErrorBean error;

	public CartResponse error(String error, String errorMessage) {
		this.error = new ErrorBean(error, errorMessage);
		this.message = errorMessage;
		this.status = false;
		return this;
	}

	public CartResponse error(AlertErrorBean error) {
		this.error = error;
		this.status = false;
		return this;
	}

	public void setData(OrderResponseBean data) {
		this.data = data;
		this.status = true;
	}
}
