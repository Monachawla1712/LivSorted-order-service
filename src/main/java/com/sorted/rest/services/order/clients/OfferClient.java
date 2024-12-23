package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "offer", url = "${client.offer.url}", configuration = { FeignCustomConfiguration.class })
public interface OfferClient {
	@PostMapping(value = "/offers/internal/validate")
	OfferResponse getOfferResponse(@RequestBody OfferClientApplyOfferRequest request);

	@PostMapping(value = "/offers/internal/franchise/validate")
	FranchiseOfferResponse getFranchiseOfferResponse(@RequestBody OfferClientApplyFranchiseOfferRequest request);

	@PostMapping(value = "/offers/internal/franchise/fetch/auto-apply")
	FranchiseAutoApplyOfferResponse getFranchiseAutoApplyCouponCode(@RequestBody OfferClientApplyFranchiseOfferRequest request);
}