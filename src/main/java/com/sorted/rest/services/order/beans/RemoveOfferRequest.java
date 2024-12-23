package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Request Bean of Apply Offer
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Remove Offer Request Bean")
public class RemoveOfferRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	public UUID customerId;

	private Date deliveryDate;

}
