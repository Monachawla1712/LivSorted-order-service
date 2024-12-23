package com.sorted.rest.services.order.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.constants.Limit;
import com.sorted.rest.common.dbsupport.constants.Operation;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.FilterCriteria;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest.SortDirection;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.upload.UploadService;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.beans.CartRequest.OrderItemGradeBean;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.ProductTags;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.StoreProductInventory;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.OrderItemStatus;
import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.entity.*;
import com.sorted.rest.services.order.repository.OrderRepository;
import com.sorted.rest.services.order.repository.OrderSlotRepository;
import com.sorted.rest.services.order.repository.UserOrderSettingRepository;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mohit on 20.6.20.
 */
@Service
public class OrderService implements BaseService<OrderEntity> {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PricingService pricingService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private DisplayOrderIdService displayOrderIdService;

	@Autowired
	private OrderSlotRepository orderSlotRepository;

	@Autowired
	private UploadService uploadService;

	@Autowired
	private UserOrderSettingRepository userOrderSettingRepository;

	AppLogger _LOGGER = LoggingManager.getLogger(OrderService.class);

	public OrderEntity findById(UUID id) {
		Optional<OrderEntity> resultOpt = orderRepository.findById(id);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	public OrderEntity findRecordByDisplayOrderId(String displayOrderId) {
		Optional<OrderEntity> resultOpt = orderRepository.findByDisplayOrderId(displayOrderId);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity save(OrderEntity entity) {
		OrderEntity result = orderRepository.save(entity);
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity findCustomerCurrentCart(UUID customerId, Date deliveryDate) {
		List<OrderEntity> resultList = orderRepository.findCustomerCurrentCart(customerId, new java.sql.Date(deliveryDate.getTime()));
		OrderEntity result = null;
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
				save(o);
			});
		}
		return result;
	}

	public List<OrderEntity> findCustomerOrderList(UUID customerId) {
		List<OrderEntity> result = orderRepository.findCustomerOrderList(customerId);
		return result;
	}

	public Long getDeliveredOrderCount(UUID customerId) {
		String validChannel = ParamsUtils.getParam("VALID_CHANNELS");
		List<String> validChannelList = List.of(validChannel.split(","));
		Long result = orderRepository.getDeliveredOrderCount(customerId, validChannelList);
		return result;
	}

	@Override
	public Class<OrderEntity> getEntity() {
		return OrderEntity.class;
	}

	@Override
	public BaseCrudRepository<?,?> getRepository() {
		return orderRepository;
	}

	public List<OrderCharges> setOrderCharges(OrderEntity order) {
		List<OrderCharges> list = new ArrayList<OrderCharges>();
		OrderCharges orderCharges = new OrderCharges();
		if (order.getTotalDiscountAmount() != null && Double.compare(order.getTotalDiscountAmount(), 0.0) > 0) {
			orderCharges = new OrderCharges();
			orderCharges.setName("Total Item Charges");
			orderCharges.setAmount(order.getTotalSpGrossAmount());
			list.add(orderCharges);

			orderCharges = new OrderCharges();
			orderCharges.setName("Discount Amount");
			orderCharges.setAmount(order.getTotalDiscountAmount());
			list.add(orderCharges);
		}
		orderCharges = new OrderCharges();
		orderCharges.setName("Delivery Charges");
		orderCharges.setAmount(order.getExtraFeeDetails().getDeliveryCharge());
		list.add(orderCharges);

		orderCharges = new OrderCharges();
		orderCharges.setName("Packing Charges");
		orderCharges.setAmount(order.getExtraFeeDetails().getPackingCharge());
		list.add(orderCharges);

		orderCharges = new OrderCharges();
		orderCharges.setName("Slot Charges");
		orderCharges.setAmount(order.getExtraFeeDetails().getSlotCharges());
		list.add(orderCharges);
		return list;
	}

	private Map<String, BigDecimal> buildProductTagMap(OrderEntity cart, String param) {
		Map<String, BigDecimal> tagMap = new HashMap<>();
		String paramString = ParamsUtils.getParam(param);
		LinkedHashSet<String> paramSet = new LinkedHashSet<>(Arrays.asList(paramString.split(",")));
		if (cart.getOrderItems() != null && cart.getOrderItems().size() > 0) {
			for (OrderItemEntity cartItem : cart.getOrderItems()) {
				for (ProductTags tag : cartItem.getProductTags()) {
					if (tag.getValue() != null && param.equals("PRODUCT_METADATA_PRIORITY") && paramSet.contains(tag.getDisplayName())) {
						BigDecimal tagValue = BigDecimal.valueOf(tag.getValue()).multiply(BigDecimal.valueOf(cartItem.getFinalQuantity()));
						if (tagMap.containsKey(tag.getDisplayName())) {
							tagValue = tagMap.get(tag.getDisplayName()).add(tagValue);
						}
						tagMap.put(tag.getDisplayName(), tagValue);
					} else if (param.equals("PRODUCT_METADATA_OTHERS") && paramSet.contains(tag.getDisplayName())) {
						tagMap.put(tag.getDisplayName(), null);
					}
				}
			}
		}
		if (param.equals("PRODUCT_METADATA_PRIORITY")) {
			for (String tag : paramSet) {
				if (!tagMap.containsKey(tag)) {
					tagMap.put(tag, BigDecimal.ZERO);
				}
			}
		}
		return tagMap;
	}

	private List<OrderProductMetadata> getProductMetadataList(Map<String, BigDecimal> tagMap, Map<String, String> tagColorMap, String param) {
		List<OrderProductMetadata> tagList = new ArrayList<>();
		String paramString = ParamsUtils.getParam(param);
		List<String> paramSet = Arrays.asList(paramString.split(","));
		for (String tagName : paramSet) {
			if (tagMap.containsKey(tagName)) {
				OrderProductMetadata productMetadata = new OrderProductMetadata();
				productMetadata.setName(tagName);
				productMetadata.setValue((tagMap.get(tagName) != null) ? tagMap.get(tagName).doubleValue() : null);
				productMetadata.setColor(tagColorMap.get(tagName));
				tagList.add(productMetadata);
			}
		}
		return tagList;
	}

	private String getPdfNameWithDate(String s, String fileName) {
		return s + "/" + fileName + ".pdf";
	}

