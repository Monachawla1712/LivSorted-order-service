package com.sorted.rest.services.order.clients;

import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import java.util.List;
import java.util.UUID;

@FeignClient(value = "storeInventory", url = "${client.storeInvetory.url}", configuration = { FeignCustomConfiguration.class })
public interface StoreInventoryClient {

	@GetMapping(value = "/store-app/internal/inventory/store/{storeId}")
	StoreInventoryResponse getStoreInventory(@PathVariable String storeId, @RequestParam String sku, @RequestParam UUID userId,
			@RequestParam String appVersion);

	@PostMapping(value = "/store-app/internal/inventory/store/{storeId}/verifyAndDeduct")
	StoreInventoryUpdateResponse verifyAndDeductStoreInventory(@PathVariable String storeId, @RequestBody StoreInventoryUpdateRequest request);

	@PostMapping(value = "/store-app/internal/inventory/store/{storeId}")
	StoreInventoryUpdateResponse addOrDeductStoreInventory(@PathVariable String storeId, @RequestBody StoreInventoryAddOrDeductRequest request);

	@PutMapping(value = "/store-app/products/per-pcs-wt")
	void updatePerPcsWeight(@RequestBody List<SkuPerPcsWeightDto> dtos);
}