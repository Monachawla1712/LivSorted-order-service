package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.*;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.DeliverySlot;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderItemStatus;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import com.sorted.rest.services.order.services.ChallanService;
import com.sorted.rest.services.order.services.DisplayOrderIdService;
import com.sorted.rest.services.order.services.FranchiseOrderService;
import com.sorted.rest.services.order.services.InvoiceService;
import com.sorted.rest.services.order.utils.ChallanPDFGenerator;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Franchise Order Services", description = "Manage franchise-app order services.")
public class FranchiseOrderController implements BaseController {

	private AppLogger _LOGGER = LoggingManager.getLogger(FranchiseOrderController.class);

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private DisplayOrderIdService displayOrderIdService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ChallanService challanService;

	@Autowired
	private ChallanPDFGenerator challanPDFGenerator;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private DeliveryDateUtils deliveryDateUtils;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "Add to Cart for franchise store.", nickname = "addToCartFranchiseStore")
	@PostMapping("/orders/franchise/cart")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> addToCartFranchiseStore(@Valid @RequestBody FranchiseCartRequest request) {
		final String storeId = SessionUtils.getStoreId();
		_LOGGER.info(String.format("ADD TO FRANCHISE CART REQUEST : storeId : %s, skuCode : %s, quantity  : %s", storeId, request.getSkuCode(),
				request.getQuantity()));
		String skuCode = request.getSkuCode();
		FranchiseCartResponse response = new FranchiseCartResponse();
		final UUID customerId = franchiseOrderService.getCustomerId();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (cart == null && request.getQuantity() == 0) {
			_LOGGER.debug("addToCart : cart and request quantity both null");
			return ResponseEntity.ok(response);
		} else if (cart == null) {
			_LOGGER.debug("addToCart : cart is null");
			validateOrderTiming(response);
			if (response.getError() != null) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
			}
			String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
			StoreResponse storeInfo = clientService.fetchWmsStoreDetails(storeId);
			cart = FranchiseOrderEntity.createNewCart(customerId, storeId, displayOrderId, storeInfo.getIsSrpStore(), false);
		}
		FranchiseStoreInventoryResponse storeItemResponse = clientService.getFranchiseStoreInventory(storeId, skuCode);
		if (storeItemResponse == null) {
			if (response.getError() == null) {
				response.error("SKU_NOT_FOUND", "Item not found in the Store.");
			}
			_LOGGER.debug("addToCart :: isValidCartRequest failed");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		_LOGGER.info(String.format("addToCart :: franchiseStoreItemResponse: %s", storeItemResponse));
		franchiseOrderService.addItemToFranchiseCart(cart, storeItemResponse, request, response);
		if (Objects.nonNull(cart)) {
			response.setData(buildFranchiseCartResponse(cart));
		}
		if (response.getError() != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		return ResponseEntity.ok(response);
	}

	private FranchiseCartResponse validateOrderTiming(FranchiseCartResponse response) {
		if (stopFranchiseOrder()) {
			if (response.getError() == null) {
				response.error("Order", "Order can't be created. Please, update your cart.");
			}
			_LOGGER.debug("addToCart :: order place window is invalid");
		}
		return response;
	}

	@ApiOperation(value = "Get Cart Info.", nickname = "getCartInfo")
	@GetMapping("/orders/franchise/cart")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> getToCartFranchiseStore() {
		final String storeId = SessionUtils.getStoreId();
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (Objects.nonNull(cart)) {
			WalletBean walletBean = clientService.getStoreWallet(cart.getStoreId());
			cart.setWalletAmount(walletBean.getAmount());
			response.setData(buildFranchiseCartResponse(cart));
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Update Cart Info.", nickname = "updateCartInfo")
	@PutMapping("/orders/franchise/cart/refresh")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> updateCartFranchise(@Valid @RequestBody UpdateFranchiseCartRequest request) {
		final String storeId = request.getStoreId();
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (cart == null) {
			return ResponseEntity.ok(response);
		}
		cart.setWalletAmount(request.getWalletAmount());
		franchiseOrderService.updateCart(cart);
		if (Objects.nonNull(cart)) {
			response.setData(buildFranchiseCartResponse(cart));
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Clear Cart in franchise store.", nickname = "clearCart")
	@GetMapping("/orders/franchise/cart/clear")
	@ResponseStatus(HttpStatus.OK)
	public void clearFranchiseStoreCart() {
		String storeId = SessionUtils.getStoreId();
		_LOGGER.debug(String.format("CLEAR FRANCHISE CART REQUEST : storeId = %s", storeId));
		franchiseOrderService.clearFranchiseStoreCart(storeId);
	}

	@ApiOperation(value = "Get order id.", nickname = "getOrderId")
	@GetMapping("/orders/franchise/{orderId}")
	public ResponseEntity<FranchiseOrderResponseBean> getFranchiseOrderInfo(@PathVariable UUID orderId,
			@RequestParam(defaultValue = "false") Boolean showImsEligibleItems) {
		final String storeId = SessionUtils.getStoreId();
		FranchiseOrderEntity entity = getFranchiseOrder(orderId);
		if (!entity.getStoreId().equals(storeId)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		FranchiseOrderResponseBean response = getMapper().mapSrcToDest(entity, FranchiseOrderResponseBean.newInstance());
		if (showImsEligibleItems) {
			filterImsEligibleResponse(response);
		}
		franchiseOrderService.setAdjustmentDetailsAndCalculations(entity, response);
		return ResponseEntity.ok(response);
	}

	private void filterImsEligibleResponse(FranchiseOrderResponseBean response) {
		List<FranchiseOrderItemResponseBean> imsEligibleItems = response.getOrderItems().stream()
				.filter(item -> !item.getStatus().equals(FranchiseOrderItemStatus.PENDING)).collect(Collectors.toList());
		response.setOrderItems(imsEligibleItems);
	}

	@ApiOperation(value = "Get Last 30 days Order details.", nickname = "getStoreOrders")
	@GetMapping("/orders/franchise/store")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<FranchiseOrderListBean>> getStoreOrders() {
		String storeId = SessionUtils.getStoreId();
		int sinceDays = ParamsUtils.getIntegerParam("FRANCHISE_ORDERS_LIST_DAYS", 30);
		List<FranchiseOrderEntity> orders = franchiseOrderService.getOrdersSinceDays(storeId, sinceDays);
		if (CollectionUtils.isEmpty(orders)) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		List<FranchiseOrderListBean> orderListResponse = getMapper().mapAsList(orders, FranchiseOrderListBean.class);
		return ResponseEntity.ok(orderListResponse);
	}

	@ApiOperation(value = "Franchise-store Order status change", nickname = "processFranchiseOrders")
	@GetMapping("/orders/franchise/cart/process")
	@ResponseStatus(HttpStatus.OK)
	public void processFranchiseOrders() {
		List<FranchiseOrderEntity> entities = franchiseOrderService.findAllActiveCart();
		if (CollectionUtils.isNotEmpty(entities)) {
			Double amountLimit = franchiseOrderService.getAmountLimit(entities.get(0).getSlot());
			Map<String, Long> franchiseOrderCountMap = franchiseOrderService.getFranchiseOrderCountMap(
					entities.stream().map(FranchiseOrderEntity::getStoreId).collect(Collectors.toList()));
			Integer blockNonPrepaidOrders = ParamsUtils.getIntegerParam("BLOCK_NON_PREPAID_ORDERS", 0);
			Set<String> byPassNonPrepaidStores = new HashSet<>(Arrays.asList(ParamsUtils.getParam("BY_PASS_NON_PREPAID_STORES").split("\\s*,\\s*")));
			Map<String, WalletBean> walletMap = franchiseOrderService.getWalletMap(
					entities.stream().map(FranchiseOrderEntity::getStoreId).collect(Collectors.toList()));
			List<FranchiseOrderEntity> failedOrders = new ArrayList<>();
			for (FranchiseOrderEntity cart : entities) {
				boolean isFailed = false;
				if ((!Boolean.TRUE.equals(cart.getMetadata().getIsEligiblePreAutoRemoval())
						&& !franchiseOrderService.validMinOrderRule(cart, amountLimit, franchiseOrderCountMap))) {
					isFailed = true;
				} else if (byPassNonPrepaidStores.contains(cart.getStoreId())) {
					continue;
				} else if (blockNonPrepaidOrder(cart, blockNonPrepaidOrders, walletMap.get(cart.getStoreId()))) {
					isFailed = true;
				}
				if (isFailed) {
					cart.setStatus(FranchiseOrderStatus.FAILED);
					failedOrders.add(cart);
				}
			}
			entities = entities.stream().filter(o -> !o.getStatus().equals(FranchiseOrderStatus.FAILED)).collect(Collectors.toList());
			List<FranchiseOrderEntity> processedOrders = processCartOrder(entities);
			buildSrRequestAndUpload(processedOrders);
			if (CollectionUtils.isNotEmpty(failedOrders)) {
				franchiseOrderService.saveAllOrder(failedOrders);
			}
		}
	}

	private boolean blockNonPrepaidOrder(FranchiseOrderEntity cart, Integer blockNonPrepaidOrders, WalletBean walletBean) {
		if (blockNonPrepaidOrders == 1 && cart.getFinalBillAmount() > walletBean.getAmount()) {
			return true;
		}
		return false;
	}

	private Set<String> fetchTestStores() {
		final String TEST_STORE_IDS = "TEST_STORE_IDS";
		String testStoreIds = ParamsUtils.getParam(TEST_STORE_IDS);
		Set<String> testStoreSet = null;
		if (testStoreIds != null) {
			String[] stores = testStoreIds.split(",");
			testStoreSet = Arrays.stream(stores).collect(Collectors.toSet());
		}
		return testStoreSet;
	}

	private List<FranchiseOrderEntity> processCartOrder(List<FranchiseOrderEntity> orders) {
		List<FranchiseOrderEntity> processedOrders = new ArrayList<>();
		Set<String> testStoreSet = fetchTestStores();
		List<ClevertapEventRequest.ClevertapEventData> clevertapOrderEventDataList = new ArrayList<>();
		for (FranchiseOrderEntity order : orders) {
			if (testStoreSet != null && testStoreSet.contains(order.getStoreId())) {
				continue;
			}
			try {
				_LOGGER.debug(String.format("processCartOrder::orderId: %s", order.getDisplayOrderId()));
				String storeId = order.getStoreId();
				String txnType = "Franchise-Order";
				franchiseOrderService.deductMoneyFromStoreWallet(storeId, null, order.getDisplayOrderId(), txnType, order.getFinalBillAmount(), null, null);
				order.setStatus(FranchiseOrderConstants.FranchiseOrderStatus.NEW_ORDER);
				order.setAmountReceived(order.getFinalBillAmount());
				if (order.getMetadata() == null) {
					order.setMetadata(new FranchiseOrderMetadata());
				}
				order.getMetadata().setOrderPlacedAmount(BigDecimal.valueOf(order.getFinalBillAmount()));
				order.setSubmittedAt(new Date());
				//				processOrderCashback(order);
				franchiseOrderService.saveFranchiseEntity(order);
				processedOrders.add(order);
				clevertapOrderEventDataList.add(buildOrderProcessedClevertapEventData(order));
			} catch (Exception e) {
				_LOGGER.error(String.format("processCartOrder::skipping order: %s", order.getDisplayOrderId()));
			}
		}
		clientService.sendMultipleClevertapEvents(clevertapOrderEventDataList);
		return processedOrders;
	}

	private ClevertapEventRequest.ClevertapEventData buildOrderProcessedClevertapEventData(FranchiseOrderEntity order) {
		try {
			Map<String, Object> clevertapEventData = new HashMap<>();
			clevertapEventData.put("orderId", order.getId());
			clevertapEventData.put("totalBillValue", order.getFinalBillAmount());
			int fruitsCount = 0;
			int vegetablesCount = 0;
			int cratesCount = 0;
			for (FranchiseOrderItemEntity orderItem : order.getOrderItems()) {
				if (Objects.equals(orderItem.getCategoryName(), "Fruits")) {
					fruitsCount++;
				} else if (Objects.equals(orderItem.getCategoryName(), "Vegetables")) {
					vegetablesCount++;
				}
				if (orderItem.getFinalCrateQty() != null) {
					cratesCount += orderItem.getFinalCrateQty();
				}
			}
			clevertapEventData.put("fruitsCount", fruitsCount);
			clevertapEventData.put("vegetablesCount", vegetablesCount);
			clevertapEventData.put("cratesCount", cratesCount);
			try {
				StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(order.getStoreId());
				clevertapEventData.put("storeName", storeDataResponse.getName());
				if (storeDataResponse.getOpenTime() != null) {
					clevertapEventData.put("openTime", storeDataResponse.getOpenTime());
				}
			} catch (Exception e) {
				_LOGGER.error("buildOrderProcessedClevertapEventData:: Failed while getting store details: ", e);
			}
			return clientService.buildClevertapEventData("orderPlaced", order.getCustomerId(), clevertapEventData);
		} catch (Exception e) {
			_LOGGER.error("buildOrderProcessedClevertapEventData:: order processing event failed", e);
		}
		return null;
	}

	private void processOrderCashback(FranchiseOrderEntity order) {
		if (order != null && order.getOfferData() != null) {
			String offerType = order.getOfferData().getOfferType();
			Double offerAmount = order.getOfferData().getAmount();
			if (offerType != null && offerAmount != null && offerType.equals("CASHBACK")) {
				franchiseOrderService.deductMoneyFromStoreWallet(order.getStoreId(), offerAmount, "CB-" + order.getDisplayOrderId(), "FO-OFFER", null,
						offerType + " on " + order.getDisplayOrderId(), null);
			}
		}
	}

	private FranchiseOrderResponseBean buildFranchiseCartResponse(FranchiseOrderEntity cart) {
		FranchiseOrderResponseBean response = getMapper().mapSrcToDest(cart, FranchiseOrderResponseBean.newInstance());
		if (cart != null && CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			Map<String, Long> categoryCount = cart.getOrderItems().stream().collect(Collectors.groupingBy(i -> i.getCategoryName(), Collectors.counting()));
			response.setCategoryCount(categoryCount);
		}
		Double amountLimit = franchiseOrderService.getAmountLimit(cart.getSlot());
		Map<String, Long> franchiseOrderCountMap = franchiseOrderService.getFranchiseOrderCountMap(Collections.singletonList(cart.getStoreId()));
		Boolean validMinOrderRule = franchiseOrderService.validMinOrderRule(cart, amountLimit, franchiseOrderCountMap);
		if (!validMinOrderRule) {
			response.setValidMinOrderRule(false);
			response.setMinAmountMsgCart(String.format("आपके cart में %s रुपए से कम की फल सब्जियां है। यह order deliver नही किया जाएगा।", amountLimit));
			response.setMinAmountMsgHome(String.format("आपके cart में %s रुपए से कम की फल सब्जियां है। यह order deliver नही किया जाएगा।", amountLimit));
		} else {
			response.setValidMinOrderRule(true);
		}
		if (cart.getExtraFeeDetails() != null && cart.getExtraFeeDetails().getDeliveryCharge() > 0) {
			if (cart.getWalletAmount() != null) {
				BigDecimal walletTopupAmount = BigDecimal.valueOf(cart.getTotalSpGrossAmount()).subtract(BigDecimal.valueOf(cart.getWalletAmount()));
				response.setWalletTopupForDeliveryCharges(walletTopupAmount.doubleValue());
				response.setDeliveryChargeMsgHome(
						"दुकानदार भाई, ध्यान दें !\n" + "आज से सभी \"post paid \" orders पर delivery charges लागू होंगे! \n" + "FREE delivery पाने के लिए कृपया करके अपने wallet में order से पहले पर्याप्त राशि डालें ।");
				response.setDeliveryChargeMsgCart(
						String.format("Free delivery के लिए  wallet में ₹%s डालें \nअन्यथा ₹%s delivery charges भरें", walletTopupAmount,
								cart.getExtraFeeDetails().getDeliveryCharge()));
			}
		}
		return response;
	}

	@ApiOperation(value = "Franchise-store Order status Changed.", nickname = "uploadSR")
	@GetMapping("/orders/franchise/upload-sr")
	@ResponseStatus(HttpStatus.OK)
	public void uploadSR() {
		List<FranchiseOrderEntity> entities = franchiseOrderService.findAllActiveOrderSRNotUploaded();
		buildSrRequestAndUpload(entities);
	}

	private void buildSrRequestAndUpload(List<FranchiseOrderEntity> entities) {
		entities.stream().forEach(o -> {
			_LOGGER.info(String.format("buildSrRequestAndUpload: %s", o.getDisplayOrderId()));
			if (CollectionUtils.isNotEmpty(o.getOrderItems())) {
				List<FranchiseSRBean> franchiseSRBeans = new ArrayList<FranchiseSRBean>();
				o.getOrderItems().stream().sorted(Comparator.comparing(FranchiseOrderItemEntity::getProductName)).forEach(s -> {
					FranchiseSRBean franchiseSRBean = new FranchiseSRBean();
					franchiseSRBean.setCratesRequested(s.getOrderedCrateQty());
					franchiseSRBean.setSkuCode(s.getSkuCode());
					franchiseSRBean.setDate(o.getDeliveryDate());
					franchiseSRBean.setStoreId(Integer.valueOf(o.getStoreId()));
					franchiseSRBean.setSlot(o.getSlot());
					franchiseSRBean.setSortOrder(0);
					franchiseSRBean.setWhId(s.getWhId());
					franchiseSRBean.setOrderId(o.getId());
					franchiseSRBeans.add(franchiseSRBean);
				});
				clientService.uploadFranchiseStoreRequisition(franchiseSRBeans);
				o.setIsSrUploaded(1);
				franchiseOrderService.saveFranchiseEntity(o);
			}
		});
	}

	@ApiOperation(value = "Franchise-store Order status Changed.", nickname = "franchiseOrderStatusUpdate")
	@PostMapping("/orders/franchise/update-status")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<ChallanResponse> franchiseOrderStatusUpdate(@RequestBody FranchiseOrderStatusUpdateRequest request) {
		_LOGGER.debug(String.format("updateFranchiseOrderStatus orderId : %s", request.getOrderId()));
		FranchiseOrderEntity franchiseOrder = getFranchiseOrder(request.getOrderId());
		franchiseOrderService.updateFranchiseOrderStatus(franchiseOrder, request);
		if (franchiseOrder.getStatus().equals(FranchiseOrderStatus.ORDER_BILLED) || franchiseOrder.getStatus()
				.equals(FranchiseOrderStatus.PARTIALLY_DISPATCHED)) {
			generateChallan(franchiseOrder);
		}
		ChallanResponse response = new ChallanResponse();
		response.setChallanUrl(franchiseOrder.getChallanUrl());
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Create Back Office Order For Franchise Store", nickname = "createBackofficeOrderForFranchiseStore")
	@PostMapping("/orders/franchise/backoffice")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> createBackofficeOrder(@Valid @RequestBody BackofficeFranchiseOrderBean request) {
		FranchiseCartResponse response = new FranchiseCartResponse();
		_LOGGER.debug(String.format("CREATE FRANCHISE ORDER FROM BACKOFFICE REQUEST storeId : %s", request.getStoreId()));
		if (request.getOrderItems() == null) {
			return ResponseEntity.ok(response);
		}
		String appId = SessionUtils.getAppId();
		//		commenting below code to allow SR required true for offline FOS orders
		//		if (Objects.equals(appId, "com.sorted.fos")) {
		//			request.setIsSrRequired(0);
		//		}
		String orderId = displayOrderIdService.getNewDisplayOrderId();
		final UUID customerId = franchiseOrderService.getCustomerId();
		StoreResponse storeInfo = clientService.fetchWmsStoreDetails(request.getStoreId());
		FranchiseOrderEntity cart = FranchiseOrderEntity.createNewCart(customerId, request.getStoreId(), orderId, storeInfo.getIsSrpStore(), true);
		List<FranchiseInventoryRequest> whRequest = getMapper().mapAsList(request.getOrderItems(), FranchiseInventoryRequest.class);
		franchiseOrderService.backofficeFranchiseOrder(cart, request, response, whRequest, request.getStoreId());
		if (response.getError() != null) {
			cart.setActive(0);
			franchiseOrderService.saveFranchiseEntity(cart);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		if (Objects.nonNull(cart)) {
			if (request.getIsSrRequired() == 1) {
				List<FranchiseOrderEntity> orders = new ArrayList<>();
				orders.add(cart);
				List<FranchiseOrderEntity> processedOrder = processCartOrder(orders);
				buildSrRequestAndUpload(processedOrder);
			} else {
				String storeId = cart.getStoreId();
				String txnType = "Franchise-Order";
				franchiseOrderService.deductMoneyFromStoreWallet(storeId, -1 * cart.getFinalBillAmount(), cart.getDisplayOrderId(), txnType, null, null, null);
				cart.setDeliveryDate(new Date());
				cart.setSubmittedAt(new Date());
				cart.setStatus(FranchiseOrderStatus.ORDER_DELIVERED);
				franchiseOrderService.saveFranchiseEntity(cart);
			}
			response.setData(buildFranchiseCartResponse(cart));
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Create Back Office Cart For Franchise Store", nickname = "createBackofficeCartForFranchiseStore")
	@PostMapping("/orders/franchise/backoffice/cart")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> createBackofficeCart(@Valid @RequestBody BackofficeFranchiseOrderBean request) {
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = null;
		if (request.getStoreId() != null) {
			cart = franchiseOrderService.findStoreCurrentCart(request.getStoreId());
		}
		List<FranchiseInventoryRequest> whRequest = getMapper().mapAsList(request.getOrderItems(), FranchiseInventoryRequest.class);
		if (cart == null) {
			String orderId = displayOrderIdService.getNewDisplayOrderId();
			final UUID customerId = franchiseOrderService.getCustomerId();
			StoreResponse storeInfo = clientService.fetchWmsStoreDetails(request.getStoreId());
			cart = FranchiseOrderEntity.createNewCart(customerId, request.getStoreId(), orderId, storeInfo.getIsSrpStore(), true);
		}
		franchiseOrderService.addItemsToCartFromList(cart, request, response, whRequest, request.getStoreId());
		franchiseOrderService.saveFranchiseEntity(cart);
		response.setData(buildFranchiseCartResponse(cart));
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "upload Franchise Order Backoffice", nickname = "uploadFranchiseOrderBackoffice")
	@PostMapping(path = "/orders/franchise/backoffice/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvUploadResult<BackofficeFranchiseCartRequest> uploadFranchiseOrderBackOffice(@RequestParam("file") MultipartFile file,
			@RequestParam("storeId") String storeId) {
		_LOGGER.debug(String.format("UPLOAD FRANCHISE ORDER FOR BACKOFFICE REQUEST : %s", storeId));
		final int maxAllowedRows = 1000;
		final String module = "franchise-order";
		List<BackofficeFranchiseCartRequest> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, BackofficeFranchiseCartRequest.newInstance());
		List<BackofficeFranchiseCartRequest> response = franchiseOrderService.preProcessFranchiseOrderUpload(rawBeans, storeId);
		CsvUploadResult<BackofficeFranchiseCartRequest> csvUploadResult = validateFranchiseStoreInventoryUpload(response);
		csvUploadResult.setHeaderMapping(response.get(0).getHeaderMapping());
		return csvUploadResult;
	}

	private CsvUploadResult<BackofficeFranchiseCartRequest> validateFranchiseStoreInventoryUpload(List<BackofficeFranchiseCartRequest> beans) {
		final CsvUploadResult<BackofficeFranchiseCartRequest> result = new CsvUploadResult<>();

		if (CollectionUtils.isNotEmpty(beans)) {
			beans.stream().forEach(bean -> {
				try {
					org.springframework.validation.Errors errors = getSpringErrors(bean);
					franchiseOrderService.validateFranchiseStoreInventoryOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);

				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Arrays.asList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.error("Franchise Store Order Uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(bean);
				}
			});
		}
		return result;
	}

	@ApiOperation(value = "List all customer Orders from Backoffice", nickname = "findCustomersOrderListBackoffice")
	@GetMapping("/orders/franchise/store/{id}")
	public ResponseEntity<List<FranchiseOrderListBean>> findCustomerAllOrdersBackoffice(@PathVariable String id) {
		final List<FranchiseOrderEntity> results = franchiseOrderService.getOrdersSinceDays(id, 30);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, FranchiseOrderListBean.class));
	}

	@ApiOperation(value = "List today's delivered orders", nickname = "getLatestOrderDetails")
	@GetMapping("/orders/franchise/store/{id}/today")
	public ResponseEntity<List<FranchiseOrderListBean>> getLatestOrderDetails(@PathVariable String id) {
		final List<FranchiseOrderEntity> results = franchiseOrderService.getTodayOrders(id);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, FranchiseOrderListBean.class));
	}

	@ApiOperation(value = "List all customer Orders from Backoffice", nickname = "findCustomersOrderListBackoffice")
	@GetMapping("/orders/franchise/admin/refund-orders/{id}")
	public ResponseEntity<List<FranchiseOrderResponseBean>> getRefundOrdersFromParentOrderId(@PathVariable UUID id) {
		_LOGGER.info(String.format("REFUND ORDERS FROM BACKOFFICE orderId : %s", id));
		final List<FranchiseOrderEntity> results = franchiseOrderService.findOrderFromParentOrderId(id);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, FranchiseOrderResponseBean.class));
	}

	@ApiOperation(value = "List all customer Orders from Backoffice", nickname = "findCustomersOrderListBackoffice")
	@GetMapping("/orders/franchise/store/refund-orders/{id}")
	public ResponseEntity<List<FranchiseOrderResponseBean>> getConfirmedRefundOrdersFromParentOrderId(@PathVariable UUID id) {
		final List<FranchiseOrderEntity> results = franchiseOrderService.findConfirmedRefundOrdersFromParentOrderId(id);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, FranchiseOrderResponseBean.class));
	}

	@ApiOperation(value = "Get one Order For Backoffice with provided id.", nickname = "findOneOrderForBackoffice")
	@GetMapping("/orders/franchise/{id}/backoffice")
	public ResponseEntity<FranchiseOrderResponseBean> findOneForBackoffice(@PathVariable UUID id) {
		FranchiseOrderEntity order = getFranchiseOrder(id);
		FranchiseOrderResponseBean orderResponse = getMapper().mapSrcToDest(order, FranchiseOrderResponseBean.newInstance());
		if (order.getParentOrderId() != null) {
			FranchiseOrderEntity parentOrder = franchiseOrderService.findRecordById(id);
			setParentOrder(parentOrder, orderResponse);
		}
		if (orderResponse != null) {
			Long orderCount = franchiseOrderService.getDeliveredOrderCount(order.getStoreId());
			orderResponse.setOrderCount(orderCount);
			FranchiseOrderEntity firstOrder = franchiseOrderService.getFirstOrder(order.getStoreId());
			if (firstOrder != null) {
				orderResponse.setFirstOrderDate(firstOrder.getDeliveryDate());
			}
		}
		return ResponseEntity.ok(orderResponse);
	}

	Map<String, Object> getPendingRefundOrderParams() {
		Map<String, Object> params = new HashMap<>();
		FranchiseOrderStatus statusList = FranchiseOrderStatus.REFUND_REQUESTED;
		params.put("status", statusList);
		return params;
	}

	@ApiOperation(value = "Franchise Refund Order", nickname = "franchiseRefundOrder")
	@GetMapping("/orders/franchise/admin/pending-refunds")
	public PageAndSortResult<FranchiseOrderListBean> getPendingRefundsList(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		sort = new LinkedHashMap<>();
		sort.put("submittedAt", PageAndSortRequest.SortDirection.DESC);
		final Map<String, Object> params = getPendingRefundOrderParams();
		PageAndSortResult<FranchiseOrderEntity> poEntityList = franchiseOrderService.findFranchiseOrdersByPage(pageSize, pageNo, params, sort);
		Map<UUID, FranchiseOrderEntity> parentOrderMap = franchiseOrderService.findRecordByIds(
						poEntityList.getData().stream().map(FranchiseOrderEntity::getParentOrderId).collect(Collectors.toList())).stream()
				.collect(Collectors.toMap(FranchiseOrderEntity::getId, Function.identity()));
		PageAndSortResult<FranchiseOrderListBean> response = new PageAndSortResult<>();
		if (poEntityList != null && poEntityList.getData() != null) {
			response = prepareResponsePageData(poEntityList, FranchiseOrderListBean.class);
		}
		for (FranchiseOrderListBean orderResponse : response.getData()) {
			if (orderResponse.getParentOrderId() != null) {
				setParentOrder(parentOrderMap.get(orderResponse.getParentOrderId()), orderResponse);
			}
		}
		return response;
	}

	private void setParentOrder(FranchiseOrderEntity parentOrder, FranchiseOrderListBean response) {
		if (response.getParentOrderId() != null) {
			RefundParentOrderResponseBean parentOrderResponse = getMapper().mapSrcToDest(parentOrder, RefundParentOrderResponseBean.newInstance());
			response.setParentOrder(parentOrderResponse);
		}
	}

	@ApiOperation(value = "Franchise Refund Order", nickname = "franchiseRefundOrder")
	@PostMapping("/orders/franchise/admin/initiate-refund")
	public ResponseEntity<FranchiseOrderResponseBean> initiateRefundOrder(@Valid @RequestBody FranchiseOrderRefundBean request) {
		_LOGGER.info(String.format("REFUND ORDER FROM BACKOFFICE parentOrderId : %s", request.getParentOrderId()));
		FranchiseOrderEntity parentOrder = getFranchiseOrder(request.getParentOrderId());
		if (!(parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_DELIVERED || parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_BILLED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		String txnType = getRefundTxnType(request.getReturnIssue());
		if (txnType == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Invalid Return Issue", "returnIssue"));
		}
		Map<String, FranchiseStoreInventoryResponse> storeItemMap = getStoreItemMap(request, parentOrder);
		FranchiseOrderEntity refundOrder = franchiseOrderService.generateRefundRequestOrder(parentOrder, request, storeItemMap);
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, FranchiseOrderResponseBean.newInstance()));
	}

	@ApiOperation(value = "Franchise Refund Order", nickname = "franchiseRefundOrder")
	@PostMapping("/orders/franchise/admin/confirm-refund")
	public ResponseEntity<FranchiseOrderResponseBean> confirmRefundOrder(@Valid @RequestBody FranchiseConfirmOrderRefundBean request) {
		FranchiseOrderEntity refundOrder = getFranchiseOrder(request.getRefundOrderId());
		FranchiseOrderEntity parentOrder = getFranchiseOrder(refundOrder.getParentOrderId());
		if (!(parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_DELIVERED || parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_BILLED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		String txnType = getRefundTxnType(refundOrder.getRefundType());
		if (txnType == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Invalid Return Issue", "returnIssue"));
		}
		refundOrder = franchiseOrderService.confirmRefundRequestWithFinalAmounts(refundOrder, parentOrder, request);
		franchiseOrderService.deductMoneyFromStoreWallet(refundOrder.getStoreId(), refundOrder.getFinalBillAmount(), parentOrder.getDisplayOrderId(), txnType,
				null, null, null);
		generateInvoice(parentOrder, refundOrder);
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, FranchiseOrderResponseBean.newInstance()));
	}

	@ApiOperation(value = "Cancel Franchise Order", nickname = "CancelFranchiseOrder")
	@PostMapping("/orders/franchise/admin/refund/{orderId}/cancel")
	public ResponseEntity<Void> cancelRefundOrder(@PathVariable UUID orderId) {
		_LOGGER.info(String.format("CancelFranchiseOrder:: orderId: %s", orderId));
		FranchiseOrderEntity order = getFranchiseOrder(orderId);
		if (!order.getStatus().equals(FranchiseOrderStatus.REFUND_REQUESTED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Franchise order Status : %s, required status : ORDER_BILLED / NEW_ORDER ", order.getStatus()), orderId.toString()));
		}
		franchiseOrderService.cancelRefundOrder(order);
		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "Franchise Refund Order", nickname = "franchiseRefundOrder")
	@PostMapping("/orders/franchise/admin/refund")
	public ResponseEntity<FranchiseOrderResponseBean> refundOrder(@Valid @RequestBody FranchiseOrderRefundBean request) {
		_LOGGER.info(String.format("REFUND ORDER FROM BACKOFFICE parentOrderId : %s", request.getParentOrderId()));
		FranchiseOrderEntity parentOrder = getFranchiseOrder(request.getParentOrderId());
		if (!(parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_DELIVERED || parentOrder.getStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_BILLED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		String txnType = getRefundTxnType(request.getReturnIssue());
		if (txnType == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Invalid Return Issue", "returnIssue"));
		}
		Map<String, FranchiseStoreInventoryResponse> storeItemMap = getStoreItemMap(request, parentOrder);
		FranchiseOrderEntity refundOrder = franchiseOrderService.createRefundOrder(parentOrder, request, storeItemMap);
		franchiseOrderService.deductMoneyFromStoreWallet(refundOrder.getStoreId(), refundOrder.getFinalBillAmount(), parentOrder.getDisplayOrderId(), txnType,
				null, null, null);
		generateInvoice(parentOrder, refundOrder);
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, FranchiseOrderResponseBean.newInstance()));
	}

	private String getRefundTxnType(String returnIssue) {
		if (Objects.equals(returnIssue, "QUANTITY_ISSUE")) {
			return "FO-QUANTITY-ISSUE-REFUND";
		} else if (Objects.equals(returnIssue, "WRONG_ITEM_ISSUE")) {
			return "FO-WRONG-ITEM-ISSUE-REFUND";
		} else if (Objects.equals(returnIssue, "QUALITY_ISSUE")) {
			return "FO-QUALITY-ISSUE-REFUND";
		}
		return null;
	}

	private Map<String, FranchiseStoreInventoryResponse> getStoreItemMap(FranchiseOrderRefundBean request, FranchiseOrderEntity parentOrder) {
		Map<String, FranchiseStoreInventoryResponse> storeItemMap = null;
		if (request.getReturnIssue().equals("RETURN_QUANTITY")) {
			List<FranchiseInventoryRequest> whRequest = getMapper().mapAsList(request.getRefundOrderItems(), FranchiseInventoryRequest.class);
			Set<String> skuCodes = whRequest.stream().map(FranchiseInventoryRequest::getSkuCode).collect(Collectors.toSet());
			List<FranchiseStoreInventoryResponse> storeItemResponse = clientService.getStoreSkuInventoryForBulkRequest(skuCodes, parentOrder.getStoreId());
			if (storeItemResponse.size() != request.getRefundOrderItems().size()) {
				_LOGGER.info("Inventory Response mismatch.");
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
			}
			if (CollectionUtils.isNotEmpty(storeItemResponse)) {
				storeItemMap = storeItemResponse.stream().collect(Collectors.toMap(FranchiseStoreInventoryResponse::getSkuCode, i -> i));
			}
		}
		return storeItemMap;
	}

	@ApiOperation(value = "Franchise Final Quantity Upload", nickname = "uploadFranchiseOrderReBilling")
	@PostMapping("/orders/franchise/{orderId}/re-bill/upload")
	public ResponseEntity<FranchiseOrderResponseBean> uploadFranchiseOrderReBilling(@RequestParam("file") MultipartFile file, @PathVariable UUID orderId) {
		_LOGGER.info(String.format("uploadFranchiseOrderReBilling:: orderId: %s", orderId));
		FranchiseOrderEntity franchiseOrder = getFranchiseOrder(orderId);
		if (franchiseOrder.getStatus().equals(FranchiseOrderStatus.CANCELLED_POST_BILLING)) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Order is in status : %s and can not be re-billed", franchiseOrder.getStatus()),
							"order"));
		}
		final int maxAllowedRows = 1000;
		final String module = "franchise-order-rebill";
		List<FranchiseReBillUploadBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, FranchiseReBillUploadBean.newInstance());
		franchiseOrder = franchiseOrderService.rebillFranchiseOrder(rawBeans, franchiseOrder);
		FranchiseOrderResponseBean uploadResponse = getMapper().mapSrcToDest(franchiseOrder, FranchiseOrderResponseBean.newInstance());
		return ResponseEntity.ok(uploadResponse);
	}

	@ApiOperation(value = "Create franchise store challan", nickname = "createFranchiseStoreChallan")
	@PostMapping("/orders/franchise/{orderId}/challan")
	public ResponseEntity<ChallanResponse> generateFranchiseStoreChallan(@PathVariable UUID orderId) {
		_LOGGER.debug(String.format("generateFranchiseOrderChallan:: orderId: %s", orderId));
		FranchiseOrderEntity franchiseOrderEntity = getFranchiseOrder(orderId);
		if (!(franchiseOrderEntity.getStatus().equals(FranchiseOrderConstants.FranchiseOrderStatus.ORDER_BILLED) || franchiseOrderEntity.getStatus()
				.equals(FranchiseOrderConstants.FranchiseOrderStatus.ORDER_DELIVERED))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Franchise order Status : %s, required status : ORDER_BILLED or ORDER_DELIVERED", franchiseOrderEntity.getStatus()),
					orderId.toString()));
		}
		franchiseOrderEntity = generateChallan(franchiseOrderEntity);
		ChallanResponse response = new ChallanResponse();
		response.setChallanUrl(franchiseOrderEntity.getChallanUrl());
		return ResponseEntity.ok(response);
	}

	private FranchiseOrderEntity generateChallan(FranchiseOrderEntity franchiseOrderEntity) {
		StoreResponse store = clientService.fetchWmsStoreDetails(franchiseOrderEntity.getStoreId());
		if (store == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("generateChallan:: WMS Store = %s not found ", franchiseOrderEntity.getStoreId()),
							"StoreId"));
		}
		ChallanDataBean challanDataBean = challanService.buildChallanBeanData(franchiseOrderEntity, store);
		challanPDFGenerator.generatePdfReport(challanDataBean);
		challanService.uploadChallan(challanDataBean);
		franchiseOrderEntity.setChallanUrl(challanDataBean.getChallanUrl());
		return franchiseOrderService.saveFranchiseEntity(franchiseOrderEntity);
	}

	@ApiOperation(value = "Cancel Franchise Order", nickname = "CancelFranchiseOrder")
	@PostMapping("/orders/franchise/{orderId}/cancel")
	public ResponseEntity<Void> cancelFranchiseOrder(@PathVariable UUID orderId) {
		_LOGGER.info(String.format("CancelFranchiseOrder:: orderId: %s", orderId));
		FranchiseOrderEntity order = getFranchiseOrder(orderId);
		if (!order.getStatus().equals(FranchiseOrderStatus.NEW_ORDER)) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Franchise order Status : %s, required status : NEW_ORDER ", order.getStatus()),
							orderId.toString()));
		}
		franchiseOrderService.cancelFranchiseOrder(order);
		clientService.sendSingleClevertapEvent("OrderCancelled", order.getCustomerId(), buildOrderCancelClevertapEventData(order));
		return ResponseEntity.ok().build();
	}

	private Map<String, Object> buildOrderCancelClevertapEventData(FranchiseOrderEntity order) {
		try {
			Map<String, Object> clevertapEventData = new HashMap<>();
			clevertapEventData.put("order Canceled", order.getId());
			return clevertapEventData;
		} catch (Exception e) {
			_LOGGER.info("order cancel event failed");
		}
		return null;
	}

	@ApiOperation(value = "Get order by display-id.", nickname = "getByDisplayOrderId")
	@GetMapping("/orders/franchise/display-id/{displayOrderId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<FranchiseOrderResponseBean> getFranchiseOrderByDisplayOrderID(@PathVariable String displayOrderId) {
		final String storeId = SessionUtils.getStoreId();
		FranchiseOrderEntity entity = franchiseOrderService.findByDisplayOrderId(displayOrderId);
		_LOGGER.info(String.format("GET FRANCHISE ORDER REQUEST : storeId = %s DisplayOrderId = %s", storeId, displayOrderId));
		if (entity == null || !entity.getStoreId().equals(storeId)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		FranchiseOrderResponseBean response = getMapper().mapSrcToDest(entity, FranchiseOrderResponseBean.newInstance());
		franchiseOrderService.setAdjustmentDetailsAndCalculations(entity, response);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Get order by display-ids.", nickname = "getByIds")
	@GetMapping("/orders/franchise/display-ids")
	@ResponseStatus
	public ResponseEntity<List<FranchiseOrderListBean>> getFranchiseOrderByDisplayIds(@RequestParam Set<String> ids) {
		List<FranchiseOrderEntity> franchiseOrderEntities = franchiseOrderService.findOrdersByDisplayOrderId(ids.stream().collect(Collectors.toList()));
		List<FranchiseOrderListBean> response = getMapper().mapAsList(franchiseOrderEntities, FranchiseOrderListBean.class);
		return ResponseEntity.ok(response);
	}

	private PaymentNoteData getLastNote(InvoiceEntity invoice) {
		int totalNotes = invoice.getPaymentNotes().getNotesList().size();
		return invoice.getPaymentNotes().getNotesList().get(totalNotes - 1);
	}

	private Boolean stopFranchiseOrder() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String orderBlockStartTime = ParamsUtils.getParam("ORDER_BLOCK_WINDOW_START_TIME", "23:01:00");
		String orderBlockEndTime = ParamsUtils.getParam("ORDER_BLOCK_WINDOW_END_TIME", "03:00:00");
		return localTime.isAfter(LocalTime.parse(orderBlockStartTime)) || localTime.isBefore(LocalTime.parse(orderBlockEndTime));
	}

	@ApiOperation(value = "Get Cart Info.", nickname = "getCartInfo")
	@GetMapping("/orders/franchise/internal/cart/{storeId}")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> getCartInternalFranchiseStore(@PathVariable String storeId) {
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (Objects.nonNull(cart)) {
			response.setData(buildFranchiseCartResponse(cart));
		}
		if (response.getData() != null) {
			Long orderCount = franchiseOrderService.getDeliveredOrderCount(storeId);
			response.getData().setOrderCount(orderCount);
			FranchiseOrderEntity firstOrder = franchiseOrderService.getFirstOrder(cart.getStoreId());
			if (firstOrder != null) {
				response.getData().setFirstOrderDate(firstOrder.getDeliveryDate());
			}
			franchiseOrderService.addSpGrossAmountWithoutBulkSkus(cart);
			response.getData().setSpGrossAmountWithoutBulkSkus(cart.getEffectiveSpGrossAmountForCashback());
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Apply offer to Cart ", nickname = "applyOffer")
	@PostMapping("/orders/franchise/cart/apply-offer")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> applyFranchiseOffer(@Valid @RequestBody ApplyOfferRequest request) {
		final String storeId = SessionUtils.getStoreId();
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		if (request.getVoucherCode() != null) {
			if (cart.getOfferData() == null) {
				cart.setOfferData(new OfferData());
			}
			cart.getOfferData().setVoucherCode(request.getVoucherCode());
			cart.getOfferData().setIsOfferApplied(false);
			cart.getOfferData().setOrderDiscount(null);
		} else {
			throw new ValidationException(ErrorBean.withError("no_discount", "No voucher provided", "cart"));
		}
		franchiseOrderService.updateCart(cart);
		franchiseOrderService.saveFranchiseEntity(cart);
		if (cart.getError() != null) {
			if (Objects.equals(cart.getError().getCode(), "OFFER_ERROR") && cart.getError().getMessage() != null) {
				response.setMessage(cart.getError().getMessage());
			}
		}
		if (Objects.nonNull(cart)) {
			response.setData(buildFranchiseCartResponse(cart));
		}
		return ResponseEntity.ok(response);
	}

	private void customerIdCheck(UUID customerId) {
		if (customerId == null) {
			throw new ValidationException(ErrorBean.withError("customer_id_missing", "Customer Id not found.", "customerId"));
		}
	}

	private void removeOfferFromCart(FranchiseOrderEntity cart) {
		_LOGGER.info(String.format("voucher %s removed from cart for storeId: %s", cart.getOfferData().getVoucherCode(), cart.getStoreId()));
		cart.getOfferData().setVoucherCode(null);
		cart.getOfferData().setIsOfferApplied(false);
		cart.getOfferData().setOfferType(null);
		cart.getOfferData().setOfferId(null);
		cart.getOfferData().setAmount(null);
		cart.getOfferData().setOrderDiscount(null);
		cart.getOfferData().setOfferTitle(null);
		cart.getOfferData().setItemOfferAmounts(null);
		cart.getOfferData().setCashbackDetails(null);
		cart.getOfferData().setRemovedBy(SessionUtils.getAuthUserId());
		cart.getOfferData().setRemovedAt(franchiseOrderService.getIstTimeString());
		cart.setIsOfferRemoved(true);
	}

	@ApiOperation(value = "Remove Offer from Cart ", nickname = "removeOffer")
	@PostMapping("/orders/franchise/cart/remove-offer")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseCartResponse> removeOffer(@Valid @RequestBody RemoveOfferRequest request) {
		final String storeId = SessionUtils.getStoreId();
		FranchiseCartResponse response = new FranchiseCartResponse();
		FranchiseOrderEntity cart = franchiseOrderService.findStoreCurrentCart(storeId);
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		removeOfferFromCart(cart);
		franchiseOrderService.updateCart(cart);
		franchiseOrderService.saveFranchiseEntity(cart);
		if (Objects.nonNull(cart)) {
			response.setData(buildFranchiseCartResponse(cart));
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Franchise Re-Bill Final Quantity", nickname = "franchiseOrderReBilling")
	@PostMapping("/orders/franchise/{orderId}/re-bill")
	public ResponseEntity<FranchiseOrderResponseBean> franchiseOrderReBilling(@RequestBody List<FranchiseReBillUploadBean> request,
			@PathVariable UUID orderId) {
		_LOGGER.info(String.format("franchiseOrderReBilling:: orderId: %s", orderId));
		FranchiseOrderEntity franchiseOrder = getFranchiseOrder(orderId);
		if ((franchiseOrder.getStatus() != FranchiseOrderStatus.ORDER_BILLED && franchiseOrder.getStatus() != FranchiseOrderStatus.ORDER_DELIVERED) || franchiseOrder.getInvoice() != null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Order is in status : %s or invoiced so can not be re-billed", franchiseOrder.getStatus()), "order"));
		}
		validateRebillTime();
		franchiseOrder = franchiseOrderService.rebillFranchiseOrder(request, franchiseOrder);
		FranchiseOrderResponseBean response = getMapper().mapSrcToDest(franchiseOrder, FranchiseOrderResponseBean.newInstance());
		return ResponseEntity.ok(response);
	}

	private void validateRebillTime() {
		String rebillWindowCloseTime = ParamsUtils.getParam("REBILL_WINDOW_CLOSE_TIME", "10:00:00");
		LocalTime time = LocalTime.now();
		if (time.isAfter(LocalTime.parse(rebillWindowCloseTime))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Order can not be re-billed after 10 AM"), "order"));
		}
	}

	@ApiOperation(value = "Get current franchise order id for store", nickname = "getCurrentFranchiseOrderId")
	@GetMapping("/orders/franchise/store/current")
	public ResponseEntity<UUID> getCurrentFranchiseOrderId() {
		final String storeId = SessionUtils.getStoreId();
		// get delivered morning slot orders for date based on cut off time
		return ResponseEntity.ok(franchiseOrderService.getCurrentOrderForTicket(
				List.of(FranchiseOrderStatus.ORDER_BILLED, FranchiseOrderStatus.PARTIALLY_DISPATCHED, FranchiseOrderStatus.ORDER_DELIVERED), storeId,
				deliveryDateUtils.getDeliveryDateForCurrentOrder(), Collections.singletonList(DeliverySlot.MORNING_7_AM.toString())));
	}

	@ApiOperation(value = "Get Store Order details to create ticket", nickname = "getStoreOrdersForTicket")
	@GetMapping("/orders/franchise/store/{id}/orders")
	public ResponseEntity<List<FranchiseOrderListBean>> getStoreOrdersForTicket(@PathVariable String id) {
		_LOGGER.debug(String.format("GET STORE PREVIOUS ORDERS : storeId = %s", id));
		java.sql.Date deliveryDate;
		if (OrderConstants.PARTNER_APP_APP_ID.equals(SessionUtils.getAppId())) {
			deliveryDate = deliveryDateUtils.getDeliveryDateForCurrentOrder();
		} else {
			deliveryDate = deliveryDateUtils.getDeliveryDateForPreviousOrder(ParamsUtils.getIntegerParam("FRANCHISE_ORDERS_TICKET_LIST_DAYS", 2) - 1);
		}
		List<FranchiseOrderEntity> orders = franchiseOrderService.getOrdersForTickets(
				List.of(FranchiseOrderStatus.ORDER_BILLED, FranchiseOrderStatus.PARTIALLY_DISPATCHED, FranchiseOrderStatus.ORDER_DELIVERED), id, deliveryDate,
				Collections.singletonList(DeliverySlot.MORNING_7_AM.toString()));
		if (CollectionUtils.isEmpty(orders)) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		List<FranchiseOrderListBean> orderListResponse = getMapper().mapAsList(orders, FranchiseOrderListBean.class);
		return ResponseEntity.ok(orderListResponse);
	}

	@ApiOperation(value = "Ims Franchise Refund Order", nickname = "imsFranchiseRefundOrder")
	@PostMapping("/orders/franchise/ims/refund")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseOrderResponseBean> imsProcessFranchiseRefundOrder(@Valid @RequestBody ImsFranchiseOrderRefundBean request,
			@RequestParam(required = false) String key) {
		_LOGGER.info(String.format("REFUND ORDER FROM IMS parentOrderId : %s, request was : %s", request.getParentOrderId(), request));
		FranchiseOrderEntity parentOrder = getParentOrder(request.getParentOrderId());
		FranchiseOrderEntity refundOrder = getRefundOrderByKey(key);
		if (refundOrder == null) {
			refundOrder = franchiseOrderService.createRefundOrderIms(parentOrder, request, key);
			franchiseOrderService.deductMoneyFromStoreWallet(refundOrder.getStoreId(), refundOrder.getFinalBillAmount(), parentOrder.getDisplayOrderId(),
					FranchiseOrderConstants.IMS_REFUND_ORDER_TXN_TYPE, null,
					String.format("Ticket id : %s and Ticket item id : %s", request.getTicketId(), request.getTicketItemId()), key);
			Map<String, Object> eventData = buildOrderRefundClevertapEventData(refundOrder);
			clientService.sendSingleClevertapEvent("Order Refund", refundOrder.getCustomerId(), eventData);
			try {
				generateInvoice(parentOrder, refundOrder);
			} catch (Exception e) {
				_LOGGER.error("Error while in trying to generate invoice for the first time", e);
				generateInvoice(parentOrder, refundOrder);
			}
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, FranchiseOrderResponseBean.newInstance()));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void generateInvoice(FranchiseOrderEntity parentOrder, FranchiseOrderEntity refundOrder) {
		if (parentOrder.getInvoice() != null) {
			InvoiceEntity invoice = invoiceService.generateRefundCreditNote(parentOrder, refundOrder);
			PaymentNoteData noteData = getLastNote(invoice);
			refundOrder.getOrderItems().forEach((item -> item.getRefundDetails().setPaymentNoteName(noteData.getName())));
			franchiseOrderService.saveFranchiseEntity(refundOrder);
		}
	}

	private Map<String, Object> buildOrderRefundClevertapEventData(FranchiseOrderEntity order) {
		try {
			Map<String, Object> clevertapEventData = new HashMap<>();
			clevertapEventData.put("refund_order_id", order.getId());
			clevertapEventData.put("amount", order.getFinalBillAmount());
			clevertapEventData.put("products", order.getOrderItems().stream().map(FranchiseOrderItemEntity::getProductName).collect(Collectors.joining(", ")));
			return clevertapEventData;
		} catch (Exception e) {
			_LOGGER.error("Error while creating order refund clevertap event ", e);
		}
		return null;
	}

	@ApiOperation(value = "Ims Franchise Refund All Order", nickname = "imsFranchiseRefundAllOrder")
	@PostMapping("/orders/franchise/ims/refund/all")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<FranchiseOrderResponseBean> imsFranchiseRefundAllOrder(@Valid @RequestBody ImsFranchiseOrderRefundAllBean request,
			@RequestParam(required = false) String key) {
		_LOGGER.info(String.format("REFUND ALL ORDER FROM IMS parentOrderId : %s, request was : %s", request.getParentOrderId(), request));
		FranchiseOrderEntity parentOrder = getParentOrder(request.getParentOrderId());
		FranchiseOrderEntity refundOrder = getRefundOrderByKey(key);
		if (refundOrder == null) {
			refundOrder = franchiseOrderService.createRefundAllOrderIms(parentOrder, request, key);
			franchiseOrderService.deductMoneyFromStoreWallet(refundOrder.getStoreId(), refundOrder.getFinalBillAmount(), parentOrder.getDisplayOrderId(),
					FranchiseOrderConstants.IMS_REFUND_ALL_ORDER_TXN_TYPE, null,
					String.format("Ticket id : %s and Ticket item id : %s", request.getTicketId(), request.getTicketItemId()), key);
			Map<String, Object> eventData = buildOrderRefundClevertapEventData(refundOrder);
			clientService.sendSingleClevertapEvent("Order Refund", refundOrder.getCustomerId(), eventData);
			try {
				generateInvoice(parentOrder, refundOrder);
			} catch (Exception e) {
				_LOGGER.error("Error while in trying to generate invoice for the first time", e);
				generateInvoice(parentOrder, refundOrder);
			}
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, FranchiseOrderResponseBean.newInstance()));
	}

	@ApiOperation(value = "fetch store wise order count and revenue", nickname = "fetchStoreWiseOrderCountAndRevenue")
	@GetMapping("/orders/franchise/store/revenue")
	public List<StoreRevenueBean> fetchStoreWiseOrderCountAndRevenue(@RequestParam("startDate") String startDate,
			@RequestParam(required = false) List<String> storeIds) {
		try {
			Date toDate = DateUtils.getDate(startDate);
			return franchiseOrderService.fetchStoreRevenue(toDate, storeIds);
		} catch (Exception e) {
			throw new ServerException(ErrorBean.withError(Errors.SERVER_EXCEPTION, "Some error occurred while fetching store wise revenue"));
		}
	}

	@ApiOperation(value = "fetch store wise order count and revenue", nickname = "fetchStoreWiseOrderCountAndRevenueV2")
	@PostMapping("/orders/franchise/store/revenue/v2")
	public List<StoreRevenueBean> fetchStoreWiseOrderCountAndRevenueV2(@Valid @RequestBody OrderRevenueRequest request) {
		try {
			Date toDate = DateUtils.getDate(request.getStartDate());
			return franchiseOrderService.fetchStoreRevenue(toDate, request.getStoreIds());
		} catch (Exception e) {
			throw new ServerException(ErrorBean.withError(Errors.SERVER_EXCEPTION, "Some error occurred while fetching store wise revenue"));
		}
	}

	@ApiOperation(value = "fetch store wise order count", nickname = "fetchStoreWiseSkuDetails")
	@GetMapping("/orders/franchise/store/sku/count")
	public List<StoreOrderDetails> fetchStoreWiseSkuDetails(@RequestParam("deliveryDate") String deliveryDate, @RequestParam("slot") String slot,
			@RequestParam("storeIds") List<String> storeIds) {
		return franchiseOrderService.fetchStoreWiseSkuDetails(storeIds, DateUtils.getDate(deliveryDate), slot);
	}

	private FranchiseOrderEntity getParentOrder(UUID parentOrderId) {
		FranchiseOrderEntity parentOrder = getFranchiseOrder(parentOrderId);
		if (!(parentOrder.getStatus().equals(FranchiseOrderStatus.ORDER_DELIVERED) || parentOrder.getStatus()
				.equals(FranchiseOrderStatus.ORDER_BILLED) || parentOrder.getStatus().equals(FranchiseOrderStatus.PARTIALLY_DISPATCHED))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return parentOrder;
	}

	private FranchiseOrderEntity getRefundOrderByKey(String key) {
		return key != null ? franchiseOrderService.findByKey(key) : null;
	}

	@ApiOperation(value = "update cart level pricing", nickname = "updateCartLevelPricing")
	@GetMapping("/orders/franchise/cart/pricing-update")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public void resetCartPricing() {
		List<FranchiseOrderEntity> cartsList = franchiseOrderService.findAllActiveCart();
		if (cartsList != null) {
			for (FranchiseOrderEntity cart : cartsList) {
				Set<String> skuCodesSet = cart.getOrderItems().stream().map(FranchiseOrderItemEntity::getSkuCode).collect(Collectors.toSet());
				Map<String, FranchiseStoreInventoryResponse> skuMap = clientService.getStoreSkuInventoryForBulkRequest(skuCodesSet, cart.getStoreId()).stream()
						.collect(Collectors.toMap(FranchiseStoreInventoryResponse::getSkuCode, bean -> bean));
				franchiseOrderService.repriceCart(skuMap, cart);
				franchiseOrderService.updateCart(cart);
			}
			if (!cartsList.isEmpty()) {
				franchiseOrderService.saveAllOrder(cartsList);
			}
		}
	}

	private Double calculateRefundAmountFromRefundOrdersWithoutBulkSkus(FranchiseOrderEntity order,
			Map<UUID, List<FranchiseOrderEntity>> confirmedRefundOrdersMap) {
		Map<String, FranchiseOrderItemEntity> cartItemSkuMap = new HashMap<>();
		if (order.getOrderItems() != null) {
			cartItemSkuMap = order.getOrderItems().stream().collect(Collectors.toMap(FranchiseOrderItemEntity::getSkuCode, Function.identity()));
		}
		List<FranchiseOrderEntity> confirmedRefunds = confirmedRefundOrdersMap.get(order.getId());
		Map<String, Integer> bulkSkuCodesSet = franchiseOrderService.getBulkSkuCodesSet();
		Double totalRefundAmount = 0d;
		if (confirmedRefunds != null) {
			for (FranchiseOrderEntity refundOrder : confirmedRefunds) {
				for (FranchiseOrderItemEntity refundItem : refundOrder.getOrderItems()) {
					FranchiseOrderItemEntity parentOrderItem = cartItemSkuMap.get(refundItem.getSkuCode());
					if (bulkSkuCodesSet.containsKey(parentOrderItem.getSkuCode()) && parentOrderItem.getFinalCrateQty() >= bulkSkuCodesSet.get(
							parentOrderItem.getSkuCode())) {
						continue;
					} else {
						totalRefundAmount += refundItem.getFinalAmount();
					}
				}
			}
		}
		return totalRefundAmount;
	}

	private void fillRefundMaps(Set<UUID> pendingRefundSet, Map<UUID, List<FranchiseOrderEntity>> confirmedRefundOrdersMap,
			List<FranchiseOrderEntity> refundOrders) {
		for (FranchiseOrderEntity refundOrder : refundOrders) {
			if (refundOrder.getStatus() == FranchiseOrderStatus.ORDER_REFUNDED) {
				if (confirmedRefundOrdersMap.containsKey(refundOrder.getParentOrderId())) {
					confirmedRefundOrdersMap.get(refundOrder.getParentOrderId()).add(refundOrder);
				} else {
					List<FranchiseOrderEntity> confirmedRefundList = new ArrayList<>();
					confirmedRefundList.add(refundOrder);
					confirmedRefundOrdersMap.put(refundOrder.getParentOrderId(), confirmedRefundList);
				}
			} else if (refundOrder.getStatus() == FranchiseOrderStatus.REFUND_REQUESTED) {
				pendingRefundSet.add(refundOrder.getParentOrderId());
			}
		}
	}

	@ApiOperation(value = "process orders cashback", nickname = "processOrdersCashback")
	@PostMapping("/orders/franchise/internal/effective-bill-amount")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<List<FranchiseOrderResponseBean>> getOrdersWithEffectiveBillAmount(
			@Valid @RequestBody FranchiseOrdersWithEffectivePriceRequest request) {
		Set<String> storeIds = new HashSet<>(request.getStoreIds());
		java.sql.Date date = request.getDate();
		List<FranchiseOrderEntity> franchiseOrders = franchiseOrderService.findStoresOrderOnDate(storeIds, date, null);
		List<UUID> parentOrderIds = franchiseOrders.stream().map(FranchiseOrderEntity::getId).collect(Collectors.toList());
		List<FranchiseOrderEntity> refundOrders = new ArrayList<>();
		Set<UUID> pendingRefundSet = new HashSet<>();
		if (parentOrderIds.size() > 0) {
			refundOrders = franchiseOrderService.findAllRefundOrdersByParentIds(parentOrderIds);
			pendingRefundSet = clientService.fetchPendingTicketOrderIds(parentOrderIds);
		}
		Map<UUID, List<FranchiseOrderEntity>> confirmedRefundOrdersMap = new HashMap<>();
		fillRefundMaps(pendingRefundSet, confirmedRefundOrdersMap, refundOrders);
		for (FranchiseOrderEntity order : franchiseOrders) {
			franchiseOrderService.addSpGrossAmountWithoutBulkSkus(order);
			Double totalRefundAmountWithoutBulkSkus = calculateRefundAmountFromRefundOrdersWithoutBulkSkus(order, confirmedRefundOrdersMap);
			order.setEffectiveSpGrossAmountForCashback(order.getEffectiveSpGrossAmountForCashback() - totalRefundAmountWithoutBulkSkus);
			order.setHasPendingRefundTicket(pendingRefundSet.contains(order.getId()));
		}
		List<FranchiseOrderResponseBean> orderListResponse = getMapper().mapAsList(franchiseOrders, FranchiseOrderResponseBean.class);
		return ResponseEntity.ok(orderListResponse);
	}

	@ApiOperation(value = "cancel franchise order post billing", nickname = "cancelFranchiseOrderPostBilling")
	@PostMapping("/orders/franchise/{orderId}/cancel/post-delivery")
	@ResponseStatus(HttpStatus.OK)
	public FranchiseOrderResponseBean cancelFranchiseOrderPostBilling(@PathVariable UUID orderId, @RequestBody FranchiseOrderCancelPostBillingRequest request,
			@RequestParam String key) {
		_LOGGER.info(String.format("CancelFranchiseOrder:: orderId: %s", orderId));
		FranchiseOrderEntity order = getFranchiseOrder(orderId);
		if (!(order.getStatus().equals(FranchiseOrderStatus.ORDER_BILLED) || order.getStatus().equals(FranchiseOrderStatus.ORDER_DELIVERED) || order.getStatus()
				.equals(FranchiseOrderStatus.PARTIALLY_DISPATCHED))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Franchise order Status : %s, can only be cancelled post billing ", order.getStatus()), orderId.toString()));
		}
		InvoiceEntity invoice = order.getInvoice();
		if (invoice != null && invoice.getInvoiceUrl() != null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Invoice has been generated for the order, so it can not be cancelled", order.getStatus()), orderId.toString()));
		}
		List<FranchiseOrderEntity> refundOrders = franchiseOrderService.findAllRefundOrdersByParentIds(Collections.singletonList(orderId));
		refundOrders = refundOrders.stream().filter(e -> !e.getStatus().equals(FranchiseOrderStatus.CANCELLED)).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(refundOrders)) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("There are active refund orders on this order"), orderId.toString()));
		}
		franchiseOrderService.cancelFranchiseOrderPostBilling(order, request, key);
		return getMapper().mapSrcToDest(order, FranchiseOrderResponseBean.newInstance());
	}

	private FranchiseOrderEntity getFranchiseOrder(UUID orderId) {
		FranchiseOrderEntity franchiseOrder = franchiseOrderService.findRecordById(orderId);
		if (Objects.isNull(franchiseOrder)) {
			_LOGGER.error(String.format("getFranchiseOrder: No order Found orderId = %s ", orderId));
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return franchiseOrder;
	}

	@ApiOperation(value = "Franchise-store order metadata update", nickname = "franchiseOrderMetadataUpdate")
	@PostMapping("/orders/franchise/metadata")
	@ResponseStatus(HttpStatus.OK)
	public void franchiseOrderMetadataUpdate(@RequestBody FranchiseOrderMetadataUpdateRequest request) {
		_LOGGER.info(String.format("franchiseOrderMetadataUpdate request : %s", request));
		FranchiseOrderEntity franchiseOrder = getFranchiseOrder(request.getOrderId());
		franchiseOrderService.updateFranchiseOrderMetadata(franchiseOrder, request);
		franchiseOrderService.saveFranchiseEntity(franchiseOrder);
	}

	@ApiOperation(value = " fetch Franchise-store orders for store ids", nickname = "fetchAllStoreOrdersCount")
	@PostMapping("/orders/franchise/orders")
	@ResponseStatus(HttpStatus.OK)
	public List<StoreOrderInfo> fetchAllStoreOrdersCount(@RequestBody StoreOrderInfoRequest request) {
		List<StoreOrderInfo> storeOrderInfo = franchiseOrderService.findActiveOrderCountByStoreId(request.getStoreIds());
		return storeOrderInfo;
	}

	@ApiOperation(value = "Auto remove items from Cart for eligible franchise stores", nickname = "autoRemovalCartFranchiseStore")
	@PostMapping("/orders/franchise/cart/auto-removal")
	@Transactional(propagation = Propagation.REQUIRED)
	@ResponseStatus(HttpStatus.OK)
	public void autoRemovalCartFranchiseStore() {
		Map<String, Integer> whSkuActualQtyMap = getWhSkuActualQtyMap();
		List<FranchiseOrderEntity> allCarts = franchiseOrderService.findAllActiveCart();
		Map<UUID, FranchiseOrderEntity> orderIdCartMap = allCarts.stream().collect(Collectors.toMap(FranchiseOrderEntity::getId, Function.identity()));
		List<StoreSecPickingViewBean> secondaryPickingItems = clientService.getStoreSecPickingView();
		Map<String, StoreSecPickingViewBean> storeSecPickingItemsMap = secondaryPickingItems.stream()
				.collect(Collectors.toMap(e -> e.getStoreId().toString() + "|" + e.getSkuCode(), Function.identity()));
		List<FranchiseOrderItemEntity> allValidCartItems = allCarts.stream().flatMap(cart -> cart.getOrderItems().stream()
				.filter(e -> e.getWhId() != 1 || !storeSecPickingItemsMap.containsKey(cart.getStoreId() + "|" + e.getSkuCode()))).collect(Collectors.toList());
		Map<String, Integer> whSkuInCartQtyMap = getWhSkuInCartQtyMap(allValidCartItems);
		Map<String, Integer> whSkuExtraQtyMap = getWhSkuExtraQtyMap(whSkuActualQtyMap, whSkuInCartQtyMap);
		Set<UUID> priorityRemovalCartIds = allCarts.stream().filter(cart -> cart.getExtraFeeDetails().getDeliveryCharge().compareTo(0d) > 0)
				.map(FranchiseOrderEntity::getId).collect(Collectors.toSet());
		franchiseOrderService.autoRemoveEligibleCartItems(whSkuExtraQtyMap, allValidCartItems, orderIdCartMap, priorityRemovalCartIds);
	}

	private Map<String, Integer> getWhSkuActualQtyMap() {
		List<FranchiseStoreInventoryResponse> allWhSkuInventoryList = clientService.getFranchiseStoreInventoryForAllSkus();
		return allWhSkuInventoryList.stream()
				.collect(Collectors.toMap(item -> item.getWhId() + "|" + item.getSkuCode(), item -> item.getActualQty() != null ? item.getActualQty() : 0));
	}

	private Map<String, Integer> getWhSkuInCartQtyMap(List<FranchiseOrderItemEntity> allCartItems) {
		Map<String, Integer> resultMap = new HashMap<>();
		for (FranchiseOrderItemEntity item : allCartItems) {
			String key = item.getWhId() + "|" + item.getSkuCode();
			resultMap.merge(key, item.getFinalCrateQty(), Integer::sum);
		}
		return resultMap;
	}

	private Map<String, Integer> getWhSkuExtraQtyMap(Map<String, Integer> whSkuActualQtyMap, Map<String, Integer> whSkuInCartQtyMap) {
		Map<String, Integer> resultMap = new HashMap<>();
		for (Map.Entry<String, Integer> entry : whSkuInCartQtyMap.entrySet()) {
			String key = entry.getKey();
			Integer cartQty = entry.getValue();
			Integer actualQty = whSkuActualQtyMap.getOrDefault(key, 0);
			if (cartQty > actualQty) {
				resultMap.put(key, cartQty - actualQty);
			}
		}
		return resultMap;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}