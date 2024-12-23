package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.Location;
import com.sorted.rest.services.order.beans.StoreDataResponse;
import com.sorted.rest.services.order.beans.StoreSearchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "store", url = "${client.store.url}", configuration = { FeignCustomConfiguration.class })
public interface StoreClient {

	@GetMapping(value = "/store-app/internal/store")
	List<StoreDataResponse> getStoreDataFromId(@RequestParam(name = "store_id") String storeId);

	@PostMapping(value = "/store-app/internal/stores")
	List<StoreDataResponse> getStoresData(@RequestBody StoreSearchRequest request);

	@PostMapping(value = "/store-app/inventory/price-update")
	void refreshStorePricing();
}