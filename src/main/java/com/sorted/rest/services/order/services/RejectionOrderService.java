package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import com.sorted.rest.services.order.constants.RejectionOrderConstants.WhInventoryUpdateType;
import com.sorted.rest.services.order.constants.RejectionOrderConstants.RejectionOrderType;
import com.sorted.rest.services.order.entity.RejectionOrderEntity;
import com.sorted.rest.services.order.entity.RejectionOrderItemEntity;
import com.sorted.rest.services.order.repository.RejectionOrderItemRepository;
import com.sorted.rest.services.order.repository.RejectionOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RejectionOrderService implements BaseService<RejectionOrderEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(RejectionOrderService.class);

	@Autowired
	ClientService clientService;

	@Autowired
	RejectionOrderRepository rejectionOrderRepository;

	@Autowired
	RejectionOrderItemRepository rejectionOrderItemRepository;

	@Autowired
	RejectionOrderPricingService rejectionOrderPricingService;

	@Autowired
	DisplayOrderIdService displayOrderIdService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	public RejectionOrderEntity findRejectionOrderById(UUID orderId) {
		Optional<RejectionOrderEntity> resultOpt = rejectionOrderRepository.findById(orderId);
		return resultOpt.orElse(null);
	}

	public UUID getCustomerId() {
		UUID customerId = SessionUtils.getAuthUserId();
		Assert.notNull(customerId, "CustomerId could not be empty");
		return customerId;
	}

	@Override
	public Class<RejectionOrderEntity> getEntity() {
		return RejectionOrderEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return rejectionOrderRepository;
	}

	public void cancelRejectionOrder(RejectionOrderEntity rejectionOrder) {
		_LOGGER.debug(String.format("cancelRejectionOrder::orderId: %s", rejectionOrder.getDisplayOrderId()));
		rejectionOrder.setAmountReceived(0d);
		rejectionOrder.setStatus(RejectionOrderConstants.RejectionOrderStatus.CANCELLED);
		rejectionOrderRepository.save(rejectionOrder);
	}

	public PageAndSortResult<RejectionOrderEntity> findRejectionOrdersByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<RejectionOrderEntity> rejectionOrders;
		try {
			rejectionOrders = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return rejectionOrders;
	}

	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	@Transactional
	public RejectionOrderEntity createRejectionOrderWithQuantities(BackofficeRejectionOrderBean backofficeRejectionOrderRequest) {
		Set<String> skuSet = backofficeRejectionOrderRequest.getOrderItems().stream().map(BackofficeRejectionOrderRequest::getSkuCode)
				.collect(Collectors.toSet());
		List<WarehouseInventoryResponseBean> whInventoryDetails = clientService.fetchWarehouseInventoryDetails(backofficeRejectionOrderRequest.getWhId(),
				skuSet);
		Map<String, WarehouseInventoryResponseBean> skuCodeToInfoMap = buildWhInventoryToDetailsMap(whInventoryDetails);
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		RejectionOrderEntity order = RejectionOrderEntity.createNewOrder(backofficeRejectionOrderRequest.getCustomerId(),
				backofficeRejectionOrderRequest.getStoreId(), displayOrderId, backofficeRejectionOrderRequest.getWhId(),
				backofficeRejectionOrderRequest.getOrderType());
		for (BackofficeRejectionOrderRequest requestItem : backofficeRejectionOrderRequest.getOrderItems()) {
			RejectionOrderItemEntity orderItem = RejectionOrderItemEntity.newRejectionOrderItem(order, skuCodeToInfoMap.get(requestItem.getSkuCode()));
			orderItem.setFinalQuantity(requestItem.getQuantity());
			orderItem.setOrderedQty(requestItem.getQuantity());
		}
		if (order.getOrderType().equals(RejectionOrderConstants.RejectionOrderType.DUMP)) {
			order.setStatus(RejectionOrderConstants.RejectionOrderStatus.ORDER_DELIVERED);
			deductWhRejectionInventory(order);
		}
		rejectionOrderPricingService.setAmountAndTaxesInOrderAndItem(order);
		return order;
	}

	private Map<String, WarehouseInventoryResponseBean> buildWhInventoryToDetailsMap(List<WarehouseInventoryResponseBean> whInventoryDetailsList) {
		Map<String, WarehouseInventoryResponseBean> skuCodeToInfoMap = new HashMap<>();
		for (WarehouseInventoryResponseBean whInventoryDetails : whInventoryDetailsList) {
			skuCodeToInfoMap.put(whInventoryDetails.getSkuCode(), whInventoryDetails);
		}
		return skuCodeToInfoMap;
	}

	public RejectionOrderEntity getOrderById(UUID orderId) {
		Optional<RejectionOrderEntity> resultOpt = rejectionOrderRepository.findById(orderId);
		return resultOpt.orElse(null);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public RejectionOrderEntity billFranchiseOrderWithPricingList(List<BackofficeRejectionOrderPricingRequest> rawBeans, RejectionOrderEntity rejectionOrder) {
		Map<String, Double> skuCodeToSalePriceMap = rawBeans.stream()
				.collect(Collectors.toMap(BackofficeRejectionOrderPricingRequest::getSkuCode, BackofficeRejectionOrderPricingRequest::getSalePrice));
		for (RejectionOrderItemEntity rejectionOrderItem : rejectionOrder.getOrderItems()) {
			rejectionOrderItem.setMarkedPrice(BigDecimal.valueOf(skuCodeToSalePriceMap.get(rejectionOrderItem.getSkuCode())));
			rejectionOrderItem.setSalePrice(BigDecimal.valueOf(skuCodeToSalePriceMap.get(rejectionOrderItem.getSkuCode())));
		}
		rejectionOrder.setStatus(RejectionOrderConstants.RejectionOrderStatus.ORDER_BILLED);
		rejectionOrderPricingService.setAmountAndTaxesInOrderAndItem(rejectionOrder);
		rejectionOrder.setAmountReceived(rejectionOrder.getFinalBillAmount());
		saveRejectionOrder(rejectionOrder);
		return rejectionOrder;
	}

	public void deductWhRejectionInventory(RejectionOrderEntity order) {
		List<DeductWhRejectionRequest.VerifyAndDeductRejectionData> storeInventoryUpdateDataList = new ArrayList<>();
		for (RejectionOrderItemEntity orderItem : order.getOrderItems()) {
			DeductWhRejectionRequest.VerifyAndDeductRejectionData storeInventoryUpdateData = new DeductWhRejectionRequest.VerifyAndDeductRejectionData();
			storeInventoryUpdateData.setSkuCode(orderItem.getSkuCode());
			storeInventoryUpdateData.setQuantity(orderItem.getFinalQuantity());
			storeInventoryUpdateDataList.add(storeInventoryUpdateData);
		}
		WhInventoryUpdateType inventoryUpdateType = getInventoryUpdateTypeFromOrderType(order.getOrderType());
		DeductWhRejectionRequest storeInvRequest = createStoreInventoryUpdateRequest(Integer.parseInt(order.getStoreId()), order.getWhId(),
				inventoryUpdateType, storeInventoryUpdateDataList);
		clientService.deductWhRejectionInventory(storeInvRequest);
	}

	private WhInventoryUpdateType getInventoryUpdateTypeFromOrderType(RejectionOrderType orderType) {
		if (orderType.equals(RejectionOrderType.DUMP)) {
			return WhInventoryUpdateType.DUMP;
		}
		return WhInventoryUpdateType.SECONDARY_ORDER;
	}

	private DeductWhRejectionRequest createStoreInventoryUpdateRequest(Integer storeId, Integer whId, WhInventoryUpdateType inventoryUpdateType,
			List<DeductWhRejectionRequest.VerifyAndDeductRejectionData> storeInventoryUpdateDataList) {
		return new DeductWhRejectionRequest(storeId, whId, inventoryUpdateType, storeInventoryUpdateDataList);
	}

	public List<BackofficeRejectionOrderRequest> preProcessRejectionOrderUpload(List<BackofficeRejectionOrderRequest> rawBeans, Integer whId) {
		List<BackofficeRejectionOrderRequest> beans = sanitizeRejectionOrderUpload(rawBeans, whId);
		return beans;
	}

	private Double getTotalRejectionQty(List<WarehouseInventoryResponseBean> warehouseInventoryEntities) {
		return warehouseInventoryEntities.stream().mapToDouble(WarehouseInventoryResponseBean::getRejectedQty).sum();
	}

	private List<BackofficeRejectionOrderRequest> sanitizeRejectionOrderUpload(List<BackofficeRejectionOrderRequest> rawBeans, Integer whId) {
		Set<String> skuCodesRequest = rawBeans.stream().map(BackofficeRejectionOrderRequest::getSkuCode).collect(Collectors.toSet());
		List<WarehouseInventoryResponseBean> whInventoryResponse = clientService.fetchWarehouseInventoryDetails(whId, skuCodesRequest);
		Map<String, List<WarehouseInventoryResponseBean>> rejectionInventoryMap = new HashMap<>();
		for (WarehouseInventoryResponseBean inventoryItem : whInventoryResponse) {
			if (!rejectionInventoryMap.containsKey(inventoryItem.getSkuCode())) {
				rejectionInventoryMap.put(inventoryItem.getSkuCode(), new ArrayList<>());
			}
			rejectionInventoryMap.get(inventoryItem.getSkuCode()).add(inventoryItem);
		}
		Set<String> processedKey = new HashSet<>();
		for (BackofficeRejectionOrderRequest bean : rawBeans) {
			if (!rejectionInventoryMap.containsKey(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Sku code not found skuCode : %s ", bean.getSkuCode()), "skuCode"));
			} else if (processedKey.contains(bean.computedKey())) {
				bean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Duplicate Sku code found skuCode : %s ", bean.computedKey()), "skuCode"));
			} else if (bean.getQuantity() != null && getTotalRejectionQty(rejectionInventoryMap.get(bean.getSkuCode())) < bean.getQuantity()) {
				bean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Insufficient Inventory for SKU : %s ", bean.getSkuCode()), "skuCode"));
			} else if (bean.getQuantity() != null && bean.getQuantity() < 0) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Negative Quantity Cannot be uploaded : %s ", bean.computedKey()),
						"quantity"));
			} else {
				processedKey.add(bean.computedKey());
			}
		}
		return rawBeans;
	}

	public void validateRejectionInventoryOnUpload(BackofficeRejectionOrderRequest bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (Objects.isNull(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Sku not found", "skuCode"));
			}
			if (Objects.isNull(bean.getQuantity())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Sale Price not found", "salePrice"));
			}
			if (!Objects.isNull(bean.getQuantity()) && bean.getQuantity() <= 0) {
				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Quantity can't be negative and zero", "quantity"));
			}
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	public RejectionOrderEntity saveRejectionOrder(RejectionOrderEntity order) {
		return rejectionOrderRepository.save(order);
	}

	public List<BackofficeRejectionOrderPricingRequest> preProcessRejectionOrderPricingUpload(List<BackofficeRejectionOrderPricingRequest> rawBeans,
			UUID orderId) {
		List<BackofficeRejectionOrderPricingRequest> beans = sanitizeRejectionOrderPricingUpload(rawBeans, orderId);
		return beans;
	}

	private List<BackofficeRejectionOrderPricingRequest> sanitizeRejectionOrderPricingUpload(List<BackofficeRejectionOrderPricingRequest> rawBeans,
			UUID orderId) {
		RejectionOrderEntity order = getOrderById(orderId);
		Set<String> skuCodesSet = order.getOrderItems().stream().map(RejectionOrderItemEntity::getSkuCode).collect(Collectors.toSet());
		Set<String> processedKey = new HashSet<>();
		for (BackofficeRejectionOrderPricingRequest bean : rawBeans) {
			if (!skuCodesSet.contains(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Sku not found in order : %s ", bean.getSkuCode()), "skuCode"));
			} else if (processedKey.contains(bean.computedKey())) {
				bean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Duplicate Sku code found skuCode : %s ", bean.computedKey()), "skuCode"));
			} else if (bean.getSalePrice() != null && bean.getSalePrice() < 0) {
				bean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Negative Sale Price for skuCode : %s ", bean.computedKey()), "skuCode"));
			} else {
				processedKey.add(bean.computedKey());
			}
		}
		return rawBeans;
	}

	public void validateRejectionPricingOnUpload(BackofficeRejectionOrderPricingRequest bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (Objects.isNull(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Sku not found", "skuCode"));
			}
			if (Objects.isNull(bean.getSalePrice())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Sale Price not found", "salePrice"));
			}
			if (!Objects.isNull(bean.getSalePrice()) && bean.getSalePrice() <= 0) {
				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Sale Price can't be negative or zero", "salePrice"));
			}
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

}