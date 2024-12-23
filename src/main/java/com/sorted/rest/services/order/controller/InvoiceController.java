package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.SRType;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.services.FranchiseOrderService;
import com.sorted.rest.services.order.services.InvoiceService;
import com.sorted.rest.services.order.services.OrderService;
import com.sorted.rest.services.order.services.ZIPService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Invoice Services", description = "Manage Invoice related services.")
public class InvoiceController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(InvoiceController.class);

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private ZIPService zipService;

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ClientService clientService;

	@ApiOperation(value = "Create franchise invoice", nickname = "createFranchiseInvoice")
	@PostMapping("/orders/invoices/franchises")
	public void createFranchiseInvoices() {
		List<FranchiseOrderEntity> franchiseOrders = franchiseOrderService.findAllDeliveredOrders();
		Set<String> storeIds = franchiseOrders.stream().map(FranchiseOrderEntity::getStoreId).collect(Collectors.toSet());
		invoiceService.generateFranchiseInvoices(franchiseOrders, storeIds);
	}

	@ApiOperation(value = "Create franchise order invoice", nickname = "createFranchiseOrderInvoice")
	@PostMapping("/orders/invoices/franchise/{orderId}")
	public ResponseEntity<FranchiseOrderResponseBean> createFranchiseOrderInvoice(@PathVariable UUID orderId) {
		FranchiseOrderEntity franchiseOrder = invoiceService.getFranchiseOrder(orderId);
		if (franchiseOrder == null || !(franchiseOrder.getStatus().equals(FranchiseOrderStatus.ORDER_BILLED) || franchiseOrder.getStatus()
				.equals(FranchiseOrderStatus.ORDER_DELIVERED))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Order Found or Order Status is not BILLED or DELIVERED", "order"));
		} else if (franchiseOrder != null && franchiseOrder.getStatus().equals(FranchiseOrderStatus.CANCELLED_POST_BILLING)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Invoice can not be generated for order in status %s", franchiseOrder.getStatus()), "order"));
		}
		List<InvoiceEntity> invoices = invoiceService.generateFranchiseInvoices(List.of(franchiseOrder), Set.of(franchiseOrder.getStoreId()));
		franchiseOrder.setInvoice(invoices.get(0));
		FranchiseOrderResponseBean response = getMapper().mapSrcToDest(franchiseOrder, FranchiseOrderResponseBean.newInstance());
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "download zip file for Invoices", nickname = "downloadZipFileForInvoices")
	@PostMapping("/orders/invoices/franchise/download-zip")
	public ResponseEntity<ByteArrayResource> downloadZipFile(@Valid @RequestBody BackofficeBulkInvoiceRequest request,
			@RequestParam(required = false) SRType type, @RequestParam(required = false) String city) {
		Set<String> storeIds = city != null ?
				clientService.getStoresData(StoreSearchRequest.builder().storeIds(request.getStoreIds()).city(city).build()).stream()
						.map(StoreDataResponse::getStoreId).collect(Collectors.toSet()) :
				new HashSet<>(request.getStoreIds());
		if (CollectionUtils.isEmpty(storeIds)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No store ids found", "stores"));
		}
		java.sql.Date date = request.getDate();
		_LOGGER.debug(String.format("generateZipFile:: storeId: %s date : %s", storeIds, date));
		List<FranchiseOrderEntity> franchiseOrders = franchiseOrderService.findStoresOrderOnDate(storeIds, date, type);
		List<InvoiceEntity> invoices = invoiceService.generateFranchiseInvoices(franchiseOrders, storeIds);
		List<String> urls = invoices.stream().map(InvoiceEntity::getInvoiceUrl).collect(Collectors.toList());
		List<String> paymentNotesUrls = getPaymentNotesUrls(invoices);
		urls.addAll(paymentNotesUrls);
		ByteArrayResource resource = zipService.generateZipFile(urls, date);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION,
				String.format("attachment; filename= %s", date.toString().concat("-").concat(String.valueOf(Instant.now().toEpochMilli())).concat(".zip")));
		return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	private List<String> getPaymentNotesUrls(List<InvoiceEntity> invoices) {
		List<String> urls = new ArrayList<>();
		if (invoices != null) {
			for (InvoiceEntity invoice : invoices) {
				if (invoice.getPaymentNotes() != null && invoice.getPaymentNotes().getNotesList() != null) {
					for (PaymentNoteData paymentNote : invoice.getPaymentNotes().getNotesList()) {
						urls.add(paymentNote.getUrl());
					}
				}
			}
		}
		return urls;
	}

	@ApiOperation(value = "Create consumer invoice", nickname = "createConsumerInvoices")
	@PostMapping("/orders/invoices/consumer")
	public void createConsumerInvoices() {
		List<OrderEntity> orders = orderService.findAllDispatchedOrders();
		invoiceService.generateConsumerInvoices(orders);
	}

	@ApiOperation(value = "Create consumer invoice", nickname = "createConsumerInvoices")
	@PostMapping("/orders/invoices/refund/consumer")
	public void createConsumerRefundInvoices(@RequestParam(required = false) String orderId) {
		OrderEntity parentOrder = orderService.findById(UUID.fromString(orderId));
		List<OrderEntity> refundOrders = orderService.findRefundOrdersFromParentId(parentOrder.getId());
		for (OrderEntity refundOrder : refundOrders) {
			invoiceService.generateConsumerRefundCreditNote(parentOrder, refundOrder);
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
