package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * Nested Offer class
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Offer details")
@Data
public class NestedOffer implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("id")
	public UUID id;

	@JsonProperty("offer_start")
	public String offerStart;

	@JsonProperty("offer_end")
	public String offerEnd;

	@JsonProperty("terms")
	public String terms;

	@JsonProperty("lithos_ref")
	public Long lithosRefId;

}