	public String uploadFile(OrderEntity orderEntity) {
		String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
		String subDirectory = ParamsUtils.getParam("PAYABLE_FILES_DIRECTORY");
		String s = System.getProperty("user.dir");
		File file = new File(getPdfNameWithDate(s, orderEntity.getDisplayOrderId()));
		try {
			if (file.exists()) {
				Date date = orderEntity.getDeliveryDate();
				SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
				String formattedDate = formatter.format(date);
				String filename = String.format("%s-%s-%s.pdf", formattedDate, orderEntity.getDisplayOrderId(), String.valueOf(Instant.now().toEpochMilli()));
				byte[] fileBytes = Files.readAllBytes(file.toPath());
				Object response = uploadService.uploadFile(bucketName, subDirectory, fileBytes, filename);
				file.delete();
				return response.toString();
			} else {
				_LOGGER.info(String.format("Could not find file with name: %s",orderEntity.getDisplayOrderId()));
			}
		} catch (IOException err) {
			_LOGGER.error("Error while uploading Invoice", err);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, String.format("Error while uploading invoice: ", err.getMessage()), "invoice"));
		}
		return null;
	}

	public void updateOrderProductMetadata(OrderEntity cart) {
		List<OrderItemEntity> orderItems = getNonOrderOfferOrderItems(cart);
		Map<String, Long> categoryCount = orderItems.stream().filter(orderItem -> orderItem.getFinalQuantity() != 0d)
				.collect(Collectors.groupingBy(OrderItemEntity::getCategoryName, Collectors.counting()));
		Long otpItemCount = getOtpItemCount(cart);
		categoryCount.put("otp", otpItemCount);
		Map<String, BigDecimal> tagMap = buildProductTagMap(cart, "PRODUCT_METADATA_PRIORITY");
		Map<String, BigDecimal> alsoContainsTagMap = buildProductTagMap(cart, "PRODUCT_METADATA_OTHERS");
		Map<String, String> tagColorMap = buildProductTagColorMap(cart);
		BigDecimal calories = new BigDecimal(0);
		if (tagMap.containsKey("Calories")) {
			calories = tagMap.get("Calories");
			tagMap.remove("Calories");
		}
		cart.getMetadata().setCalories(calories);
		List<OrderProductMetadata> productMetadataList = getProductMetadataList(tagMap, tagColorMap, "PRODUCT_METADATA_PRIORITY");
		List<OrderProductMetadata> alsoContainsProductMetadataList = getProductMetadataList(alsoContainsTagMap, tagColorMap, "PRODUCT_METADATA_OTHERS");
		cart.getMetadata().setProductMetadata(productMetadataList);
		cart.getMetadata().setAlsoContains(alsoContainsProductMetadataList);
		cart.getMetadata().setCategoryCount(categoryCount);
	}

	public List<OrderItemEntity> getNonOrderOfferOrderItems(OrderEntity cart) {
		if (CollectionUtils.isEmpty(cart.getOrderItems())) {
			return Collections.emptyList();
		}
		return cart.getOrderItems().stream()
				.filter(orderItem -> orderItem.getMetadata().getIsOrderOfferItem() == null || !orderItem.getMetadata().getIsOrderOfferItem())
				.collect(Collectors.toList());
	}

	private Map<String, String> buildProductTagColorMap(OrderEntity cart) {
		Map<String, String> tagColorMap = new HashMap<>();
		if (cart.getOrderItems() != null && cart.getOrderItems().size() > 0) {
			for (OrderItemEntity cartItem : cart.getOrderItems()) {
				for (ProductTags tag : cartItem.getProductTags()) {
					if (tag.getColor() != null) {
						tagColorMap.put(tag.getDisplayName(), tag.getColor());
					}
				}
			}
		}
		return tagColorMap;
	}

	private Long getOtpItemCount(OrderEntity cart) {
		Long count = 0L;
		if(CollectionUtils.isEmpty(cart.getOrderItems())) {
			return count;
		}
		for (OrderItemEntity orderItem : cart.getOrderItems()) {
			if (orderItem.getIsCoinsRedeemedItem() == 1) {
				count++;
			}
		}
		return count;
	}

	public UUID getUserId() {
		UUID userId = SessionUtils.getAuthUserId();
		Assert.notNull(userId, "CustomerId could not be empty");
		return userId;
	}

	private StoreInventoryAddOrDeductRequest createStoreInventoryAddOrDeductRequest(List<StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData> data) {
		StoreInventoryAddOrDeductRequest storeInvRequest = new StoreInventoryAddOrDeductRequest(data);
		return storeInvRequest;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity createOrderEntityFromBackoffice(OrderEntity order, Map<String, StoreProductInventory> storeInvMap) {
		order.setPaymentMethod(OrderConstants.PaymentMethod.WALLET);
		order.setShippingMethod(OrderConstants.ShippingMethod.HOME_DELIVERY);
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		order.setDisplayOrderId("OD-" + displayOrderId);

		List<OrderItemEntity> orderItemEntityList = order.getOrderItems();
		order.setOrderItems(null);

		int itemCount = 0;
		order.setItemCount(itemCount);
		order.setStatus(OrderStatus.IN_CART);
		order = save(order);

		order.setOrderItems(new ArrayList<>());

		for (OrderItemEntity item : orderItemEntityList) {
			StoreProductInventory storeItem = null;
			if (storeInvMap != null) {
				storeItem = storeInvMap.get(item.getSkuCode());
			}
			if (storeItem != null) {
				buildOrderItemEntityFromBackoffice(item, storeItem);
				item.setOrderId(order.getId());
				item.setOrder(order);
				order.getOrderItems().add(item);
				itemCount++;
			}
		}
		order.setItemCount(itemCount);
		order.setSubmittedAt(new Date());
		pricingService.setAmountAndTaxesInOrderAndItems(order, CoinsParamsObject.newInstance());
		validateOrderCountAndWallet(order);
		order.setEstimatedBillAmount(order.getFinalBillAmount());
		updateOrderProductMetadata(order);
		return save(order);
	}

	private void validateOrderCountAndWallet(OrderEntity order) {
		Integer codCountLimit = ParamsUtils.getIntegerParam("COD_ORDER_COUNT_LIMIT", 7);
		if (order.getMetadata().getOrderCount() != null && order.getMetadata().getOrderCount() > codCountLimit && !hasSufficientWalletMoney(order)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order cannot be placed due to insufficient wallet balance.", "wallet"));
		}
	}

	private boolean hasSufficientWalletMoney(OrderEntity order) {
		WalletBean walletBean = clientService.getUserWallet(order.getCustomerId().toString());
		BigDecimal walletAmount = BigDecimal.valueOf(0);
		if (walletBean == null) {
			return false;
		} else {
			walletAmount = BigDecimal.valueOf(walletBean.getAmount());
		}
		BigDecimal expectedAmount = BigDecimal.valueOf(order.getFinalBillAmount());
		if (expectedAmount.subtract(walletAmount).compareTo((BigDecimal.valueOf(0))) > 0) {
			return false;
		}
		return true;
	}

	private void buildOrderItemEntityFromBackoffice(OrderItemEntity orderItem, StoreProductInventory storeItem) {
		if (storeItem != null) {
			orderItem.setSkuCode(storeItem.getInventorySkuCode());
			orderItem.setProductName(storeItem.getProductDisplayName());
			orderItem.setImageUrl(storeItem.getProductImageUrl());
			orderItem.setUom(storeItem.getProductUnitOfMeasurement());
			orderItem.setProductTags(storeItem.getProductTags());
			orderItem.setCategoryId(storeItem.getProductCategoryId());
			orderItem.setCategoryName(storeItem.getCategoryName());
			orderItem.setMarkedPrice(BigDecimal.valueOf(storeItem.getInventoryMarketPrice()));
			orderItem.setSalePrice(OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(), orderItem.getOrderedQty(), orderItem));
			if (storeItem.getInventoryQuantity() < orderItem.getOrderedQty()) {
				orderItem.setFinalQuantity(storeItem.getInventoryQuantity());
			} else {
				orderItem.setFinalQuantity(orderItem.getOrderedQty());
			}
			orderItem.setSpGrossAmount(0d);
			orderItem.setMrpGrossAmount(0d);
			orderItem.setFinalAmount(0d);
		}
		orderItem.setStatus(OrderItemStatus.PENDING);
		orderItem.setIsRefundable(1);
		orderItem.setIsReturnable(1);
	}

	public PageAndSortResult<OrderEntity> findOrdersByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<OrderEntity> poList = null;
		try {
			poList = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error("Some error occurred while fetching findOrdersByPage ", e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return poList;
	}

	/**
	 * PPD related methods
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity updateOrderEntityFromPPD(OrderEntity order, @Valid List<PpdOrderItemBean> requestItems) {
		order.setStatus(OrderStatus.ORDER_BILLED);
		updateOrderItemFromPPD(order, requestItems, false);
		//		BigDecimal prevCoinsAmount = new BigDecimal("0");
		//		BigDecimal currCoinsAmount = new BigDecimal("0");
		CoinsParamsObject coinsParamsObject = CoinsParamsObject.newInstance();
		//		if (coinsParamsObject.getIsOtpEnabled() == 1 && order.getFinalBillCoins() != null) {
		//			prevCoinsAmount = BigDecimal.valueOf(order.getFinalBillCoins()).setScale(2, RoundingMode.HALF_UP);
		//		}
		pricingService.setAmountAndTaxesInOrderAndItems(order, coinsParamsObject);
		BigDecimal currAmount = BigDecimal.valueOf(order.getFinalBillAmount()).setScale(2, RoundingMode.HALF_UP);
		//		if (coinsParamsObject.getIsOtpEnabled() == 1 && order.getFinalBillCoins() != null) {
		//			currCoinsAmount = BigDecimal.valueOf(order.getFinalBillCoins()).setScale(2, RoundingMode.HALF_UP);
		//		}
		//		if (prevAmount.compareTo(currAmount) != 0) {
		//			walletAdjustment(order.getCustomerId().toString(), prevAmount, currAmount, order.getDisplayOrderId(), order.getStatus(), "WALLET");
		//			order.setAmountReceived(currAmount.doubleValue());
		//		}
		//		if (coinsParamsObject.getIsOtpEnabled() == 1) {
		//			if (prevCoinsAmount.compareTo(currCoinsAmount) != 0) {
		//				walletAdjustment(order.getCustomerId().toString(), prevAmount, currAmount, order.getDisplayOrderId(), request.getStatus(), "COINS");
		//				order.setAmountReceived(currAmount.doubleValue());
		//			}
		//		}
		String txnType = "Consumer-Order";
		if (!(currAmount.compareTo(BigDecimal.ZERO) == 0 && order.getMetadata().getOrderPlacedAmount().compareTo(BigDecimal.ZERO) == 0)) {
			String key = getKey(order);
			addOrDeductMoneyFromUserWallet(order.getCustomerId().toString(), -1 * currAmount.doubleValue(), order.getDisplayOrderId(), txnType,
					-1 * order.getMetadata().getOrderPlacedAmount().doubleValue(), null, key);
		}
		updateOrderProductMetadata(order);
		return save(order);
	}

	private void updateOrderItemFromPPD(OrderEntity order, @Valid List<PpdOrderItemBean> requestItems, boolean isRebilled) {
		Map<String, OrderItemEntity> orderSkuMap = order.getOrderItems().stream().collect(Collectors.toMap(OrderItemEntity::getSkuCode, Function.identity()));
		for (PpdOrderItemBean reqItem : requestItems) {
			if (orderSkuMap.containsKey(reqItem.getSkuCode())) {
				OrderItemEntity item = orderSkuMap.get(reqItem.getSkuCode());
				if (isRebilled && reqItem.getFinalQuantity().compareTo(0d) == 0) {
					item.setStatus(OrderItemStatus.NOT_AVAILABLE);
				} else {
					item.setStatus(OrderItemStatus.PACKED);
				}
				item.setFinalQuantity(reqItem.getFinalQuantity());
				item.getMetadata().setFinalPieces(reqItem.getFinalPieces());
			}
		}
		updateOrderProductMetadata(order);
	}

	public OrderEntity updateOrderStatusFromPPD(OrderEntity order, UpdateOrderStatusBean request) {
		order.setStatus(request.getStatus());
		if (order.getMetadata().getDeliveryDetails() == null) {
			DeliveryDetails deliveryDetails = new DeliveryDetails();
			order.getMetadata().setDeliveryDetails(deliveryDetails);
		}
		order.getMetadata().getDeliveryDetails().setArrivedAt(request.getArrivedAt());
		order.getMetadata().getDeliveryDetails().setDeliveredAt(request.getDeliveredAt());
		return save(order);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void setOrderStatus(OrderEntity order, OrderStatus status) {
		OrderStatus existingStatus = order.getStatus();
		order.setStatus(status);
		if (order.getFinalBillAmount().compareTo(0d) > 0) {
			order.setIsRefunded(1);
			releaseWalletAmount(order, existingStatus);
		}
		save(order);
	}

	private void releaseWalletAmount(OrderEntity order, OrderStatus existingStatus) {
		String txnType = "Consumer-Order-Cancelled";
		String key = getKey(order);
		Double amount = order.getFinalBillAmount();
		Double holdAmount = null;
		if (existingStatus.equals(OrderStatus.NEW_ORDER)) {
			amount = 0d;
			holdAmount = -1 * order.getFinalBillAmount();
		}
		addOrDeductMoneyFromUserWallet(order.getCustomerId().toString(), amount, order.getDisplayOrderId(), txnType, holdAmount, null, key);
	}

	public void deactivateOrder(OrderEntity order) {
		if (Objects.nonNull(order)) {
			order.setActive(0);
			List<OrderItemEntity> cartItems = order.getOrderItems();
			for (OrderItemEntity cartItem : cartItems) {
				cartItem.setActive(0);
			}
		}
		save(order);
	}

	/**
	 * End PPD
	 */

	public void updateOrderItemsFromUpdateList(OrderEntity order, List<OrderItemUpdateBean> itemList) {
		Map<String, OrderItemEntity> orderProductMap = order.getOrderItems().stream()
				.collect(Collectors.toMap(OrderItemEntity::getSkuCode, Function.identity()));
		for (OrderItemUpdateBean requestItem : itemList) {
			if (orderProductMap.containsKey(requestItem.getSkuCode())) {
				OrderItemEntity item = orderProductMap.get(requestItem.getSkuCode());
				item.setFinalQuantity(requestItem.getQuantity());
				item.setStatus(OrderItemStatus.PACKED);
				orderProductMap.remove(requestItem.getSkuCode());
			}
		}
		if (!orderProductMap.isEmpty()) {
			for (OrderItemEntity removeItem : orderProductMap.values()) {
				removeItem.setFinalQuantity(0d);
				removeItem.setActive(0);
			}
			order.getOrderItems().removeAll(orderProductMap.values());
		}
	}

	public void updateOrderItems(OrderEntity order, List<OrderItemUpdateBean> itemList) {
		updateOrderItemsFromUpdateList(order, itemList);
		pricingService.setAmountAndTaxesInOrderAndItems(order, CoinsParamsObject.newInstance());
		order.setEstimatedBillAmount(order.getFinalBillAmount());
		updateOrderProductMetadata(order);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void createPosOrder(OrderEntity order, Map<String, StoreProductInventory> storeInvMap, PosOrderBean request) {
		int itemCount = 0;
		for (PosOrderItemBean item : request.getOrderItems()) {
			StoreProductInventory storeItem = null;
			if (storeInvMap != null) {
				storeItem = storeInvMap.get(item.getSkuCode());
			}
			if (storeItem != null) {
				OrderItemEntity orderItem = OrderItemEntity.newCartItem(order, storeItem, item.getQuantity(), false);
				if (order.getStatus() != OrderStatus.NEW_ORDER && order.getStatus() != OrderStatus.IN_CART) {
					orderItem.setStatus(OrderItemStatus.PACKED);
				}
				orderItem.setOrderId(order.getId());
				orderItem.setOrder(order);
				orderItem.setMrpGrossAmount(orderItem.getMarkedPrice().multiply(BigDecimal.valueOf(orderItem.getFinalQuantity())).doubleValue());
				orderItem.setSpGrossAmount(orderItem.getSalePrice().multiply(BigDecimal.valueOf(orderItem.getFinalQuantity())).doubleValue());
				orderItem.setFinalAmount(orderItem.getSalePrice().multiply(BigDecimal.valueOf(orderItem.getFinalQuantity())).doubleValue());
				if (item.getDiscountAmount() != null) {
					orderItem.setDiscountAmount(item.getDiscountAmount());
				}
				order.addOrderItem(orderItem);
				itemCount++;
			}
		}
		order.setItemCount(itemCount);
		pricingService.setAmountAndTaxesInOrderAndItems(order, CoinsParamsObject.newInstance());
		order.setEstimatedBillAmount(order.getFinalBillAmount());
		order.setAmountReceived(order.getFinalBillAmount());
		updateOrderProductMetadata(order);
	}

	private OrderEntity createNewRefundOrder(OrderEntity parentOrder, String key) {
		String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
		OrderEntity refundOrder = OrderEntity.createNewRefundOrder(parentOrder, displayOrderId, key);
		return save(refundOrder);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity createRefundOrder(OrderEntity parentOrder, RefundOrderBean request, String key, Boolean adjust) {
		Map<String, OrderItemEntity> parentOrderItemMap = getParentOrderItemMap(parentOrder);
		if (parentOrderItemMap == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Items not found in parent order.", "order"));
		}
		int itemCount = 0;
		Map<String, Double> refundedOrderQtyMap = getRefundedOrderSkuWiseQtyMap(parentOrder.getId());
		OrderEntity refundOrder = createNewRefundOrder(parentOrder, key);

		for (RefundOrderItemBean item : request.getRefundOrderItems()) {
			if (parentOrderItemMap.containsKey(item.getSkuCode())) {
				OrderItemEntity parentOrderItem = parentOrderItemMap.get(item.getSkuCode());
				BigDecimal totalReturnedQty = BigDecimal.valueOf(refundedOrderQtyMap.getOrDefault(item.getSkuCode(), 0d));
				Double refundableQty = parentOrderItem.getFinalQuantity();
				if (parentOrderItem.getMetadata().getIsItemCashbackProcessed()) {
					Double checkEligibleRefundQty = BigDecimal.valueOf(parentOrderItem.getFinalQuantity())
							.subtract(BigDecimal.valueOf(parentOrderItem.getMetadata().getItemCashbackQty())).subtract(totalReturnedQty).doubleValue();
					if (checkEligibleRefundQty.compareTo(item.getQuantity()) < 0) {
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, String.format(
										"Total refund quantity for Sku Code %s will exceed total delivered quantity : %s. Quantity cashback given is : %s",
										item.getSkuCode(), parentOrderItemMap.get(item.getSkuCode()).getFinalQuantity(),
								parentOrderItem.getMetadata().getItemCashbackQty()), "order"));
					}
					refundableQty = BigDecimal.valueOf(parentOrderItem.getFinalQuantity())
							.subtract(BigDecimal.valueOf(parentOrderItem.getMetadata().getItemCashbackQty())).doubleValue();
				}
				if (Boolean.TRUE.equals(adjust)) {
					item.setQuantity(BigDecimal.valueOf(refundableQty).subtract(totalReturnedQty).doubleValue());
				} else {
					Double totalReturnedQtyAfterRefund = totalReturnedQty.add(BigDecimal.valueOf(item.getQuantity())).doubleValue();
					if (refundableQty.compareTo(totalReturnedQtyAfterRefund) < 0) {
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, String.format(
										"Total refund quantity for Sku Code %s will exceed total delivered quantity : %s. Quantity already refunded is : %s",
										item.getSkuCode(), parentOrderItemMap.get(item.getSkuCode()).getFinalQuantity(), refundedOrderQtyMap.get(item.getSkuCode())),
								"order"));
					}
				}
				if (item.getQuantity().compareTo(0d) > 0) {
					OrderItemEntity refundOrderItem = OrderItemEntity.createNewRefundOrderItem(refundOrder, parentOrderItem, item.getQuantity());
					pricingService.setAmountAndTaxesInItem(refundOrderItem, null, CoinsParamsObject.newInstance(), refundOrder);
					refundOrder.addOrderItem(refundOrderItem);
					parentOrderItem.setRefundAmount(
							((parentOrderItem.getRefundAmount() != null) ? parentOrderItem.getRefundAmount() : 0d) + refundOrderItem.getFinalAmount());
					itemCount++;
				}
			} else {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, String.format("SKU %s not found in parent order", item.getSkuCode()), "order"));

			}
		}
		refundOrder.setItemCount(itemCount);
		refundOrder.setSubmittedAt(new Date());
		pricingService.setAmountAndTaxesInOrder(refundOrder, null, CoinsParamsObject.newInstance());
		refundOrder.setEstimatedBillAmount(refundOrder.getFinalBillAmount());
		updateOrderProductMetadata(refundOrder);

		parentOrder.setIsRefunded(1);
		parentOrder.setRefundAmount(parentOrder.getRefundAmount() + refundOrder.getFinalBillAmount());
		save(parentOrder);
		return refundOrder;
	}

	private Map<String, OrderItemEntity> getParentOrderItemMap(OrderEntity requestOrder) {
		Map<String, OrderItemEntity> parentOrderItems = null;
		if (CollectionUtils.isNotEmpty(requestOrder.getOrderItems())) {
			parentOrderItems = requestOrder.getOrderItems().stream().collect(Collectors.toMap(OrderItemEntity::getSkuCode, i -> i));
		}
		return parentOrderItems;
	}

	public List<StoreOrderTotalInfoResponse> findStoreTotalSaleDetails(List<String> storeIds, LocalDate fromDate, LocalDate toDate,
			OrderConstants.ReportBreakdownType storeReportBreakdownType, List<String> skuCodes) {
		List<StoreOrderTotalInfoResponse> totalStoreSalesInfoResponseList = new ArrayList<>();
		List<StoreOrderTotalInfo> orderTotalInfoList = orderRepository.findStoreOrderTotalDetailsByStoreId(storeIds, java.sql.Date.valueOf(fromDate),
				java.sql.Date.valueOf(toDate));
		List<OrderBreakdownItemBean> breakdownItemList = null;
		if (OrderConstants.ReportBreakdownType.CHANNEL.equals(storeReportBreakdownType)) {
			breakdownItemList = findStoreTotalBreakdownByChannel(storeIds, fromDate, toDate);
		} else if (OrderConstants.ReportBreakdownType.PAYMENT_MODE.equals(storeReportBreakdownType)) {
			breakdownItemList = findStoreTotalDetailsByStoreIdAndPaymentMode(storeIds, fromDate, toDate);
		} else if (OrderConstants.ReportBreakdownType.SKU_CODE.equals(storeReportBreakdownType)) {
			breakdownItemList = findStoreTotalDetailsByStoreIdAndPaymentMode(storeIds, fromDate, toDate);
		}
		Map<String, List<OrderBreakdownItemBean>> storeOrderBreakdownMap = buildBreakdownMap(breakdownItemList);
		for (StoreOrderTotalInfo storeOrderTotalInfo : orderTotalInfoList) {
			StoreOrderTotalInfoResponse orderInfoResponse = new StoreOrderTotalInfoResponse();
			orderInfoResponse.setStoreId(storeOrderTotalInfo.getStoreId());
			orderInfoResponse.setOrderCount(storeOrderTotalInfo.getOrderCount());
			orderInfoResponse.setTotalSale(storeOrderTotalInfo.getTotalSale());
			orderInfoResponse.setBreakdown(storeOrderBreakdownMap.get(storeOrderTotalInfo.getStoreId()));
			totalStoreSalesInfoResponseList.add(orderInfoResponse);
		}
		return totalStoreSalesInfoResponseList;
	}

	private Map<String, List<OrderBreakdownItemBean>> buildBreakdownMap(List<OrderBreakdownItemBean> breakdownItemList) {
		Map<String, List<OrderBreakdownItemBean>> resultMap = new HashMap<>();
		if (breakdownItemList != null) {
			for (OrderBreakdownItemBean breakdownItem : breakdownItemList) {
				if (!resultMap.containsKey(breakdownItem.getIdentifier())) {
					resultMap.put(breakdownItem.getIdentifier(), new ArrayList<>());
				}
				resultMap.get(breakdownItem.getIdentifier()).add(breakdownItem);
			}
		}
		return resultMap;
	}

	public Double getStoresMtd(List<String> storeIds) {
		LocalDate toDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
		LocalDate fromDate = YearMonth.from(toDate).atDay(1);
		return orderRepository.getStoresTotalSalesBetweenDates(storeIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	public Double getStoresWtd(List<String> storeIds) {
		LocalDate currentDate = LocalDate.now();
		LocalDate mondayOfWeek = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		return orderRepository.getStoresTotalSalesBetweenDates(storeIds, java.sql.Date.valueOf(mondayOfWeek), java.sql.Date.valueOf(currentDate));
	}

	public Double getStoresTodaySales(List<String> storeIds) {
		LocalDate currentDate = LocalDate.now();
		return orderRepository.getStoresTotalSalesOnDate(storeIds, java.sql.Date.valueOf(currentDate));
	}

	public List<SkuOrderTotalInfoResponse> findSkuTotalSaleDetails(List<String> storeIds, LocalDate fromDate, LocalDate toDate,
			OrderConstants.ReportBreakdownType reportBreakdownType, List<String> breakdownStoreIds) {
		List<SkuOrderTotalInfoResponse> totalOrderInfoResponseList = new ArrayList<>();
		List<SkuOrderTotalInfo> orderTotalInfoList = orderRepository.findSkuTotalDetailsByStoreId(storeIds, java.sql.Date.valueOf(fromDate),
				java.sql.Date.valueOf(toDate));
		List<OrderBreakdownItemBean> breakdownItemList = null;
		if (OrderConstants.ReportBreakdownType.CHANNEL.equals(reportBreakdownType)) {
			breakdownItemList = findSkuTotalBreakdownByChannel(storeIds, fromDate, toDate);
		} else if (OrderConstants.ReportBreakdownType.PAYMENT_MODE.equals(reportBreakdownType)) {
			breakdownItemList = findSkuTotalBreakdownByPaymentMode(storeIds, fromDate, toDate);
		} else if (OrderConstants.ReportBreakdownType.STORE_IDS.equals(reportBreakdownType)) {
			breakdownItemList = findSkuTotalBreakdownByStoreIds(breakdownStoreIds, fromDate, toDate);
		}
		Map<String, List<OrderBreakdownItemBean>> storeOrderBreakdownMap = buildBreakdownMap(breakdownItemList);
		for (SkuOrderTotalInfo storeOrderTotalInfo : orderTotalInfoList) {
			SkuOrderTotalInfoResponse orderInfoResponse = new SkuOrderTotalInfoResponse();
			orderInfoResponse.setSkuCode(storeOrderTotalInfo.getSkuCode());
			orderInfoResponse.setOrderCount(storeOrderTotalInfo.getOrderCount());
			orderInfoResponse.setTotalSale(storeOrderTotalInfo.getTotalSale());
			orderInfoResponse.setBreakdown(storeOrderBreakdownMap.get(storeOrderTotalInfo.getSkuCode()));
			totalOrderInfoResponseList.add(orderInfoResponse);
		}
		return totalOrderInfoResponseList;
	}

	private List<OrderBreakdownItemBean> findSkuTotalBreakdownByStoreIds(List<String> breakdownStoreIds, LocalDate fromDate, LocalDate toDate) {
		return orderRepository.findSkuTotalBreakdownByStoreIds(breakdownStoreIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	private List<OrderBreakdownItemBean> findSkuTotalBreakdownByPaymentMode(List<String> storeIds, LocalDate fromDate, LocalDate toDate) {
		return orderRepository.findSkuTotalBreakdownByPaymentMode(storeIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	private List<OrderBreakdownItemBean> findSkuTotalBreakdownByChannel(List<String> storeIds, LocalDate fromDate, LocalDate toDate) {
		return orderRepository.findSkuTotalBreakdownByChannel(storeIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	public List<OrderBreakdownItemBean> findStoreTotalBreakdownByChannel(List<String> storeIds, LocalDate fromDate, LocalDate toDate) {
		return orderRepository.findStoreTotalDetailsByStoreIdAndChannel(storeIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	public List<OrderBreakdownItemBean> findStoreTotalDetailsByStoreIdAndPaymentMode(List<String> storeIds, LocalDate fromDate, LocalDate toDate) {
		return orderRepository.findStoreTotalDetailsByStoreIdAndPaymentMode(storeIds, java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate));
	}

	public List<OrderSlotEntity> getAllOrderSlots() {
		Map<String, SortDirection> sort = defaultSortMap();
		sort.put("priority", SortDirection.ASC);
		return orderSlotRepository.findAll(sort, 100, 1);
	}

	public OrderSlotEntity getOrderSlotById(Integer slotId) {
		return orderSlotRepository.getById(slotId);
	}

	public void reserveOrderSlot(Integer slotId) {
		orderSlotRepository.reserveOrderSlot(slotId);
	}

	public void releaseOrderSlot(Integer slotId) {
		orderSlotRepository.releaseOrderSlot(slotId);
	}

	public List<OrderEntity> findOrdersByDisplayOrderId(List<String> displayOrderIds) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("displayOrderId", displayOrderIds);
		return findAllRecords(filters, Limit.NO_LIMIT);
	}

	public OrderEntity findByKey(String key) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("key", key);
		List<OrderEntity> entityList = findAllRecords(filters);
		if (!entityList.isEmpty()) {
			return entityList.get(0);
		}
		return null;
	}

	public void addOrDeductMoneyFromUserWallet(String userId, Double amount, String txnDetail, String txnType, Double holdAmount, String remarks, String key) {
		WalletAddOrDeductBean walletDeductBean = new WalletAddOrDeductBean();
		walletDeductBean.setAmount(amount);
		walletDeductBean.setTxnType(txnType);
		walletDeductBean.setTxnDetail(txnDetail);
		walletDeductBean.setHoldAmount(holdAmount);
		walletDeductBean.setRemarks(remarks);
		_LOGGER.debug(String.format("addOrDeductMoneyFromUserWallet::walletDeductBean: %s", walletDeductBean));
		clientService.addOrDeductFromWallet(userId, walletDeductBean, key);
	}

	public String getKey(OrderEntity order) {
		String key = String.format("ORDER|%s|%s", order.getDisplayOrderId(), order.getStatus());
		return key;
	}

	public List<OrderEntity> findAllActiveCarts(Date deliveryDate) {
		Optional<List<OrderEntity>> resultOpt = orderRepository.findAllCartsInActiveState((java.sql.Date) deliveryDate);
		return resultOpt.orElse(null);
	}

	public Page<CustomerCartInfo> findCustomerCartInfoBetween(Date startTime, Date endTime, Date deliveryDate, Pageable pageable) {
		return orderRepository.findCustomerCartInfoBetween(startTime, endTime, deliveryDate, pageable);
	}

	public boolean validOrderForProcessingRule(OrderEntity order, List<String> validChannelList, Set<String> testUserList) {
		if (order.getDeliveryAddress() == null) {
			order.getMetadata().setFailureReason(Collections.singletonList("Delivery Address not found."));
			return false;
		} else if (!validChannelList.contains(order.getChannel().toString())) { //TODO-- for time being until we start taking orders from app
			order.getMetadata().setFailureReason(Collections.singletonList(String.format("Invalid Order Channel")));
			return false;
		} else if (order.getMetadata().getContactDetail() != null && order.getMetadata().getContactDetail().getPhone() != null && testUserList.contains(
				order.getMetadata().getContactDetail().getPhone())) {
			order.getMetadata().setFailureReason(Collections.singletonList(String.format("Test User Order")));
			return false;
		}
		return true;
	}

	public void saveAllOrders(List<OrderEntity> orders) {
		orderRepository.saveAll(orders);
	}

	public Map<UUID, ConsumerAddressResponse> getAddressMap(List<Long> addressIds) {
		List<ConsumerAddressResponse> userWallets = clientService.getUserAddressesInternal(addressIds);
		return userWallets.stream().collect(Collectors.toMap(ConsumerAddressResponse::getUserId, Function.identity()));
	}

	public ConsumerDeliveryCharges getConsumerDeliveryCharges() {
		String paramString = ParamsUtils.getParam("CONSUMER_DELIVERY_CHARGES_MAP",
				"charge_lower_limit:0,charge_upper_limit:100,charges:150,discount_lower_limit:100,discount_upper_limit:1000,discount:250");
		ConsumerDeliveryCharges deliveryCharges = new ConsumerDeliveryCharges();
		Map<String, Double> deliveryChargesMap = new HashMap<>();
		for (String deliveryChargesPairString : paramString.split(",")) {
			List<String> keyValuePair = Arrays.asList(deliveryChargesPairString.split(":"));
			deliveryChargesMap.put(keyValuePair.get(0), Double.valueOf(keyValuePair.get(1)));
		}
		deliveryCharges.setChargeLowerLimit(deliveryChargesMap.get("charge_lower_limit"));
		deliveryCharges.setChargeUpperLimit(deliveryChargesMap.get("charge_upper_limit"));
		deliveryCharges.setChargeLimit(deliveryChargesMap.get("charges"));
		deliveryCharges.setDiscountLowerLimit(deliveryChargesMap.get("discount_lower_limit"));
		deliveryCharges.setDiscountUpperLimit(deliveryChargesMap.get("discount_upper_limit"));
		deliveryCharges.setDiscountLimit(deliveryChargesMap.get("discount"));
		return deliveryCharges;
	}

	public List<PreviousOrderItemBean> getPreviouslyOrderedSkus(UUID userId) throws JsonMappingException, JsonProcessingException {
		List<List<Object>> resultOpt = orderRepository.getPreviouslyOrderedSkus(userId);
		List<PreviousOrderItemBean> response = new ArrayList<>();
		for (List<Object> result : resultOpt) {
			PreviousOrderItemBean previousOrderItemBean = new PreviousOrderItemBean();
			previousOrderItemBean.setSkuCode((String) result.get(0));
			previousOrderItemBean.setProductName((String) result.get(1));
			previousOrderItemBean.setUom((String) result.get(2));
			previousOrderItemBean.setImageUrl((String) result.get(3));
			previousOrderItemBean.setFinalQuantity((BigDecimal) result.get(4));
			if (result.get(5) != null) {
				previousOrderItemBean.setPieces(Integer.parseInt((String) result.get(5)));
			}
			if (result.get(6) != null) {
				previousOrderItemBean.setGrades(new ObjectMapper().readValue((String) result.get(6), new TypeReference<List<OrderItemGradeBean>>() {
				}));
			}
			previousOrderItemBean.setDeliveryDate((Date) result.get(7));
			previousOrderItemBean.setCategoryName((String) result.get(8));
			previousOrderItemBean.setIsOzoneWashedItem((Boolean.parseBoolean((String) result.get(9))));
			response.add(previousOrderItemBean);
		}
		return response;
	}

	public List<OrderEntity> findRefundOrdersFromParentId(UUID parentOrderId) {
		return orderRepository.findRefundOrdersFromParentId(parentOrderId);
	}

	public OrderEntity findLatestOrder(UUID userId) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", DeliveryDateUtils.getDeliveryDate());
		filters.put("customerId", userId);
		filters.put("status", List.of(OrderStatus.ORDER_DELIVERED, OrderStatus.ORDER_BILLED, OrderStatus.NEW_ORDER));
		List<OrderEntity> orders = findAllRecords(filters);
		if (CollectionUtils.isEmpty(orders)) {
			return null;
		}
		return orders.get(0);
	}

	public List<OrderEntity> findNewOrders() {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("deliveryDate", DeliveryDateUtils.getDeliveryDateForCurrentOrder());
		filters.put("status", List.of(OrderStatus.NEW_ORDER));
		List<OrderEntity> orders = findAllRecords(filters, Limit.NO_LIMIT);
		return orders;
	}

	public List<OrderEntity> findAllOrdersInCart() {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("deliveryDate", DeliveryDateUtils.getConsumerDeliveryDate(false));
		filters.put("status", List.of(OrderStatus.IN_CART));
		List<OrderEntity> orders = findAllRecords(filters, Limit.NO_LIMIT);
		return orders;
	}

	public List<OrderEntity> getOrdersForTickets(UUID id, java.sql.Date deliveryDate) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", new FilterCriteria("deliveryDate", deliveryDate, Operation.GTE));
		filters.put("customerId", id);
		filters.put("status", List.of(OrderStatus.ORDER_DELIVERED, OrderStatus.ORDER_BILLED, OrderStatus.ORDER_OUT_FOR_DELIVERY));
		List<OrderEntity> orders = findAllRecords(filters);
		return orders;
	}

	public Map<String, Double> getRefundedOrderSkuWiseQtyMap(UUID parentOrderId) {
		List<OrderEntity> refundedOrderEntityList = orderRepository.findRefundConfirmedOrdersWithParentOrderId(parentOrderId);
		Map<String, Double> refundedOrderQtyMap = new HashMap<>();
		for (OrderEntity refundedOrder : refundedOrderEntityList) {
			refundedOrder.getOrderItems().forEach(item -> {
				Double totalReturnedQty = refundedOrderQtyMap.containsKey(item.getSkuCode()) ?
						BigDecimal.valueOf(refundedOrderQtyMap.get(item.getSkuCode())).add(BigDecimal.valueOf(item.getFinalQuantity())).doubleValue() :
						item.getFinalQuantity();
				refundedOrderQtyMap.put(item.getSkuCode(), totalReturnedQty);
			});
		}
		return refundedOrderQtyMap;
	}

	public List<OrderEntity> findToBeReProcessedOrderByDate(Date deliveryDate) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", deliveryDate);
		filters.put("hasPpdOrder", 0);
		filters.put("status", OrderStatus.NEW_ORDER);
		return findAllRecords(filters);
	}

	private void adjustWalletAmount(OrderEntity order, String txnType) {
		// currently txn type is considered as adjustment, need to make changes if txn type changes
		BigDecimal prevAmount = BigDecimal.valueOf(order.getFinalBillAmount());
		pricingService.setAmountAndTaxesInOrderAndItems(order, null);
		BigDecimal currAmount = BigDecimal.valueOf(order.getFinalBillAmount());
		if (prevAmount.compareTo(currAmount) != 0) {
			BigDecimal diffAmount = prevAmount.subtract(currAmount);
			String key = String.format("ORDER|%s|%s|%s", order.getDisplayOrderId(), "ORDER_RE_BILLED",
					prevAmount.compareTo(currAmount) > 0 ? "CREDIT" : "DEBIT");
			addOrDeductMoneyFromUserWallet(order.getCustomerId().toString(), diffAmount.doubleValue(), order.getDisplayOrderId(), txnType, null, null, key);
		}
		_LOGGER.info(String.format("adjustWalletAmount:: wallet updated for order %s ", order.getDisplayOrderId()));
		order.setAmountReceived(currAmount.doubleValue());
	}

	public OrderEntity reBillOrder(List<PpdOrderItemBean> rebillData, OrderEntity order) {
		updateOrderItemFromPPD(order, rebillData, true);
		adjustWalletAmount(order, "OD-ADJUSTMENT");
		save(order);
		return order;
	}

	public List<OrderEntity> findAllOrdersByCurrentDeliveryDate() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", DeliveryDateUtils.getCustomDeliveryDate());
		return findAllRecords(filters);
	}

	public List<OrderEntity> findAllDispatchedOrders() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("deliveryDate", DeliveryDateUtils.getDeliveryDate());
		filters.put("status", List.of(OrderStatus.ORDER_DELIVERED));
		return findAllRecords(filters);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity updateOrder(OrderEntity order) {
		pricingService.setAmountAndTaxesInOrderAndItems(order, CoinsParamsObject.newInstance());
		updateOrderProductMetadata(order);
		return order;
	}

	public List<OrderEntity> findOrdersWithCashbackItems(Date fromDeliveryDate) {
		return orderRepository.findOrdersWithCashbackItems(fromDeliveryDate);
	}

	public List<OrderEntity> updateDeliveryDelayMetaData(List<OrderEntity> orders, String deliveryDelayTime, String deliveryDelayReason) {
		String deliveryMessage = ParamsUtils.getParam("DELIVERY_DELAY_MESSAGE");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		for (OrderEntity order : orders) {
			String eta = order.getMetadata().getEta().getEta();
			LocalTime etaTime = LocalTime.parse(eta, timeFormatter);
			LocalTime delayTime = LocalTime.parse(deliveryDelayTime, timeFormatter);
			eta = etaTime.plusHours(delayTime.getHour()).plusMinutes(delayTime.getMinute()).format(timeFormatter);
			String delayReason = String.format(deliveryMessage, deliveryDelayReason, eta);

			order.getMetadata().getEta().setEta(eta);
			order.getMetadata().getEta().setDelayTime(deliveryDelayTime);
			order.getMetadata().getEta().setDelayReason(delayReason);
		}
		return orders;
	}

	public List<OrderEntity> getOrdersForDsrByDeliveryDate(java.sql.Date deliveryDate) {
		Optional<List<OrderEntity>> resultOpt = orderRepository.findAllOrdersForDSR(deliveryDate);
		return resultOpt.orElse(null);
	}

	public void createAndSendWmsDsPayload(List<StoreRequisitionBean> storeRequisitions, Date deliveryDate) {
		Map<String, WmsDsPayload> payloadMap = new HashMap<>();
		for (StoreRequisitionBean srBean : storeRequisitions) {
			srBean.setDeliveryDate(new java.sql.Date(deliveryDate.getTime()));
			if (CollectionUtils.isEmpty(srBean.getMetadata().getGrades())) {
				CartRequest.OrderItemGradeBean grade = CartRequest.OrderItemGradeBean.newInstance();
				grade.setName(OrderConstants.DEFAULT_SKU_GRADE);
				grade.setQuantity(srBean.getOrderedQty());
				grade.setPieces(srBean.getMetadata().getPieces());
				srBean.getMetadata().setGrades(Collections.singletonList(grade));
			}
			for (CartRequest.OrderItemGradeBean gradeBean : srBean.getMetadata().getGrades()) {
				if (!StringUtils.isEmpty(srBean.getMetadata().getNotes())) {
					setFormattedNotes(srBean, gradeBean);
				}
				String key = String.format("%s|%s|%s", srBean.getStoreId(), srBean.getSkuCode(), gradeBean.getName());
				if (payloadMap.containsKey(key)) {
					addToExistingPayload(payloadMap.get(key), srBean, gradeBean);
				} else {
					WmsDsPayload wmsDsPayload = createWmsDsPayload(srBean, gradeBean);
					payloadMap.put(key, wmsDsPayload);
				}
			}
		}
		clientService.sendRequisitionToWms(new ArrayList<>(payloadMap.values()));
	}

	private void setFormattedNotes(StoreRequisitionBean srBean, CartRequest.OrderItemGradeBean gradeBean) {
		StringBuilder formattedNotes = new StringBuilder(srBean.getMetadata().getNotes());
		if (gradeBean.getPieces() != null && gradeBean.getPieces() != 0) {
			formattedNotes.append(String.format(", pieces: %s", gradeBean.getPieces()));
		}
		if (gradeBean.getQuantity() != null && gradeBean.getQuantity() != 0d) {
			formattedNotes.append(String.format(", qty: %s", gradeBean.getQuantity()));
		}
		srBean.getMetadata().setNotes(formattedNotes.toString());
	}

	private WmsDsPayload createWmsDsPayload(StoreRequisitionBean srBean, CartRequest.OrderItemGradeBean gradeBean) {
		WmsDsPayload wmsDsPayload = WmsDsPayload.newInstance();
		wmsDsPayload.setSkuCode(srBean.getSkuCode());
		wmsDsPayload.setGrade(gradeBean.getName());
		wmsDsPayload.setStoreId(Integer.parseInt(srBean.getStoreId()));
		wmsDsPayload.setPieces(gradeBean.getPieces());
		wmsDsPayload.setPerPcWeight(srBean.getMetadata().getPerPiecesWeight());
		wmsDsPayload.setOrderedQty(gradeBean.getQuantity() == null ? 0d : gradeBean.getQuantity());
		wmsDsPayload.setDeliveryDate(srBean.getDeliveryDate());
		wmsDsPayload.setProductName(srBean.getProductName());
		wmsDsPayload.setSuffix(srBean.getMetadata().getSuffix());
		wmsDsPayload.setUom(srBean.getUom());
		wmsDsPayload.setOzoneWashingQty(srBean.getMetadata().getIsOzoneWashedItem() ? gradeBean.getQuantity() : 0d);
		if (!StringUtils.isEmpty(srBean.getMetadata().getNotes())) {
			wmsDsPayload.getNotes().add(srBean.getMetadata().getNotes());
		}
		return wmsDsPayload;
	}

	private void addToExistingPayload(WmsDsPayload wmsDsPayload, StoreRequisitionBean bean, CartRequest.OrderItemGradeBean gradeBean) {
		if (gradeBean.getPieces() != null) {
			wmsDsPayload.setPieces(Math.addExact(wmsDsPayload.getPieces() == null ? 0 : wmsDsPayload.getPieces(),
					gradeBean.getPieces() == null ? 0 : gradeBean.getPieces()));
		}
		wmsDsPayload.setOrderedQty(BigDecimal.valueOf(wmsDsPayload.getOrderedQty()).add(BigDecimal.valueOf(gradeBean.getQuantity()))
				.setScale(2, RoundingMode.HALF_UP).doubleValue());
		wmsDsPayload.setOzoneWashingQty(BigDecimal.valueOf(wmsDsPayload.getOzoneWashingQty()).add(BigDecimal.valueOf(
				bean.getMetadata().getIsOzoneWashedItem() ? gradeBean.getQuantity() : 0d)).setScale(2, RoundingMode.HALF_UP).doubleValue());
		if (!StringUtils.isEmpty(bean.getMetadata().getNotes())) {
			wmsDsPayload.getNotes().add(bean.getMetadata().getNotes());
		}
	}

	public void itemsAvailable(OrderEntity order) {
		boolean allItemsNotPresent = order.getOrderItems().stream().allMatch(orderItemEntity -> orderItemEntity.getStatus().equals(OrderItemStatus.NOT_AVAILABLE));
		if (allItemsNotPresent) {
			order.setStatus(OrderStatus.ORDER_CANCELLED_BY_STORE);
		}
	}

	public void updateSaleAndMarkedPriceInOrderItems(List<OrderEntity> orders) {
		Integer threadCount = ParamsUtils.getIntegerParam("PRICING_REFRESH_EXECUTOR_THREAD_COUNT", 5);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		for (OrderEntity order : orders) {
			executor.submit(() -> {
				try {
					updateSaleAndMarkedPriceInOrderItem(order);
				} catch (Exception e) {
					_LOGGER.error("Error while updating price in order items", e);
				}
			});
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
			_LOGGER.error("InterruptedException occurred in executor", e);
		}
	}

	private void updateSaleAndMarkedPriceInOrderItem(OrderEntity order) {
		StoreInventoryResponse storeInventoryResponse = null;
		List<String> skuCodes = order.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList());
		storeInventoryResponse = clientService.getStoreInventory(order.getStoreId(), String.join(",", skuCodes), order.getCustomerId());
		assignMinimumPrice(storeInventoryResponse.getInventory());
		Map<String, StoreProductInventory> storeItemMap = new HashMap<>();
		if (storeInventoryResponse != null && CollectionUtils.isNotEmpty(storeInventoryResponse.getInventory())) {
			storeItemMap = storeInventoryResponse.getInventory().stream()
					.collect(Collectors.toMap(StoreProductInventory::getInventorySkuCode, Function.identity()));
		}
		for (OrderItemEntity orderItem : order.getOrderItems()) {
			if (!orderItem.getMetadata().getIsOrderOfferItem() && storeItemMap.containsKey(orderItem.getSkuCode())) {
				StoreProductInventory storeItem = storeItemMap.get(orderItem.getSkuCode());
				Integer pieceQty = null;
				if (orderItem.getMetadata().getPieces() != null && orderItem.getMetadata().getPieces() != 0) {
					pieceQty = orderItem.getMetadata().getPieces();
				}
				orderItem.setSalePrice(OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), orderItem.getPriceBracket(),
						pieceQty != null ? pieceQty.doubleValue() : orderItem.getFinalQuantity(), orderItem));
				orderItem.setMarkedPrice(BigDecimal.valueOf(storeItem.getInventoryMarketPrice()));
			}
		}
		updateOrder(order);
	}

	private void assignMinimumPrice(List<StoreProductInventory> inventory) {
		if (CollectionUtils.isNotEmpty(inventory)) {
			inventory.forEach(i -> {
				if (i.getInventoryMaxPrice() != null) {
					i.setInventorySalePrice(Math.min(i.getInventorySalePrice(), i.getInventoryMaxPrice()));
					i.setInventoryMarketPrice(Math.max(i.getInventoryMarketPrice(), i.getInventoryMaxPrice()));
				}
			});
		}
	}

	public void updatePerPcsWeight() {
		LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
		Optional<List<OrderEntity>> deliveredOrders = orderRepository.findAllDeliveredOrdersOfDate(java.sql.Date.valueOf(localDate));
		if (deliveredOrders.isPresent()) {
			Map<String, SkuPerPcsWeightDto> skuPerPcsWeightDtoMap = new HashMap<>();
			for (OrderEntity order : deliveredOrders.get()) {
				for (OrderItemEntity orderItem : order.getOrderItems()) {
					if (orderItem.getMetadata().getFinalPieces() != null && Double.compare(orderItem.getMetadata().getFinalPieces(), 0d) > 0) {
						SkuPerPcsWeightDto dto = skuPerPcsWeightDtoMap.get(orderItem.getSkuCode());
						if (dto == null) {
							dto = new SkuPerPcsWeightDto();
							dto.setSkuCode(orderItem.getSkuCode());
							dto.setTotalFinalQty(orderItem.getFinalQuantity());
							dto.setTotalFinalPcs(orderItem.getMetadata().getFinalPieces());
							skuPerPcsWeightDtoMap.put(orderItem.getSkuCode(), dto);
						} else {
							dto.setTotalFinalQty(dto.getTotalFinalQty() + orderItem.getFinalQuantity());
							dto.setTotalFinalPcs(dto.getTotalFinalPcs() + orderItem.getMetadata().getFinalPieces());
						}
					}
				}
			}
			for (SkuPerPcsWeightDto dto : skuPerPcsWeightDtoMap.values()) {
				BigDecimal initialValue = BigDecimal.valueOf(dto.getTotalFinalQty())
						.divide(BigDecimal.valueOf(dto.getTotalFinalPcs()), 3, RoundingMode.HALF_UP);
				Double roundedValue = roundToNextMultipleOf0005(initialValue);
				dto.setPerPcsWt(roundedValue);
			}
			if (!skuPerPcsWeightDtoMap.isEmpty()) {
				clientService.updatePerPcsWeight(new ArrayList<>(skuPerPcsWeightDtoMap.values()));
			}
		}
	}
	public void buildAndSendClevertapProfileUpdateRequest(List<CustomerCartInfo> cartInfos, Double cartAmount) {
		ClevertapEventRequest clevertapRequest = buildClevertapProfileUpdateRequest(cartInfos, cartAmount);
		clientService.sendClevertapEvent(clevertapRequest);
	}

	private ClevertapEventRequest buildClevertapProfileUpdateRequest(List<CustomerCartInfo> cartInfos, Double cartAmount) {
		Set<UUID> customerIds = cartInfos.stream().filter(c -> c.getActive() == 1).map(CustomerCartInfo::getCustomerId).collect(Collectors.toSet());
		List<WalletBean> userWallets = clientService.findAllCustomersWallet(Collections.list(Collections.enumeration(customerIds)));
		Map<String, WalletBean> customerWalletMap = userWallets.stream().collect(Collectors.toMap(WalletBean::getEntityId, Function.identity()));
		ClevertapEventRequest clevertapEventData = new ClevertapEventRequest();
		List<ClevertapEventRequest.ClevertapEventData> profileUpdates = new ArrayList<>();
		for (CustomerCartInfo info : cartInfos) {
			Double currentCartAmount = cartAmount != null ? cartAmount : info.getCurrentCartAmount();
			double walletAmount = getWalletAmount(customerWalletMap, info.getCustomerId().toString());
			Map<String, Object> profileData = new HashMap<>();
			profileData.put("walletAmount", walletAmount);
			profileData.put("currentCartAmount", currentCartAmount);
			ClevertapEventRequest.ClevertapEventData profileUpdateDto = clientService.buildClevertapProfileData(info.getCustomerId().toString(), profileData);
			profileUpdates.add(profileUpdateDto);
		}
		clevertapEventData.setD(profileUpdates);
		return clevertapEventData;
	}

	private double getWalletAmount(Map<String, WalletBean> customerWalletMap, String customerId) {
		if (customerWalletMap != null && CollectionUtils.isNotEmpty(customerWalletMap)) {
			WalletBean walletBean = customerWalletMap.get(customerId);
			if (walletBean != null) {
				return walletBean.getAmount();
			}
		}
		return 0d;
	}

	public  Double roundToNextMultipleOf0005(BigDecimal initialValue) {
		return initialValue
				.divide(BigDecimal.valueOf(0.005), 0, RoundingMode.UP)
				.multiply(BigDecimal.valueOf(0.005)).doubleValue();
	}

	public List<OrderEntity> findInactiveAutoCheckoutOrders(Date deliveryDate, Date beforeCreateAt) {
		Optional<List<OrderEntity>> orders = orderRepository.findInactiveAutoCheckoutOrdersAndCreatedAt(new java.sql.Date(deliveryDate.getTime()),
				beforeCreateAt);
		return orders.orElse(null);
	}

	public void notifyCustomerToEnableAutoCheckout(List<OrderEntity> orders) {
		if (CollectionUtils.isNotEmpty(orders)) {
			List<PnRequest> requests = new ArrayList<>();
			Map<String, String> fillers = new HashMap<>();
			for (OrderEntity order : orders) {
				PnRequest pnRequest = PnRequest.builder().userId(order.getCustomerId().toString())
						.templateName(OrderConstants.ENABLE_AUTO_CHECKOUT_TEMPLATE_NAME).fillers(fillers).build();
				requests.add(pnRequest);
			}
			CollectionUtils.batches(requests, 100).forEach(batch -> clientService.sendPushNotifications(batch));
		}
	}

	public UserOrderSettingEntity getUserOrderSetting(UUID customerId, String key) {
		return userOrderSettingRepository.findByCustomerIdAndKey(customerId, key).orElse(null);
	}

	public void saveUserOrderSetting(UserOrderSettingEntity userOrderSetting) {
		userOrderSettingRepository.save(userOrderSetting);
	}

	public void deactivateFirstOrderFlow(List<OrderEntity> processedOrders) {
		List<String> customerIds = new ArrayList<>();
		for (OrderEntity order : processedOrders) {
			if (Boolean.TRUE.equals(order.getMetadata().getIsFirstOrder()) && !order.getChannel().equals(OrderConstants.OrderChannel.BACKOFFICE.getValue())) {
				customerIds.add(order.getCustomerId().toString());
			}
		}
		if (CollectionUtils.isNotEmpty(customerIds)) {
			CollectionUtils.batches(customerIds, 100).forEach(batch -> clientService.deactivateFirstOrderFlow(batch));
		}
	}

	public void disableOnboardingOffer(List<OrderEntity> processedOrders) {
		Set<OrderConstants.OfferType> applicableOffers = Collections.singleton(OrderConstants.OfferType.ORDER_DISCOUNT_OFFER);
		List<String> customerIds = new ArrayList<>();
		for (OrderEntity order : processedOrders) {
			String offerType = order.getOfferData().getOfferType();
			if (offerType != null && applicableOffers.contains(OrderConstants.OfferType.valueOf(offerType))) {
				customerIds.add(order.getCustomerId().toString());
			}
		}
		if (CollectionUtils.isNotEmpty(customerIds)) {
			CollectionUtils.batches(customerIds,100).forEach(batch -> clientService.disableOnboardingOffer(batch));
		}
	}

	public List<OrderSlotResponseBean> buildOrderSlotResponse(String societyId) {
		List<OrderSlotEntity> orderSlotList = getAllOrderSlots();
		_LOGGER.info(String.format("All OrderSlots from DB : %s, societyId : %s", orderSlotList, societyId));
		if (societyId != null) {
			SocietyListItemBean society = clientService.getSocietyById(Integer.valueOf(societyId));
			if (society.getMetadata() != null && society.getMetadata().getValidSlots() != null) {
				Set<Integer> validSlotSet = new HashSet<>(society.getMetadata().getValidSlots());
				orderSlotList = orderSlotList.stream().filter(orderSlotEntity -> validSlotSet.contains(orderSlotEntity.getId())).collect(Collectors.toList());
			}
		}
		List<OrderSlotResponseBean> orderSlotResponse = new ArrayList<>();
		Integer defaultSlotId = ParamsUtils.getIntegerParam("DEFAULT_SLOT_ID", 1);
		String slotFeeMessage = ParamsUtils.getParam("ORDER_SLOT_FEE_MESSAGE");
		for (OrderSlotEntity orderSlot : orderSlotList) {
			OrderSlotResponseBean orderSlotBean = new OrderSlotResponseBean();
			BeanUtils.copyProperties(orderSlot, orderSlotBean);
			if (org.apache.commons.lang3.StringUtils.isNotEmpty(slotFeeMessage)) {
				orderSlotBean.setFeeMessage(
						String.format(slotFeeMessage, orderSlot.getFees().intValue(), orderSlot.getMetadata().getFreeDeliveryAbove().intValue()));
			}
			if (orderSlotBean.getRemainingCount() <= 0) {
				orderSlotBean.setIsAvailable(false);
			}
			if (orderSlotBean.getId().compareTo(defaultSlotId) == 0) {
				orderSlotBean.setIsDefault(true);
			}
			orderSlotResponse.add(orderSlotBean);
		}
		sortEta(orderSlotResponse);
		_LOGGER.info(String.format("OrderSlots Response : %s", orderSlotResponse));
		return orderSlotResponse;
	}

	private void sortEta(List<OrderSlotResponseBean> orderSlotResponse) {
		orderSlotResponse.sort(Comparator.comparing(orderSlot -> LocalTime.parse(orderSlot.getEta())));
	}

	public Page<OrderWithRepeatItemBean> getRepeatOrdersByDeliveryDate(Date deliveryDate, Pageable pageable) {
		return orderRepository.findOrdersWithRepeatItem(new java.sql.Date(deliveryDate.getTime()), pageable);
	}

	public List<OrderWithRepeatItemBean> getLowWalletBalanceCarts(List<OrderWithRepeatItemBean> carts) {
		try {
			List<UUID> customerIds = carts.stream().map(OrderWithRepeatItemBean::getCustomerId).distinct().collect(Collectors.toList());
			List<WalletBean> wallets = clientService.findAllCustomersWallet(customerIds);
			Map<String, WalletBean> walletMap = wallets.stream().collect(Collectors.toMap(WalletBean::getEntityId, Function.identity()));
			return carts.stream().filter(cart -> {
				if (cart.getMetadata().getIsFirstOrder() && cart.getMetadata().getIsCod()) {
					return false;
				}
				WalletBean wallet = walletMap.get(cart.getCustomerId().toString());
				if (wallet == null) {
					_LOGGER.warn("No wallet found for customer ID: " + cart.getCustomerId());
					return true;
				}
				double totalBalance = wallet.getAmount() + wallet.getCreditLimit();
				return cart.getFinalBillAmount() > totalBalance;
			}).collect(Collectors.toList());
		} catch (Exception e) {
			_LOGGER.error("Error while getting low wallet balance carts", e);
		}
		return null;
	}

	public void failRepeatOrdersWithLowBalance(List<UUID> orderIds) {
		List<OrderEntity> orders = findRecordByIds(orderIds);
		for (OrderEntity order : orders) {
			order.setStatus(OrderStatus.ORDER_FAILED);
			order.getMetadata().setFailureReason(Collections.singletonList("Low wallet balance"));
		}
		saveAllOrders(orders);
	}
}