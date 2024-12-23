package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Offer details class
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Offer details")
@Data
public class OfferDetails implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("offer")
	public NestedOffer offer;

}
