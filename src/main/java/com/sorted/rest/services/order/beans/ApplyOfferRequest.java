package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Request Bean of Apply Offer
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Apply Offer Request Bean")
public class ApplyOfferRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	public String voucherCode;

	public UUID customerId;

	public Double discountAmount;

	private Date deliveryDate;

}
