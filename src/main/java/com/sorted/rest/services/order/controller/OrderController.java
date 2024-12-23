package com.sorted.rest.services.order.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.constants.Operation;
import com.sorted.rest.common.dbsupport.pagination.FilterCriteria;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.*;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CSVBulkRequest;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.StoreProductInventory;
import com.sorted.rest.services.order.beans.StoreInventoryUpdateRequest.StoreInventoryUpdateData;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.*;
import com.sorted.rest.services.order.entity.*;
import com.sorted.rest.services.order.services.*;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import com.sorted.rest.services.order.utils.PayablePDFGenerator;
import com.sorted.rest.services.params.service.ParamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sorted.rest.services.common.upload.csv.CsvUtils.cancelUpload;

/**
 * Created by mohit on 19.6.20.
 */
@RestController
@Api(tags = "Order Services", description = "Manage Order related services.")
public class OrderController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(OrderController.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderEventLogService orderEventLogService;

	@Autowired
	private CartService cartService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private ClientService clientService;

	@Autowired
	private DisplayOrderIdService displayOrderIdService;

	@Autowired
	private ParamService paramService;

	@Autowired
	private DeliveryDateUtils deliveryDateUtils;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private PreBookOrderService preBookOrderService;

	private void setMessages(OrderResponseBean response) {
//		Integer additionalBufferTime = ParamsUtils.getIntegerParam("ADDITIONAL_BUFFER_TIME", 0);
//		String checkoutMessage = ParamsUtils.getParam("CHECKOUT_MESSAGE", "Your order will auto checkout at 12:00 midnight.");
//		String etaMessage = ParamsUtils.getParam("ETA_MESSAGE", "before 9am");
//		if (additionalBufferTime > 0) {
//			response.setBufferETAMessage(
//					String.format("Due to high demand, there will be an additional time of %s mins required to deliver your order.", additionalBufferTime));
//		}
//		if (checkoutMessage != null) {
//			response.setCheckoutMessage(checkoutMessage);
//		}
//		if (etaMessage != null) {
//			response.setEtaMessage(etaMessage);
//		}
		String deliveryFeeMessage = ParamsUtils.getParam("ORDER_DELIVERY_FEE_MESSAGE");
		if (StringUtils.isNotEmpty(deliveryFeeMessage)) {
			response.setDeliveryFeeMessage(String.format(deliveryFeeMessage, orderService.getConsumerDeliveryCharges().getChargeLimit().intValue(),
					orderService.getConsumerDeliveryCharges().getChargeUpperLimit().intValue()));
		}
	}

	private void validateOrderSlot(OrderSlotEntity orderSlot) {
		if (orderSlot == null) {
			throw new ValidationException(ErrorBean.withError("slot_not_found", "slot not found.", "slot"));
		}
		if (orderSlot.getRemainingCount() <= 0) {
			throw new ValidationException(ErrorBean.withError("slot_not_available", "slot cannot serve more orders.", "slot"));
		}
	}

	private Location getAddressLocation(Long consumerAddressId) {
		Location addressLocation = null;
		if (consumerAddressId != null) {
			ConsumerAddressResponse consumerAddressResponse = clientService.getConsumerAddressById(consumerAddressId);
			_LOGGER.info(String.format("getAddressLocation :: ConsumerAddressResponse for id: %s is: %s", consumerAddressId, consumerAddressResponse));
			if (consumerAddressResponse != null) {
				addressLocation = new Location();
				addressLocation.setLongitude(String.valueOf(consumerAddressResponse.getLongitude()));
				addressLocation.setLatitude(String.valueOf(consumerAddressResponse.getLatitude()));
			}
		}
		return addressLocation;
	}

	private StoreInventoryUpdateRequest createStoreInventoryUpdateRequest(OrderEntity cart) {
		List<StoreInventoryUpdateData> data = cart.getOrderItems().stream().map(i -> new StoreInventoryUpdateData(i.getFinalQuantity(), i.getSkuCode()))
				.collect(Collectors.toList());
		StoreInventoryUpdateRequest storeInvRequest = new StoreInventoryUpdateRequest(cart.getCustomerId(), data);
		return storeInvRequest;
	}

	private void validateSubmitCart(OrderEntity cart, @Valid PlaceOrderRequest request, String appId) {
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		if (!isPosRequest(appId)) {
			Long addressId = cart.getDeliveryAddress();
			if (request.getShippingMethod().equals(ShippingMethod.HOME_DELIVERY) && addressId == null) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Delivery Address not set for Home Delivery"));
			}
			validateAddress(cart, addressId);
		}
	}

	private void validateAddress(OrderEntity cart, @Valid Long addressId) {

		if (addressId != null) {
			ConsumerAddressResponse address = clientService.getConsumerAddressById(addressId);
			if (!cart.getCustomerId().equals(address.getUserId())) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Address does not match to the user ordering."));
			}
			if (address.getSocietyId() == null) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Address is not linked to a society."));
			}
		}
	}

	private void addInventoryErrors(OrderEntity cart, StoreInventoryUpdateResponse inventoryUpdateResponse) {
		Map<String, com.sorted.rest.services.order.beans.StoreInventoryUpdateResponse.Error> errorMap = inventoryUpdateResponse.getErrors().stream()
				.collect(Collectors.toMap(e -> e.getSkuCode(), e -> e));
		boolean isCartEmpty = true;
		List<OrderItemEntity> removeItems = new ArrayList<>();
		for (OrderItemEntity item : cart.getOrderItems()) {
			if (errorMap.containsKey(item.getSkuCode())) {
				if (errorMap.get(item.getSkuCode()).getMaxQuantity() == null || Double.compare(errorMap.get(item.getSkuCode()).getMaxQuantity(), 0.0) <= 0) {
					_LOGGER.info(String.format("addInventoryErrors :: removing item: %s", item.getSkuCode()));
					item.setActive(0);
					item.setFinalQuantity(0d);
					ErrorBean error = new ErrorBean(errorMap.get(item.getSkuCode()).getCode(), errorMap.get(item.getSkuCode()).getCode());
					item.setError(error);
					removeItems.add(item);
				} else {
					_LOGGER.info(String.format("addInventoryErrors :: reducing quantity of item: %s from %s to %s", item.getSkuCode(), item.getFinalQuantity(),
							errorMap.get(item.getSkuCode()).getMaxQuantity()));
					item.setFinalQuantity(errorMap.get(item.getSkuCode()).getMaxQuantity());
					ErrorBean error = new ErrorBean(errorMap.get(item.getSkuCode()).getCode(),
							String.format("Requested Quantity not available. Only %s available", errorMap.get(item.getSkuCode()).getMaxQuantity()));
					item.setError(error);
					isCartEmpty = false;
				}
			} else {
				isCartEmpty = false;
			}
		}
		cart.getOrderItems().removeAll(removeItems);
		if (isCartEmpty) {
			_LOGGER.info("addInventoryErrors :: cart is empty");
			cart.setActive(0);
		}
	}

	private void sendOrderToLithos(OrderEntity order) {
		LithosOrderBean lithosOrder = getMapper().mapSrcToDest(order, LithosOrderBean.newInstance());
		if (clientService.sendOrderToLithos(lithosOrder)) {
			_LOGGER.info("Order Pushed to Lithos");
			order.setLithosSynced(1);
			orderService.save(order);
		} else {
			_LOGGER.error("Error Pushing Order to Lithos");
		}
	}

	private boolean validateStoreInventory(OrderEntity cart) {
		String storeId = cart.getStoreId();
		List<String> skuCodes = cart.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList());
		_LOGGER.info(String.format("validateStoreInventory :: skuCodes: %s", skuCodes));
		StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(storeId, String.join(",", skuCodes), null);
		if (storeItemResponse != null) {
			cartService.doStoreChangeAction(cart, storeItemResponse.getInventory());
			return true;
		}
		return false;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	@ApiOperation(value = "Apply offer to Cart ", nickname = "applyOffer")
	@PostMapping("/orders/cart/apply-offer")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<CartResponse> applyOffer(@Valid @RequestBody ApplyOfferRequest request) {
		_LOGGER.info(String.format("APPLY OFFER REQUEST : %s", request));
		String appId = SessionUtils.getAppId();
		UUID customerId = orderService.getUserId();
		if (isPosRequest(appId)) {
			cartService.customerIdCheck(request.getCustomerId());
			customerId = request.getCustomerId();
		}
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate());
		OrderEntity cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		if (request.getVoucherCode() != null) {
			cart.getOfferData().setVoucherCode(request.getVoucherCode());
			cart.getOfferData().setIsOfferApplied(false);
			cart.getOfferData().setOrderDiscount(null);
		} else if (request.getDiscountAmount() != null) {
			removeOfferFromCart(cart);
			cart.getOfferData().setOrderDiscount(request.getDiscountAmount());
		} else {
			throw new ValidationException(ErrorBean.withError("no_discount", "No discount type provided", "cart"));
		}
		orderService.updateOrder(cart);
		orderService.save(cart);

		CartResponse response = new CartResponse();
		response.setData(buildCartResponse(cart, null));
		if (cart.getError() != null) {
			if (Objects.equals(cart.getError().getCode(), "OFFER_ERROR") && cart.getError().getMessage() != null) {
				response.setMessage(cart.getError().getMessage());
			}
		}
		return ResponseEntity.ok(response);
	}

	private void removeOfferFromCart(OrderEntity cart) {
		cart.getOfferData().setVoucherCode(null);
		cart.getOfferData().setIsOfferApplied(false);
		cart.getOfferData().setOfferId(null);
		cart.getOfferData().setAmount(null);
		cart.getOfferData().setOrderDiscount(null);
	}

	@ApiOperation(value = "Remove Offer from Cart ", nickname = "removeOffer")
	@PostMapping("/orders/cart/remove-offer")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<CartResponse> removeOffer(@Valid @RequestBody RemoveOfferRequest request) {
		String appId = SessionUtils.getAppId();
		UUID customerId = orderService.getUserId();
		if (isPosRequest(appId)) {
			cartService.customerIdCheck(request.getCustomerId());
			customerId = request.getCustomerId();
		}
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate());
		OrderEntity cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		removeOfferFromCart(cart);
		orderService.updateOrder(cart);
		orderService.save(cart);
		CartResponse response = new CartResponse();
		response.setData(buildCartResponse(cart, null));
		return ResponseEntity.ok(response);
	}

	/**
	 * Find one.
	 */
	@ApiOperation(value = "Get one Order with provided id.", nickname = "findOneOrder")
	@GetMapping("/orders/{id}")
	public ResponseEntity<OrderDetailsResponseBean> findOne(@PathVariable UUID id) {
		final UUID customerId = orderService.getUserId();
		OrderEntity order = orderService.findById(id);
		if (order == null || order.getStatus() == OrderStatus.IN_CART || !order.getCustomerId().equals(customerId)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(order.getStoreId());
		List<OrderEntity> refundOrders = orderService.findRefundOrdersFromParentId(id);
		OrderDetailsResponseBean response = buildOrderResponse(order);
		if (refundOrders != null) {
			response.setRefundOrders(getMapper().mapAsList(refundOrders, OrderResponseBean.class));
		}
		if (storeDataResponse != null) {
			response.setStoreName(storeDataResponse.getName());
		}
		if (isToday(order.getDeliveryDate()) == 0 && isTicketableTime()) {
			response.setPageAction(OrderPageAction.HELP);
		} else if (isToday(order.getDeliveryDate()) < 0) {
			response.setPageAction(OrderPageAction.FEEDBACK);
		}
		return ResponseEntity.ok(response);
	}

	private int isToday(Date orderDeliveryDate) {
		LocalDate currentTime = LocalDate.now();
		if (orderDeliveryDate == null) {
			return 2;
		}
		LocalDate deliveryDate = orderDeliveryDate.toInstant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
		return deliveryDate.compareTo(currentTime);
	}

	@ApiOperation(value = "Get one Order with provided id on support page.", nickname = "findOneOrderSupport")
	@GetMapping("/orders/support")
	@ResponseStatus(HttpStatus.OK)
	public OrderDetailsResponseBean findOneOrderSupport(@RequestParam(value = "channel", required = false) String channel,
			@RequestParam(value = "customerId", required = false) UUID customerId) {
		if (!isTicketableTime()) {
			_LOGGER.info("Can not raise ticket during this time");
			return null;
		}
		validateCustomerId(channel, customerId);
		UUID userId = SessionUtils.getAuthUserId();
		if (channel != null && channel.equalsIgnoreCase(Channel.DELIVERY_APP.toString())) {
			userId = customerId;
		}
		return findCustomerOrder(userId);
	}

	private OrderDetailsResponseBean findCustomerOrder(UUID userId) {
		OrderEntity order = orderService.findLatestOrder(userId);
		if (order == null || !isTicketableTime()) {
			_LOGGER.info("No Order Found");
			return null;
		}
		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(order.getStoreId());
		List<OrderEntity> refundOrders = orderService.findRefundOrdersFromParentId(order.getId());
		TicketBean ticketInfo = clientService.fetchTicketByReferenceId(order.getId().toString());
		OrderDetailsResponseBean response = buildOrderResponse(order);
		if (refundOrders != null) {
			response.setRefundOrders(getMapper().mapAsList(refundOrders, OrderResponseBean.class));
		}
		response.setTicketInfo(ticketInfo);
		if (storeDataResponse != null) {
			response.setStoreName(storeDataResponse.getName());
		}
		return response;
	}

	private void validateCustomerId(String channel, UUID customerId) {
		if (StringUtils.isNotEmpty(channel) && channel.equalsIgnoreCase(Channel.DELIVERY_APP.toString()) && customerId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Customer id is required", "customerId"));
		}
	}

	private boolean isTicketableTime() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String ticketBlockStartTime = ParamsUtils.getParam("CONSUMER_ORDER_TICKET_END_TIME", "15:00:00");
		return localTime.isBefore(LocalTime.parse(ticketBlockStartTime));
	}

	@ApiOperation(value = "Get one Order with provided id on support page.", nickname = "findOneOrderSupport")
	@PostMapping("/orders/support/internal")
	@ResponseStatus(HttpStatus.OK)
	public List<OrderDetailsResponseBean> findCurrentDayOrdersByCustomerIds(@Valid @RequestBody FetchBulkSupportOrdersRequest request) {
		if (!isTicketableTime()) {
			_LOGGER.info("Can not raise ticket during this time");
			return null;
		}
		List<OrderDetailsResponseBean> response = new ArrayList<>();
		for (String customerId : request.getUserIds()) {
			UUID userId = UUID.fromString(customerId);
			OrderDetailsResponseBean order = findCustomerOrder(userId);
			if (order != null) {
				response.add(order);
			}
		}
		return response;
	}

	/**
	 * Find one.
	 */
	@ApiOperation(value = "Get one Order ForInternal with provided id.", nickname = "findOneOrderForInternal")
	@GetMapping("/orders/{id}/internal")
	public ResponseEntity<OrderResponseBean> findOneForInternal(@PathVariable UUID id) {
		OrderEntity entity = orderService.findById(id);
		if (entity == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(entity, OrderResponseBean.newInstance()));
	}

	private OrderDetailsResponseBean buildOrderResponse(OrderEntity order) {
		cartService.sortOrderItems(order);
		OrderDetailsResponseBean response = getMapper().mapSrcToDest(order, OrderDetailsResponseBean.newInstance());
		response.setOrderCharges(orderService.setOrderCharges(order));
		setCashbackDetails(order, response);
		return response;
	}

	/**
	 * Find all for customer.
	 */
	@ApiOperation(value = "List all customer Orders", nickname = "findCustomersOrderList")
	@GetMapping("/orders/customer")
	public ResponseEntity<List<OrderListWithItemsBean>> findCustomerAllOrders() {
		final UUID customerId = orderService.getUserId();
		final List<OrderEntity> results = orderService.findCustomerOrderList(customerId);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<OrderListWithItemsBean>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, OrderListWithItemsBean.class));
	}

	private void sendPaymentUpdate(PaymentNotifyBean payment) {
		_LOGGER.info(String.format("sendLithosPaymentUpdate:: sending Lithos Payment Update: %s", payment));
		clientService.sendPaymentUpdate(payment);
	}

	private StoreInventoryUpdateRequest createStoreInventoryUpdateRequestBackoffice(OrderEntity cart) {
		List<StoreInventoryUpdateData> data = cart.getOrderItems().stream().map(i -> new StoreInventoryUpdateData(i.getOrderedQty(), i.getSkuCode()))
				.collect(Collectors.toList());
		StoreInventoryUpdateRequest storeInvRequest = new StoreInventoryUpdateRequest(cart.getCustomerId(), data);
		return storeInvRequest;
	}

	@ApiOperation(value = "Create Back Office Order", nickname = "createBackofficeOrder")
	@PostMapping("/orders/backoffice")
	@Transactional
	public ResponseEntity<CartResponse> createBackofficeOrder(@Valid @RequestBody BackofficeOrderBean request) {
		_LOGGER.info(String.format("CREATE ORDER FROM BACKOFFICE REQUEST : %s", request));
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate());
		OrderEntity cart = orderService.findCustomerCurrentCart(request.getCustomerId(), deliveryDate);
		OrderSlotEntity orderSlot = orderService.getOrderSlotById(request.getSlotId());
		validateOrderSlot(orderSlot);
		OrderEntity requestOrder = getMapper().mapSrcToDest(request, OrderEntity.newInstance());
		validateAddress(requestOrder, request.getDeliveryAddress());
		String storeId = SessionUtils.getStoreId();
		if (storeId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store Id not provided.", "order"));
		}
		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(storeId);
		_LOGGER.info(String.format("addToCartV2 :: storeDataResponse: %s", storeDataResponse));
		requestOrder.getMetadata().setZoneId(storeDataResponse.getZoneId());
		// orderService.setOrderEta(storeDataResponse.getDistance(), requestOrder);
		requestOrder.setStoreId(storeDataResponse.getStoreId());
		Map<String, StoreProductInventory> storeInvMap = getStoreInventory(requestOrder);
		StoreInventoryUpdateRequest storeInvRequest = createStoreInventoryUpdateRequestBackoffice(requestOrder);
		StoreInventoryUpdateResponse inventoryUpdateResponse = clientService.verifyAndDeductStoreInventory(requestOrder.getStoreId(), storeInvRequest);
		CartResponse response = new CartResponse();
		if (inventoryUpdateResponse.getSuccess()) {
			if (cart != null) {
				orderService.deactivateOrder(cart);
				orderService.releaseOrderSlot(cart.getSlotId());
			}
			requestOrder.setSlotId(requestOrder.getSlotId());
			cartService.addOrderCountToCart(requestOrder);
			requestOrder = orderService.createOrderEntityFromBackoffice(requestOrder, storeInvMap);
			orderService.reserveOrderSlot(requestOrder.getSlotId());
			// sendOrderToLithos(requestOrder);
		} else {
			addInventoryErrors(requestOrder, inventoryUpdateResponse);
			OrderResponseBean orderResponse = getMapper().mapSrcToDest(requestOrder, OrderResponseBean.newInstance());
			orderResponse.setOrderCharges(orderService.setOrderCharges(requestOrder));
			response.setData(orderResponse);
			response.error("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		OrderResponseBean orderResponse = getMapper().mapSrcToDest(requestOrder, OrderResponseBean.newInstance());
		orderResponse.setOrderCharges(orderService.setOrderCharges(requestOrder));
		response.setData(orderResponse);
		saveOrderEventLog(requestOrder, request, response);
		return ResponseEntity.ok(response);
	}

	private StoreInventoryAddOrDeductRequest createStoreInventoryAddOrDeductRequest(OrderEntity cart) {
		List<StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData> data = cart.getOrderItems().stream()
				.map(i -> new StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData((-1) * i.getFinalQuantity(), i.getSkuCode()))
				.collect(Collectors.toList());
		StoreInventoryAddOrDeductRequest storeInvRequest = new StoreInventoryAddOrDeductRequest(data);
		return storeInvRequest;
	}

	private Map<String, StoreProductInventory> getStoreInventory(OrderEntity requestOrder) {
		Map<String, StoreProductInventory> storeInvMap = null;
		try {
			List<String> skuCodes = requestOrder.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList());
			StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(requestOrder.getStoreId(), String.join(",", skuCodes), null);
			if (CollectionUtils.isNotEmpty(storeItemResponse.getInventory())) {
				storeInvMap = storeItemResponse.getInventory().stream().collect(Collectors.toMap(i -> i.getInventorySkuCode(), i -> i));
			}
		} catch (Exception e) {
			// do nothing
		}
		return storeInvMap;
	}

	Map<String, Object> getPendingOrdersParams(UUID customerId) {
		Map<String, Object> params = new HashMap<>();
		params.put("customerId", customerId);
		List<Integer> statusList = new ArrayList<>(Arrays.asList(1, 2, 4, 6, 7));
		params.put("status", statusList);
		return params;
	}

	@ApiOperation(value = "List all pending customer Orders", nickname = "findPendingCustomersOrderList")
	@GetMapping("/orders/customer/pending")
	public List<OrderListBean> findPendingCustomerAllOrders(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "5") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		sort = new LinkedHashMap<>();
		sort.put("submittedAt", PageAndSortRequest.SortDirection.DESC);
		final UUID customerId = orderService.getUserId();
		final Map<String, Object> params = getPendingOrdersParams(customerId);
		PageAndSortResult<OrderEntity> poEntityList = orderService.findOrdersByPage(pageSize, pageNo, params, sort);
		PageAndSortResult<OrderListBean> response = new PageAndSortResult<>();
		if (poEntityList != null && poEntityList.getData() != null) {
			response = prepareResponsePageData(poEntityList, OrderListBean.class);
		}
		// for (PendingOrderResponseBean resp : response.getData()) {
		// if (resp.getMetadata().getEta() != null) {
		// resp.getMetadata().getEta().setActualEta(calculateActualEta(resp.getMetadata(),
		// resp.getSubmittedAt()));
		// }
		// }
		return response.getData();
	}

	private void sendPaymentUpdateToLithos(OrderEntity cart) {
		LithosOrderBean lithosOrder = getMapper().mapSrcToDest(cart, LithosOrderBean.newInstance());
		clientService.sendPaymentUpdateToLithos(lithosOrder);
	}

	@ApiOperation(value = "Update Order Payment Status", nickname = "updateOrderPayment")
	@PostMapping("/orders/internal/payment-update")
	@ResponseStatus(code = HttpStatus.OK)
	public void updateOrderPayment(@Valid @RequestBody(required = true) OrderPaymentBean request) {
		_LOGGER.info(String.format("UPDATE ORDER PAYMENT REQUEST : %s", request));
		OrderEntity order = orderService.findById(request.getId());
		if (order != null) {
			order.setPaymentDetail(request.getPaymentDetail());
			if (order.getPaymentDetail() != null && order.getPaymentDetail().getPaymentStatus().equals(PaymentStatus.SUCCESS)) {
				order.setAmountReceived(request.getAmountReceived());
				if (!request.getPaymentDetail().getPaymentGateway().equals("LITHOS")) {
					sendPaymentUpdateToLithos(order);
				}
			}
			orderService.save(order);
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order to update", "order"));
		}
	}

	@ApiOperation(value = "List all customer Orders from Backoffice", nickname = "findCustomersOrderListBackoffice")
	@GetMapping("/orders/customer/{id}")
	public ResponseEntity<List<OrderListBean>> findCustomerAllOrdersBackoffice(@PathVariable UUID id) {
		final List<OrderEntity> results = orderService.findCustomerOrderList(id);
		if (results == null || results.isEmpty()) {
			return ResponseEntity.ok(new ArrayList<OrderListBean>());
		}
		return ResponseEntity.ok(getMapper().mapAsList(results, OrderListBean.class));
	}

	@ApiOperation(value = "Get one Order For Backoffice with provided id.", nickname = "findOneOrderForBackoffice")
	@GetMapping("/orders/{id}/backoffice")
	public ResponseEntity<OrderResponseBean> findOneForBackoffice(@PathVariable UUID id) {
		OrderEntity entity = orderService.findById(id);
		if (entity == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(entity, OrderResponseBean.newInstance()));
	}

	@ApiOperation(value = "Customer Cart Internal Call", nickname = "findCartInternal")
	@GetMapping("/orders/internal/cart/{id}")
	public ResponseEntity<CartResponse> getCartInternal(@PathVariable UUID id, @RequestHeader(required = false) String date) {
		Date deliveryDate = cartService.getDeliveryDate(DateUtils.getDate(DateUtils.SHORT_DATE_FMT, date));
		final OrderEntity cart = orderService.findCustomerCurrentCart(id, deliveryDate);
		CartResponse response = new CartResponse();
		if (cart == null) {
			_LOGGER.info("getCartInternal :: no cart found");
			return ResponseEntity.ok(response);
		}
		_LOGGER.info(String.format("getCartInternal :: cart found: %s", cart.getId()));
		response.setData(buildCartResponse(cart, null));
		if (response.getData() != null) {
			Long orderCount = orderService.getDeliveredOrderCount(id);
			response.getData().setOrderCount(orderCount);
		}
		_LOGGER.info(String.format("getCartInternal :: response: %s", response));
		return ResponseEntity.ok(response);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveOrderEventLog(OrderEntity order, Object request, Object response) {
		try {
			OrderEventLogEntity orderLog = OrderEventLogEntity.createLog(order, request, response);
			_LOGGER.info("Saving Order Event");
			orderEventLogService.save(orderLog);
		} catch (Exception e) {
			_LOGGER.error("Error while saving order event", e);
		}
	}

	@ApiOperation(value = "Ppd order billing", nickname = "ppdOrderBill")
	@PostMapping("/orders/ppd/bill")
	public ResponseEntity<OrderResponseBean> ppdOrderBill(@RequestBody PpdOrderBean request) {
		_LOGGER.info(String.format("CREATE PPD ORDER BILL REQUEST : %s", request));
		OrderEntity order = getOrder(request.getId());
		if (order.getStatus().equals(OrderStatus.NEW_ORDER)) {
			order = orderService.updateOrderEntityFromPPD(order, request.getOrderItems());
		}
		generateInvoice(order);
		generateBill(order);
		OrderResponseBean response = getMapper().mapSrcToDest(order, OrderResponseBean.newInstance());
		saveOrderEventLog(order, request, response);
		return ResponseEntity.ok(response);
	}

	private void generateBill(OrderEntity order) {
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(order.getCustomerId());
		String fileUrl = generateOrderBill(order, null, userDetails);
		order.getMetadata().setBillUrl(fileUrl);
		orderService.save(order);
	}

	@ApiOperation(value = "Ppd order billing", nickname = "ppdOrderBill")
	@PostMapping("/orders/pricing/refresh")
	public void orderPricingRefresh() {
		_LOGGER.info("Refresh Order Items' Pricing:: Started");
		List<OrderEntity> orders = orderService.findNewOrders();
		if (CollectionUtils.isEmpty(orders)) {
			_LOGGER.info("No Order found to refresh the pricing");
			return;
		}
		_LOGGER.info(String.format("Refresh Orders count: %s", orders.size()));
		orderService.updateSaleAndMarkedPriceInOrderItems(orders);
		_LOGGER.info("Refresh Order Items' Pricing:: Completed");
		clientService.refreshStorePricing();
	}

	@ApiOperation(value = "Ppd order billing", nickname = "ppdOrderBill")
	@PostMapping("/orders/pricing/cart/refresh")
	public void orderPricingRefreshInCart() {
		List<OrderEntity> orders = orderService.findAllOrdersInCart();
		if (CollectionUtils.isEmpty(orders)) {
			_LOGGER.info("No Order found to refresh the pricing");
			return;
		}
		_LOGGER.info(String.format("Refresh Orders count: %s", orders.size()));
		orderService.updateSaleAndMarkedPriceInOrderItems(orders);
		_LOGGER.info("Refresh Order Items' Pricing:: Completed");
	}

	private void generateInvoice(OrderEntity order) {
		try {
			invoiceService.generateConsumerInvoices(List.of(order));
		} catch (Exception e) {
			_LOGGER.error("Exception while generating invoice", e);
		}
	}

	private void generateRefundInvoice(OrderEntity order, OrderEntity refundOrder) {
		try {
			if (order.getInvoice() != null) {
				invoiceService.generateConsumerRefundCreditNote(order, refundOrder);
			}
		} catch (Exception e) {
			_LOGGER.error("Exception while generating refund invoice", e);
		}
	}

	@ApiOperation(value = "Update order status", nickname = "updateOrderStatus")
	@PostMapping("/orders/{id}/status")
	public ResponseEntity<Boolean> updateOrderStatus(@PathVariable UUID id, @RequestBody UpdateOrderStatusBean request) {
		request.setOrderId(id);
		_LOGGER.info(String.format("Updating Order Status from PPD:: REQUEST : %s", request));
		OrderEntity order = getOrder(id);
		if (request.getStatus().equals(OrderStatus.ORDER_OUT_FOR_DELIVERY) && !isValidOutForDelivery(order)) {
			return ResponseEntity.ok(true);
		}
		order = orderService.updateOrderStatusFromPPD(order, request);
		OrderResponseBean response = getMapper().mapSrcToDest(order, OrderResponseBean.newInstance());
		if (order.getStatus().equals(OrderStatus.ORDER_DELIVERED)) {
			try {
				WalletBean wallet = clientService.getUserWallet(order.getCustomerId().toString());
				_LOGGER.info(String.format("processItemCashBack initiated for order : %s", order.getDisplayOrderId()));
				processCashBackItemForOrders(Collections.singletonList(order));
				preparePdfAndSendWhatsAppMessage(order, wallet);
				updateUserOrderCount(order);
				Map<String, Object> profileData = new HashMap<>();
				profileData.put("lastOrderDeliveryDate", order.getDeliveryDate());
				clientService.sendClevertapProfileUpdate(order.getCustomerId().toString(), profileData);
			} catch (Exception e) {
				_LOGGER.error("Exception while sending pdf to customer", e);
			}

		}
		saveOrderEventLog(order, request, response);
		/*if (order.getStatus().equals(OrderStatus.ORDER_DELIVERED)) {
			clientService.sendPushNotifications(Collections.singletonList(notificationService.getOrderDeliveredPnRequest(order)));
		}*/
		return ResponseEntity.ok(true);
	}

	private boolean isValidOutForDelivery(OrderEntity order) {
		return !order.getStatus().equals(OrderStatus.ORDER_DELIVERED) && !order.getStatus().equals(OrderStatus.ORDER_CANCELLED_BY_CUSTOMER)
				&& !order.getStatus().equals(OrderStatus.ORDER_CANCELLED_BY_STORE);
	}

	private void updateUserOrderCount(OrderEntity order) {
		Long count = orderService.getDeliveredOrderCount(order.getCustomerId());
		if (count == null) {
			count = 0l;
		}
		clientService.updateUserOrderCount(order.getCustomerId(), count);
	}

	private String preparePdfAndSendWhatsAppMessage(OrderEntity order, WalletBean wallet) {
		List<String> params = new ArrayList<>();
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(order.getCustomerId());
		String fileUrl = generateOrderBill(order, wallet, userDetails);
		params.add(order.getFinalBillAmount().toString());
		WhatsappSendMsgRequest request = new WhatsappSendMsgRequest();
		Map<String, String> fillers = new HashMap<>();
		fillers.put("amount", order.getFinalBillAmount().toString());
		fillers.put("fileName", "Order-bill.pdf");
		request.getMessageRequests()
				.add(WhatsappSendMsgRequest.WhatsappSendMsgSingleRequest.builder().userId(order.getCustomerId()).messageType("doc")
						.phoneNumber(order.getMetadata().getContactDetail().getPhone()).url(fileUrl).templateName("ORDER_DELIVERED_CUSTOMER_WA")
						.fillers(fillers).params(params).build());
		clientService.sendWhatsappMessages(request);
		return fileUrl;
	}

	private synchronized String generateOrderBill(OrderEntity order, WalletBean wallet, UserServiceResponse userDetails) {
		PayablePDFGenerator generator = new PayablePDFGenerator();
		generator.generatePdfReport(order, wallet, userDetails);
		String filePath = orderService.uploadFile(order);
		_LOGGER.info("OrderBill filePath: " + filePath);
		return ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(filePath);
	}

	@ApiOperation(value = "Cancel Order", nickname = "cancelOrder")
	@PostMapping("/orders/cancel")
	@ResponseStatus(HttpStatus.OK)
	public void cancelOrder(@RequestBody UpdateOrderStatusBean request) {
		request.setOrderId(request.getOrderId());
		_LOGGER.info(String.format("Cancelling Order : %s", request));
		OrderEntity order = getOrder(request.getOrderId());
		if (order.getStatus().equals(OrderStatus.ORDER_CANCELLED_BY_STORE) || order.getStatus().equals(OrderStatus.ORDER_CANCELLED_BY_CUSTOMER)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order already cancelled", "order"));
		}
		OrderStatus existingStatus = order.getStatus();
		orderService.setOrderStatus(order, request.getStatus());
		if (request.getRemarks() != null) {
			order.getMetadata().setPpdRemarks(request.getRemarks());
		}
		if (!existingStatus.equals(OrderStatus.NEW_ORDER) && order.getIsRefunded().equals(1)) {
			generateRefundInvoice(order, order);
		}
		saveOrderEventLog(order, request, null);
	}

	@ApiOperation(value = "Cart Address Mapping", nickname = "cartAddressMapping")
	@PostMapping("/orders/cart/update-address")
	@ResponseStatus(HttpStatus.OK)
	public void mapAddressToOrder(@RequestBody CartAddressMappingRequest request) {
		_LOGGER.info(String.format("Cancelling Order : %s", request));
		OrderEntity order = orderService.findRecordByDisplayOrderId(request.getDisplayOrderId());
		if (order == null || order.getStatus() != OrderStatus.IN_CART) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Cart Found", "order"));
		}
		order.setDeliveryAddress(request.getAddressId());
		orderService.save(order);
	}

	@ApiOperation(value = "Update Cart V2 in application store.", nickname = "updateCartV2")
	@PutMapping("/orders/cart/game/cashback-item")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<CartResponse> addGameCashbackItem(@Valid @RequestBody GameCashbackItemRequest request) {
		_LOGGER.info(String.format("Game Complementary Item : %s", request));
		UUID customerId = orderService.getUserId();
		Date deliveryDate = cartService.getDeliveryDate(null);
		final OrderEntity cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		if (Objects.isNull(cart)) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		cart.getOrderItems().stream().forEach(i -> {
			if (i.getSkuCode().equals(request.getSkuCode())) {
				i.getMetadata().setIsCashbackItem(true);
				i.getMetadata().setItemCashbackAmount(i.getFinalAmount());
				i.getMetadata().setCashbackType(CashbackType.TREE_GAME);
			} else {
				i.getMetadata().setCashbackType(null);
				i.getMetadata().setIsCashbackItem(false);
			}
		});
		CartResponse response = new CartResponse();
		response.setData(buildCartResponse(cart, null));
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Process item cashback", nickname = "processItemCashBack")
	@PostMapping("/orders/process/cashback-item")
	public void processItemCashBack() {
		Date fromDeliveryDate = DeliveryDateUtils.getDeliveryDateForCashback();
		_LOGGER.info(String.format("processItemCashBack initiated for delivery date : %s", fromDeliveryDate));
		List<OrderEntity> orders = orderService.findOrdersWithCashbackItems(fromDeliveryDate);
		if (CollectionUtils.isNotEmpty(orders)) {
			processCashBackItemForOrders(orders);
		} else {
			_LOGGER.info(String.format("No orders to process cashback "));
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void processCashBackItemForOrders(List<OrderEntity> orders) {
		List<UUID> customerIds = orders.stream().map(OrderEntity::getCustomerId).collect(Collectors.toList());
		List<WalletBean> userWallets = clientService.findAllCustomersWallet(customerIds);
		Map<String, WalletBean> customerWalletMap = userWallets.stream().collect(Collectors.toMap(WalletBean::getEntityId, Function.identity()));
		Double thresholdAmount = Double.valueOf(ParamsUtils.getParam("B2C_PROCESS_CASHBACK_WALLET_LIMIT", "-100"));
		Double itemCashbackLimit = Double.valueOf(ParamsUtils.getParam("B2C_ITEM_CASHBACK_LIMIT_PERCENT", "100"));
		for (OrderEntity order : orders) {
			String customerId = order.getCustomerId().toString();
			Map<String, Double> refundedOrderQtyMap = orderService.getRefundedOrderSkuWiseQtyMap(order.getId());
			for (OrderItemEntity item : order.getOrderItems()) {
				if (item.getMetadata().getIsCashbackItem() && !item.getMetadata().getIsItemCashbackProcessed() && customerWalletMap.containsKey(customerId)) {
					Double amount = customerWalletMap.get(customerId).getAmount();
					if (amount.compareTo(thresholdAmount) >= 0) {
						Double itemCashbackAmount = calculateCashbackAmount(item, refundedOrderQtyMap);
						_LOGGER.info(String.format("Cashback amount : %s", itemCashbackAmount));
						if (itemCashbackAmount.compareTo(0d) > 0) {
							try {
								BigDecimal orderLimit = BigDecimal.valueOf(order.getFinalBillAmount()).multiply(BigDecimal.valueOf(itemCashbackLimit))
										.divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
								Double cashbackAmount = Math.min(itemCashbackAmount, orderLimit.doubleValue());
								item.getMetadata().setItemCashbackAmount(cashbackAmount);
								orderService.addOrDeductMoneyFromUserWallet(customerId, cashbackAmount, order.getDisplayOrderId(), "Promo-Cashback", null, null,
										String.format("%s|%s", OrderConstants.PROMO_CASHBACK_TXN_TYPE, order.getDisplayOrderId()));
								_LOGGER.info(String.format("Promo-Cashback amount of %s added in wallet to user %s", cashbackAmount, customerId));
								item.getMetadata().setIsItemCashbackProcessed(true);
								item.getMetadata().setCashBackErrorMsg(null);
								if (item.getMetadata().getItemCashbackMaxQty() != null) {
									sendTomatoItemCashbackNotificationToCustomer(order.getCustomerId(), cashbackAmount);
								} else {
									sendTreeItemCashbackNotificationToCustomer(order.getCustomerId(), cashbackAmount);
								}
							} catch (Exception e) {
								item.getMetadata().setCashBackErrorMsg(String.format("Some issue happened while crediting cashback in wallet %s", e.getMessage()));
							}
						} else {
							item.getMetadata().setIsItemCashbackProcessed(true);
							item.getMetadata().setCashBackErrorMsg(String.format("Invalid cashback amount: Rs.%s", itemCashbackAmount));
						}
					} else {
						item.getMetadata().setCashBackErrorMsg(
								String.format("Promo-Cashback not added as the wallet amount %s is below Threshold %s", amount, thresholdAmount));
						_LOGGER.info(String.format("Promo-Cashback not added in wallet to user %s due to wallet check", customerId));
					}
				} else if (!customerWalletMap.containsKey(customerId)) {
					_LOGGER.info(String.format("No Wallet found for user : %s", customerId));
				}
			}
			processOrderLevelCashback(order, customerId);
			orderService.save(order);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void processOrderLevelCashback(OrderEntity order, String customerId) {
		if (order.getMetadata().getIsCashbackApplicable() && !order.getMetadata().getIsCashbackProcessed()) {
			Double cashbackAmount = order.getMetadata().getCashbackAmount();
			orderService.addOrDeductMoneyFromUserWallet(customerId, cashbackAmount, order.getDisplayOrderId(), "Promo-Cashback", null, null,
					String.format("%s|%s", OrderConstants.PROMO_CASHBACK_TXN_TYPE, order.getDisplayOrderId()));
			order.getMetadata().setIsCashbackProcessed(Boolean.TRUE);
			sendOrderCashbackNotificationToCustomer(order.getCustomerId(), cashbackAmount);
		}
	}

	private Double calculateCashbackAmount(OrderItemEntity item, Map<String, Double> refundedOrderQtyMap) {
		BigDecimal totalReturnedQty = BigDecimal.valueOf(refundedOrderQtyMap.getOrDefault(item.getSkuCode(), 0d));
		Double cashbackQty = BigDecimal.valueOf(item.getFinalQuantity()).subtract(totalReturnedQty).doubleValue();
		if (item.getMetadata().getItemCashbackMaxQty() != null) {
			cashbackQty = Math.min(item.getMetadata().getItemCashbackMaxQty(), cashbackQty);
		}
		if (item.getFinalQuantity().compareTo(0d) == 0) {
			return 0d;
		}
		item.getMetadata().setItemCashbackQty(cashbackQty);
		return cartService.getCashbackAmount(item, cashbackQty);
	}

	private void sendTreeItemCashbackNotificationToCustomer(UUID customerId, Double cashbackAmount) {
		Map<String, String> fillers = new HashMap<>();
		fillers.put("amount", cashbackAmount.toString());
		PnRequest pnRequest = PnRequest.builder().userId(customerId.toString()).templateName(OrderConstants.CASHBACK_CREDITED_TEMPLATE_NAME).fillers(fillers)
				.build();
		clientService.sendPushNotifications(Collections.singletonList(pnRequest));
	}

	private void sendTomatoItemCashbackNotificationToCustomer(UUID customerId, Double cashbackAmount) {
		Map<String, String> fillers = new HashMap<>();
		fillers.put("amount", cashbackAmount.toString());
		PnRequest pnRequest = PnRequest.builder().userId(customerId.toString()).templateName(OrderConstants.CASHBACK_FREE_TOMATO_TEMPLATE_NAME).fillers(fillers)
				.build();
		clientService.sendPushNotifications(Collections.singletonList(pnRequest));
	}

	private void sendOrderCashbackNotificationToCustomer(UUID customerId, Double cashbackAmount) {
		Map<String, String> fillers = new HashMap<>();
		fillers.put("amount", cashbackAmount.toString());
		PnRequest pnRequest = PnRequest.builder().userId(customerId.toString()).templateName(OrderConstants.CASHBACK_ORDER).fillers(fillers).build();
		clientService.sendPushNotifications(Collections.singletonList(pnRequest));
	}

	/**
	 * Add to Cart V2
	 */
	@ApiOperation(value = "Add to Cart V2 in application store.", nickname = "addToCartV2")
	@PostMapping({ "/orders/cart/v2", "/orders/pos/cart" })
	public ResponseEntity<CartResponse> addToCartV2(@Valid @RequestBody CartRequest request, @RequestHeader(required = false) String appVersion,
			@RequestParam(required = false) Map<String, String> additionalParams,
			@RequestParam(required = false, defaultValue = "false") Boolean isInternalRequest) {
		_LOGGER.info(String.format("ADD TO CART V2 REQUEST : %s", request));
		CartResponse response = new CartResponse();
		String appId;
		if (CollectionUtils.isNotEmpty(additionalParams)) {
			appId = additionalParams.getOrDefault("appId", SessionUtils.getAppId());
		} else {
			appId = SessionUtils.getAppId();
		}
		String storeId = SessionUtils.getStoreId();
		if (StringUtils.isEmpty(storeId) && StringUtils.isNotEmpty(request.getStoreId())) {
			storeId = request.getStoreId();
		}
		validateReqGrades(request);
		validateStoreId(storeId);
		SocietyListItemBean society = validateAndGetSociety(request.getSocietyId(), storeId);
		OrderEntity cart;
		UUID customerId = orderService.getUserId();
		if (isPosRequest(appId)
				|| (StringUtils.isNotEmpty(request.getChannel()) && request.getChannel().equalsIgnoreCase(OrderConstants.Channel.BACKOFFICE.toString())
						|| (!Objects.isNull(isInternalRequest) && isInternalRequest))) {
			cartService.customerIdCheck(request.getCustomerId());
			customerId = request.getCustomerId();
		} else {
			request.setDiscountAmount(0d);
		}
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate(), false);
		cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		if (cart == null && request.getQuantity() == 0) {
			_LOGGER.info("addToCartV2 : cart and request quantity both null");
			return ResponseEntity.ok(response);
		}
		if (appVersion == null) {
			appVersion = ParamsUtils.getParam("DEFAULT_APP_VERSION");
		}
		// check for future order
		if (Objects.isNull(isInternalRequest) || !isInternalRequest) {
			validateOrderTiming(response, cart);
			boolean isReducingQuantity = cartService.checkIfReducingQuantity(cart, request);
			if (!isReducingQuantity) {
				cartService.hasSufficientCodWalletAmount(response, customerId, appVersion);
			}
		} else if (cart != null) {
			request.setSlotId(cart.getSlotId());
		}
		if (response.getError() != null) {
			if (cart == null) {
				response.setData(OrderResponseBean.newInstance());
				response.setStatus(false);
				GameBean gameBean = cartService.prepareGameDetails(customerId, response);
				response.getData().setGameDetails(gameBean);
				response.getData().setCustomerId(customerId);
				response.getData().getMetadata().setAppVersion(appVersion);
				cartService.buildOrderOfferResponse(storeId, cart, response);
			} else {
				response.setData(buildCartResponse(cart, null));
				cartService.buildOrderOfferResponse(storeId, cart, response);
			}
			return ResponseEntity.ok(response);
		}
		try {
			cart = cartService.addToCartV2(cart, request, deliveryDate, customerId, storeId, appId, society, appVersion, additionalParams, isInternalRequest,
					response);
		} catch (ValidationException e) {
			_LOGGER.error("Something went wrong while adding to cart.", e);
			throw e;
		} catch (Exception e) {
			_LOGGER.error("Something went wrong while adding to cart.", e);
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong while adding to cart. Our team is on it. Please retry.", null));
		}
		if (Objects.nonNull(cart) && cart.getItemCount() > 0) {
			response.setData(buildCartResponse(cart, null));
			if (response.getData().getGameDetails() != null && response.getData().getGameDetails().getGamePlayEligible()) {
				response.setScratchCardItem(cartService.setScratchCardItem(cart));
			}
		} else if (Objects.nonNull(cart) && (cart.getActive() == 0 || cart.getItemCount() == 0)) {
			cart.setActive(0);
			orderService.releaseOrderSlot(cart.getSlotId());
			response.setData(OrderResponseBean.newInstance());
			response.setStatus(false);
			GameBean gameBean = cartService.prepareGameDetails(customerId, response);
			response.getData().setGameDetails(gameBean);
		}
		handleVersion101QuantityError(response, appVersion);
		return ResponseEntity.ok(response);
	}

	private void handleVersion101QuantityError(CartResponse response,String appVersion) {
		if (response.getError() == null || !appVersion.equals("1.1.101")) {
			return;
		}
		if (response.getError().getCode() != null && (response.getError().getCode().equals("INSUFFICIENT_QUANTITY") || response.getError().getCode()
				.equals("ITEMS_OOS"))) {
			response.setError(null);
		}
	}

	private void validateOrderTiming(CartResponse response, OrderEntity cart) {
		boolean addError = false;
		if (stopConsumerOrder()) {
			if (cart != null && cart.getMetadata().getGracePeriodAllowed()) {
				if (isBeforeGracePeriodEnd()) {
					addError = true;
				}
			} else {
				addError = true;
			}
		}
		if (addError && response.getError() == null) {
			String message = paramService.findValueByParamKey("BLOCK_WINDOW_ORDER_MESSAGE");
			response.error("Order", message);
		}
	}

	private Boolean stopConsumerOrder() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String orderBlockStartTime = ParamsUtils.getParam("CONSUMER_ORDER_BLOCK_WINDOW_START_TIME", "23:00:00");
		String orderBlockEndTime = ParamsUtils.getParam("CONSUMER_ORDER_BLOCK_WINDOW_END_TIME", "05:00:00");
		return localTime.isAfter(LocalTime.parse(orderBlockStartTime)) || localTime.isBefore(LocalTime.parse(orderBlockEndTime));
	}

	private Boolean isBeforeGracePeriodEnd() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String orderBlockStartTime = ParamsUtils.getParam("CONSUMER_ORDER_GRACE_BLOCK_TIME", "23:10:00");
		return localTime.isAfter(LocalTime.parse(orderBlockStartTime));
	}

	public OrderResponseBean buildCartResponse(OrderEntity cart, WalletBean wallet) {
		cartService.sortOrderItems(cart);
		OrderResponseBean response = mapper.mapSrcToDest(cart, OrderResponseBean.newInstance());
		response.setOrderCharges(orderService.setOrderCharges(cart));
		response.setCartImages(getCartImagesArray());
		if (wallet != null) {
			response.setWalletBalance(wallet.getAmount());
			response.setWalletLoyaltyCoins(wallet.getLoyaltyCoins());
			response.setCoinsAfterDeduction(BigDecimal.valueOf(wallet.getLoyaltyCoins()).add(BigDecimal.valueOf(cart.getCartCoinsEarned()))
					.subtract(BigDecimal.valueOf(cart.getFinalBillCoins())));
			if (cart.getFinalBillAmount() > wallet.getAmount()) {
				response.setWalletError(true);
			}
		}
		if (cart.getDeliveryAddress() == null) {
			response.setAddressError("Order would fail as delivery address is not added.");
		}
		cartService.addGameDetailsToResponse(cart, response);
		setMessages(response);
		setCashbackDetails(cart, response);
		return response;
	}

	private List<String> getCartImagesArray() {
		String paramString = ParamsUtils.getParam("CART_IMAGES", "cart_image1.jpg");
		List<String> imagesList = Arrays.asList(paramString.split(","));
		return imagesList;
	}

	public void setCashbackDetails(OrderEntity order, OrderResponseBean response) {
		Double totalCashback = 0d;
		Double itemCashback = 0d;
		// cashback for tree game and free item, just leaving it by setting values
		boolean isFreeItemCashback = false;
		boolean isTreeGameCashback = false;
		for (OrderItemEntity i : order.getOrderItems()) {
			if (i.getMetadata().getIsCashbackItem()) {
				itemCashback += i.getFinalAmount();
				if (CashbackType.FREE_ITEM.equals(i.getMetadata().getCashbackType())) {
					isFreeItemCashback = true;
				} else if (CashbackType.TREE_GAME.equals(i.getMetadata().getCashbackType())) {
					isTreeGameCashback = true;
				}
			}
		}
		totalCashback += itemCashback;
		if (order.getMetadata().getIsCashbackApplicable() && order.getMetadata().getCashbackAmount() != null) {
			totalCashback += order.getMetadata().getCashbackAmount();
		}
		if (totalCashback.compareTo(0d) > 0) {
			String cashbackMsg = getCashbackMsg(order, totalCashback);
			response.setCashbackLabel("Gifting Tree Cashback");
			response.setCashbackMessage(cashbackMsg);
			response.setItemCashBackAmount(itemCashback);
			response.setTotalCashbackAmount(totalCashback);
		}
	}

	private String getCashbackMsg(OrderEntity order, Double totalCashback) {
		boolean isFirstOrderFlowVersion = cartService.isFirstOrderFlowVersion(order.getMetadata().getAppVersion());
		String cashbackMsg = String.format(ParamsUtils.getParam("CASHBACK_MSG"), BigDecimal.valueOf(totalCashback).setScale(0, RoundingMode.HALF_UP));
		if (!isFirstOrderFlowVersion) {
			cashbackMsg = cartService.removeTagFromString(cashbackMsg);
		}
		return cashbackMsg;
	}

	private void validateReqGrades(@Valid CartRequest request) {
		if (CollectionUtils.isNotEmpty(request.getGrades())) {
			request.getGrades().forEach(grade -> {
				if (grade.getPieces() != null && grade.getPieces().equals(0)) {
					grade.setPieces(null);
				}
			});
		}
	}

	private void validateStoreId(String storeId) {
		if (storeId == null) {
			throw new ValidationException(ErrorBean.withError("store_id_missing", "Store Id not found.", "storeId"));
		}
	}

	private SocietyListItemBean validateAndGetSociety(Integer societyId, String storeId) {
		if (societyId == null) {
			throw new ValidationException(ErrorBean.withError("society_id_missing", "Society Id not found.", "storeId"));
		}
		SocietyListItemBean society = clientService.getSocietyById(societyId);
		if (!Objects.equals(society.getStoreId(), storeId)) {
			throw new ValidationException(ErrorBean.withError("society_store_mismatch", "Society is not mapped to given store.", "storeId"));
		}
		return society;
	}

	/**
	 * Update cart V2
	 */
	@ApiOperation(value = "Update Cart V2 in application store.", nickname = "updateCartV2")
	@PutMapping("/orders/cart/v2")
	public ResponseEntity<CartResponse> updateCartV2(@Valid @RequestBody UpdateCartRequest request) {
		String appId = SessionUtils.getAppId();
		String storeId = SessionUtils.getStoreId();
		validateStoreId(storeId);
		_LOGGER.info(String.format("UPDATE CART V2 REQUEST : %s", request));
		UUID customerId = orderService.getUserId();
		if (isPosRequest(appId) || (StringUtils.isNotEmpty(request.getChannel()) && request.getChannel().equalsIgnoreCase(Channel.BACKOFFICE.toString()))) {
			cartService.customerIdCheck(request.getCustomerId());
			customerId = request.getCustomerId();
		}
		SocietyListItemBean society = validateAndGetSociety(request.getSocietyId(), storeId);
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate());
		final OrderEntity cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		CartResponse response = new CartResponse();

		if (Objects.isNull(cart)) {
			_LOGGER.info("updateCartV2 :: No cart found");
			response.setData(OrderResponseBean.newInstance());
			return ResponseEntity.ok(response);
		}

		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(storeId);
		Location location = request.getAddressId() != null ? getAddressLocation(request.getAddressId()) : cart.getMetadata().getLocation();

		if (cart.getOrderItems() == null && cart.getOrderItems().size() < 1) {
			throw new ValidationException(ErrorBean.withError("no_cart_item", "No cart item found", "cart"));
		}
		cart.getMetadata().setSocietyId(society.getId());
		List<String> skuCodes = cart.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList());
		StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(storeId, String.join(",", skuCodes), customerId);

		if (!isPosRequest(appId)) {
			if (cartService.isCartRefreshedV2(cart, location, response, storeDataResponse, storeItemResponse.getInventory(), skuCodes)) {
				_LOGGER.info("updateCartV2 :: cart refreshed");
				orderService.updateOrder(cart);
			}
		}
		if (request.getAddressId() != null) {
			cart.setDeliveryAddress(request.getAddressId());
		}
		if (request.getNotes() != null) {
			cart.setNotes(request.getNotes());
		}
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(customerId);
		if (userDetails != null && userDetails.getUserPreferences() != null && userDetails.getUserPreferences().getSlot() != null) {
			request.setSlotId(userDetails.getUserPreferences().getSlot());
		}
		if (request.getSlotId() != null) {
			cartService.setValidSlotInOrder(cart,request.getSlotId(),userDetails,request.getSocietyId() );
			orderService.updateOrder(cart);
		}
		if (request.getIsAutoCheckoutEnabled() != null && request.getIsAutoCheckoutEnabled().equals(Boolean.TRUE)) {
			cartService.setAutoCheckoutFlag(userDetails, response);
			cartService.setIsCodFlag(cart, userDetails);
			if (cart.getMetadata().getIsCod() != null && cart.getMetadata().getIsCod()) {
				cartService.sendCodNotificationToCustomer(cart.getCustomerId());
			}
		}
		cartService.applyOnboardingOffer(cart, userDetails, response);
		checkAndProcessSameDayOrder(cart, deliveryDate);
		orderService.save(cart);
		response.setData(buildCartResponse(cart, null));
		if (!CollectionUtils.isEmpty(cart.getOrderItems())) {
			cartService.buildOrderOfferResponse(storeId, cart, response);
		}
		return ResponseEntity.ok(response);
	}

	private void checkAndProcessSameDayOrder(OrderEntity cart, Date deliveryDate) {
		LocalDate convertedLocalDate = LocalDate.parse(DateUtils.toString(deliveryDate), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		LocalDate currentDate = LocalDateTime.now().plusHours(5).plusMinutes(30).toLocalDate();
		if (convertedLocalDate.equals(currentDate)) {
			markOrderDeliveredAndDeductMoney(cart);
			generateInvoice(cart);
		}
	}

	private void markOrderDeliveredAndDeductMoney(OrderEntity cart) {
		String txnType = "Consumer-Order";
		orderService.addOrDeductMoneyFromUserWallet(String.valueOf(cart.getCustomerId()), -1 * cart.getFinalBillAmount(), cart.getDisplayOrderId(), txnType,
				null, null, null);
		cart.setAmountReceived(cart.getFinalBillAmount());
		cart.setSubmittedAt(new Date());
		cart.setPaymentMethod(PaymentMethod.WALLET);
		cart.setStatus(OrderStatus.ORDER_DELIVERED);
		cart.getOrderItems().forEach(item -> item.setStatus(OrderItemStatus.PACKED));
	}

	/**
	 * Place Order from Cart V2
	 */
	@ApiOperation(value = "Submit Cart V2 in application store.", nickname = "confirmCartV2")
	@PostMapping({ "/orders/cart/submit/v2", "/orders/pos/cart/submit" })
	public ResponseEntity<CartResponse> submitV2(@Valid @RequestBody PlaceOrderRequest request) {
		_LOGGER.info(String.format("SUBMIT CART V2 REQUEST : %s", request));
		String appId = SessionUtils.getAppId();
		UUID customerId = orderService.getUserId();
		OrderEntity cart = null;
		if (isPosRequest(appId)) {
			cartService.customerIdCheck(request.getCustomerId());
			customerId = request.getCustomerId();
		} else {
			request.setOrderStatus(OrderStatus.NEW_ORDER);
		}
		Date deliveryDate = cartService.getDeliveryDate(request.getDeliveryDate());
		cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		validateSubmitCart(cart, request, appId);
		_LOGGER.info(String.format("submitCartV2 :: validation done for cart: %s", cart.getId()));
		CartResponse response = new CartResponse();

		StoreInventoryUpdateResponse inventoryUpdateResponse = null;

		if (isPosRequest(appId)) {
			StoreInventoryAddOrDeductRequest storeInvRequest = createStoreInventoryAddOrDeductRequest(cart);
			_LOGGER.info(String.format("submitCartV2 :: storeInvRequest: %s", storeInvRequest));
			inventoryUpdateResponse = clientService.addOrDeductStoreInventory(cart.getStoreId(), storeInvRequest);
			_LOGGER.info(String.format("submitCartV2 :: inventoryUpdateResponse: %s", inventoryUpdateResponse));
		} else {
			StoreInventoryUpdateRequest storeInvRequest = createStoreInventoryUpdateRequest(cart);
			_LOGGER.info(String.format("submitCartV2 :: storeInvRequest: %s", storeInvRequest));
			inventoryUpdateResponse = clientService.verifyAndDeductStoreInventory(cart.getStoreId(), storeInvRequest);
			_LOGGER.info(String.format("submitCartV2 :: inventoryUpdateResponse: %s", inventoryUpdateResponse));
		}

		if (inventoryUpdateResponse.getSuccess()) {
			if (isPosRequest(appId)) {
				cart.setAmountReceived(cart.getFinalBillAmount());
			}
			cartService.changeOrderState(cart, request.getShippingMethod(), request.getPaymentMethod(), request.getOrderStatus(), null);
			_LOGGER.info("submitCart :: order placed");
			if (!isPosRequest(appId)) {
				sendOrderToLithos(cart);
			}
		} else {
			_LOGGER.info("submitCart :: adding Inventory Errors");
			addInventoryErrors(cart, inventoryUpdateResponse);
			orderService.updateOrder(cart);
			response.error("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
			_LOGGER.info(String.format("submitCartV2 :: error response: %s", response));
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		}
		response.setData(buildCartResponse(cart, null));
		saveOrderEventLog(cart, request, response);
		_LOGGER.info(String.format("submitCartV2 :: response: %s", response));
		return ResponseEntity.ok(response);
	}

	/**
	 * Find all Carts V2.
	 */
	@ApiOperation(value = "Customer Cart V2", nickname = "findCartV2")
	@GetMapping({ "/orders/cart/v2", "/orders/pos/cart" })
	public ResponseEntity<CartResponse> getCartV2(@RequestParam(required = false, name = "lat") String latitude,
			@RequestParam(required = false, name = "long") String longitude, @RequestParam(required = false, defaultValue = "false") boolean checkInventory,
			@RequestParam(required = false) UUID customerId, @RequestParam(required = false) String channel, @RequestParam(required = false) String date,
			@RequestHeader(required = false) String appVersion) {
		String storeId = SessionUtils.getStoreId();
		String appId = SessionUtils.getAppId();
		checkInventory = Boolean.FALSE;
		_LOGGER.info(String.format("getCartV2 :: checkInventory: %s", checkInventory));
		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(storeId);
		Location locationReq = new Location();
		if (isPosRequest(appId)) {
			cartService.customerIdCheck(customerId);
			locationReq.setLatitude(String.valueOf(storeDataResponse.getLocation().getCoordinates().get(1)));
			locationReq.setLongitude(String.valueOf(storeDataResponse.getLocation().getCoordinates().get(0)));
		}

		OrderEntity cart = null;
		if (customerId == null && Channel.BACKOFFICE.toString().equalsIgnoreCase(channel)) {
			throw new ValidationException(ErrorBean.withError("customer_id_missing", "Customer Id not found.", "customerId"));
		}
		if (!isPosRequest(appId) && !Channel.BACKOFFICE.toString().equalsIgnoreCase(channel)) {
			customerId = orderService.getUserId();
		}
		Date deliveryDate = cartService.getDeliveryDate(DateUtils.getDate(DateUtils.SHORT_DATE_FMT, date));
		cart = orderService.findCustomerCurrentCart(customerId, deliveryDate);
		CartResponse response = new CartResponse();
		// resetExpiredCart(cart, 2, appId, response);
		if (cart == null) {
			response.setData(OrderResponseBean.newInstance());
			response.setStatus(false);
			GameBean gameBean = cartService.prepareGameDetails(customerId, response);
			response.getData().setGameDetails(gameBean);
			response.getData().setCustomerId(customerId);
			response.getData().getMetadata().setAppVersion(appVersion);
			cartService.buildOrderOfferResponse(storeId, cart, response);
			_LOGGER.info("getCartV2 :: no cart found");
			return ResponseEntity.ok(response);
		}

		_LOGGER.info(String.format("getCartV2 :: cart found: %s", cart.getId()));
		Boolean isCartUpdated = false;

		if (cart.getOrderItems() == null && cart.getOrderItems().size() < 1) {
			throw new ValidationException(ErrorBean.withError("no_cart_item", "No cart item found", "cart"));
		}
		List<String> skuCodes = cart.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList());
		StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(storeId, String.join(",", skuCodes), null);
		if (!isPosRequest(appId)) {
			if (cartService.isCartRefreshedV2(cart, locationReq, response, storeDataResponse, storeItemResponse.getInventory(), skuCodes)) {
				_LOGGER.info("getCartV2 :: cart refreshed");
				orderService.updateOrder(cart);
				isCartUpdated = true;
			} else if (checkInventory && validateStoreInventory(cart)) {
				_LOGGER.info("getCartV2 :: validateStoreInventory");
				if (cart.getError() != null) {
					response.setError(cart.getError());
					response.setMessage(cart.getError().getMessage());
				}
				orderService.updateOrder(cart);
				isCartUpdated = true;
			}
		}
		if (isGraceAllowed()) {
			cart.getMetadata().setGracePeriodAllowed(true);
			isCartUpdated = true;
		}
		if (isCartUpdated) {
			orderService.save(cart);
		}
		if (cart.getActive() != 0) {
			response.setData(buildCartResponse(cart, null));
			if (response.getData().getGameDetails() != null && response.getData().getGameDetails().getGamePlayEligible()) {
				response.setScratchCardItem(cartService.setScratchCardItem(cart));
			}
		}
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(customerId);
		cartService.applyOnboardingOffer(cart, userDetails, response);
		cartService.checkIfAutoCheckout(userDetails, response);
		cartService.buildOrderOfferResponse(storeId, cart, response);
		return ResponseEntity.ok(response);
	}

	private boolean isPosRequest(String appId) {
		return Objects.equals(appId, "com.example.pos_flutter_app");
	}

	Map<String, Object> getOrderParams(UUID customerId, String storeId, String channel, String fromDate, String toDate, String deliveryDate)
			throws ParseException {
		Map<String, Object> params = new HashMap<>();
		if (customerId != null) {
			params.put("customerId", customerId);
		}
		if (storeId != null) {
			params.put("storeId", storeId);
		}
		if (channel != null) {
			params.put("channel", channel);
		}
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(TimeZone.getDefault());
		if (fromDate != null) {
			params.put("fromDate", new FilterCriteria("createdAt", formatter.parse(fromDate), Operation.GTE));
		}
		if (toDate != null) {
			params.put("toDate", new FilterCriteria("createdAt", formatter.parse(toDate), Operation.LTE));
		}
		if (deliveryDate != null) {
			params.put("deliveryDate", formatter.parse(deliveryDate));
		}
		params.put("status0", new FilterCriteria("status", OrderStatus.IN_CART, Operation.NOT_EQUALS));
		return params;
	}

	@ApiOperation(value = "List all pos Orders", nickname = "findPendingCustomersOrderList")
	@GetMapping("/orders/pos")
	public PageAndSortResult<OrderListBean> findOrdersFromPos(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "20") Integer pageSize, @RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) String deliveryDate, @RequestParam(required = false) String channel,
			@RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate) throws ParseException {
		String storeId = SessionUtils.getStoreId();
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		Map<String, Object> params = getOrderParams(customerId, storeId, channel, fromDate, toDate, deliveryDate);
		PageAndSortResult<OrderEntity> poEntityList = orderService.findOrdersByPage(pageSize, pageNo, params, sort);
		PageAndSortResult<OrderListBean> response = new PageAndSortResult<>();
		if (poEntityList != null && poEntityList.getData() != null) {
			response = prepareResponsePageData(poEntityList, OrderListBean.class);
		}
		return response;
	}

	@ApiOperation(value = "Create Pos Order", nickname = "createPosOrder")
	@PostMapping("/orders/internal/pos")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<OrderResponseBean> createPosOrder(@Valid @RequestBody PosOrderBean request) {
		OrderEntity order = null;
		if (request.getId() != null) {
			order = orderService.findById(request.getId());
		}
		if (order == null || order.getStatus() == OrderStatus.IN_CART) {
			if (order != null) {
				orderService.deactivateOrder(order);
			}
			StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(request.getStoreId());
			String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
			Date deliveryDate = DeliveryDateUtils.getPosDeliveryDate();
			order = OrderEntity.createNewCart(request.getCustomerId(), request.getDeliveryAddress(), request.getStoreId(), request.getChannel(),
					request.getMetadata().getContactDetail(), request.getMetadata().getLocation(), (new Date()).getTime(), displayOrderId,
					storeDataResponse.getDistance(), storeDataResponse.getLocation().getCoordinates(), storeDataResponse.getZoneId(), true, deliveryDate, null);
			cartService.changeOrderState(order, request.getShippingMethod(), request.getPaymentMethod(), request.getStatus(), request.getAmountReceived());
			Map<String, StoreProductInventory> storeInvMap = getStoreInventoryFromSkuCodes(
					request.getOrderItems().stream().map(PosOrderItemBean::getSkuCode).collect(Collectors.toList()), request.getStoreId());
			if (request.getOrderDiscountAmount() != null) {
				order.getOfferData().setOrderDiscount(request.getOrderDiscountAmount());
			}
			orderService.createPosOrder(order, storeInvMap, request);
			StoreInventoryAddOrDeductRequest storeInvRequest = createStoreInventoryAddOrDeductRequest(order);
			clientService.addOrDeductStoreInventory(order.getStoreId(), storeInvRequest);
		} else {
			List<OrderItemUpdateBean> itemList = request.getOrderItems().stream().map(item -> new OrderItemUpdateBean(item.getSkuCode(), item.getQuantity()))
					.collect(Collectors.toList());
			orderService.updateOrderItems(order, itemList);
			if (order.getStatus() != request.getStatus()) {
				cartService.changeOrderState(order, request.getShippingMethod(), request.getPaymentMethod(), request.getStatus(), request.getAmountReceived());
			}
		}
		if (request.getPayment() != null && PaymentStatus.SUCCESS.toString().equals(request.getPayment().getTxStatus())) {
			request.getPayment().setOrderId(order.getId().toString());
			request.getPayment().setPaymentGateway("SORTEDPOS");
			sendPaymentUpdate(request.getPayment());
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(order, OrderResponseBean.newInstance()));
	}

	private Map<String, StoreProductInventory> getStoreInventoryFromSkuCodes(List<String> skuCodes, String storeId) {
		Map<String, StoreProductInventory> storeInvMap = null;
		try {
			StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(storeId, String.join(",", skuCodes), null);
			if (CollectionUtils.isNotEmpty(storeItemResponse.getInventory())) {
				storeInvMap = storeItemResponse.getInventory().stream().collect(Collectors.toMap(StoreProductInventory::getInventorySkuCode, i -> i));
			}
		} catch (Exception e) {
			// do nothing
		}
		return storeInvMap;
	}

	@ApiOperation(value = "Refund Order", nickname = "refundOrder")
	@PostMapping("/orders/refund")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<OrderResponseBean> refundOrder(@RequestBody RefundOrderBean request) {
		OrderEntity parentOrder = getParentOrder(request.getParentOrderId());
		OrderEntity refundOrder = orderService.createRefundOrder(parentOrder, request, null, false);
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, OrderResponseBean.newInstance()));
	}

	private OrderEntity getOrder(UUID orderId) {
		OrderEntity order = orderService.findRecordById(orderId);
		if (Objects.isNull(order)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return order;
	}

	private OrderEntity getParentOrder(UUID parentOrderId) {
		OrderEntity parentOrder = getOrder(parentOrderId);
		if (!(parentOrder.getStatus().equals(OrderStatus.ORDER_DELIVERED) || parentOrder.getStatus().equals(OrderStatus.ORDER_BILLED))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return parentOrder;
	}

	private OrderEntity getRefundOrderByKey(String key) {
		return key != null ? orderService.findByKey(key) : null;
	}

	@ApiOperation(value = "Ims Consumer Refund Order", nickname = "imsConsumerRefundOrder")
	@PostMapping("/orders/ims/refund")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<OrderResponseBean> imsProcessConsumerRefundOrder(@Valid @RequestBody ImsConsumerOrderRefundBean request,
			@RequestParam(required = false) String key) {
		_LOGGER.info(String.format("REFUND ORDER FROM IMS parentOrderId : %s, request was : %s", request.getParentOrderId(), request));
		OrderEntity parentOrder = getParentOrder(request.getParentOrderId());
		OrderEntity refundOrder = getRefundOrderByKey(key);
		if (refundOrder == null) {
			RefundOrderBean refundOrderBean = createRefundOrderBean(request);
			refundOrder = orderService.createRefundOrder(parentOrder, refundOrderBean, key, false);
			if (refundOrder.getFinalBillAmount().compareTo(0d) > 0) {
				orderService.addOrDeductMoneyFromUserWallet(refundOrder.getCustomerId().toString(), refundOrder.getFinalBillAmount(),
						parentOrder.getDisplayOrderId(), OrderConstants.IMS_REFUND_CONSUMER_ORDER_TXN_TYPE, null,
						populateRefundOrderRemarks(refundOrder, false), key);
			}
			generateRefundInvoice(parentOrder, refundOrder);
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, OrderResponseBean.newInstance()));
	}

	private RefundOrderBean createRefundOrderBean(ImsConsumerOrderRefundBean request) {
		RefundOrderBean refundOrderBean = RefundOrderBean.newInstance();
		refundOrderBean.setParentOrderId(request.getParentOrderId());
		List<RefundOrderItemBean> refundOrderItems = new ArrayList<>();
		for (ImsConsumerOrderRefundItemBean item : request.getRefundOrderItems()) {
			RefundOrderItemBean refundOrderItem = new RefundOrderItemBean();
			refundOrderItem.setSkuCode(item.getSkuCode());
			refundOrderItem.setQuantity(item.getRefundQuantity());
			refundOrderItems.add(refundOrderItem);
		}
		refundOrderBean.setRefundOrderItems(refundOrderItems);
		return refundOrderBean;
	}

	private String populateRefundOrderRemarks(OrderEntity refundOrder, Boolean isFullRefundOrder) {
		String deliveryDate = DateUtils.toString(DateUtils.DATE_MM_FMT, refundOrder.getDeliveryDate());
		if (isFullRefundOrder) {
			return String.format("Order Dated: %s", deliveryDate);
		}
		if (CollectionUtils.isNotEmpty(refundOrder.getOrderItems())) {
			String orderItems = refundOrder.getOrderItems().stream().map(OrderItemEntity::getProductName).collect(Collectors.joining(", "));
			return String.format("Order Dated: %s, Items: %s", deliveryDate, orderItems);
		}
		return String.format("Order Dated: %s", deliveryDate);
	}

	@ApiOperation(value = "Ims Consumer Refund All Order", nickname = "imsConsumerRefundAllOrder")
	@PostMapping("/orders/ims/refund/all")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<OrderResponseBean> imsConsumerRefundAllOrder(@Valid @RequestBody ImsConsumerOrderRefundAllBean request,
			@RequestParam(required = false) String key) {
		_LOGGER.info(String.format("REFUND ALL ORDER FROM IMS parentOrderId : %s, request was : %s", request.getParentOrderId(), request));
		OrderEntity parentOrder = getParentOrder(request.getParentOrderId());
		OrderEntity refundOrder = getRefundOrderByKey(key);
		if (refundOrder == null) {
			RefundOrderBean refundOrderBean = createRefundOrderBean(request, parentOrder);
			refundOrder = orderService.createRefundOrder(parentOrder, refundOrderBean, key, true);

			orderService.addOrDeductMoneyFromUserWallet(refundOrder.getCustomerId().toString(), refundOrder.getFinalBillAmount(),
					parentOrder.getDisplayOrderId(), OrderConstants.IMS_REFUND_CONSUMER_ALL_ORDER_TXN_TYPE, null,
					populateRefundOrderRemarks(refundOrder, true), key);
			generateRefundInvoice(parentOrder, refundOrder);
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(refundOrder, OrderResponseBean.newInstance()));
	}

	private RefundOrderBean createRefundOrderBean(ImsConsumerOrderRefundAllBean request, OrderEntity parentOrder) {
		RefundOrderBean refundOrderBean = RefundOrderBean.newInstance();
		refundOrderBean.setParentOrderId(request.getParentOrderId());
		List<RefundOrderItemBean> refundOrderItems = new ArrayList<>();
		for (OrderItemEntity item : parentOrder.getOrderItems()) {
			RefundOrderItemBean refundOrderItem = new RefundOrderItemBean();
			refundOrderItem.setSkuCode(item.getSkuCode());
			refundOrderItem.setQuantity(item.getFinalQuantity());
			refundOrderItems.add(refundOrderItem);
		}
		refundOrderBean.setRefundOrderItems(refundOrderItems);
		return refundOrderBean;
	}

	@ApiOperation(value = "POS store report", nickname = "posStoreReport")
	@GetMapping("/orders/pos/store-report")
	public PosStoreReport getStoreWiseReport(@RequestParam PosAdminReportViewType reportViewType,
			@RequestParam(required = false) ReportBreakdownType reportBreakdownType, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to, @RequestParam(required = false) List<String> storeIds,
			@RequestParam(required = false) List<String> skuCodes) {
		UUID userId = orderService.getUserId();
		AuthServiceStoreDetailsBean authServiceStoreDetails = clientService.getUserMappedStores(String.valueOf(userId));
		List<String> mappedStoreIds = getStoreIds(authServiceStoreDetails);
		PosStoreReport response = new PosStoreReport();
		LocalDate fromDate = parseDateOrDefault(from, LocalDate.now());
		LocalDate toDate = parseDateOrDefault(to, LocalDate.now());
		if (reportViewType.equals(PosAdminReportViewType.DAILY_ORDER_VIEW) || reportViewType.equals(PosAdminReportViewType.STORE_VIEW)) {
			response.setStoreOrderInfo(orderService.findStoreTotalSaleDetails(mappedStoreIds, fromDate, toDate, reportBreakdownType, skuCodes));
		}
		if (reportViewType.equals(PosAdminReportViewType.DAILY_ORDER_VIEW) || reportViewType.equals(PosAdminReportViewType.SKU_VIEW)) {
			response.setSkuOrderInfo(orderService.findSkuTotalSaleDetails(mappedStoreIds, fromDate, toDate, reportBreakdownType, storeIds));
		}
		if (reportViewType.equals(PosAdminReportViewType.DAILY_ORDER_VIEW)) {
			response.setMonthlySales(orderService.getStoresMtd(mappedStoreIds));
			response.setWeeklySales(orderService.getStoresWtd(mappedStoreIds));
			response.setTodaySales(orderService.getStoresTodaySales(mappedStoreIds));
		}
		return response;
	}

	private List<String> getStoreIds(AuthServiceStoreDetailsBean authServiceStoreDetails) {
		return authServiceStoreDetails.getWhData().stream().map(whData -> String.valueOf(whData.getId())).collect(Collectors.toList());
	}

	private LocalDate parseDateOrDefault(String date, LocalDate defaultValue) {
		if (date == null) {
			return defaultValue;
		}
		return LocalDate.parse(date);
	}

	@ApiOperation(value = "fetch order slots", nickname = "fetchOrderSlotsList")
	@GetMapping("/orders/slots")
	public ResponseEntity<List<OrderSlotResponseBean>> getOrderSlots(@RequestParam(required = false, name = "societyId") String societyId) {
		List<OrderSlotResponseBean> orderSlotResponse = orderService.buildOrderSlotResponse(societyId);
		return ResponseEntity.ok(orderSlotResponse);
	}

	private Boolean isGraceAllowed() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String orderBlockStartTime = ParamsUtils.getParam("CONSUMER_ORDER_GRACE_WINDOW_START_TIME", "22:50:00");
		String orderBlockEndTime = ParamsUtils.getParam("CONSUMER_ORDER_GRACE_WINDOW_END_TIME", "22:59:59");
		return localTime.isAfter(LocalTime.parse(orderBlockStartTime)) && localTime.isBefore(LocalTime.parse(orderBlockEndTime));
	}

	@ApiOperation(value = "Get order by display-ids", nickname = "getOrderByDisplayIds")
	@GetMapping("/orders/display-ids")
	public ResponseEntity<List<OrderListBean>> getOrderByDisplayIds(@RequestParam Set<String> ids) {
		List<OrderEntity> orderEntities = orderService.findOrdersByDisplayOrderId(new ArrayList<>(ids));
		List<OrderListBean> response = getMapper().mapAsList(orderEntities, OrderListBean.class);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Order Process", nickname = "processOrders")
	@PostMapping("/orders/cart/process")
	@ResponseStatus(HttpStatus.OK)
	public void processOrders(@RequestParam(required = false) String reqDeliveryDate) {
		// TODO -- need to move date logic based time
		Date deliveryDate = null;
		if (reqDeliveryDate != null) {
			deliveryDate = DateUtils.getDate(DateUtils.SHORT_DATE_FMT, reqDeliveryDate);
		} else {
			deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		}
		if (DeliveryDateUtils.isHoliday(deliveryDate, null)) {
			return;
		}
		_LOGGER.info(String.format("processOrders for deliveryDate : %s", deliveryDate.toString()));
		addPrebookSkusToCart(deliveryDate);
		cartService.failInactiveAutoCheckoutOrders(deliveryDate);
		List<OrderEntity> carts = orderService.findAllActiveCarts(new java.sql.Date(deliveryDate.getTime()));
		if (CollectionUtils.isNotEmpty(carts)) {
			String validChannel = ParamsUtils.getParam("VALID_CHANNELS");
			String testUsers = ParamsUtils.getParam("TEST_USERS");
			List<String> validChannelList = List.of(validChannel.split(","));
			Set<String> testUserList = new HashSet<>(List.of(testUsers.split(",")));
			List<OrderEntity> failedOrders = carts.stream().filter(o -> !orderService.validOrderForProcessingRule(o, validChannelList, testUserList))
					.peek(x -> x.setStatus(OrderStatus.ORDER_FAILED)).collect(Collectors.toList());
			carts = carts.stream().filter(o -> !o.getStatus().equals(OrderStatus.ORDER_FAILED)).collect(Collectors.toList());
			boolean addFreeItem = ParamsUtils.getBooleanParam("ADD_FREE_ITEM", Boolean.TRUE);
			if (addFreeItem) {
				addFreeItemsToCarts(carts);
			}
			List<OrderEntity> processedOrders = processCartOrder(carts);
			Map<UUID, ConsumerAddressResponse> addressMap = orderService
					.getAddressMap(processedOrders.stream().map(OrderEntity::getDeliveryAddress).collect(Collectors.toList()));
			if (!processedOrders.isEmpty()) {
				buildAndSendWmsOrderRequest(processedOrders, addressMap);
//				clientService.createFulfilment();
				orderService.saveAllOrders(processedOrders);
				orderService.disableOnboardingOffer(processedOrders);
				orderService.deactivateFirstOrderFlow(processedOrders);
			}
			if (CollectionUtils.isNotEmpty(failedOrders)) {
				orderService.saveAllOrders(failedOrders);
			}
			List<CustomerCartInfo> customerCartInfoList = carts.stream()
					.map(cart -> new CustomerCartInfo(cart.getCustomerId(), cart.getFinalBillAmount(), cart.getActive())).collect(Collectors.toList());
			orderService.buildAndSendClevertapProfileUpdateRequest(customerCartInfoList, 0d);
		}
	}

	private void addPrebookSkusToCart(Date deliveryDate) {
		List<PreBookOrderEntity> preBookOrders = preBookOrderService.findPrebookedOrdersByDeliveryDate(deliveryDate);
		if (CollectionUtils.isEmpty(preBookOrders)) {
			_LOGGER.info(String.format("addPrebookSkusToCart :: No prebook orders exist for date : %s", deliveryDate));
			return;
		}
		List<UUID> orderIds = preBookOrders.stream().map(PreBookOrderEntity::getOrderId).map(UUID::fromString).collect(Collectors.toList());
		List<OrderEntity> originalOrders = orderService.findRecordByIds(orderIds);
		if (CollectionUtils.isEmpty(originalOrders)) {
			_LOGGER.info("addPrebookSkusToCart :: No original orders found for prebook orders.");
			return;
		}
		Map<UUID, OrderEntity> orderIdMap = originalOrders.stream()
				.collect(Collectors.toMap(OrderEntity::getId, Function.identity(), (first, second) -> first));
		Map<String, List<PreBookOrderEntity>> cutomerIdPreBookOrderMap = preBookOrders.stream()
				.collect(Collectors.groupingBy(PreBookOrderEntity::getCustomerId));
		String appVersion = ParamsUtils.getParam("DEFAULT_APP_VERSION", "1.1.081");
		List<PreBookOrderEntity> preBookOrderEntities = new ArrayList<>();
		for (Map.Entry<String, List<PreBookOrderEntity>> entry : cutomerIdPreBookOrderMap.entrySet()) {
			for (PreBookOrderEntity preBookOrder : entry.getValue()) {
				OrderEntity originalOrder = orderIdMap.get(UUID.fromString(preBookOrder.getOrderId()));
				if (Objects.isNull(originalOrder)) {
					return;
				}
				OrderItemEntity originalOrderItem = originalOrder.getOrderItems().stream().filter(item -> item.getSkuCode().equals(preBookOrder.getSkuCode()))
						.findFirst().orElse(null);
				if (Objects.isNull(originalOrderItem)) {
					_LOGGER.error("addPrebookSkusToCart :: originalOrderItem is null");
				}
				CartRequest request = CartRequest.builder().addressId(originalOrder.getDeliveryAddress()).channel(originalOrder.getChannel())
						.notes(originalOrderItem.getMetadata().getNotes()).skuCode(originalOrderItem.getSkuCode())
						.grades(originalOrderItem.getMetadata().getGrades()).pieceQty(originalOrderItem.getMetadata().getPieces())
						.customerId(originalOrder.getCustomerId()).societyId(Math.toIntExact(originalOrder.getMetadata().getSocietyId()))
						.deliveryDate(deliveryDate).quantity(originalOrderItem.getOrderedQty()).time(System.currentTimeMillis())
						.slotId(originalOrder.getSlotId()).storeId(originalOrder.getStoreId()).build();
				Map<String, String> additionalParams = new HashMap<>();
				additionalParams.put("appId", OrderConstants.BACKOFFICE_APP_ID);
				ResponseEntity<CartResponse> cartResponseResponseEntity = addToCartV2(request, appVersion, additionalParams, true);
				if (cartResponseResponseEntity.getStatusCode().equals(HttpStatus.OK)) {
					preBookOrder.setActive(0);
					preBookOrderEntities.add(preBookOrder);
				}
			}
		}
		preBookOrderService.saveAllPrebookOrders(preBookOrderEntities);
	}

	private void addFreeItemsToCarts(List<OrderEntity> carts) {
		String appId = SessionUtils.getAppId();
		String freeItemStr = ParamsUtils.getParam("FREE_ITEM_SKUS");
		String aov = ParamsUtils.getParam("B2C_AOV", "300");
		String freeSkuListStr = ParamsUtils.getParam("B2C_FREE_ITEM_SKU_LIST");
		String orderOfferAppVersion = ParamsUtils.getParam("ORDER_OFFER_APP_VERSION");
		List<String> freeItemSkuList = List.of(freeSkuListStr.split(","));
		Map<String, Double> skuQtyMap = Arrays.stream(freeItemStr.split(",")).map(pair -> pair.split(":"))
				.collect(Collectors.toMap(pair -> pair[0], pair -> Double.valueOf(pair[1])));
		Map<String, StoreInventoryResponse> storeItemResponseMap = new HashMap<>();
		for (OrderEntity cart : carts) {
			String orderAppVersion = cart.getMetadata().getAppVersion();
			if (orderAppVersion != null && cartService.isVersionGreaterOrEqual(orderAppVersion, orderOfferAppVersion)) {
				continue;
			}
			if (isEligibleForFreeItems(cart, aov, freeItemSkuList)) {
				StoreInventoryResponse storeItemResponse = null;
				if (storeItemResponseMap.containsKey(cart.getStoreId())) {
					storeItemResponse = storeItemResponseMap.get(cart.getStoreId());
				} else {
					storeItemResponse = clientService.getStoreInventory(cart.getStoreId(), String.join(",", skuQtyMap.keySet()), null,
							cart.getMetadata().getAppVersion());
					storeItemResponseMap.put(cart.getStoreId(), storeItemResponse);
				}
				for (Map.Entry<String, Double> entry : skuQtyMap.entrySet()) {
					try {
						StoreProductInventory storeItem = storeItemResponse.getInventory().stream().filter(i -> i.getProductSkuCode().equals(entry.getKey()))
								.findFirst().get();
						if (storeItem == null) {
							_LOGGER.error(String.format("addFreeItemsToCarts:: sku : %s not found in store inventory", entry.getKey()));
							continue;
						}
						cartService.addItemToCartV2(cart, storeItem, entry.getValue(), null, storeItem.getProductName(), null, appId, new CartResponse(), null,
								null, false, false, false);
					} catch (Exception e) {
						_LOGGER.error(String.format("addFreeItemsToCarts:: failed for skus %s for order : %s", entry.getKey(), cart.getDisplayOrderId()));
					}
				}
			}
		}
	}

	private boolean isEligibleForFreeItems(OrderEntity cart, String aov, List<String> freeItemSkuList) {
		if (cart.getFinalBillAmount().compareTo(Double.valueOf(aov)) < 0) {
			return false;
		}
		Boolean containsVeggies = Boolean.FALSE;
		Boolean containsFreeItems = Boolean.FALSE;
		for (OrderItemEntity item : cart.getOrderItems()) {
			if (item.getCategoryName().equalsIgnoreCase("Vegetables")) {
				containsVeggies = true;
			}
			if (freeItemSkuList.contains(item.getSkuCode())) {
				containsFreeItems = true;
			}
		}
		if (containsVeggies && !containsFreeItems) {
			return true;
		}
		return false;
	}

	private void buildAndSendWmsOrderRequest(List<OrderEntity> processedOrders, Map<UUID, ConsumerAddressResponse> addressMap) {
		String cloudfrontUrl = ParamsUtils.getParam("CLOUDFRONT_URL").concat("/public/");
		for (OrderEntity order : processedOrders) {
			try {
				sendSingleOrderToWms(addressMap, cloudfrontUrl, order);
			} catch (Exception e) {
				_LOGGER.error(String.format("sendToWmsError::error while sending to WMS: %s", order.getDisplayOrderId()), e);
				order.setStatus(OrderStatus.ORDER_FAILED);
				order.getMetadata().setFailureReason(Collections.singletonList(String.format("Order failed while sending to wms: %s", e.getMessage())));

				String key = orderService.getKey(order);
				String txnType = "Consumer-Order-Failed";
				Double holdAmount = -1 * order.getFinalBillAmount();
				orderService.addOrDeductMoneyFromUserWallet(order.getCustomerId().toString(), null, order.getDisplayOrderId(), txnType, holdAmount, null, key);
			}
		}
	}

	private void sendSingleOrderToWms(Map<UUID, ConsumerAddressResponse> addressMap, String cloudfrontUrl, OrderEntity order) {
		_LOGGER.info(String.format("Build And Send Wms Order Request: %s", order.getDisplayOrderId()));
		ConsumerAddressResponse address = addressMap.get(order.getCustomerId());
		clientService.sendConsumerOrderToWms(orderToWmsOrderPayload(order, address, cloudfrontUrl));
		order.setHasPpdOrder(1);
	}

	public WmsOrderPayload orderToWmsOrderPayload(OrderEntity order, ConsumerAddressResponse address, String cloudfrontUrl) {
		WmsOrderPayload wmsOrder = new WmsOrderPayload();
		wmsOrder.setOrderId(order.getId().toString());
		wmsOrder.setDisplayOrderId(order.getDisplayOrderId());
		wmsOrder.setStoreId(Integer.parseInt(order.getStoreId()));
		wmsOrder.setCustomerId(order.getCustomerId().toString());
		wmsOrder.setDeliveryDate(DateUtils.toString(DateUtils.SHORT_DATE_FMT, order.getDeliveryDate()));
		wmsOrder.setSlot(order.getMetadata().getOrderSlot());
		wmsOrder.setAddress(toWmsAddress(address));
		wmsOrder.setOrderCount(order.getMetadata().getOrderCount());
		wmsOrder.setNotes(order.getNotes());
		wmsOrder.setContactDetails(order.getMetadata().getContactDetail());
		wmsOrder.setIsVip(order.getMetadata().getIsVip());
		List<OrderItemEntity> preBookItems = new ArrayList<>();
		wmsOrder.setAppVersion(order.getMetadata().getAppVersion());
		wmsOrder.setIsCod(order.getMetadata().getIsCod());
		wmsOrder.setItems(order.getOrderItems().stream().filter(item -> {
			boolean isPrebooked = item.getMetadata().getIsPrebooked();
			if (isPrebooked) {
				if (DateUtils.toString(DateUtils.DATE_MM_FMT, order.getDeliveryDate()).equals(item.getMetadata().getPrebookDeliveryDate())) {
					return true;
				}
				preBookItems.add(item);
			}
			return !isPrebooked;
		}).map(orderItem -> orderItemToWmsOrderItem(orderItem, cloudfrontUrl)).collect(Collectors.toList()));
		savePrebookOrder(order, preBookItems);
		return wmsOrder;
	}

	private void savePrebookOrder(OrderEntity order, List<OrderItemEntity> preBookItems) {
		if (CollectionUtils.isNotEmpty(preBookItems)) {
			List<PreBookOrderEntity> preBookOrders = new ArrayList<>();
			for (OrderItemEntity item : preBookItems) {
				PreBookOrderEntity prebookOrder = createPrebookOrder(order, item);
				preBookOrders.add(prebookOrder);
			}
			preBookOrderService.saveAllPrebookOrders(preBookOrders);
		}
	}

	private PreBookOrderEntity createPrebookOrder(OrderEntity order, OrderItemEntity item) {
		PreBookOrderEntity preBookOrderEntity = PreBookOrderEntity.newInstance();
		preBookOrderEntity.setOrderId(order.getId().toString());
		preBookOrderEntity.setCustomerId(order.getCustomerId().toString());
		preBookOrderEntity.setOrderItemId(item.getId().toString());
		preBookOrderEntity.setSkuCode(item.getSkuCode());
		preBookOrderEntity.setPrebookDeliveryDate(DateUtils.getDate(DateUtils.DATE_MM_FMT, item.getMetadata().getPrebookDeliveryDate()));
		return preBookOrderEntity;
	}

	public WmsOrderPayload.Address toWmsAddress(ConsumerAddressResponse address) {
		WmsOrderPayload.Address wmsAddress = new WmsOrderPayload.Address();
		if (address != null) {
			wmsAddress.setStreet(address.getStreet());
			wmsAddress.setCity(address.getCity());
			wmsAddress.setState(address.getState());
			if (address.getPincode() != null) {
				wmsAddress.setPincode(address.getPincode().toString());
			}
			wmsAddress.setAddressLine1(address.getAddressLine1());
			wmsAddress.setAddressLine2(address.getAddressLine2());
			wmsAddress.setLandmark(address.getLandmark());
			if (address.getLatitude() != null && address.getLongitude() != null) {
				wmsAddress.setLatitude(address.getLatitude().toString());
				wmsAddress.setLongitude(address.getLongitude().toString());
			}
			wmsAddress.setSocietyId(address.getSocietyId());
			wmsAddress.setSociety(address.getSociety());
			wmsAddress.setFloor(address.getFloor());
			wmsAddress.setHouse(address.getHouse());
			wmsAddress.setSector(address.getSector());
			wmsAddress.setTower(address.getTower());
		}
		return wmsAddress;
	}

	public WmsOrderPayload.WmsOrderItem orderItemToWmsOrderItem(OrderItemEntity orderItem, String cloudfrontUrl) {
		WmsOrderPayload.WmsOrderItem wmsOrderItem = new WmsOrderPayload.WmsOrderItem();
		wmsOrderItem.setId(orderItem.getId().toString());
		wmsOrderItem.setSkuCode(orderItem.getSkuCode());
		wmsOrderItem.setProductName(orderItem.getProductName());
		wmsOrderItem.setProductImage(cloudfrontUrl.concat(orderItem.getImageUrl()));
		wmsOrderItem.setUom(orderItem.getUom());
		wmsOrderItem.setQty(orderItem.getFinalQuantity());
		wmsOrderItem.setGrades(orderItem.getMetadata().getGrades());
		wmsOrderItem.setSuffix(orderItem.getMetadata().getSuffix());
		wmsOrderItem.setPieces(orderItem.getMetadata().getPieces());
		wmsOrderItem.setPerPcWeight(orderItem.getMetadata().getPerPiecesWeight());
		wmsOrderItem.setNotes(orderItem.getMetadata().getNotes());
		wmsOrderItem.setPacketDescription(orderItem.getMetadata().getPacketDescription());
		wmsOrderItem.setIsOzoneWashedItem(orderItem.getMetadata().getIsOzoneWashedItem());
		return wmsOrderItem;
	}

	private List<OrderEntity> processCartOrder(List<OrderEntity> carts) {
		List<OrderEntity> processedOrders = new ArrayList<>();
		for (OrderEntity cart : carts) {
			try {
				_LOGGER.debug(String.format("processCartOrder::orderId: %s", cart.getDisplayOrderId()));
				if (cart.getFinalBillAmount().compareTo(0d) != 0) {
					String key = orderService.getKey(cart);
					orderService.addOrDeductMoneyFromUserWallet(cart.getCustomerId().toString(), null, cart.getDisplayOrderId(), "Consumer-Order",
							cart.getFinalBillAmount(), null, key);
				}
				updatePlaceOrderDetails(cart, cart.getFinalBillAmount(), PaymentMethod.WALLET);
				processedOrders.add(cart);
			} catch (Exception e) {
				_LOGGER.error(String.format("processCartOrder::skipping order: %s", cart.getDisplayOrderId()), e);
			}
		}
		return processedOrders;
	}

	@ApiOperation(value = "Get User Order details to create ticket", nickname = "getUserOrdersForTicket")
	@GetMapping("/orders/user/{id}")
	public ResponseEntity<List<OrderResponseBean>> getUserOrdersForTicket(@PathVariable UUID id) {
		_LOGGER.debug(String.format("GET USER PREVIOUS ORDERS : userId = %s", id));
		java.sql.Date deliveryDate = deliveryDateUtils.getDeliveryDateForPreviousOrder(ParamsUtils.getIntegerParam("CONSUMER_ORDERS_TICKET_LIST_DAYS", 10) - 1);
		List<OrderEntity> orders = orderService.getOrdersForTickets(id, deliveryDate);
		if (CollectionUtils.isEmpty(orders)) {
			return ResponseEntity.ok(new ArrayList<>());
		}
		List<OrderResponseBean> orderListResponse = getMapper().mapAsList(orders, OrderResponseBean.class);
		return ResponseEntity.ok(orderListResponse);
	}

	private void updatePlaceOrderDetails(OrderEntity cart, Double amountReceived, PaymentMethod paymentMethod) {
		cart.setPaymentMethod(paymentMethod);
		cart.setAmountReceived(amountReceived);
		cart.setStatus(OrderStatus.NEW_ORDER);
		cart.setAmountReceived(amountReceived);
		cart.getMetadata().setOrderPlacedAmount(BigDecimal.valueOf(cart.getFinalBillAmount()));
		cart.setSubmittedAt(new Date());
		orderService.save(cart);
	}

	@ApiOperation(value = "Order Previous Item History", nickname = "order previous item history")
	@GetMapping("/orders/item/previous")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<PreviousOrderItemBean>> getPreviouslyOrderedSkus() throws JsonMappingException, JsonProcessingException {
		UUID userId = SessionUtils.getAuthUserId();
		List<PreviousOrderItemBean> response = orderService.getPreviouslyOrderedSkus(userId);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Order Resend Failed PPD Orders And Re Create Fulfilment", nickname = "reprocessOrdersByDate")
	@PostMapping("/orders/cart/re-process")
	@ResponseStatus(HttpStatus.OK)
	public void reprocessOrdersByDate(@RequestParam java.sql.Date deliveryDate) {
		List<OrderEntity> toBeReProcessedOrders = orderService.findToBeReProcessedOrderByDate(deliveryDate);
		if (CollectionUtils.isNotEmpty(toBeReProcessedOrders)) {
			Map<UUID, ConsumerAddressResponse> addressMap = orderService
					.getAddressMap(toBeReProcessedOrders.stream().map(OrderEntity::getDeliveryAddress).collect(Collectors.toList()));
			buildAndSendWmsOrderRequest(toBeReProcessedOrders, addressMap);
			orderService.saveAllOrders(toBeReProcessedOrders);
		}
//		clientService.createFulfilment();
	}

	@ApiOperation(value = "List all customers having order today", nickname = "findAllCustomersHavingOrderByCurrentDeliveryDate")
	@GetMapping("/orders/customer/today")
	public ResponseEntity<List<OrderBeanV2>> findAllCustomersHavingOrderByCurrentDeliveryDate() {
		List<OrderEntity> results = orderService.findAllOrdersByCurrentDeliveryDate();
		List<OrderBeanV2> orderBeanV2s = CollectionUtils.isNotEmpty(results) ? getMapper().mapAsList(results, OrderBeanV2.class) : new ArrayList<>();
		return ResponseEntity.ok(orderBeanV2s);
	}

	@ApiOperation(value = "Franchise Re-Bill Final Quantity", nickname = "franchiseOrderReBilling")
	@PostMapping("/orders/{orderId}/re-bill")
	public ResponseEntity<OrderResponseBean> orderReBilling(@Valid @RequestBody List<PpdOrderItemBean> request, @PathVariable UUID orderId) {
		_LOGGER.info(String.format("orderReBilling:: orderId: %s", orderId));
		OrderEntity order = getOrder(orderId);
		List<OrderStatus> validStatus = List.of(OrderStatus.NEW_ORDER, OrderStatus.ORDER_BILLED, OrderStatus.ORDER_DELIVERED,
				OrderStatus.ORDER_OUT_FOR_DELIVERY);
		if (!validStatus.contains(order.getStatus())) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Order is in status : %s or invoiced so can not be re-billed", order.getStatus()), "order"));
		}
		if (order.getStatus().equals(OrderStatus.NEW_ORDER)) {
			order = orderService.updateOrderEntityFromPPD(order, request);
		} else {
			order = orderService.reBillOrder(request, order);
		}
		orderService.itemsAvailable(order);
		generateInvoice(order);
		generateBill(order);
		OrderResponseBean response = getMapper().mapSrcToDest(order, OrderResponseBean.newInstance());
		saveOrderEventLog(order, request, response);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Update current cart address of user", nickname = "updateCurrentCartAddress")
	@PostMapping("/orders/cart/address")
	@Transactional
	public void updateCurrentCartAddress(@Valid @RequestBody UpdateOrderAddress request) {
		Date deliveryDate = cartService.getDeliveryDate(null);
		OrderEntity cart = orderService.findCustomerCurrentCart(request.getCustomerId(), deliveryDate);
		if (cart == null) {
			throw new ValidationException(ErrorBean.withError("no_cart", "No cart found", "cart"));
		}
		cart.setDeliveryAddress(request.getAddressId());
		orderService.save(cart);
	}

	@ApiOperation(value = "Update current cart slot of user", nickname = "updateCurrentCartSlot")
	@PostMapping("/orders/cart/slot")
	@Transactional
	public ResponseEntity<Void> updateCurrentCartSlot(@Valid @RequestBody UpdateOrderSlot request) {
		Date deliveryDate = cartService.getDeliveryDate(null);
		OrderEntity cart = orderService.findCustomerCurrentCart(request.getCustomerId(), deliveryDate);
		if (cart == null) {
			return ResponseEntity.noContent().build();
		}
		cartService.setValidSlotInOrder(cart, request.getSlotId(), null, request.getSocietyId());
		orderService.save(cart);
		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "Update delivery delay time", nickname = "updateDeliveryDelayTime")
	@PostMapping("/orders/delivery-delay")
	@Transactional
	public void updateDeliveryDelayTime(@RequestBody OrderDelayUpdateReq request) {
		_LOGGER.info(String.format("updateDeliveryDelayTime:: request: %s", request));
		List<OrderEntity> orders = orderService.findRecordByIds(request.getOrderIds());
		orders = orderService.updateDeliveryDelayMetaData(orders, request.getDeliveryDelayTime(), request.getDeliveryDelayReason());
		orderService.saveAllOrders(orders);
	}

	@ApiOperation(value = "Order Process in wave", nickname = "processCartWave")
	@PostMapping("/orders/cart/wave")
	@ResponseStatus(HttpStatus.OK)
	public void processCartWave() {
		_LOGGER.info("processCartWave :: triggered");
		Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		if (DeliveryDateUtils.isHoliday(deliveryDate, null)) {
			return;
		}
		java.sql.Date sqlDeliveryDate = new java.sql.Date(deliveryDate.getTime());
		List<OrderEntity> orders = orderService.getOrdersForDsrByDeliveryDate(sqlDeliveryDate);
		if (CollectionUtils.isNotEmpty(orders)) {
			String validChannel = ParamsUtils.getParam("VALID_CHANNELS");
			String testUsers = ParamsUtils.getParam("TEST_USERS");
			List<String> validChannelList = List.of(validChannel.split(","));
			Set<String> testUserList = new HashSet<>(List.of(testUsers.split(",")));
			List<OrderEntity> validOrders = orders.stream().filter(o -> orderService.validOrderForProcessingRule(o, validChannelList, testUserList))
					.collect(Collectors.toList());
			List<StoreRequisitionBean> storeRequisitions = new ArrayList<>();
			validOrders.forEach(o -> o.getOrderItems().forEach(item -> storeRequisitions.add(new StoreRequisitionBean(item.getSkuCode(),
					Objects.equals(o.getStoreId(), OrderConstants.STORE_PS_3) ? OrderConstants.STORE_PS_2 : o.getStoreId(), item.getUom(),
					item.getProductName(), item.getFinalQuantity(), sqlDeliveryDate, item.getMetadata()))));
			if (CollectionUtils.isNotEmpty(storeRequisitions)) {
				orderService.createAndSendWmsDsPayload(storeRequisitions, deliveryDate);
			}
		}
	}

	@ApiOperation(value = "Update vip order", nickname = "updateToVipOrder")
	@PostMapping("/orders/mark-vip")
	public ResponseEntity<Void> markVipOrder(@RequestBody VipOrderBean request) {
		Date deliveryDate = cartService.getDeliveryDate(null);
		OrderEntity cart = orderService.findCustomerCurrentCart(request.getCustomerId(), deliveryDate);
		if (cart != null) {
			cartService.checkIfVipOrder(cart.getMetadata().getOrderCount().toString(), request.getVipOrderNum(), cart);
			orderService.save(cart);
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/orders/users/clevertap-profile-update")
	public ResponseEntity<Void> updateClevertapUserProfile() throws ParseException {
		_LOGGER.info("updateClevertapUserProfile :: triggered");
		Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		Integer interval = ParamsUtils.getIntegerParam(ClevertapConstants.CLEVERTAP_PROFILE_UPDATE_CRON_INTERVAL, 15);
		String lastExecutionTimeStr = ParamsUtils.getParam(ClevertapConstants.CLEVERTAP_CRON_LAST_SYNCED_TILL,
				DateUtils.toString(DateUtils.DATE_FMT_WITH_TIME, new Date()));
		Date startTime = DateUtils.getDate(DateUtils.DATE_FMT_WITH_TIME, lastExecutionTimeStr);
		Date endTime = DateUtils.addMinutes(new Date(), -interval);
		int page = 0;
		int size = 500;
		Page<CustomerCartInfo> cartInfoPage = null;
		do {
			Pageable pageable = PageRequest.of(page, size);
			cartInfoPage = orderService.findCustomerCartInfoBetween(startTime, endTime, deliveryDate, pageable);
			List<CustomerCartInfo> cartInfos = cartInfoPage.getContent();
			if (CollectionUtils.isNotEmpty(cartInfos)) {
				orderService.buildAndSendClevertapProfileUpdateRequest(cartInfos, null);
			}
			page++;
		} while (cartInfoPage.hasNext());
		ParamsUtils.updateParam(ClevertapConstants.CLEVERTAP_CRON_LAST_SYNCED_TILL, DateUtils.toString(DateUtils.DATE_FMT_WITH_TIME, endTime));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/orders/per-pcs-wt")
	public ResponseEntity<Void> updatePerPcsWeight() {
		orderService.updatePerPcsWeight();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/orders/notify/enable-auto-checkout")
	public void notifyAutoCheckoutEnable() {
		Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		List<OrderEntity> orders = orderService.findInactiveAutoCheckoutOrders(deliveryDate, new Date());
		orderService.notifyCustomerToEnableAutoCheckout(orders);
	}

	@PostMapping("/orders/fail/inactive-auto-checkout")
	public void failInactiveAutoCheckoutOrders() {
		Integer minutes = ParamsUtils.getIntegerParam("AUTO_CHECKOUT_INACTIVE_MINUTES", 20);
		LocalDateTime createdAt = LocalDateTime.now().minusMinutes(minutes);
		Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		List<OrderEntity> orders = orderService.findInactiveAutoCheckoutOrders(deliveryDate, new Date(createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()));
		cartService.failOrdersAndUpdateInventory(orders);
	}

	@PostMapping(value = "/orders/bulk-a2c/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvUploadResult<BulkA2cUploadBean> uploadBulkA2C(@RequestParam("file") MultipartFile file, @RequestParam("items") String itemsJson) throws JsonProcessingException {
		final int maxAllowedRows = 500;
		final String module = "bulk-a2c";
		ObjectMapper objectMapper = new ObjectMapper();
		List<BulkA2cItemRequest> items = objectMapper.readValue(itemsJson, new TypeReference<List<BulkA2cItemRequest>>() {});
		List<BulkA2cSheetBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, BulkA2cSheetBean.newInstance());
		List<BulkA2cSheetBean> preProcessedBeans = cartService.preProcessBulkA2cUpload(rawBeans);
		CsvUploadResult<BulkA2cUploadBean> result = validateBulkA2cUpload(preProcessedBeans, items);
		result.setHeaderMapping(preProcessedBeans.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(SessionUtils.getAuthUserId(), module, result);
		return result;
	}

	@PostMapping("/orders/bulk-a2c/save")
	public ResponseEntity<List<BulkA2cResponse>> saveBulkA2c(@RequestParam(name = "key") String key,
			@RequestParam(name = "cancel", required = false) Integer cancel) {
		if (cancel != null) {
			cancelUpload(key);
			return ResponseEntity.ok(new ArrayList<>());
		}
		final CSVBulkRequest<BulkA2cUploadBean> uploadedData = CsvUtils.getBulkRequestData(key, BulkA2cUploadBean.class);
		List<BulkA2cResponse> response = new ArrayList<>();
		if (uploadedData != null && CollectionUtils.isNotEmpty(uploadedData.getData())) {
			response = cartService.saveBulkA2cData(uploadedData.getData());
		}
		CsvUtils.markUploadProcessed(key);
		return ResponseEntity.ok(response);
	}

	private CsvUploadResult<BulkA2cUploadBean> validateBulkA2cUpload(List<BulkA2cSheetBean> beans, List<BulkA2cItemRequest> items) {
		CsvUploadResult<BulkA2cUploadBean> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			if (CollectionUtils.isNotEmpty(items)) {
				for (BulkA2cSheetBean bean : beans) {
					BulkA2cUploadBean uploadBean = new BulkA2cUploadBean();
					uploadBean.setItemNotes(bean.getItemNotes());
					uploadBean.setUserId(bean.getUserId());
					uploadBean.setItems(items);
					try {
						org.springframework.validation.Errors errors = getSpringErrors(uploadBean);
						cartService.validateBulkA2cDataOnUpload(uploadBean, errors);
						checkError(errors);
						result.addSuccessRow(uploadBean);
					} catch (final Exception e) {
						if (CollectionUtils.isEmpty(bean.getErrors())) {
							_LOGGER.error(e.getMessage(), e);
							final List<ErrorBean> errors = e instanceof ValidationException ? BeanValidationUtils.prepareValidationResponse(
									(ValidationException) e).getErrors() : List.of(ErrorBean.withError("ERROR", e.getMessage(), null));
							bean.addErrors(errors);
							_LOGGER.info("Bulk A2C uploaded data is having error =>" + errors.toString());
						}
						result.addFailedRow(uploadBean);
					}
				}
			}
		}
		return result;
	}

}
