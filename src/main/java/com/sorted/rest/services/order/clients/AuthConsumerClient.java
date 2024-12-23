package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(value = "authConsumer", url = "${client.auth.url}", configuration = { FeignCustomConfiguration.class })
public interface AuthConsumerClient {

	@GetMapping(value = "/auth/internal/addresses/{addressId}")
	ConsumerAddressResponse getAddressById(@PathVariable Long addressId);

	@GetMapping(value = "/auth/internal/user/{customerId}")
	UserServiceResponse getUserDetailsFromCustomerId(@RequestHeader Map<String, Object> headers, @PathVariable String customerId);

	@GetMapping(value = "/auth/franchise-store/list/v2/internal")
	AuthServiceStoreDetailsBean getFranchiseStoresV2(@RequestParam(name = "userId") String userId);

	@GetMapping(value = "/auth/am-store-mapping/store/{storeId}")
	AmStoreDetailsResponse getStoreDetailsFromStoreId(@PathVariable String storeId);

	@PostMapping(value = "/auth/internal/addresses/bulk-fetch")
	List<ConsumerAddressResponse> getUserAddressesInternal(@RequestBody BulkConsumerAddressRequest bulkConsumerAddressRequest);

	@PostMapping(value = "/auth/internal/user/{customerId}")
	void updateUserOrderCount( @PathVariable UUID customerId, @RequestParam("orderCount") Long orderCount);

	@GetMapping(value = "/auth/internal/user/{customerId}/audience")
	List<UserAudienceBean> getUserAudience(@PathVariable UUID customerId);

	@PostMapping(value = "/auth/user-offers/onboarding-offer/bulk/disable")
	void disableOnboardingOffer(@RequestBody List<String> userIds);

	@PostMapping(value = "/auth/internal/user/first-order-flow/deactivate")
	void deactivateFirstOrderFlow(@RequestBody List<String> userIds);

	@PostMapping(value = "/auth/user/event")
	void sendUserEvent(@RequestParam("name") String name, @RequestBody UserEventRequest userEventRequest);

	@GetMapping(value = "/auth/internal/addresses/user/{userId}")
	List<UserAddressResponse> getUserAddressById(@PathVariable UUID userId, @RequestParam("active") boolean active);

	@PostMapping(value = "auth/internal/users")
	List<UserServiceResponse> getUserDetailsByIds(@RequestBody UserIdsRequest request);

}
