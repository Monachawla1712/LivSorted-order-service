package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EligibleOffersResponse implements Serializable {

	private static final long serialVersionUID = 5632851684565861551L;

	private List<CashbackOfferSku> skus;

	private Double minCartValue;

	private List<String> terms;

	private String imageUrl;

	private Date expiry;

	private String offerName;

	private Boolean treeGame;

	private Boolean freeTomatoes;

	private OnboardingOfferResponse onboardingOffer;

	@Data
	public static class CashbackOfferSku implements Serializable {

		private static final long serialVersionUID = 7836190247898779539L;

		private String skuCode;

		private Double quantity;
	}

}
