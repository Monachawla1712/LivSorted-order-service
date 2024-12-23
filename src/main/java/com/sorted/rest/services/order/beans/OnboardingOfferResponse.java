package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.OfferType;
import lombok.Data;

@Data
public class OnboardingOfferResponse {

	private String voucherCode;

	private OfferType offerType;

	private Date offerExpiry;

	private List<OnboardingOfferSku> skus;

	private OfferConstraint constraint;

	private OrderConstants.DiscountType discountType;

	private double amount;

	private List<String> terms;

	private Boolean isOnboardingOfferValid;

	@Data
	public static class OnboardingOfferSku implements Serializable {

		private static final long serialVersionUID = 317074467412220389L;

		private String skuCode;

		private Double quantity;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class OfferConstraint implements Serializable {

		private static final long serialVersionUID = -8299821986639181679L;

		private Double minRechargeAmount;

		private Double minCartValue;

		private int offerValidDays;

		private Double maxDiscount;
	}
}