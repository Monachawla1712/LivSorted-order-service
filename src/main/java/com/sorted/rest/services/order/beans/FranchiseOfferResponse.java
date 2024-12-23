package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class FranchiseOfferResponse implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@JsonProperty("voucher")
	private String voucher;

	@JsonProperty("offerType")
	private String offerType;

	@JsonProperty("offerId")
	private String offerId;

	@JsonProperty("offerTitle")
	private String offerTitle;

	@JsonProperty("orderLevel")
	public FranchiseOrderLevelOffer orderLevel;

	@JsonProperty("skuLevel")
	public List<FranchiseSkuLevelOffer> skuLevel;

	@JsonProperty("cashbackDetails")
	public CashbackDetails cashbackDetails;
	@Data
	@NoArgsConstructor
	public static class FranchiseOrderLevelOffer {
		@JsonProperty("discountValue")
		private Double discountValue;
	}
	@Data
	@NoArgsConstructor
	public static class CashbackDetails implements Serializable {
		@JsonProperty("cashbackPercent")
		private Double cashbackPercent;

		@JsonProperty("cashbackAmount")
		private Double cashbackAmount;
	}

	@Data
	@NoArgsConstructor
	public static class FranchiseSkuLevelOffer implements Serializable {

		private static final long serialVersionUID = 2102504245219017738L;

		@JsonProperty("skuCode")
		private String skuCode;

		@JsonProperty("discountValue")
		private Double discountValue;

	}
}