package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.BeanValidationUtils;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import com.sorted.rest.services.order.entity.RejectionOrderEntity;
import com.sorted.rest.services.order.entity.RejectionOrderItemEntity;
import com.sorted.rest.services.order.services.DisplayOrderIdService;
import com.sorted.rest.services.order.services.FranchiseOrderService;
import com.sorted.rest.services.order.services.RejectionOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Rejection Order Services", description = "Manage rejection order services.")
public class RejectionOrderController implements BaseController {

	private final AppLogger _LOGGER = LoggingManager.getLogger(RejectionOrderController.class);

	@Autowired
	private RejectionOrderService rejectionOrderService;

	@Autowired
	DisplayOrderIdService displayOrderIdService;

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "Rejection Order Quantity Upload", nickname = "uploadRejectionOrderQuantity")
	@PostMapping("/orders/rejection/backoffice/quantity-upload")
	public CsvUploadResult<BackofficeRejectionOrderRequest> uploadRejectionOrderQuantitySheet(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "storeId") String storeId, @RequestParam(value = "whId") Integer whId, @RequestParam(value = "customerId") UUID customerId) {
		final int maxAllowedRows = 1000;
		final String module = "rejection-order";
		List<BackofficeRejectionOrderRequest> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, BackofficeRejectionOrderRequest.newInstance());
		List<BackofficeRejectionOrderRequest> response = rejectionOrderService.preProcessRejectionOrderUpload(rawBeans, whId);
		CsvUploadResult<BackofficeRejectionOrderRequest> csvUploadResult = validateRejectionOrderQuantityUpload(response);
		csvUploadResult.setHeaderMapping(response.get(0).getHeaderMapping());
		return csvUploadResult;
	}

	@ApiOperation(value = "upload Franchise Order Backoffice", nickname = "uploadFranchiseOrderBackoffice")
	@PostMapping(path = "/orders/rejection/backoffice/place-order")
	@Transactional
	public ResponseEntity<RejectionOrderResponseBean> uploadFranchiseOrderBackOffice(@Valid @RequestBody BackofficeRejectionOrderBean request) {
		RejectionOrderEntity order = rejectionOrderService.createRejectionOrderWithQuantities(request);
		RejectionOrderResponseBean response = getMapper().mapSrcToDest(order, RejectionOrderResponseBean.newInstance());
		return ResponseEntity.ok(response);
	}

	private CsvUploadResult<BackofficeRejectionOrderRequest> validateRejectionOrderQuantityUpload(List<BackofficeRejectionOrderRequest> beans) {
		final CsvUploadResult<BackofficeRejectionOrderRequest> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			for (BackofficeRejectionOrderRequest bean : beans) {
				try {
					org.springframework.validation.Errors errors = getSpringErrors(bean);
					rejectionOrderService.validateRejectionInventoryOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Collections.singletonList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.error("Rejection Order Uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(bean);
				}
			}
		}
		return result;
	}

	@ApiOperation(value = "Rejection Order Pricing Upload", nickname = "uploadRejectionOrderPricing")
	@PostMapping("/orders/rejection/backoffice/{orderId}/pricing-upload")
	public CsvUploadResult<BackofficeRejectionOrderPricingRequest> uploadRejectionOrderPricingSheet(@RequestParam("file") MultipartFile file,
			@PathVariable UUID orderId) {
		final int maxAllowedRows = 1000;
		final String module = "rejection-order";
		RejectionOrderEntity order = rejectionOrderService.getOrderById(orderId);
		validateOrderStatus(order);
		List<BackofficeRejectionOrderPricingRequest> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows,
				BackofficeRejectionOrderPricingRequest.newInstance());
		List<BackofficeRejectionOrderPricingRequest> response = rejectionOrderService.preProcessRejectionOrderPricingUpload(rawBeans, orderId);
		CsvUploadResult<BackofficeRejectionOrderPricingRequest> csvUploadResult = validateRejectionOrderPricingUpload(response);
		csvUploadResult.setHeaderMapping(response.get(0).getHeaderMapping());
		return csvUploadResult;
	}

	private CsvUploadResult<BackofficeRejectionOrderPricingRequest> validateRejectionOrderPricingUpload(List<BackofficeRejectionOrderPricingRequest> beans) {
		final CsvUploadResult<BackofficeRejectionOrderPricingRequest> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			for (BackofficeRejectionOrderPricingRequest bean : beans) {
				try {
					org.springframework.validation.Errors errors = getSpringErrors(bean);
					rejectionOrderService.validateRejectionPricingOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Collections.singletonList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.error("Rejection Order Uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(bean);
				}
			}
		}
		return result;
	}

	@ApiOperation(value = "Rejection Order Pricing Upload", nickname = "uploadRejectionOrderPricing")
	@PostMapping("/orders/rejection/backoffice/pricing")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<RejectionOrderCartResponse> uploadRejectionOrderPricingSheet(
			@Valid @RequestBody BackofficeRejectionOrderPricingBean backofficeRejectionOrderPricingBean) {
		RejectionOrderCartResponse response = new RejectionOrderCartResponse();
		RejectionOrderEntity order = rejectionOrderService.getOrderById(backofficeRejectionOrderPricingBean.getOrderId());
		validateOrderStatus(order);
		Map<String, Double> skuCodeToSalePriceMap = backofficeRejectionOrderPricingBean.getSkuPriceList().stream()
				.collect(Collectors.toMap(BackofficeRejectionOrderPricingRequest::getSkuCode, BackofficeRejectionOrderPricingRequest::getSalePrice));
		matchRequestAndOrderItems(order, skuCodeToSalePriceMap);
		order.setSubmittedAt(new Date());
		order = rejectionOrderService.billFranchiseOrderWithPricingList(backofficeRejectionOrderPricingBean.getSkuPriceList(), order);
		rejectionOrderService.deductWhRejectionInventory(order);
		clientService.makeCashCollectionEntry(
				buildCashCollectionRequest(order.getStoreId(), order.getFinalBillAmount(), OrderConstants.CashCollectionStatus.RECEIVED, "Payment-CASH",
						SessionUtils.getAuthUserId()));
		franchiseOrderService.deductMoneyFromStoreWallet(order.getStoreId(), -1 * order.getFinalBillAmount(), order.getDisplayOrderId(), "Rejection-Order",
				null, null, null);
		RejectionOrderResponseBean orderResponse = getMapper().mapSrcToDest(order, RejectionOrderResponseBean.newInstance());
		response.setData(orderResponse);
		return ResponseEntity.ok(response);
	}

	private CashCollectionRequest buildCashCollectionRequest(String storeId, Double amount, OrderConstants.CashCollectionStatus cashCollectionStatus,
			String txnMode, UUID userId) {
		CashCollectionRequest cashCollectionRequest = new CashCollectionRequest();
		cashCollectionRequest.setUserId(userId);
		cashCollectionRequest.setStatus(cashCollectionStatus);
		cashCollectionRequest.setStoreId(storeId);
		cashCollectionRequest.setBillAmount(amount);
		cashCollectionRequest.setAmount(amount);
		cashCollectionRequest.setTxnMode(txnMode);
		return cashCollectionRequest;
	}

	private void matchRequestAndOrderItems(RejectionOrderEntity rejectionOrder, Map<String, Double> skuCodeToSalePriceMap) {
		if (rejectionOrder.getOrderItems().size() != skuCodeToSalePriceMap.size()) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order request items missing in pricing sheet ", "order"));
		}
		for (RejectionOrderItemEntity item : rejectionOrder.getOrderItems()) {
			if (!skuCodeToSalePriceMap.containsKey(item.getSkuCode())) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order request items do not match pricing sheet items", "order"));
			}
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	@ApiOperation(value = "View rejection order from order id.", nickname = "ViewRejectionOrderInfo")
	@GetMapping("/orders/rejection/backoffice/{orderId}")
	public ResponseEntity<RejectionOrderResponseBean> viewRejectionOrderInfo(@PathVariable UUID orderId) {
		_LOGGER.debug(String.format("View Rejection ORDER Response : orderId = %s", orderId));
		RejectionOrderEntity entity = rejectionOrderService.findRejectionOrderById(orderId);
		if (entity == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(entity, RejectionOrderResponseBean.newInstance()));
	}

	@ApiOperation(value = "View rejection orders paginated list", nickname = "viewRejectionOrdersList")
	@GetMapping("/orders/rejection/backoffice")
	public PageAndSortResult<RejectionOrderListBean> viewNewRejectionOrderInfo(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "100") Integer pageSize, @RequestParam(required = false) Integer status) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		final Map<String, Object> params = new HashMap<>();
		PageAndSortResult<RejectionOrderEntity> rejectionOrdersList = rejectionOrderService.findRejectionOrdersByPage(pageSize, pageNo, params, sort);
		PageAndSortResult<RejectionOrderListBean> response = new PageAndSortResult<>();
		if (rejectionOrdersList != null && rejectionOrdersList.getData() != null) {
			response = prepareResponsePageData(rejectionOrdersList, RejectionOrderListBean.class);
		}
		return response;
	}

	@ApiOperation(value = "Cancel Rejection Order", nickname = "CancelRejectionOrder")
	@PostMapping("/orders/rejection/backoffice/{orderId}/cancel")
	public ResponseEntity<Void> cancelFranchiseOrder(@PathVariable UUID orderId) {
		_LOGGER.info(String.format("CancelRejectionOrder:: orderId: %s", orderId));
		RejectionOrderEntity order = rejectionOrderService.findRecordById(orderId);
		validateOrderStatus(order);
		rejectionOrderService.cancelRejectionOrder(order);
		return ResponseEntity.ok().build();
	}

	private void validateOrderStatus(RejectionOrderEntity order) {
		if (Objects.isNull(order)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found", "order"));
		}
		if (!order.getStatus().equals(RejectionOrderConstants.RejectionOrderStatus.NEW_ORDER)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order not in New Order Status", "order"));
		}
	}
}