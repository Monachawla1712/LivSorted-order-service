package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class DiscountedOrderOfferResponse {

	private List<OrderOfferResponseBean> freeComboOffers;

	private String offerTitle;

	private String offerName;

	private String description;

	private Boolean showBottomSheet = Boolean.FALSE;

	private String ctaText;
}
