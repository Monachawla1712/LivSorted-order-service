package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class OfferData implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String offerId;
	private String voucherCode;
	private Boolean isOfferApplied;

	private Boolean hasAutoApplicableVoucher;
	private String offerType;
	private Double amount;
	private Double orderDiscount;
	private String offerTitle;

	public List<FranchiseOfferResponse.FranchiseSkuLevelOffer> itemOfferAmounts;

	public FranchiseOfferResponse.CashbackDetails cashbackDetails;

	private UUID appliedBy;

	private UUID removedBy;

	private String appliedAt;

	private String removedAt;

	private List<String> terms;

	private OnboardingOfferResponse.OfferConstraint constraint;

	private OrderConstants.DiscountType discountType;

	private String couponLabel;

	public OfferData() {
		offerId = null;
		voucherCode = null;
		isOfferApplied = false;
		amount = null;
		orderDiscount = null;
		offerType = null;
		offerTitle = null;
	}

}