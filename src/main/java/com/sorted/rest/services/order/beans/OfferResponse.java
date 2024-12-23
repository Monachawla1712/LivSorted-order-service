package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Bean to be returned from Offers client
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Offer Response Bean")
@Data
public class OfferResponse implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("offerDetails")
	public OfferDetails offerDetails;

	@JsonProperty("orderLevel")
	public OrderLevelOffer orderLevel;

	@JsonProperty("skuLevel")
	public List<SkuLevelOffer> skuLevel;
}
