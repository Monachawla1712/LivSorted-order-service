package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.BeanValidationUtils;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CSVBulkRequest;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.order.beans.BackofficeOrderAdjustmentRequest;
import com.sorted.rest.services.order.beans.ConfirmOrderAdjustmentRequest;
import com.sorted.rest.services.order.beans.OrderAdjustmentResponseBean;
import com.sorted.rest.services.order.beans.UserServiceResponse;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.OrderAdjustmentEntity;
import com.sorted.rest.services.order.services.FranchiseOrderService;
import com.sorted.rest.services.order.services.InvoiceService;
import com.sorted.rest.services.order.services.OrderAdjustmentsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Order Adjustment Service", description = "Manage order adjustment services")
public class OrderAdjustmentsController implements BaseController {

	private final AppLogger _LOGGER = LoggingManager.getLogger(OrderAdjustmentsController.class);

	@Autowired
	private OrderAdjustmentsService orderAdjustmentsService;

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	ClientService clientService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "Rejection Order Quantity Upload", nickname = "uploadRejectionOrderQuantity")
	@PostMapping("/orders/adjustment/backoffice/upload")
	public CsvUploadResult<BackofficeOrderAdjustmentRequest> uploadOrderAdjustmentsSheet(@RequestParam("file") MultipartFile file) {
		final int maxAllowedRows = 1000;
		final String module = "order-adjustments";
		List<BackofficeOrderAdjustmentRequest> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, BackofficeOrderAdjustmentRequest.newInstance());
		List<BackofficeOrderAdjustmentRequest> response = orderAdjustmentsService.preProcessOrderAdjustmentsUpload(rawBeans);
		CsvUploadResult<BackofficeOrderAdjustmentRequest> csvUploadResult = validateOrderAdjustmentsUpload(response);
		csvUploadResult.setHeaderMapping(response.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(orderAdjustmentsService.getUserId(), module, csvUploadResult);
		return csvUploadResult;
	}

	private CsvUploadResult<BackofficeOrderAdjustmentRequest> validateOrderAdjustmentsUpload(List<BackofficeOrderAdjustmentRequest> beans) {
		final CsvUploadResult<BackofficeOrderAdjustmentRequest> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			for (BackofficeOrderAdjustmentRequest bean : beans) {
				try {
					org.springframework.validation.Errors errors = getSpringErrors(bean);
					orderAdjustmentsService.validateOrderAdjustmentsOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Collections.singletonList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
					}
					result.addFailedRow(bean);
				}
			}
		}
		return result;
	}

	@ApiOperation(value = "upload Franchise Order Backoffice", nickname = "uploadFranchiseOrderBackoffice")
	@PostMapping(path = "/orders/adjustment/backoffice")
	@ResponseStatus(HttpStatus.OK)
	public void uploadOrderAdjustmentsBackOffice(@RequestParam(name = "key", required = true) String key,
			@RequestParam(name = "cancel", required = false) Integer cancel) {
		final boolean cleanup = cancel != null;
		if (cleanup) {
			cancelUpload(key);
		} else {
			saveOrderAdjustments(key);
		}
	}

	public void cancelUpload(String key) {
		final int deleteCount = CsvUtils.cancelUpload(key);
		_LOGGER.info(String.format("Upload Cancel called with Key = %s and delete count is = %s", key, deleteCount));
		if (deleteCount <= 0) {
			throw new ValidationException(ErrorBean.withError("UPLOAD_CANCEL_ERROR", "Unable to cancel bulk upload request.", null));
		}
	}

	private void saveOrderAdjustments(String key) {
		final CSVBulkRequest<BackofficeOrderAdjustmentRequest> uploadedData = CsvUtils.getBulkRequestData(key, BackofficeOrderAdjustmentRequest.class);
		if (uploadedData != null && CollectionUtils.isNotEmpty(uploadedData.getData())) {
			List<BackofficeOrderAdjustmentRequest> adjustments = uploadedData.getData();
			orderAdjustmentsService.createOrderAdjustments(adjustments);
			CsvUtils.markUploadProcessed(key);
		} else {
			throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Uploaded data not found or it is expired.", null));
		}
	}

	@ApiOperation(value = "Rejection Order Pricing Upload", nickname = "uploadRejectionOrderPricing")
	@PostMapping("/orders/adjustment/backoffice/confirm")
	@ResponseStatus(HttpStatus.OK)
	public void confirmOrderAdjustments(@Valid @RequestBody ConfirmOrderAdjustmentRequest request) {
		final Map<String, Object> params = getOrderAdjustmentFiltersMap(FranchiseOrderConstants.OrderAdjustmentStatus.PENDING, null,
				request.getAdjustmentIds());
		List<OrderAdjustmentEntity> adjustmentList = orderAdjustmentsService.findAllRecords(params);
		if (adjustmentList == null || adjustmentList.isEmpty()) {
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", "No Adjustments found to process.", "adjustmentIds"));
		}
		UUID userId = orderAdjustmentsService.getUserId();
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(userId);
		if (Objects.equals(request.getAction(), "APPROVE")) {
			List<String> displayOrderIdList = adjustmentList.stream().map(OrderAdjustmentEntity::getDisplayOrderId).distinct().collect(Collectors.toList());
			List<FranchiseOrderEntity> orders = franchiseOrderService.findOrdersByDisplayOrderId(displayOrderIdList);
			Map<String, FranchiseOrderEntity> displayOrderIdToOrderMap = orders.stream()
					.collect(Collectors.toMap(FranchiseOrderEntity::getDisplayOrderId, Function.identity()));
			Map<String, List<OrderAdjustmentEntity>> adjustmentsMap = new HashMap<>();
			for (OrderAdjustmentEntity adjustment : adjustmentList) {
				Double refundAmount = adjustment.getAmount();
				if (Objects.equals(adjustment.getTxnMode(), FranchiseOrderConstants.OrderAdjustmentTransactionMode.DEBIT.toString())) {
					refundAmount = -1 * refundAmount;
				}
				franchiseOrderService.deductMoneyFromStoreWallet(displayOrderIdToOrderMap.get(adjustment.getDisplayOrderId()).getStoreId(), refundAmount,
						adjustment.getDisplayOrderId(), adjustment.getTxnType(), null, adjustment.getRemarks(), null);
				orderAdjustmentsService.changeAdjustmentStatus(adjustment, FranchiseOrderConstants.OrderAdjustmentStatus.APPROVED, userDetails);
				if (adjustmentsMap.containsKey(adjustment.getDisplayOrderId())) {
					adjustmentsMap.get(adjustment.getDisplayOrderId()).add(adjustment);
				} else {
					List<OrderAdjustmentEntity> newAdjustmentList = new ArrayList<>();
					newAdjustmentList.add(adjustment);
					adjustmentsMap.put(adjustment.getDisplayOrderId(), newAdjustmentList);
				}
			}
			for (FranchiseOrderEntity order : orders) {
				if (order.getInvoice() != null) {
					invoiceService.generatePaymentNotes(order, adjustmentsMap.get(order.getDisplayOrderId()));
				}
			}
		} else if (Objects.equals(request.getAction(), "REJECT")) {
			for (OrderAdjustmentEntity adjustment : adjustmentList) {
				orderAdjustmentsService.changeAdjustmentStatus(adjustment, FranchiseOrderConstants.OrderAdjustmentStatus.REJECTED, userDetails);
			}
		} else {
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", "invalid action.", "action"));
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	public Map<String, Object> getOrderAdjustmentFiltersMap(FranchiseOrderConstants.OrderAdjustmentStatus status, String displayOrderId, List<Long> ids) {
		final Map<String, Object> params = new HashMap<>();
		if (status != null) {
			params.put("status", status);
		}
		if (displayOrderId != null) {
			params.put("displayOrderId", displayOrderId);
		}
		if (ids != null) {
			params.put("id", ids);
		}
		return params;
	}

	@ApiOperation(value = "View rejection orders paginated list", nickname = "viewRejectionOrdersList")
	@GetMapping("/orders/adjustment/backoffice")
	public PageAndSortResult<OrderAdjustmentResponseBean> viewNewRejectionOrderInfo(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "5") Integer pageSize, @RequestParam(required = false) FranchiseOrderConstants.OrderAdjustmentStatus status,
			@RequestParam(required = false) String displayOrderId) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		final Map<String, Object> params = getOrderAdjustmentFiltersMap(status, displayOrderId, null);
		PageAndSortResult<OrderAdjustmentEntity> rejectionOrdersList = orderAdjustmentsService.findRejectionOrdersByPage(pageSize, pageNo, params, sort);
		PageAndSortResult<OrderAdjustmentResponseBean> response = new PageAndSortResult<>();
		if (rejectionOrdersList != null && rejectionOrdersList.getData() != null) {
			response = prepareResponsePageData(rejectionOrdersList, OrderAdjustmentResponseBean.class);
		}
		return response;
	}
}