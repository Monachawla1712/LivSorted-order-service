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
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderItemLogType;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderItemStatus;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.SRType;
import com.sorted.rest.services.order.constants.OrderConstants.WalletStatus;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemLogEntity;
import com.sorted.rest.services.order.repository.FranchiseOrderItemLogRepository;
import com.sorted.rest.services.order.repository.FranchiseOrderItemRepository;
import com.sorted.rest.services.order.repository.FranchiseOrderRepository;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FranchiseOrderService implements BaseService<FranchiseOrderEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(FranchiseOrderService.class);

	@Autowired
	ClientService clientService;

	@Autowired
	FranchiseOrderRepository franchiseOrderRepository;

	@Autowired
	FranchisePricingService franchisePricingService;

	@Autowired
	FranchiseOrderItemRepository franchiseOrderItemRepository;

	@Autowired
	FranchiseOrderItemLogRepository franchiseOrderItemLogRepository;

	@Autowired
	DisplayOrderIdService displayOrderIdService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Transactional(propagation = Propagation.REQUIRED)
	public void addItemToFranchiseCart(FranchiseOrderEntity cart, FranchiseStoreInventoryResponse storeItem, FranchiseCartRequest request,
			FranchiseCartResponse response) {
		Integer requestedQuantity = request.getQuantity();
		FranchiseOrderItemEntity cartItem = null;
		_LOGGER.info(
				String.format("FranchiseOrderService:addItemToFranchiseCart :: storeItem Sku: %s :: moq : %s", storeItem.getSkuCode(), storeItem.getMoq()));
		if (CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			Optional<FranchiseOrderItemEntity> opCartItem = cart.getOrderItems().stream()
					.filter(i -> (i.getSkuCode().equals(storeItem.getSkuCode()) && (Double.compare(i.getMoq(), storeItem.getMoq()) == 0))).findFirst();
			if (opCartItem.isPresent()) {
				cartItem = opCartItem.get();
				_LOGGER.debug(String.format("FranchiseOrderService:addItemToFranchiseCart :: cartItem found: %s", cartItem));
			}
		}
		if (((cartItem != null && cartItem.getFinalCrateQty().compareTo(
				requestedQuantity) < 0) || cartItem == null) && storeItem.getMaxOrderQty() != null && requestedQuantity > storeItem.getMaxOrderQty()) {
			addMaxQuantityLimitErrorResponse(response, storeItem.getMaxOrderQty());
			return;
		}
		StoreItemPrices storeItemPrices = getCartItemPrices(requestedQuantity, storeItem, cart.getIsSrpStore());
		if (cart.getIsSrpStore() == 1) {
			storeItemPrices.setSalePrice(BigDecimal.valueOf(storeItem.getSortedRetailPrice()));
		}
		if (!hasSufficientWalletMoney(cart, cartItem, storeItemPrices.getSalePrice(), requestedQuantity, BigDecimal.valueOf(storeItem.getMoq()),
				storeItem.getMarginDiscount(), response)) {
			return;
		}
		if (cartItem == null && Double.compare(requestedQuantity, 0.0d) == 0) {
			_LOGGER.info("CartService:addItemToCart :: cartItem null and quantity zero");
			return;
		} else if (cartItem == null && Double.compare(requestedQuantity, 0.0d) > 0) {
			_LOGGER.debug("CartService:addItemToCart :: create new item in cart " + cart + " " + storeItem + " " + requestedQuantity);
			if (!inventoryAvailable(cartItem, storeItem, requestedQuantity, response)) {
				return;
			}
			cartItem = FranchiseOrderItemEntity.newCartItem(cart, storeItem, requestedQuantity, cart.getIsSrpStore());
			cart.addOrderItem(cartItem);
			updateWHInventory(cartItem, -requestedQuantity, cart.getStoreId());
			cartItem.setSalePrice(storeItemPrices.getSalePrice());
			cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
			addOrderItemMetadata(cartItem, storeItem, request.getPartnerContents());
			franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
			updateFranchiseOrderItemLogs(storeItem.getSkuCode(), cart.getStoreId(), requestedQuantity, 0, cart.getId());
		} else if (!cartItem.getOrderedCrateQty().equals(requestedQuantity) && Double.compare(requestedQuantity, 0.0d) > 0) {
			_LOGGER.debug("CartService:addItemToCart :: updating item quantity");
			Integer previousQuantity = cartItem.getFinalCrateQty();
			Integer invUpdateQty = previousQuantity - requestedQuantity;
			if (invUpdateQty.compareTo(0) < 0 && !inventoryAvailable(cartItem, storeItem, requestedQuantity, response)) {
				return;
			}
			cartItem.setFinalQuantity(storeItem.getMoq() * requestedQuantity);
			cartItem.setOrderedQty(storeItem.getMoq() * requestedQuantity);
			cartItem.setFinalCrateQty(requestedQuantity);
			cartItem.setOrderedCrateQty(requestedQuantity);
			cartItem.setSalePrice(storeItemPrices.getSalePrice());
			cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
			addOrderItemMetadata(cartItem, storeItem, request.getPartnerContents());
			franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
			updateWHInventory(cartItem, invUpdateQty, cart.getStoreId());
			updateFranchiseOrderItemLogs(storeItem.getSkuCode(), cart.getStoreId(), requestedQuantity, previousQuantity, cart.getId());
		} else if (Double.compare(requestedQuantity, 0.0d) == 0) {
			_LOGGER.info("CartService:addItemToCart :: removing item from cart");
			Integer previousQuantity = cartItem.getOrderedCrateQty();
			removeItemFromFranchiseCart(cart, cartItem);
			franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
			updateWHInventory(cartItem, previousQuantity, cart.getStoreId());
			updateFranchiseOrderItemLogs(storeItem.getSkuCode(), cart.getStoreId(), 0, previousQuantity, cart.getId());
		}
	}

	private void addOrderItemMetadata(FranchiseOrderItemEntity cartItem, FranchiseStoreInventoryResponse storeItem, List<PartnerContent> partnerContents) {
		if (cartItem.getMetadata() == null) {
			FranchiseOrderItemMetadata metadata = new FranchiseOrderItemMetadata();
			cartItem.setMetadata(metadata);
		}
		cartItem.getMetadata().setStartOrderQty(storeItem.getStartOrderQty());
		cartItem.getMetadata().setPartnerContents(partnerContents);
	}

	public void repriceCart(Map<String, FranchiseStoreInventoryResponse> skuMap, FranchiseOrderEntity cart) {
		for (int i = cart.getOrderItems().size() - 1; i >= 0; i--) {
			FranchiseOrderItemEntity cartItem = cart.getOrderItems().get(i);
			FranchiseStoreInventoryResponse franchiseStoreInventoryResponse = skuMap.get(cartItem.getSkuCode());
			if (franchiseStoreInventoryResponse != null && franchiseStoreInventoryResponse.getSalePrice() != null) {
				StoreItemPrices storeItemPrices = getCartItemPrices(cartItem.getFinalCrateQty(), franchiseStoreInventoryResponse, cart.getIsSrpStore());
				cartItem.setSalePrice(storeItemPrices.getSalePrice());
				cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
			} else {
				removeItemFromFranchiseCart(cart, cartItem);
			}
		}
	}

	private StoreItemPrices getCartItemPrices(double itemQuantity, FranchiseStoreInventoryResponse storeItem, Integer isSrpStore) {
		BigDecimal newSalePrice = BigDecimal.valueOf(storeItem.getSalePrice());
		BigDecimal newMarkedPrice = FranchiseOrderItemEntity.calcMarkedPrice(storeItem, isSrpStore);
		if (storeItem.getPriceBrackets() != null) {
			for (FranchiseStoreInventoryResponse.PriceBracketsResponseBean priceBracket : storeItem.getPriceBrackets()) {
				if (itemQuantity >= priceBracket.getMin() && itemQuantity <= priceBracket.getMax()) {
					newSalePrice = BigDecimal.valueOf(priceBracket.getSalePrice());
					newMarkedPrice = BigDecimal.valueOf(priceBracket.getMarkedPrice());
					if (newMarkedPrice.compareTo(newSalePrice) < 0) {
						newMarkedPrice = newSalePrice;
					}
					break;
				}
			}
		}
		return new StoreItemPrices(newSalePrice, newMarkedPrice);
	}

	private void updateFranchiseOrderItemLogs(String skuCode, String storeId, Integer newQty, Integer previousQty, UUID orderId) {
		String skus = ParamsUtils.getParam("ITEM_LOG_SKUS");
		List<String> allowedSkus = List.of(skus.split(","));
		if (CollectionUtils.isNotEmpty(allowedSkus) && allowedSkus.contains(skuCode)) {
			FranchiseOrderItemLogEntity itemLogEntity = buildOrderItemLog(skuCode, storeId, newQty, previousQty, orderId);
			CompletableFuture.runAsync(() -> franchiseOrderItemLogRepository.save(itemLogEntity));
		}
	}

	private FranchiseOrderItemLogEntity buildOrderItemLog(String skuCode, String storeId, Integer newQty, Integer previousQty, UUID orderId) {
		FranchiseOrderItemLogEntity itemLogEntity = FranchiseOrderItemLogEntity.newInstance();
		itemLogEntity.setStoreId(storeId);
		itemLogEntity.setSkuCode(skuCode);
		itemLogEntity.setFromQty(previousQty);
		itemLogEntity.setToQty(newQty);
		itemLogEntity.setOrderId(orderId);
		return itemLogEntity;
	}

	private boolean inventoryAvailable(FranchiseOrderItemEntity cartItem, FranchiseStoreInventoryResponse storeItem, Integer requestedQuantity,
			FranchiseCartResponse response) {
		_LOGGER.debug("CartService:addItemToCart :: inventory check");
		if (cartItem == null) {
			Integer storeQuantity = storeItem.getQuantity();
			if (storeQuantity <= 0) {
				response.error("product_not_available", "Oops! यह item out of stock हो गया है।");
				return false;
			} else if (storeQuantity < requestedQuantity) {
				response.error("product_not_available",
						String.format("Oops! इस item की %s crate ही उपलब्ध है। कृपया अपनी selected quantity change करके दोबारा try करें।", storeQuantity));
				return false;
			}
		} else {
			Integer cartQuantity = cartItem.getOrderedCrateQty();
			Integer storeQuantity = storeItem.getQuantity();
			if (cartQuantity + storeQuantity <= 0) {
				response.error("product_not_available", "Oops! यह item out of stock हो गया है।");
				return false;
			} else if (requestedQuantity > cartQuantity + storeQuantity) {
				response.error("product_not_available",
						String.format("Oops! इस item की %s crate ही उपलब्ध है। कृपया अपनी selected quantity change करके दोबारा try करें।",
								cartQuantity + storeQuantity));
				return false;
			}
		}
		return true;
	}

	private boolean hasSufficientWalletMoney(FranchiseOrderEntity cart, FranchiseOrderItemEntity cartItem, BigDecimal salePrice, Integer requestedQuantity,
			BigDecimal moq, Double marginDiscount, FranchiseCartResponse response) {
		_LOGGER.debug(String.format("CART SERVICE :: WALLET CHECK"));
		WalletBean walletBean = clientService.getStoreWallet(cart.getStoreId());
		BigDecimal creditLimit = BigDecimal.valueOf(0.0);
		BigDecimal walletAmount = BigDecimal.valueOf(0);
		if (walletBean == null) {
			addedWalletErrorResponse(response);
			return false;
		} else {
			walletAmount = BigDecimal.valueOf(walletBean.getAmount());
		}
		cart.setWalletAmount(walletBean.getAmount());
		if (walletBean.getStatus().equals(WalletStatus.INACTIVE)) {
			addedWalletInactiveErrorResponse(response, walletBean.getMetadata() != null ? walletBean.getMetadata().getMinOutstanding() : null);
			return false;
		}
		if (walletBean.getCreditLimit() != null) {
			creditLimit = BigDecimal.valueOf(walletBean.getCreditLimit());
		}
		BigDecimal additionalAmount;
		BigDecimal marginalDiscount;
		if (cartItem == null) {
			additionalAmount = BigDecimal.valueOf(requestedQuantity).multiply(salePrice).multiply(moq);
		} else if (cartItem.getFinalCrateQty() < requestedQuantity) {
			additionalAmount = BigDecimal.valueOf(requestedQuantity).multiply(salePrice).multiply(moq)
					.subtract(BigDecimal.valueOf(cartItem.getSpGrossAmount()));
		} else {
			return true;
		}
		if (cart.getIsSrpStore() == 1) {
			marginalDiscount = additionalAmount.multiply(BigDecimal.valueOf(marginDiscount)).divide(BigDecimal.valueOf(100));
			additionalAmount = additionalAmount.subtract(marginalDiscount);
		}
		BigDecimal expectedAmount = BigDecimal.valueOf(cart.getFinalBillAmount()).add(additionalAmount);
		if (expectedAmount.subtract(walletAmount).compareTo((creditLimit)) > 0) {
			addedWalletErrorResponse(response);
			return false;
		}
		return true;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void removeItemFromFranchiseCart(FranchiseOrderEntity cart, FranchiseOrderItemEntity cartItem) {
		cartItem.setFinalQuantity(0d);
		cartItem.setOrderedQty(0d);
		cartItem.setOrderedCrateQty(0);
		cartItem.setFinalCrateQty(0);
		cartItem.setActive(0);
		saveFranchiseOrderItemEntity(cartItem);
		cart.getOrderItems().remove(cartItem);
		if (cart.getOrderItems().size() == 0) {
			cart.setActive(0);
		}
	}

	public FranchiseOrderItemEntity saveFranchiseOrderItemEntity(FranchiseOrderItemEntity entity) {
		FranchiseOrderItemEntity result = franchiseOrderItemRepository.save(entity);
		return result;
	}

	private void addedWalletErrorResponse(FranchiseCartResponse response) {
		_LOGGER.debug("ADD money to your wallet ");
		if (response.getError() == null) {
			response.error("Insufficient money", "Oops! आपके Wallet में प्रयाप्त राशि नहीं है। कृपया अपने wallet को recharge करके दोबारा try करें।");
		}
	}

	private void addedWalletInactiveErrorResponse(FranchiseCartResponse response, Double lastOrderOutstanding) {
		_LOGGER.debug("ADD money to your wallet ");
		if (response.getError() == null) {
			response.error("Insufficient money", String.format("नया Order करने के लिए पिछले आर्डर की बकाया राशि %s चुकता करें।", lastOrderOutstanding));
		}
	}

	private void addMaxQuantityLimitErrorResponse(FranchiseCartResponse response, Integer maxOrderLimit) {
		_LOGGER.debug("quantity_limit_reached");
		if (response.getError() == null) {
			response.error("quantity_limit_reached", String.format("Oops! आपके Cart मे %s Quantity ही add हो सकती है", maxOrderLimit));
		}
	}

	private void updateWHInventory(FranchiseOrderItemEntity orderItem, Integer quantity, String storeId) {
		_LOGGER.debug(String.format("UPDATE FRANCHISE INVENTORY : QUANTITY = " + quantity));
		FranchiseStoreInventoryAddOrDeductRequest request = new FranchiseStoreInventoryAddOrDeductRequest();
		request.setMoq(orderItem.getMoq());
		request.setQuantity(quantity);
		request.setStoreId(storeId);
		clientService.saveFranchiseStoreInventory(orderItem.getWhId(), orderItem.getSkuCode(), request);
	}

	private void updateWHInventoryForBulkRequest(List<FranchiseCartRequest> orderItems) {
		_LOGGER.debug(String.format("UPDATE FRANCHISE INVENTORIES : Request = %s", orderItems));
		clientService.saveFranchiseStoreInventoryForBulkRequest(orderItems);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void clearFranchiseStoreCart(final String storeId) {
		final FranchiseOrderEntity cart = findStoreCurrentCart(storeId);
		if (Objects.nonNull(cart)) {
			cart.setActive(0);
			List<FranchiseOrderItemEntity> cartItems = cart.getOrderItems();
			for (FranchiseOrderItemEntity cartItem : cartItems) {
				cartItem.setActive(0);
				updateWHInventory(cartItem, cartItem.getOrderedCrateQty(), storeId);
			}
			saveFranchiseEntity(cart);
		}
	}

	public List<FranchiseOrderEntity> findAllActiveCart() {
		Optional<List<FranchiseOrderEntity>> resultOpt = franchiseOrderRepository.findAllCartWithActiveState();
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	public List<FranchiseOrderEntity> findAllPendingRefundOrders() {
		Optional<List<FranchiseOrderEntity>> resultOpt = franchiseOrderRepository.findAllCartWithActiveState();
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	public List<FranchiseOrderEntity> findAllActiveOrderSRNotUploaded() {
		Optional<List<FranchiseOrderEntity>> resultOpt = franchiseOrderRepository.findAllCartWithActiveStateAndSrNotUploaded();
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	public List<FranchiseOrderEntity> getOrdersSinceDays(String storeId, int sinceDays) {
		LocalDateTime thirtyDaysAgoDate = LocalDateTime.now().minusDays(sinceDays);
		Date from = thirtyDaysAgoDate.toDate();

		List<FranchiseOrderEntity> orderEntityList = franchiseOrderRepository.findOrdersWithDateAfter(storeId, from);
		return orderEntityList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity findStoreCurrentCart(String storeId) {
		List<FranchiseOrderEntity> resultList = franchiseOrderRepository.findStoreCurrentCart(storeId);
		FranchiseOrderEntity result = null;
		if (CollectionUtils.isNotEmpty(resultList)) {
			result = resultList.get(0);
			resultList.remove(0);
			resultList.stream().forEach(o -> {
				o.setActive(0);
				if (o.getOrderItems() != null) {
					o.getOrderItems().stream().forEach(i -> {
						i.setActive(0);
					});
				}
				saveFranchiseEntity(o);
			});
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity saveFranchiseEntity(FranchiseOrderEntity entity) {
		FranchiseOrderEntity result = franchiseOrderRepository.save(entity);
		return result;
	}

	public UUID getCustomerId() {
		UUID customerId = SessionUtils.getAuthUserId();
		Assert.notNull(customerId, "CustomerId could not be empty");
		return customerId;
	}

	@Override
	public Class<FranchiseOrderEntity> getEntity() {
		return FranchiseOrderEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return franchiseOrderRepository;
	}

	public FranchiseOrderEntity findByStoreIdAndDeliveryDateAndSlot(String storeId, Date deliveryDate, String slot) {
		FranchiseOrderEntity franchiseOrder = franchiseOrderRepository.findByStoreIdAndDeliveryDateAndActiveAndSlot(storeId, deliveryDate, 1, slot);
		return franchiseOrder;
	}

	public List<FranchiseOrderEntity> findConfirmedRefundOrdersFromParentOrderId(UUID parentOrderId) {
		List<FranchiseOrderEntity> franchiseOrderEntityList = franchiseOrderRepository.findRefundInitiatedOrdersWithParentOrderId(parentOrderId);
		return franchiseOrderEntityList;
	}

	public List<FranchiseOrderEntity> findOrderFromParentOrderId(UUID parentOrderId) {
		List<FranchiseOrderEntity> franchiseOrderEntityList = franchiseOrderRepository.findRefundConfirmedOrdersWithParentOrderId(parentOrderId);
		return franchiseOrderEntityList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateFranchiseOrderStatus(FranchiseOrderEntity franchiseOrder, FranchiseOrderStatusUpdateRequest request) {
		_LOGGER.debug(String.format("updateFranchiseOrderStatus : %s", franchiseOrder.getStatus()));
		franchiseOrder.setType(request.getType());
		if ((franchiseOrder.getStatus().equals(FranchiseOrderStatus.NEW_ORDER) || (franchiseOrder.getStatus()
				.equals(FranchiseOrderStatus.PARTIALLY_DISPATCHED))) && request.getStatus().equals(FranchiseOrderStatus.ORDER_BILLED)) {
			updateFranchiseOrderItems(franchiseOrder, request);
			franchiseOrder.setStatus(request.getStatus());
			adjustWalletAmount(franchiseOrder, "Franchise-Order");
		} else {
			updateFranchiseOrderItems(franchiseOrder, request);
			franchiseOrder.setStatus(request.getStatus());
			franchisePricingService.setAmountAndTaxesInOrderAndItem(franchiseOrder);
		}
		List<ClevertapEventRequest.ClevertapEventData> clevertapOrderEventDataList = new ArrayList<>();
		for (FranchiseOrderItemEntity franchiseOrderItem : franchiseOrder.getOrderItems()) {
			if (franchiseOrderItem.getOrderedQty() != null && franchiseOrderItem.getOrderedQty() > 0 && franchiseOrderItem.getFinalQuantity() == 0) {
				clevertapOrderEventDataList.add(buildOrderItemUnfulfilledClevertapEventData(franchiseOrderItem, franchiseOrder));
			}
		}
		if (!clevertapOrderEventDataList.isEmpty()) {
			clientService.sendMultipleClevertapEvents(clevertapOrderEventDataList);
		}
		saveFranchiseEntity(franchiseOrder);
	}

	public void updateFranchiseOrderMetadata(FranchiseOrderEntity franchiseOrder, FranchiseOrderMetadataUpdateRequest request) {
		if (request.getDeliveryDetails() != null && !StringUtils.isEmpty(request.getDeliveryDetails().getCompletedAt())) {
			Map<String, FranchiseOrderDeliveryBean> deliveryBeanMap = franchiseOrder.getMetadata().getDeliveryDetails() != null ?
					franchiseOrder.getMetadata().getDeliveryDetails().stream()
							.collect(Collectors.toMap(FranchiseOrderDeliveryBean::getCompletedAt, Function.identity())) :
					new HashMap<>();
			if (deliveryBeanMap.containsKey(request.getDeliveryDetails().getCompletedAt())) {
				if (request.getDeliveryDetails().getUrls() != null) {
					deliveryBeanMap.get(request.getDeliveryDetails().getCompletedAt()).setUrls(request.getDeliveryDetails().getUrls());
				}
			} else {
				deliveryBeanMap.put(request.getDeliveryDetails().getCompletedAt(), request.getDeliveryDetails());
			}
			franchiseOrder.getMetadata().setDeliveryDetails(deliveryBeanMap.values().stream().collect(Collectors.toList()));
		}
	}

	private ClevertapEventRequest.ClevertapEventData buildOrderItemUnfulfilledClevertapEventData(FranchiseOrderItemEntity franchiseOrderItem,
			FranchiseOrderEntity franchiseOrder) {
		Map<String, Object> clevertapEventData = new HashMap<>();
		clevertapEventData.put("displayOrderId", franchiseOrder.getDisplayOrderId());
		clevertapEventData.put("productName", franchiseOrderItem.getProductName());
		return clientService.buildClevertapEventData("orderItemUnfulfilled", franchiseOrder.getCustomerId(), clevertapEventData);
	}

	public void deductMoneyFromStoreWallet(String storeId, Double amount, String txnDetail, String txnType, Double holdAmount, String remarks, String key) {
		WalletAddOrDeductBean walletDeductBean = new WalletAddOrDeductBean();
		walletDeductBean.setAmount(amount);
		walletDeductBean.setTxnType(txnType);
		walletDeductBean.setTxnDetail(txnDetail);
		walletDeductBean.setHoldAmount(holdAmount);
		walletDeductBean.setRemarks(remarks);
		_LOGGER.debug(String.format("deductMoneyFromStoreWallet::walletDeductBean: %s", walletDeductBean));
		clientService.addOrDeductFromStoreWallet(storeId, walletDeductBean, key);
	}

	private void updateFranchiseOrderItems(FranchiseOrderEntity franchiseOrder, FranchiseOrderStatusUpdateRequest request) {
		Map<String, FranchiseOrderItemEntity> orderProductMap = franchiseOrder.getOrderItems().stream()
				.collect(Collectors.toMap(FranchiseOrderItemEntity::getSkuCode, Function.identity()));
		for (FranchiseSROrderItemBean requestItem : request.getOrderItems()) {
			if (orderProductMap.containsKey(requestItem.getSkuCode())) {
				FranchiseOrderItemEntity item = orderProductMap.get(requestItem.getSkuCode());
				if (requestItem.getCratePicked() == 0) {
					item.setStatus(FranchiseOrderItemStatus.NOT_AVAILABLE);
				} else {
					item.setStatus(FranchiseOrderItemStatus.PACKED);
				}
				item.setCratesPicked(requestItem.getCratePicked());
				item.setWeightPicked(requestItem.getWeightPicked());
				item.setFinalCrateQty(requestItem.getCratePicked());
				item.setFinalQuantity(requestItem.getWeightPicked());
				if (item.getMetadata() == null) {
					item.setMetadata(FranchiseOrderItemMetadata.newInstance());
				}
				item.getMetadata().setDeliveryNumber(requestItem.getDeliveryNumber());
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void backofficeFranchiseOrder(FranchiseOrderEntity cart, BackofficeFranchiseOrderBean request, FranchiseCartResponse response,
			List<FranchiseInventoryRequest> whRequest, String storeId) {
		Set<String> skuCodes = whRequest.stream().map(FranchiseInventoryRequest::getSkuCode).collect(Collectors.toSet());
		List<FranchiseStoreInventoryResponse> storeItemResponse = clientService.getStoreSkuInventoryForBulkRequest(skuCodes, storeId);
		if (storeItemResponse.size() != request.getOrderItems().size()) {
			_LOGGER.debug("Inventory Response mismatch.");
			if (response.getError() == null) {
				response.error("Response Mismatch", "Inventory Response Mismatch from wms");
			}
			return;
		}
		Map<String, FranchiseStoreInventoryResponse> storeSkuMap = storeItemResponse.stream()
				.collect(Collectors.toMap(FranchiseStoreInventoryResponse::getSkuCode, Function.identity()));
		Set<String> oosSkuCodes = new HashSet<String>();
		for (BackofficeFranchiseCartRequest requestItem : request.getOrderItems()) {
			Integer crates = Integer.valueOf((int) Math.ceil(requestItem.getQuantity() / requestItem.getMoq()));
			if (crates > storeSkuMap.get(requestItem.getSkuCode()).getQuantity()) {
				oosSkuCodes.add(requestItem.getSkuCode());
			}
		}
		if (CollectionUtils.isNotEmpty(oosSkuCodes)) {
			response.error("Requested Quantity not available", String.format("quantity Not Available skuCodes = %s", oosSkuCodes));
			return;
		}
		for (BackofficeFranchiseCartRequest requestItem : request.getOrderItems()) {
			Integer crates = Integer.valueOf((int) Math.ceil(requestItem.getQuantity() / requestItem.getMoq()));
			FranchiseOrderItemEntity cartItem = FranchiseOrderItemEntity.newCartItem(cart, storeSkuMap.get(requestItem.getSkuCode()), crates,
					cart.getIsSrpStore());
			cartItem.setOrderedQty(requestItem.getQuantity());
			cartItem.setFinalQuantity(requestItem.getQuantity());
			StoreItemPrices storeItemPrices = getCartItemPrices(cartItem.getFinalCrateQty(), storeSkuMap.get(requestItem.getSkuCode()), cart.getIsSrpStore());
			cartItem.setSalePrice(storeItemPrices.getSalePrice());
			cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
			addOrderItemMetadata(cartItem, storeSkuMap.get(requestItem.getSkuCode()), null);
			cart.addOrderItem(cartItem);
		}
		//		List<FranchiseCartRequest> cartRequests = getMapper().mapAsList(request.getOrderItems(), FranchiseCartRequest.class);
		//		for (FranchiseCartRequest cartRequest : cartRequests) {
		//			Integer crates = Integer.valueOf((int) Math.ceil(cartRequest.getQuantity() / cartRequest.getMoq()));
		//			cartRequest.setQuantity(crates);
		//			cartRequest.setStoreId(storeId);
		//		}
		franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
		if (hasSufficientWalletMoneyForBO(cart)) {
			throw new ValidationException(
					ErrorBean.withError("Insufficient money", String.format("Oops! स्टोर - %s के Wallet में प्रयाप्त राशि नहीं है।", cart.getStoreId()),
							"wallet_amount"));
		}
		//		updateWHInventoryForBulkRequest(cartRequests);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addItemsToCartFromList(FranchiseOrderEntity cart, BackofficeFranchiseOrderBean request, FranchiseCartResponse response,
			List<FranchiseInventoryRequest> whRequest, String storeId) {
		Set<String> skuCodes = whRequest.stream().map(FranchiseInventoryRequest::getSkuCode).collect(Collectors.toSet());
		List<FranchiseStoreInventoryResponse> storeItemResponse = clientService.getStoreSkuInventoryForBulkRequest(skuCodes, storeId);
		if (storeItemResponse.size() != request.getOrderItems().size()) {
			_LOGGER.debug("Inventory Response mismatch.");
			if (response.getError() == null) {
				response.error("Response Mismatch", "Inventory Response Mismatch from wms");
			}
			return;
		}
		Map<String, FranchiseStoreInventoryResponse> storeSkuMap = storeItemResponse.stream()
				.collect(Collectors.toMap(FranchiseStoreInventoryResponse::getSkuCode, Function.identity()));
		List<FranchiseCartRequest> cartRequests = new ArrayList<>();
		updateCartItemsFromList(cart, request, storeId, storeSkuMap, cartRequests);
		franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
		if (hasSufficientWalletMoneyForBO(cart)) {
			throw new ValidationException(
					ErrorBean.withError("Insufficient money", String.format("Oops! स्टोर - %s के Wallet में प्रयाप्त राशि नहीं है।", cart.getStoreId()),
							"wallet_amount"));
		}
		updateWHInventoryForBulkRequest(cartRequests);
	}

	private void updateCartItemsFromList(FranchiseOrderEntity cart, BackofficeFranchiseOrderBean request, String storeId,
			Map<String, FranchiseStoreInventoryResponse> storeSkuMap, List<FranchiseCartRequest> cartRequests) {
		Map<String, FranchiseOrderItemEntity> cartItemSkuMapping = new HashMap<>();
		if (cart.getOrderItems() != null) {
			cartItemSkuMapping = cart.getOrderItems().stream().collect(Collectors.toMap(FranchiseOrderItemEntity::getSkuCode, Function.identity()));
		}
		for (BackofficeFranchiseCartRequest requestItem : request.getOrderItems()) {
			Integer requestedCrates = Integer.valueOf((int) Math.ceil(requestItem.getQuantity() / requestItem.getMoq()));
			Integer whCrates = storeSkuMap.get(requestItem.getSkuCode()).getQuantity();
			Integer currentCrates = 0;
			if (cartItemSkuMapping.containsKey(requestItem.getSkuCode())) {
				currentCrates = cartItemSkuMapping.get(requestItem.getSkuCode()).getFinalCrateQty();
			}
			Integer diffCrates = requestedCrates - currentCrates;
			FranchiseStoreInventoryResponse storeItem = storeSkuMap.get(requestItem.getSkuCode());
			Integer finalCartQty = 0;
			FranchiseCartRequest franchiseCartRequest = buildWhDeductionRequest(requestItem, storeId);
			if (whCrates <= 0 && diffCrates > 0) {
				continue;
			} else if (whCrates < diffCrates) {
				finalCartQty = whCrates;
			} else {
				finalCartQty = requestedCrates;
			}
			Integer invUpdateQty = 0;
			if (cartItemSkuMapping.containsKey(requestItem.getSkuCode())) {
				FranchiseOrderItemEntity cartItem = cartItemSkuMapping.get(requestItem.getSkuCode());
				Integer previousQuantity = cartItem.getFinalCrateQty();
				updateCartItemQuantity(cartItem, cart, finalCartQty, storeItem.getMoq());
				StoreItemPrices storeItemPrices = getCartItemPrices(cartItem.getFinalCrateQty(), storeItem, cart.getIsSrpStore());
				cartItem.setSalePrice(storeItemPrices.getSalePrice());
				cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
				addOrderItemMetadata(cartItem, storeItem, null);
				invUpdateQty = finalCartQty - previousQuantity;
			} else {
				FranchiseOrderItemEntity cartItem = FranchiseOrderItemEntity.newCartItem(cart, storeSkuMap.get(requestItem.getSkuCode()), finalCartQty,
						cart.getIsSrpStore());
				updateCartItemQuantity(cartItem, cart, finalCartQty, storeItem.getMoq());
				StoreItemPrices storeItemPrices = getCartItemPrices(cartItem.getFinalCrateQty(), storeItem, cart.getIsSrpStore());
				cartItem.setSalePrice(storeItemPrices.getSalePrice());
				cartItem.setMarkedPrice(storeItemPrices.getMarkedPrice());
				addOrderItemMetadata(cartItem, storeItem, null);
				cart.addOrderItem(cartItem);
				invUpdateQty = finalCartQty;
			}
			franchiseCartRequest.setQuantity(invUpdateQty);
			cartRequests.add(franchiseCartRequest);
		}
	}

	private FranchiseCartRequest buildWhDeductionRequest(BackofficeFranchiseCartRequest requestItem, String storeId) {
		FranchiseCartRequest whDeductionReq = new FranchiseCartRequest();
		whDeductionReq.setSkuCode(requestItem.getSkuCode());
		whDeductionReq.setWhId(requestItem.getWhId());
		whDeductionReq.setMoq(requestItem.getMoq());
		whDeductionReq.setStoreId(storeId);
		return whDeductionReq;
	}

	private void updateCartItemQuantity(FranchiseOrderItemEntity cartItem, FranchiseOrderEntity cart, Integer crateQty, Double moq) {
		if (crateQty <= 0) {
			removeItemFromFranchiseCart(cart, cartItem);
		} else {
			cartItem.setFinalQuantity(moq * crateQty);
			cartItem.setOrderedQty(moq * crateQty);
			cartItem.setFinalCrateQty(crateQty);
			cartItem.setOrderedCrateQty(crateQty);
		}
	}

	private Boolean hasSufficientWalletMoneyForBO(FranchiseOrderEntity cart) {
		BigDecimal finalBillAmount = BigDecimal.valueOf(cart.getFinalBillAmount());
		BigDecimal creditLimit = BigDecimal.valueOf(0.0);
		WalletBean walletBean = clientService.getStoreWallet(cart.getStoreId());
		if (walletBean == null) {
			throw new ValidationException(ErrorBean.withError("wallet_not_found", "store wallet not found", "wallet_not_found"));
		}
		if (walletBean.getStatus().equals(WalletStatus.INACTIVE)) {
			throw new ValidationException(ErrorBean.withError("Insufficient money",
					String.format("स्टोर %s की पिछले आर्डर की राशि %s बकाया है।", cart.getStoreId(),
							walletBean.getMetadata() != null ? walletBean.getMetadata().getMinOutstanding() : null), "last_order_outstanding"));
		}
		BigDecimal walletAmount = BigDecimal.valueOf(walletBean.getAmount());
		if (walletBean.getCreditLimit() != null) {
			creditLimit = BigDecimal.valueOf(walletBean.getCreditLimit());
		}
		if (finalBillAmount.subtract(walletAmount).compareTo((creditLimit)) >= 0) {
			return true;
		}
		return false;
	}

	public List<BackofficeFranchiseCartRequest> preProcessFranchiseOrderUpload(List<BackofficeFranchiseCartRequest> rawBeans, String storeId) {
		List<BackofficeFranchiseCartRequest> beans = sanitizeFranchiseOrderUpload(rawBeans, storeId);
		return beans;
	}

	private List<BackofficeFranchiseCartRequest> sanitizeFranchiseOrderUpload(List<BackofficeFranchiseCartRequest> rawBeans, String storeId) {
		Set<String> skuCodesRequest = rawBeans.stream().map(BackofficeFranchiseCartRequest::getSkuCode).collect(Collectors.toSet());

		List<FranchiseStoreInventoryResponse> storeItemResponse = clientService.getStoreSkuInventoryForBulkRequest(skuCodesRequest, storeId);
		List<BackofficeFranchiseCartRequest> storeResponse = getMapper().mapAsList(storeItemResponse, BackofficeFranchiseCartRequest.class);
		Map<String, BackofficeFranchiseCartRequest> skuMap = storeResponse.stream()
				.collect(Collectors.toMap(BackofficeFranchiseCartRequest::getSkuCode, Function.identity()));
		Set<String> processedKey = new HashSet<>();
		rawBeans.stream().forEach(bean -> {
			if (skuMap == null || !skuMap.containsKey(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Sku code not found skuCode : %s ", bean.getSkuCode()), "skuCode"));
			} else if (processedKey.contains(bean.computedKey())) {
				bean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Duplicate Sku code found skuCode : %s ", bean.computedKey()), "skuCode"));
			} else if (Integer.valueOf((int) Math.ceil(bean.getQuantity() / skuMap.get(bean.getSkuCode()).getMoq())) > skuMap.get(bean.getSkuCode())
					.getQuantity()) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE,
						String.format("Requested quantity is not available on warehouse for skuCode : %s ", bean.computedKey()), "skuCode"));
			} else {
				processedKey.add(bean.computedKey());
				bean.setMoq(skuMap.get(bean.getSkuCode()).getMoq());
				bean.setWhId(skuMap.get(bean.getSkuCode()).getWhId());
			}
		});
		return rawBeans;
	}

	public void validateFranchiseStoreInventoryOnUpload(BackofficeFranchiseCartRequest bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {

			if (Objects.isNull(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Sku not found", "skuCode"));
			}
			if (!Objects.isNull(bean.getQuantity()) && bean.getQuantity() <= 0) {
				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Quantity can't be negative and zero", "quantity"));
			}
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	public List<FranchiseOrderEntity> findAllDeliveredOrders() {
		List<FranchiseOrderStatus> status = List.of(FranchiseOrderStatus.ORDER_BILLED, FranchiseOrderStatus.ORDER_DELIVERED);
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", DeliveryDateUtils.getDeliveryDate());
		filters.put("status", status);
		List<FranchiseOrderEntity> franchiseOrders = findAllRecords(filters);
		if (CollectionUtils.isEmpty(franchiseOrders)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Franchise Order found", "franchiseOrders"));
		}
		return franchiseOrders;
	}

	public List<FranchiseOrderEntity> findAllRefundOrders(UUID parentOrderId) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("parentOrderId", parentOrderId);
		filters.put("status", FranchiseOrderStatus.ORDER_REFUNDED);
		List<FranchiseOrderEntity> refundOrders = findAllRecords(filters);
		return refundOrders;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity createRefundOrder(FranchiseOrderEntity parentOrder, FranchiseOrderRefundBean request,
			Map<String, FranchiseStoreInventoryResponse> storeItemMap) {
		FranchiseOrderEntity refundOrder = generateRefundRequestOrder(parentOrder, request, storeItemMap);
		confirmRefundRequest(parentOrder, refundOrder);
		franchiseOrderRepository.save(refundOrder);
		franchiseOrderRepository.save(parentOrder);
		return refundOrder;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity generateRefundRequestOrder(FranchiseOrderEntity parentOrder, FranchiseOrderRefundBean request,
			Map<String, FranchiseStoreInventoryResponse> storeItemMap) {
		Map<String, FranchiseOrderItemEntity> parentOrderItemMap = getFranchiseOrderItemMap(parentOrder);
		Map<String, Double> requestQuantityMap = getRequestQuantityMap(request);
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		FranchiseOrderEntity refundOrder = FranchiseOrderEntity.createNewRefundOrder(parentOrder, displayOrderId, request.getReturnIssue());
		refundOrder.setOrderItems(new ArrayList<>());
		int itemCount = 0;

		Set<String> franchiseRefundOrderSet = getFranchiseRefundOrderSet(parentOrder.getId(), request.getReturnIssue());
		for (FranchiseRefundOrderItemBean item : request.getRefundOrderItems()) {
			if (franchiseRefundOrderSet.contains(item.getSkuCode())) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Refund already initiated for SKU %s in parent order", item.getSkuCode()),
								"order"));
			}
			if (parentOrderItemMap != null && parentOrderItemMap.containsKey(item.getSkuCode()) && parentOrderItemMap.get(item.getSkuCode())
					.getFinalQuantity() >= item.getRefundQuantity()) {
				FranchiseOrderItemEntity parentOrderItem = parentOrderItemMap.get(item.getSkuCode());
				Double refundQty = item.getRefundQuantity();
				if (Objects.equals(request.getReturnIssue(), "RETURN_QUANTITY")) {
					Double permissibleQty = (parentOrderItemMap.get(item.getSkuCode()).getFinalQuantity()) * (storeItemMap.get(item.getSkuCode())
							.getPermissibleRefundQuantity()) / 100;
					if (refundQty > permissibleQty) {
						refundQty = permissibleQty;
					}
				}
				FranchiseRefundDetails refundDetails = getRefundDetails(item.getRefundRemarks(), item.getWarehouseReturnCheck(), getCustomerId());
				FranchiseOrderItemEntity refundOrderItem = FranchiseOrderItemEntity.createNewRefundOrderItem(refundOrder, refundQty, parentOrderItem,
						refundDetails);
				refundOrder.getOrderItems().add(refundOrderItem);
				itemCount++;
			} else {
				if (parentOrderItemMap == null) {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
							String.format("Sku Code = %s not found with Quantity = %s in parent order", item.getSkuCode(), item.getRefundQuantity()), "order"));
				} else if (!parentOrderItemMap.containsKey(item.getSkuCode())) {
					throw new ValidationException(
							ErrorBean.withError(Errors.INVALID_REQUEST, String.format("SKU %s not found in parent order", item.getSkuCode()), "order"));
				} else {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
							String.format("Sku Code = %s not found with Quantity = %s in parent order", item.getSkuCode(), item.getRefundQuantity()), "order"));
				}
			}
		}

		refundOrder.setItemCount(itemCount);
		refundOrder.setSubmittedAt(new Date());
		franchisePricingService.setAmountAndTaxesInOrderAndItem(refundOrder);
		refundOrder.setEstimatedBillAmount(refundOrder.getFinalBillAmount());

		if (Objects.equals(request.getReturnIssue(), "RETURN_QUANTITY")) {
			setFinalQuantities(refundOrder, requestQuantityMap);
		}

		return refundOrder;
	}

	private FranchiseRefundDetails getRefundDetails(String refundRemarks, Boolean warehouseReturnCheck, UUID userId) {
		FranchiseRefundDetails refundDetails = new FranchiseRefundDetails();
		refundDetails.setRemarks(refundRemarks);
		refundDetails.setRequestedBy(getUserNameFromId(userId));
		refundDetails.setWarehouseReturnCheck(warehouseReturnCheck);
		return refundDetails;
	}

	private String getUserNameFromId(UUID userId) {
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(userId);
		if (userDetails != null) {
			return userDetails.getName();
		}
		return null;
	}

	private Set<String> getFranchiseRefundOrderSet(UUID parentOrderId, String refundType) {
		Set<String> refundOrderSkuSet = new HashSet<>();
		List<FranchiseOrderEntity> franchiseOrderEntityList = franchiseOrderRepository.findRefundInitiatedOrdersWithParentOrderId(parentOrderId);
		for (FranchiseOrderEntity franchiseOrder : franchiseOrderEntityList) {
			if (Objects.equals(franchiseOrder.getRefundType(), refundType)) {
				franchiseOrder.getOrderItems().forEach(item -> refundOrderSkuSet.add(item.getSkuCode()));
			}
		}
		return refundOrderSkuSet;
	}

	private void setFinalQuantities(FranchiseOrderEntity refundOrder, Map<String, Double> requestQuantityMap) {
		for (FranchiseOrderItemEntity item : refundOrder.getOrderItems()) {
			item.setFinalQuantity(requestQuantityMap.get(item.getSkuCode()));
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void confirmRefundRequest(FranchiseOrderEntity parentOrder, FranchiseOrderEntity refundOrder) {
		Map<String, FranchiseOrderItemEntity> parentOrderItemMap = getFranchiseOrderItemMap(parentOrder);
		for (FranchiseOrderItemEntity item : refundOrder.getOrderItems()) {
			FranchiseOrderItemEntity parentOrderItem = parentOrderItemMap.get(item.getSkuCode());
			parentOrderItem.setRefundAmount(BigDecimal.valueOf(parentOrderItem.getRefundAmount()).add(BigDecimal.valueOf(item.getFinalAmount())).doubleValue());
			if (item.getRefundDetails() != null) {
				item.getRefundDetails().setApprovedBy(getUserNameFromId(getCustomerId()));
			}
		}
		refundOrder.setStatus(FranchiseOrderStatus.ORDER_REFUNDED);
		parentOrder.setIsRefunded(1);
		parentOrder.setRefundAmount(refundOrder.getFinalBillAmount());
	}

	private Map<String, Double> getRequestQuantityMap(FranchiseOrderRefundBean request) {
		Map<String, Double> requestQtyMap = null;
		if (CollectionUtils.isNotEmpty(request.getRefundOrderItems())) {
			requestQtyMap = request.getRefundOrderItems().stream()
					.collect(Collectors.toMap(FranchiseRefundOrderItemBean::getSkuCode, FranchiseRefundOrderItemBean::getRefundQuantity));
		}
		return requestQtyMap;
	}

	private Map<String, FranchiseOrderItemEntity> getFranchiseOrderItemMap(FranchiseOrderEntity franchiseOrder) {
		Map<String, FranchiseOrderItemEntity> orderItemsMap = null;
		if (CollectionUtils.isNotEmpty(franchiseOrder.getOrderItems())) {
			orderItemsMap = franchiseOrder.getOrderItems().stream().collect(Collectors.toMap(i -> i.getSkuCode(), i -> i));
		}
		return orderItemsMap;
	}

	public FranchiseOrderEntity rebillFranchiseOrder(List<FranchiseReBillUploadBean> rebillData, FranchiseOrderEntity franchiseOrder) {
		Map<String, FranchiseOrderItemEntity> orderSkuMap = franchiseOrder.getOrderItems().stream()
				.collect(Collectors.toMap(FranchiseOrderItemEntity::getSkuCode, Function.identity()));
		for (FranchiseReBillUploadBean request : rebillData) {
			if (orderSkuMap.containsKey(request.getSkuCode())) {
				FranchiseOrderItemEntity item = orderSkuMap.get(request.getSkuCode());
				if (request.getFinalQty().compareTo(0d) == 0) {
					item.setStatus(FranchiseOrderItemStatus.NOT_AVAILABLE);
				} else {
					item.setStatus(FranchiseOrderItemStatus.PACKED);
				}
				item.setFinalCrateQty(request.getCrateQty());
				item.setFinalQuantity(request.getFinalQty());
			}
		}
		adjustWalletAmount(franchiseOrder, "FO-ADJUSTMENT");
		saveFranchiseEntity(franchiseOrder);
		return franchiseOrder;
	}

	private void adjustWalletAmount(FranchiseOrderEntity franchiseOrder, String txnType) {
		BigDecimal prevAmount;
		if (Objects.equals(txnType, "FO-ADJUSTMENT")) {
			prevAmount = BigDecimal.valueOf(franchiseOrder.getFinalBillAmount()).setScale(2, RoundingMode.HALF_UP);
		} else {
			prevAmount = franchiseOrder.getMetadata().getOrderPlacedAmount();
		}
		franchisePricingService.setAmountAndTaxesInOrderAndItem(franchiseOrder);
		BigDecimal currAmount = BigDecimal.valueOf(franchiseOrder.getFinalBillAmount()).setScale(2, RoundingMode.HALF_UP);
		if (Objects.equals(txnType, "Franchise-Order")) {
			deductMoneyFromStoreWallet(franchiseOrder.getStoreId(), -1 * currAmount.doubleValue(), franchiseOrder.getDisplayOrderId(), txnType,
					-1 * prevAmount.doubleValue(), null, null);
		} else if (prevAmount.compareTo(currAmount) != 0) {
			BigDecimal diffAmount = prevAmount.subtract(currAmount);
			deductMoneyFromStoreWallet(franchiseOrder.getStoreId(), diffAmount.doubleValue(), franchiseOrder.getDisplayOrderId(), txnType, null, null, null);
		}
		_LOGGER.info(String.format("updateFranchiseOrderStatus:: wallet updated for order %s ", franchiseOrder.getDisplayOrderId()));
		franchiseOrder.setAmountReceived(currAmount.doubleValue());
	}

	public FranchiseOrderEntity cancelFranchiseOrder(FranchiseOrderEntity franchiseOrder) {
		_LOGGER.debug(String.format("cancelFranchiseOrder::orderId: %s", franchiseOrder.getDisplayOrderId()));
		String storeId = franchiseOrder.getStoreId();
		String txnType = "FO-CANCELLED";
		deductMoneyFromStoreWallet(storeId, 0d, franchiseOrder.getDisplayOrderId(), txnType, -1 * franchiseOrder.getFinalBillAmount(), null, null);
		franchiseOrder.setAmountReceived(0d);
		franchiseOrder.setStatus(FranchiseOrderStatus.CANCELLED);
		saveFranchiseEntity(franchiseOrder);
		if (franchiseOrder.getIsSrUploaded() != null && franchiseOrder.getIsSrUploaded() == 1) {
			clientService.cancelSR(franchiseOrder.getId());
		}
		return franchiseOrder;
	}

	public FranchiseOrderEntity cancelFranchiseOrderPostBilling(FranchiseOrderEntity franchiseOrder, FranchiseOrderCancelPostBillingRequest request,
			String key) {
		_LOGGER.debug(String.format("cancelFranchiseOrder::orderId: %s", franchiseOrder.getDisplayOrderId()));
		String storeId = franchiseOrder.getStoreId();
		String txnType = "FO-CANCELLED_POST_BILLING";
		deductMoneyFromStoreWallet(storeId, franchiseOrder.getFinalBillAmount(), franchiseOrder.getDisplayOrderId(), txnType, null, request.getRemarks(), key);
		franchiseOrder.setStatus(FranchiseOrderStatus.CANCELLED_POST_BILLING);
		if (franchiseOrder.getMetadata() == null) {
			franchiseOrder.setMetadata(new FranchiseOrderMetadata());
		}
		franchiseOrder.getMetadata().setRemarks(request.getRemarks());
		saveFranchiseEntity(franchiseOrder);
		//TODO -- need confirmation what to do with SR
		return franchiseOrder;
	}

	public List<FranchiseOrderEntity> findStoresOrderOnDate(Set<String> storeIds, Date date, SRType type) {
		List<FranchiseOrderStatus> status = List.of(FranchiseOrderStatus.ORDER_BILLED, FranchiseOrderStatus.ORDER_DELIVERED);
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", date);
		filters.put("storeId", storeIds);
		filters.put("status", status);
		if (type != null) {
			filters.put("type", type);
		}
		List<FranchiseOrderEntity> franchiseOrders = findAllRecords(filters);
		return franchiseOrders;
	}

	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	public FranchiseOrderEntity confirmRefundRequestWithFinalAmounts(FranchiseOrderEntity refundOrder, FranchiseOrderEntity parentOrder,
			FranchiseConfirmOrderRefundBean request) {
		Map<String, Double> skuCodeToFinalAmountMap = request.getRefundOrderItems().stream()
				.collect(Collectors.toMap(FranchiseConfirmRefundOrderItemBean::getSkuCode, FranchiseConfirmRefundOrderItemBean::getRefundAmount));
		franchisePricingService.overrideOrderFinalAmount(refundOrder, skuCodeToFinalAmountMap);
		confirmRefundRequest(parentOrder, refundOrder);
		saveFranchiseEntity(refundOrder);
		saveFranchiseEntity(parentOrder);
		return refundOrder;
	}

	public PageAndSortResult<FranchiseOrderEntity> findFranchiseOrdersByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<FranchiseOrderEntity> poList = null;
		try {
			poList = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return poList;
	}

	public void cancelRefundOrder(FranchiseOrderEntity refundOrder) {
		refundOrder.setStatus(FranchiseOrderStatus.CANCELLED);
		for (FranchiseOrderItemEntity item : refundOrder.getOrderItems()) {
			if (item.getRefundDetails() != null) {
				item.getRefundDetails().setApprovedBy(getUserNameFromId(getCustomerId()));
			}
		}
		saveFranchiseEntity(refundOrder);
	}

	public void setAdjustmentDetailsAndCalculations(FranchiseOrderEntity entity, FranchiseOrderResponseBean response) {
		List<WalletStatementBean> walletStatement = clientService.fetchWalletStatementByTxnDetail(entity.getDisplayOrderId());
		if (!walletStatement.isEmpty()) {
			List<WalletStatementBean> adjustments = walletStatement.stream().filter(w -> w.getTxnType().startsWith("FO-"))
					.filter(w -> !w.getTxnType().equals("FO-ADJUSTMENT")).collect(Collectors.toList());
			if (!adjustments.isEmpty()) {
				BigDecimal totalAdjustment = BigDecimal.ZERO;
				for (WalletStatementBean statement : adjustments) {
					if (statement.getTxnMode().equals("CREDIT")) {
						totalAdjustment = totalAdjustment.add(BigDecimal.valueOf(statement.getAmount()));
					} else if (statement.getTxnMode().equals("DEBIT")) {
						totalAdjustment = totalAdjustment.subtract(BigDecimal.valueOf(statement.getAmount()));
					}
				}
				Double totalBillAfterAdjustment = BigDecimal.valueOf(entity.getFinalBillAmount()).subtract(totalAdjustment).doubleValue();
				response.setAdjustments(adjustments);
				response.setTotalAdjustment(totalAdjustment.doubleValue());
				response.setTotalBillAfterAdjustment(totalBillAfterAdjustment);
				addLabels(adjustments);
			}
		}
	}

	private void addLabels(List<WalletStatementBean> walletStatement) {
		String txnTypeLabels = ParamsUtils.getParam("TXN_TYPE_LABELS");
		if (txnTypeLabels != null) {
			Map<String, String> txnTypeLabelMap = Arrays.asList(txnTypeLabels.split(",")).stream().map(s -> s.split(":"))
					.collect(Collectors.toMap(e -> e[0], e -> e[1]));
			updateLabels(walletStatement, txnTypeLabelMap);
		}
	}

	private void updateLabels(List<WalletStatementBean> walletStatement, Map<String, String> txnTypeLabelMap) {
		for (WalletStatementBean ws : walletStatement) {
			if (txnTypeLabelMap.containsKey(ws.getTxnType())) {
				ws.setTxnType(txnTypeLabelMap.get(ws.getTxnType()));
			}
		}
	}

	public FranchiseOrderEntity findByDisplayOrderId(String displayOrderId) {
		FranchiseOrderEntity result = franchiseOrderRepository.findByDisplayOrderId(displayOrderId);
		if (result != null) {
			return result;
		}
		return null;
	}

	public List<FranchiseOrderEntity> findOrdersByDisplayOrderId(List<String> displayOrderIdList) {
		return franchiseOrderRepository.findOrdersByDisplayOrderId(displayOrderIdList);
	}

	public Map<String, Long> getFranchiseOrderCountMap(List<String> storeIds) {
		List<Object[]> results = franchiseOrderRepository.getOrderCountMap(storeIds);
		return results.stream().collect(Collectors.toMap(result -> (String) result[0], result -> (Long) result[1]));
	}

	public Long getDeliveredOrderCount(String storeId) {
		return franchiseOrderRepository.getDeliveredOrderCount(storeId);
	}

	public void saveAllOrder(List<FranchiseOrderEntity> orders) {
		franchiseOrderRepository.saveAll(orders);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity updateCart(FranchiseOrderEntity cart) {
		franchisePricingService.setAmountAndTaxesInOrderAndItem(cart);
		return cart;
	}

	public List<FranchiseOrderEntity> getTodayOrders(String storeId) {
		Date today = java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata")));
		List<FranchiseOrderEntity> orderEntityList = franchiseOrderRepository.findOrdersForToday(storeId, today);
		return orderEntityList;
	}

	public FranchiseOrderEntity getFirstOrder(String storeId) {
		Pageable pageable = PageRequest.of(0, 1);
		List<FranchiseOrderEntity> orderEntityList = franchiseOrderRepository.findFirstOrderByStoreId(storeId, pageable);
		if (orderEntityList.isEmpty()) {
			return null;
		}
		return orderEntityList.get(0);
	}

	public UUID getCurrentOrderForTicket(List<FranchiseOrderStatus> statuses, String storeId, Date deliveryDate, List<String> slots) {
		return franchiseOrderRepository.getDeliveredOrderByStoreIdAndDateAndSlotIn(statuses, storeId, deliveryDate, slots);
	}

	public List<FranchiseOrderEntity> getOrdersForTickets(List<FranchiseOrderStatus> statuses, String storeId, Date fromDate, List<String> slots) {
		return franchiseOrderRepository.findOrdersForTicketsWithDateAfter(statuses, storeId, fromDate, slots);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity createRefundOrderIms(FranchiseOrderEntity parentOrder, ImsFranchiseOrderRefundBean request, String key) {
		FranchiseOrderEntity refundOrder = generateRefundRequestOrderIms(parentOrder, request);
		confirmRefundRequest(parentOrder, refundOrder);
		refundOrder.setKey(key);
		franchiseOrderRepository.save(refundOrder);
		franchiseOrderRepository.save(parentOrder);
		return refundOrder;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity generateRefundRequestOrderIms(FranchiseOrderEntity parentOrder, ImsFranchiseOrderRefundBean request) {
		Map<String, FranchiseOrderItemEntity> parentOrderItemMap = getFranchiseOrderItemMap(parentOrder);
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		FranchiseOrderEntity refundOrder = FranchiseOrderEntity.createNewRefundOrder(parentOrder, displayOrderId,
				FranchiseOrderConstants.IMS_REFUND_ORDER_REFUND_TYPE);
		refundOrder.setOrderItems(new ArrayList<>());
		int itemCount = 0;

		List<FranchiseOrderEntity> refundedOrderEntityList = franchiseOrderRepository.findRefundConfirmedOrdersWithParentOrderId(parentOrder.getId());
		Map<String, Double> refundedOrderQtyMap = getRefundedOrderQtyMap(refundedOrderEntityList);

		for (ImsFranchiseOrderRefundItemBean item : request.getRefundOrderItems()) {
			Double totalReturnedQtyAfterRefund = refundedOrderQtyMap.containsKey(item.getSkuCode()) ?
					BigDecimal.valueOf(refundedOrderQtyMap.get(item.getSkuCode())).add(BigDecimal.valueOf(item.getRefundQuantity())).doubleValue() :
					item.getRefundQuantity();
			if (parentOrderItemMap != null && parentOrderItemMap.containsKey(item.getSkuCode()) && parentOrderItemMap.get(item.getSkuCode()).getFinalQuantity()
					.compareTo(totalReturnedQtyAfterRefund) != -1) {
				FranchiseOrderItemEntity parentOrderItem = parentOrderItemMap.get(item.getSkuCode());
				FranchiseOrderItemEntity refundOrderItem = FranchiseOrderItemEntity.createNewRefundOrderItem(refundOrder, item.getRefundQuantity(),
						parentOrderItem, getRefundDetails(item.getRefundRemarks(), item.getWarehouseReturnCheck(), getCustomerId()));
				refundOrder.addOrderItem(refundOrderItem);
				itemCount++;
			} else {
				if (parentOrderItemMap == null) {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
							String.format("Items not found in parent order with id : %s", parentOrder.getId().toString()), "order"));
				} else if (!parentOrderItemMap.containsKey(item.getSkuCode())) {
					throw new ValidationException(
							ErrorBean.withError(Errors.INVALID_REQUEST, String.format("SKU %s not found in parent order", item.getSkuCode()), "order"));
				} else {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
							String.format("Total refund quantity for Sku Code %s will exceed total delivered quantity : %s. Quantity already refunded is : %s",
									item.getSkuCode(), parentOrderItemMap.get(item.getSkuCode()).getFinalQuantity(),
									refundedOrderQtyMap.get(item.getSkuCode())), "order"));
				}
			}
		}

		refundOrder.setItemCount(itemCount);
		refundOrder.setSubmittedAt(new Date());
		franchisePricingService.setAmountAndTaxesInOrderAndItem(refundOrder);
		refundOrder.setEstimatedBillAmount(refundOrder.getFinalBillAmount());

		return refundOrder;
	}

	public FranchiseOrderEntity findByKey(String key) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("key", key);
		List<FranchiseOrderEntity> entityList = findAllRecords(filters);
		if (!entityList.isEmpty()) {
			return entityList.get(0);
		}
		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity createRefundAllOrderIms(FranchiseOrderEntity parentOrder, ImsFranchiseOrderRefundAllBean request, String key) {
		FranchiseOrderEntity refundOrder = generateRefundAllRequestOrderIms(parentOrder, request);
		confirmRefundRequest(parentOrder, refundOrder);
		refundOrder.setKey(key);
		franchiseOrderRepository.save(refundOrder);
		franchiseOrderRepository.save(parentOrder);
		return refundOrder;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FranchiseOrderEntity generateRefundAllRequestOrderIms(FranchiseOrderEntity parentOrder, ImsFranchiseOrderRefundAllBean request) {
		Map<String, FranchiseOrderItemEntity> parentOrderItemMap = getFranchiseOrderItemMap(parentOrder);
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		FranchiseOrderEntity refundOrder = FranchiseOrderEntity.createNewRefundOrder(parentOrder, displayOrderId,
				FranchiseOrderConstants.IMS_REFUND_ORDER_REFUND_TYPE);
		refundOrder.setOrderItems(new ArrayList<>());
		int itemCount = 0;

		List<FranchiseOrderEntity> refundedOrderEntityList = franchiseOrderRepository.findRefundConfirmedOrdersWithParentOrderId(parentOrder.getId());
		Map<String, Double> refundedOrderQtyMap = getRefundedOrderQtyMap(refundedOrderEntityList);

		for (FranchiseOrderItemEntity parentOrderItem : parentOrderItemMap.values()) {
			Double availableQtyForRefund = refundedOrderQtyMap.containsKey(parentOrderItem.getSkuCode()) ?
					BigDecimal.valueOf(parentOrderItem.getFinalQuantity()).subtract(BigDecimal.valueOf(refundedOrderQtyMap.get(parentOrderItem.getSkuCode())))
							.doubleValue() :
					parentOrderItem.getFinalQuantity();
			FranchiseOrderItemEntity refundOrderItem = FranchiseOrderItemEntity.createNewRefundOrderItem(refundOrder, availableQtyForRefund, parentOrderItem,
					getRefundDetails(request.getRefundRemarks(), request.getWarehouseReturnCheck(), getCustomerId()));
			refundOrder.addOrderItem(refundOrderItem);
			itemCount++;
		}

		refundOrder.setItemCount(itemCount);
		refundOrder.setSubmittedAt(new Date());
		franchisePricingService.setAmountAndTaxesInOrderAndItem(refundOrder);
		refundOrder.setEstimatedBillAmount(refundOrder.getFinalBillAmount());

		return refundOrder;
	}

	private Map<String, Double> getRefundedOrderQtyMap(List<FranchiseOrderEntity> refundedOrderEntityList) {
		Map<String, Double> refundedOrderQtyMap = new HashMap<>();
		for (FranchiseOrderEntity refundedOrder : refundedOrderEntityList) {
			refundedOrder.getOrderItems().forEach(item -> {
				Double totalReturnedQty = refundedOrderQtyMap.containsKey(item.getSkuCode()) ?
						BigDecimal.valueOf(refundedOrderQtyMap.get(item.getSkuCode())).add(BigDecimal.valueOf(item.getFinalQuantity())).doubleValue() :
						item.getFinalQuantity();
				refundedOrderQtyMap.put(item.getSkuCode(), totalReturnedQty);
			});
		}
		return refundedOrderQtyMap;
	}

	public List<StoreRevenueBean> fetchStoreRevenue(Date startDate, List<String> storeIds) {
		List<FranchiseOrderStatus> statuses = Arrays.asList(FranchiseOrderStatus.IN_CART, FranchiseOrderStatus.NEW_ORDER, FranchiseOrderStatus.ORDER_BILLED,
				FranchiseOrderStatus.OUT_FOR_DELIVERY, FranchiseOrderStatus.ORDER_DELIVERED, FranchiseOrderStatus.PARTIALLY_DISPATCHED);
		List<StoreRevenueBean> revenueList;
		if (CollectionUtils.isEmpty(storeIds)) {
			revenueList = franchiseOrderRepository.fetchStoreRevenue(startDate, statuses);
		} else {
			revenueList = franchiseOrderRepository.fetchStoreRevenueByStoreIds(startDate, statuses, storeIds);
		}
		return revenueList;
	}

	public List<StoreOrderDetails> fetchStoreWiseSkuDetails(List<String> storeIds, Date deliveryDate, String slot) {
		List<StoreSkuOrderCount> storeSkuOrderCounts = franchiseOrderRepository.fetchStoreLevelSkuOrderCount(storeIds, deliveryDate, slot);
		List<StoreOrderDetails> storeOrderDetailList = new ArrayList<>();
		Map<String, List<StoreSkuOrderCount>> storeIdToSkusMap = storeSkuOrderCounts.stream().collect(Collectors.groupingBy(StoreSkuOrderCount::getStoreId));
		storeIdToSkusMap.entrySet().forEach(entry -> {
			StoreOrderDetails storeOrderDetail = StoreOrderDetails.newInstance();
			storeOrderDetail.setStoreId(Integer.valueOf(entry.getKey()));
			List<SkuDetails> skus = new ArrayList<>();
			entry.getValue().forEach(value -> {
				SkuDetails skuDetails = SkuDetails.newInstance();
				skuDetails.setSkuCode(value.getSkuCode());
				skuDetails.setOrderedCrateQty(Math.toIntExact(value.getOrderedCrateQty()));
				skus.add(skuDetails);
			});
			storeOrderDetail.setSkuDetails(skus);
			storeOrderDetailList.add(storeOrderDetail);
		});
		return storeOrderDetailList;
	}

	public List<FranchiseOrderEntity> findAllRefundOrdersByParentIds(List<UUID> parentOrderIds) {
		Map<String, Object> params = new HashMap<>();
		if (parentOrderIds != null) {
			params.put("parentOrderId", parentOrderIds);
		}
		return findAllRecords(params);
	}

	public void addSpGrossAmountWithoutBulkSkus(FranchiseOrderEntity order) {
		Double spGrossAmountWithoutBulkSkus = order.getTotalSpGrossAmount();
		Map<String, Integer> bulkSkuCodesSet = getBulkSkuCodesSet();
		for (FranchiseOrderItemEntity orderItem : order.getOrderItems()) {
			if (bulkSkuCodesSet.containsKey(orderItem.getSkuCode()) && orderItem.getFinalCrateQty() >= bulkSkuCodesSet.get(orderItem.getSkuCode())) {
				spGrossAmountWithoutBulkSkus -= orderItem.getSpGrossAmount();
			}
		}
		order.setSpGrossAmountWithoutBulkSkus(spGrossAmountWithoutBulkSkus);
		order.setEffectiveSpGrossAmountForCashback(spGrossAmountWithoutBulkSkus);
	}

	public Map<String, Integer> getBulkSkuCodesSet() {
		String bulkSkusQtyMapString = ParamsUtils.getParam("BULK_SKU_QTY_MAP");
		Map<String, Integer> bulkSkusQtyMap = new HashMap<>();
		for (String deliveryChargesPairString : bulkSkusQtyMapString.split(",")) {
			List<String> KeyValuePair = Arrays.asList(deliveryChargesPairString.split(":"));
			bulkSkusQtyMap.put(KeyValuePair.get(0), Integer.valueOf(KeyValuePair.get(1)));
		}
		return bulkSkusQtyMap;
	}

	public List<StoreOrderInfo> findActiveOrderCountByStoreId(List<String> storeIds) {
		return franchiseOrderRepository.findActiveOrderCountByStoreId(storeIds);
	}

	public String getIstTimeString() {
		return java.time.LocalDateTime.now().plusHours(5).plusMinutes(30).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
	}

	public void autoRemoveEligibleCartItems(Map<String, Integer> whSkuExtraQtyMap, List<FranchiseOrderItemEntity> allCartItems,
			Map<UUID, FranchiseOrderEntity> orderIdCartMap, Set<UUID> priorityRemovalCartIds) {
		Map<String, List<FranchiseOrderItemEntity>> skuWiseOrderedCartItems = allCartItems.stream()
				.filter(item -> whSkuExtraQtyMap.containsKey(item.getWhId() + "|" + item.getSkuCode()))
				.collect(Collectors.groupingBy(item -> item.getWhId() + "|" + item.getSkuCode(), Collectors.toList()));
		Comparator<FranchiseOrderItemEntity> customComparator = (item1, item2) -> {
			if (priorityRemovalCartIds.contains(item1.getOrderId()) != priorityRemovalCartIds.contains(item2.getOrderId())) {
				return priorityRemovalCartIds.contains(item1.getOrderId()) ? -1 : 1;
			}
			return item2.getCreatedAt().compareTo(item1.getCreatedAt());
		};
		skuWiseOrderedCartItems.values().forEach(list -> list.sort(customComparator));
		HashSet<UUID> affectedCartIds = new HashSet<>();
		Map<String, List<String>> storeIdAffectedSkuNameMap = new HashMap<>();
		for (Map.Entry<String, Integer> entry : whSkuExtraQtyMap.entrySet()) {
			Integer extraQty = entry.getValue();
			List<FranchiseOrderItemEntity> cartItemsQueue = skuWiseOrderedCartItems.getOrDefault(entry.getKey(), new ArrayList<>());
			int i = 0;
			while (extraQty > 0 && i < cartItemsQueue.size()) {
				FranchiseOrderItemEntity item = cartItemsQueue.get(i);
				FranchiseOrderEntity cart = orderIdCartMap.get(item.getOrderId());
				Integer previousQuantity = item.getFinalCrateQty();
				Integer newQuantity;
				if (item.getFinalCrateQty().compareTo(extraQty) > 0) {
					newQuantity = item.getFinalCrateQty() - extraQty;
					item.setFinalQuantity(item.getMoq() * newQuantity);
					item.setOrderedQty(item.getMoq() * newQuantity);
					item.setFinalCrateQty(newQuantity);
					item.setOrderedCrateQty(newQuantity);
				} else {
					newQuantity = 0;
					removeItemFromFranchiseCart(cart, item);
				}
				extraQty -= previousQuantity - newQuantity;
				affectedCartIds.add(cart.getId());
				updateWHInventory(item, previousQuantity - newQuantity, cart.getStoreId());
				addFranchiseOrderItemAutoRemovalLogs(cart.getId(), item.getSkuCode(), cart.getStoreId(), newQuantity, previousQuantity);
				storeIdAffectedSkuNameMap.computeIfAbsent(cart.getStoreId(), k -> new ArrayList<>()).add(item.getProductName());
				i++;
			}
		}
		if (CollectionUtils.isNotEmpty(affectedCartIds)) {
			Map<String, Long> franchiseOrderCountMap = getFranchiseOrderCountMap(new ArrayList<>(storeIdAffectedSkuNameMap.keySet()));
			Double amountLimit = getAmountLimit(orderIdCartMap.get(affectedCartIds.stream().findFirst().get()).getSlot());
			for (UUID id : affectedCartIds) {
				FranchiseOrderEntity affectedCart = orderIdCartMap.get(id);
				affectedCart.getMetadata().setIsEligiblePreAutoRemoval(validMinOrderRule(affectedCart, amountLimit, franchiseOrderCountMap));
				franchisePricingService.setAmountAndTaxesInOrderAndItem(orderIdCartMap.get(id));
			}
		}
		WhatsappSendMsgRequest whatsappSendMsgRequest = buildAutoCartRemovalWhatsappMsgRequests(storeIdAffectedSkuNameMap);
		if (!whatsappSendMsgRequest.getMessageRequests().isEmpty()) {
			clientService.sendWhatsappMessages(whatsappSendMsgRequest);
		}
	}

	private void addFranchiseOrderItemAutoRemovalLogs(UUID orderId, String skuCode, String storeId, Integer newQty, Integer previousQty) {
		FranchiseOrderItemLogEntity itemLogEntity = buildOrderItemAutoRemovalLog(orderId, skuCode, storeId, newQty, previousQty);
		CompletableFuture.runAsync(() -> franchiseOrderItemLogRepository.save(itemLogEntity));
	}

	private FranchiseOrderItemLogEntity buildOrderItemAutoRemovalLog(UUID orderId, String skuCode, String storeId, Integer newQty, Integer previousQty) {
		FranchiseOrderItemLogEntity itemLogEntity = FranchiseOrderItemLogEntity.newInstance();
		itemLogEntity.setStoreId(storeId);
		itemLogEntity.setSkuCode(skuCode);
		itemLogEntity.setFromQty(previousQty);
		itemLogEntity.setToQty(newQty);
		itemLogEntity.setOrderId(orderId);
		itemLogEntity.setType(FranchiseOrderItemLogType.AUTO_REMOVAL);
		return itemLogEntity;
	}

	private WhatsappSendMsgRequest buildAutoCartRemovalWhatsappMsgRequests(Map<String, List<String>> storeIdAffectedSkuNameMap) {
		WhatsappSendMsgRequest request = new WhatsappSendMsgRequest();
		if (!storeIdAffectedSkuNameMap.isEmpty()) {
			List<StoreDataResponse> storeDataResponseList = clientService.getStoresData(
					StoreSearchRequest.builder().storeIds(new ArrayList<>(storeIdAffectedSkuNameMap.keySet())).build());
			Map<String, UUID> storeIdOwnerIdMap = storeDataResponseList.stream()
					.collect(Collectors.toMap(StoreDataResponse::getStoreId, StoreDataResponse::getOwnerId));
			for (Map.Entry<String, List<String>> entry : storeIdAffectedSkuNameMap.entrySet()) {
				if (storeIdOwnerIdMap.containsKey(entry.getKey())) {
					Map<String, String> fillers = new HashMap<>();
					fillers.put("skunames", String.join(", ", entry.getValue()));
					request.getMessageRequests()
							.add(WhatsappSendMsgRequest.WhatsappSendMsgSingleRequest.builder().userId(storeIdOwnerIdMap.get(entry.getKey())).fillers(fillers)
									.messageType("text").templateName("AUTO_CART_REMOVAL").build());
				}
			}
		}
		return request;
	}

	public Double getAmountLimit(String slot) {
		Double amountLimit;
		String morningSlot = ParamsUtils.getParam("MORNING_7_AM_SLOT", "MORNING_7_AM");
		if (morningSlot.equals(slot)) {
			amountLimit = Double.parseDouble(ParamsUtils.getParam("FO_MIN_AMOUNT_LIMIT_MORNING_SLOT", "1000"));
		} else {
			amountLimit = Double.parseDouble(ParamsUtils.getParam("FO_MIN_AMOUNT_LIMIT_NOON_SLOT", "100"));
		}
		return amountLimit;
	}

	public boolean validMinOrderRule(FranchiseOrderEntity order, Double amountLimit, Map<String, Long> franchiseOrderCountMap) {
		Long orderCount = franchiseOrderCountMap.getOrDefault(order.getStoreId(), 0L);
		if (orderCount > 0 && Double.compare(order.getTotalSpGrossAmount(), amountLimit) < 0) {
			return false;
		}
		return true;
	}

	public Map<String, WalletBean> getWalletMap(List<String> storeIds) {
		List<WalletBean> userWallets = clientService.getStoreWalletsInternal(storeIds);
		return userWallets.stream().collect(Collectors.toMap(WalletBean::getEntityId, Function.identity()));
	}
}