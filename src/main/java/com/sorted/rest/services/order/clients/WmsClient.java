package com.sorted.rest.services.order.clients;

import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FeignClient(value = "warehouse", url = "${client.wms.url}")
public interface WmsClient {

	@PostMapping(value = "/api/v1/ppd/order/{orderId}/lithos")
	Boolean updateLithosOrderAndStatusToPPD(@RequestHeader Map<String, Object> headers, @PathVariable String orderId, @RequestBody PpdLithosUpdateBean payload);

	@GetMapping(value = "/api/v1/catalog/store/{storeId}/sku/{skuCode}")
	FranchiseStoreInventoryResponse getFranchiseStoreInventory(@RequestHeader Map<String, Object> headers, @PathVariable Integer storeId,
			@PathVariable String skuCode);

	@GetMapping(value = "/api/v1/catalog/bulk-request/store-sku")
	List<FranchiseStoreInventoryResponse> getStoreSkuInventoryForBulkRequest(@RequestHeader Map<String, Object> headers, @RequestParam Set<String> skuCodes,
			@RequestParam String storeId);

	@PostMapping(value = "/api/v1/catalog/wh/{whId}/sku/{skuCode}/addOrDeduct")
	FranchiseStoreInventoryUpdateResponse addOrDeductFranchiseStoreInventory(@RequestHeader Map<String, Object> headers, @PathVariable Integer whId,
			@PathVariable String skuCode, @RequestBody FranchiseStoreInventoryAddOrDeductRequest request);

	@PostMapping(value = "/api/v1/catalog/bulk-request/wh-sku/deduct")
	FranchiseStoreInventoryUpdateResponse deductFranchiseStoreInventoryForBulkRequest(@RequestHeader Map<String, Object> headers,
			List<FranchiseCartRequest> orderItems);

	@PostMapping(value = "/api/v1/stores/requisitions/franchise/save")
	FranchiseStoreInventoryUpdateResponse updateFranchiseStoreRequisitions(@RequestHeader Map<String, Object> headers, @RequestBody FranchiseSRRequest request);

	@GetMapping(value = "/api/v1/store/{storeId}")
	StoreResponse fetchWmsStoreDetails(@RequestHeader Map<String, Object> headers, @PathVariable String storeId);

	@GetMapping(value = "/api/v1/stores")
	List<StoreResponse> fetchStoresDetails(@RequestHeader Map<String, Object> headers, @RequestParam Set<Integer> ids);

	@PostMapping(value = "/api/v1/inventory/warehouse/{whId}/bulk-request")
	ResponseEntity<List<WarehouseInventoryResponseBean>> fetchWarehouseInventoryDetails(@RequestHeader Map<String, Object> headers, @PathVariable Integer whId,
			@RequestBody Set<String> skuCodes);

	@PostMapping(value = "/api/v1/inventory/verify-update-rejected")
	void deductRejectionInventory(@RequestHeader Map<String, Object> headers, DeductWhRejectionRequest deductRejectionInventoryBean);

	@PutMapping(value = "api/v1/stores/requisitions/cancel/order/{orderId}")
	void cancelSR(@RequestHeader Map<String, Object> headers, @PathVariable UUID orderId);

	@GetMapping(value = "api/v1/masterSkus")
	List<MasterSkuBean> getMasterSkus(@RequestHeader Map<String, Object> headers, @RequestParam Set<String> skuCodes);

	@GetMapping(value = "/api/v1/catalog/bulk-request")
	List<FranchiseStoreInventoryResponse> getFranchiseStoreInventoryForAllSkus(@RequestHeader Map<String, Object> headers);

	@GetMapping(value = "/api/v1/stores/secondary-picking/view")
	List<StoreSecPickingViewBean> getStoreSecPickingView(@RequestHeader Map<String, Object> headers);

	@GetMapping(value = "/api/v1/ppd/society/{id}")
	SocietyListItemBean getSocietyInformationFromId(@RequestHeader Map<String, Object> headers, @PathVariable Integer id);

	@PostMapping(value = "/api/v1/ppd/order/create")
	void pushWmsConsumerOrder(@RequestHeader Map<String, Object> headers, WmsOrderPayload wmsOrderPayload);

//	@PostMapping(value = "/api/v1/ppd/fulfilment/create")
//	void createFulfilment(@RequestHeader Map<String, Object> headers);

	@PostMapping(value = "/api/v1/dark-store/requisitions/create")
	void sendOrdersToWms(@RequestHeader Map<String, Object> headers, List<WmsDsPayload> wmsDsPayloads);
}
