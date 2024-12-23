package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Bean to be returned from Offers client
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Offer Response Bean")
@Data
public class FranchiseAutoApplyOfferResponse implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("voucherCode")
	private String voucherCode;


}