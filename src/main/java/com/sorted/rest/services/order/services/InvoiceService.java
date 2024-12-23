package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.upload.AWSUploadService;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants.InvoiceType;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import com.sorted.rest.services.order.entity.OrderAdjustmentEntity;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.repository.InvoiceRepository;
import com.sorted.rest.services.order.utils.InvoicePDFGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mohit on 20.8.22.
 */
@Service
public class InvoiceService implements BaseService<InvoiceEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(InvoiceService.class);

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private OrderAdjustmentsService orderAdjustmentsService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private InvoicePDFGenerator pdfGenerator;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private AWSUploadService awsUploadService;

	public List<InvoiceEntity> generateFranchiseInvoices(List<FranchiseOrderEntity> franchiseOrders, Set<String> storeIds) {
		if (CollectionUtils.isEmpty(franchiseOrders)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Franchise order found", "orderId"));
		}
		Map<Integer, StoreResponse> storesMap = getStoresDetails(storeIds);
		if (storesMap.size() != storeIds.size()) {
			Set<String> storeIdsResponse = storesMap.keySet().stream().map(Object::toString).collect(Collectors.toSet());
			storeIds.removeAll(storeIdsResponse);
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("stores = %s are not found on wms ", storeIds), "storeIds"));
		}
		List<InvoiceEntity> invoices = new ArrayList<>();
		List<ClevertapEventRequest.ClevertapEventData> clevertapEventDataList = new ArrayList<>();
		for (FranchiseOrderEntity franchiseOrder : franchiseOrders) {
			InvoiceEntity invoice = franchiseOrder.getInvoice();
			StoreResponse store = storesMap.get(Integer.parseInt(franchiseOrder.getStoreId()));
			if ((invoice == null || invoice.getInvoiceUrl() == null) && !franchiseOrder.getStatus().equals(FranchiseOrderStatus.CANCELLED_POST_BILLING)) {
				_LOGGER.info(String.format("Invoice is not created for %s ", franchiseOrder.getDisplayOrderId()));
				invoice = processInvoice(franchiseOrder, store, null, InvoiceType.FRANCHISE_INVOICE, null);
				clevertapEventDataList.add(buildCleverTapEventData(franchiseOrder));
			}
			invoices.add(invoice);
		}
		clientService.sendMultipleClevertapEvents(clevertapEventDataList);
		return invoices;
	}

	@Transactional
	public void generateConsumerInvoices(List<OrderEntity> orders) {
		if (CollectionUtils.isEmpty(orders)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No Consumer order found", "orderId"));
		}
		for (OrderEntity order : orders) {
			_LOGGER.info(String.format("Creating Consumer Invoice for %s ", order.getDisplayOrderId()));
			processConsumerInvoice(order, InvoiceType.CONSUMER_INVOICE, null);
		}
	}

	private InvoiceEntity processConsumerInvoice(OrderEntity order, InvoiceType invoiceType, OrderEntity refundOrder) {
		ConsumerInvoiceDataBean invoiceData = buildConsumerInvoiceData(order, invoiceType);
		InvoiceEntity invoice = null;
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.CONSUMER_INVOICE)) {
			invoice = saveConsumerInvoice(order, invoiceData);
		} else {
			invoice = saveConsumerCreditDebitNote(order, invoiceData, refundOrder);
		}
		return invoice;
	}

	private ConsumerInvoiceDataBean buildConsumerInvoiceData(OrderEntity order, InvoiceType invoiceType) {
		_LOGGER.debug(String.format("Creating Invoice Data for Consumer order: %s", order.getDisplayOrderId()));
		ConsumerInvoiceDataBean invoiceData = new ConsumerInvoiceDataBean();
		setConsumerInvoiceTypeAndParent(invoiceData, invoiceType, order.getInvoice());
		return invoiceData;
	}

	private void setConsumerInvoiceTypeAndParent(ConsumerInvoiceDataBean invoiceData, InvoiceType invoiceType, InvoiceEntity invoice) {
		invoiceData.setInvoiceType(invoiceType);
		if (!Objects.equals(invoiceType, InvoiceType.CONSUMER_INVOICE)) {
			invoiceData.setParentInvoice(invoice);
		}
	}

	private InvoiceEntity saveConsumerInvoice(OrderEntity order, ConsumerInvoiceDataBean invoiceData) {
		InvoiceEntity invoice = invoiceRepository.findByDisplayOrderIdAndActive(order.getDisplayOrderId(), 1);
		String dateString = getFinancialYear();
		Long invoiceId = null;
		if (Objects.isNull(invoice) && Objects.equals(invoiceData.getInvoiceType(), InvoiceType.CONSUMER_INVOICE)) {
			invoiceId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.CONSUMER_INVOICE.toString());
			String invoiceName = "BC/FNV/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(invoiceId.toString())));
			invoiceData.setInvoiceName(invoiceName);
			invoiceData.setInvoiceId(invoiceId.toString());
			invoice = InvoiceEntity.createConsumerInvoiceEntity(order, dateString, invoiceId, invoiceName);
		} else if (!Objects.isNull(invoice)) {
			invoiceData.setInvoiceName(invoice.getInvoiceName());
			invoiceData.setInvoiceId(invoice.getInvoiceId().toString());
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Parent Invoice Not Found.", "order"));
		}
		invoice.setAmount(order.getFinalBillAmount());
		invoice = invoiceRepository.save(invoice);
		return invoice;
	}

	@Transactional
	public InvoiceEntity generateConsumerRefundCreditNote(OrderEntity parentOrder, OrderEntity refundOrder) {
		return processConsumerInvoice(parentOrder, InvoiceType.CONSUMER_CREDIT_NOTE, refundOrder);
	}

	private InvoiceEntity saveConsumerCreditDebitNote(OrderEntity parentOrder, ConsumerInvoiceDataBean invoiceData, OrderEntity refundOrder) {
		InvoiceEntity invoice = parentOrder.getInvoice();
		if (Objects.isNull(invoice)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Parent Invoice Not Found.", "order"));
		}
		if (CollectionUtils.isNotEmpty(invoice.getPaymentNotes().getNotesList())) {
			Optional<PaymentNoteData> noteDataOp = invoice.getPaymentNotes().getNotesList().stream()
					.filter(n -> refundOrder.getDisplayOrderId().equals(n.getDisplayOrderId())).findFirst();
			if (noteDataOp.isPresent()) {
				PaymentNoteData noteData = noteDataOp.get();
				invoiceData.setInvoiceName(noteData.getName());
				return invoice;
			}
		}
		String dateString = getFinancialYear();
		String invoiceName = null;
		Long noteId = null;
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.CONSUMER_CREDIT_NOTE)) {
			noteId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.CONSUMER_CREDIT_NOTE.toString());
			invoiceName = "CN/FNV/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(noteId.toString())));
		} else if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.CONSUMER_DEBIT_NOTE)) {
			noteId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.CONSUMER_DEBIT_NOTE.toString());
			invoiceName = "DN/FNV/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(noteId.toString())));
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Invoice Type Not Found.", "invoiceType"));
		}
		invoiceData.setInvoiceName(invoiceName);
		PaymentNoteData noteData = PaymentNoteData.createPaymentNoteData(invoiceName, invoiceData.getInvoiceType().toString(), null, new Date(),
				refundOrder.getFinalBillAmount(), refundOrder.getDisplayOrderId());
		if (CollectionUtils.isEmpty(invoice.getPaymentNotes().getNotesList())) {
			invoice.getPaymentNotes().setNotesList(new ArrayList<>());
		}
		invoice.getPaymentNotes().getNotesList().add(noteData);
		invoice = invoiceRepository.save(invoice);
		return invoice;
	}

	private ClevertapEventRequest.ClevertapEventData buildCleverTapEventData(FranchiseOrderEntity franchiseOrder) {
		try {
			Map<String, Object> clevertapEventData = new HashMap<>();

			return clientService.buildClevertapEventData("Invoice Generated", franchiseOrder.getCustomerId(), clevertapEventData);
		} catch (Exception e) {
			_LOGGER.error(String.format("buildCleverTapEventData:: Invoice Generate event failed for : %s", franchiseOrder.getCustomerId()));
		}
		return null;
	}

	private List<OrderAdjustmentEntity> getAdjustmentsByType(List<OrderAdjustmentEntity> adjustments, String txnMode) {
		return adjustments.stream().filter(statement -> statement.getTxnMode().equals(txnMode)).collect(Collectors.toList());
	}

	public InvoiceEntity processInvoice(FranchiseOrderEntity franchiseOrder, StoreResponse store, List<OrderAdjustmentEntity> adjustments,
			InvoiceType invoiceType, FranchiseOrderEntity refundOrder) {
		InvoiceDataBean invoiceData = buildInvoiceData(franchiseOrder, store, adjustments, invoiceType, refundOrder);
		InvoiceEntity invoice = saveInvoice(franchiseOrder, invoiceData);
		createInvoicePdf(invoiceData);
		String fileUrl = uploadInvoice(invoiceData);
		saveInvoiceNameUrlAndAmount(invoice, invoiceData.getInvoiceName(), fileUrl, invoiceData.getTotalAmount().doubleValue(), invoiceData.getInvoiceType());
		return invoice;
	}

	private static String getFinancialYear() {
		Date date = new Date();
		SimpleDateFormat yearFormat = new SimpleDateFormat("yy");
		String lastTwoDigits = yearFormat.format(date);
		int currentYearTwoInt = Integer.parseInt(lastTwoDigits);
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH);
		if (month < 3) {
			currentYearTwoInt--;
		}
		int nextYearTwoInt = currentYearTwoInt + 1;
		String financialYear = String.valueOf(currentYearTwoInt).concat(String.valueOf(nextYearTwoInt));
		return financialYear;
	}

	@Transactional
	public InvoiceEntity saveInvoice(FranchiseOrderEntity franchiseOrder, InvoiceDataBean invoiceData) {
		InvoiceEntity invoice = invoiceRepository.findByDisplayOrderIdAndActive(franchiseOrder.getDisplayOrderId(), 1);
		String dateString = getFinancialYear();
		if (Objects.isNull(invoice)) {
			Long invoiceId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.FRANCHISE_INVOICE.toString());
			invoice = InvoiceEntity.createInvoiceEntity(franchiseOrder, invoiceData, dateString, invoiceId);
			invoice = invoiceRepository.save(invoice);
		}
		if (!dateString.equals(invoice.getDateString())) {
			invoice.setDateString(dateString);
		}
		String invoiceName;
		Long noteId = null;
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			invoiceName = "FV/A/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(invoice.getInvoiceId().toString())));
		} else if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_CREDIT_NOTE)) {
			noteId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.FRANCHISE_CREDIT_NOTE.toString());
			invoiceName = "CN/A/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(noteId.toString())));
		} else if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_DEBIT_NOTE)) {
			noteId = invoiceRepository.getGeneratedInvoiceId(dateString, InvoiceType.FRANCHISE_DEBIT_NOTE.toString());
			invoiceName = "DN/A/".concat(dateString).concat("/").concat(String.format("%0" + 5 + "d", Integer.parseInt(noteId.toString())));
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Invoice Type Not Found.", "invoiceType"));
		}
		invoiceData.setInvoiceName(invoiceName);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			invoiceData.setInvoiceId(invoice.getInvoiceId().toString());
		} else {
			invoiceData.setInvoiceId(String.valueOf(noteId));
		}
		return invoice;
	}

	private String uploadInvoice(InvoiceDataBean invoiceData) {
		String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
		String subDirectory = ParamsUtils.getParam("INVOICE_FILES_SUBDIRECTORY");
		File directoryPath = new File(System.getProperty("user.dir"));
		File files[] = directoryPath.listFiles();
		try {
			for (File file : files) {
				if (file.getName().endsWith(".pdf")) {
					Date date = invoiceData.getDeliveryDate();
					SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
					String formattedDate = formatter.format(date);
					String filename = invoiceData.getStoreId().toString().concat("-").concat(formattedDate).concat("-").concat(invoiceData.getInvoiceId())
							.concat("-").concat(String.valueOf(Instant.now().toEpochMilli())).concat(".pdf");
					byte[] fileBytes = Files.readAllBytes(Path.of(directoryPath.getPath().concat("/").concat(file.getName())));
					Object response = awsUploadService.uploadFile(bucketName, subDirectory, fileBytes, filename);
					file.delete();
					return response.toString();
				}
			}
		} catch (IOException err) {
			_LOGGER.error("Error while uploading Invoice", err);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, String.format("Error while uploading invoice: ", err.getMessage()), "invoice"));
		}
		return null;
	}

	public void saveInvoiceNameUrlAndAmount(InvoiceEntity invoice, String invoiceName, String invoiceUrl, Double invoiceAmount, InvoiceType invoiceType) {
		if (Objects.equals(invoiceType, InvoiceType.FRANCHISE_INVOICE)) {
			invoice.setInvoiceName(invoiceName);
			invoice.setInvoiceUrl(ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(invoiceUrl));
			invoice.setAmount(invoiceAmount);
		} else {
			if (invoice.getPaymentNotes() == null || invoice.getPaymentNotes().getNotesList() == null) {
				invoice.setPaymentNotes(new PaymentNoteDetails());
				PaymentNoteData noteData = PaymentNoteData.createPaymentNoteData(invoiceName, invoiceType.toString(),
						ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(invoiceUrl), new Date(), invoiceAmount, null);
				List<PaymentNoteData> noteList = new ArrayList<>();
				noteList.add(noteData);
				invoice.getPaymentNotes().setNotesList(noteList);
			} else {
				PaymentNoteData noteData = PaymentNoteData.createPaymentNoteData(invoiceName, invoiceType.toString(),
						ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(invoiceUrl), new Date(), invoiceAmount, null);
				invoice.getPaymentNotes().getNotesList().add(noteData);
			}
		}
		invoiceRepository.save(invoice);
	}

	private List<WalletStatementBean> fetchStoreWalletStatement(String displayOrderId) {
		List<WalletStatementBean> statement = clientService.fetchWalletStatementByTxnDetail(displayOrderId);
		if (CollectionUtils.isEmpty(statement)) {
			throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "Statement Not found", displayOrderId));
		}
		return statement;
	}

	private InvoiceDataBean buildInvoiceData(FranchiseOrderEntity franchiseOrder, StoreResponse store, List<OrderAdjustmentEntity> adjustments,
			InvoiceType invoiceType, FranchiseOrderEntity refundOrder) {
		_LOGGER.debug(String.format("Creating Invoice Data for franchise order: %s and Store: %s", franchiseOrder.getDisplayOrderId(), store.getId()));
		InvoiceDataBean invoiceData = new InvoiceDataBean();
		setInvoiceTypeAndParent(invoiceData, invoiceType, franchiseOrder.getInvoice());
		setInvoiceStoreData(invoiceData, store);
		setInvoiceOrderData(invoiceData, franchiseOrder);
		setInvoiceRefundOrdersData(invoiceData, franchiseOrder, invoiceType, refundOrder);
		createAdjustmentsData(invoiceData, adjustments);
		calculateQtySubTotalAndPackingCharges(franchiseOrder, invoiceData);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			invoiceData.setTotalAmount(invoiceData.getDisplayFinalAmount().add(invoiceData.getTotalAdjustments()));
		} else if (refundOrder != null) {
			invoiceData.setTotalAmount(BigDecimal.valueOf(refundOrder.getFinalBillAmount()));
		} else {
			invoiceData.setTotalAmount(invoiceData.getTotalAdjustments());
		}
		InvoiceAdjustmentsBean invoiceAdjustment = new InvoiceAdjustmentsBean();
		invoiceAdjustment.setAdjustmentTxnType("CLOSING-BALANCE");
		invoiceAdjustment.setAmount(invoiceData.getAdjustmentsMap().get("OPENING-BALANCE").getAmount().subtract(invoiceData.getTotalAmount()));
		invoiceData.getAdjustmentsMap().put("CLOSING-BALANCE", invoiceAdjustment);
		invoiceData.setShowPackingDetails(Integer.parseInt(ParamsUtils.getParam("INVOICE_PACKING_DETAILS_ENABLED")));
		return invoiceData;
	}

	private void setInvoiceTypeAndParent(InvoiceDataBean invoiceData, InvoiceType invoiceType, InvoiceEntity invoice) {
		invoiceData.setInvoiceType(invoiceType);
		if (!Objects.equals(invoiceType, InvoiceType.FRANCHISE_INVOICE)) {
			invoiceData.setParentInvoice(invoice);
		}
	}

	private void setInvoiceOrderData(InvoiceDataBean invoiceData, FranchiseOrderEntity franchiseOrder) {
		invoiceData.setIsSrpStore(franchiseOrder.getIsSrpStore());
		invoiceData.setOrderId(franchiseOrder.getId());
		invoiceData.setDeliveryDate(franchiseOrder.getDeliveryDate());
		invoiceData.setDisplayOrderId(franchiseOrder.getDisplayOrderId());
		invoiceData.setOrderItems(franchiseOrder.getOrderItems());
		invoiceData.setTotalSpGrossAmount(BigDecimal.valueOf(franchiseOrder.getTotalSpGrossAmount()));
		invoiceData.setTotalMrpGrossAmount(BigDecimal.valueOf(franchiseOrder.getTotalMrpGrossAmount()));
		invoiceData.setFinalBillAmount(BigDecimal.valueOf(franchiseOrder.getFinalBillAmount()));
		invoiceData.setTotalDiscountAmount(calculateTotalDiscountAmount(franchiseOrder));
		if (franchiseOrder.getExtraFeeDetails() != null && franchiseOrder.getExtraFeeDetails().getDeliveryCharge() != null) {
			invoiceData.setOrderDeliveryCharge(BigDecimal.valueOf(franchiseOrder.getExtraFeeDetails().getDeliveryCharge()));
		}
	}

	private BigDecimal calculateTotalDiscountAmount(FranchiseOrderEntity franchiseOrder) {
		return BigDecimal.valueOf(franchiseOrder.getTotalDiscountAmount())
				.subtract(BigDecimal.valueOf(franchiseOrder.getTotalMrpGrossAmount()).subtract(BigDecimal.valueOf(franchiseOrder.getTotalSpGrossAmount())));
	}

	private void setInvoiceRefundOrdersData(InvoiceDataBean invoiceData, FranchiseOrderEntity franchiseOrder, InvoiceType invoiceType,
			FranchiseOrderEntity refundOrder) {
		if (Objects.equals(invoiceType, InvoiceType.FRANCHISE_INVOICE) && refundOrder == null) {
			List<FranchiseOrderEntity> refundOrders = getRefundOrders(franchiseOrder.getId());
			if (!CollectionUtils.isEmpty(refundOrders)) {
				invoiceData.setRefundOrders(refundOrders);
				calculateRefundQtyTotals(refundOrders, invoiceData);
			}
		} else if (refundOrder != null) {
			invoiceData.setIsRefundInvoice(1);
			invoiceData.setRefundOrders(List.of(refundOrder));
			calculateRefundQtyTotals(List.of(refundOrder), invoiceData);
		}
	}

	private void setInvoiceStoreData(InvoiceDataBean invoiceData, StoreResponse store) {
		invoiceData.setStoreId(store.getId());
		invoiceData.setAddress(store.getAddress());
		invoiceData.setStoreType(store.getStoreType());
		invoiceData.setRecipientName(store.getName());
		if (store.getMetadata() != null) {
			if (store.getMetadata().getGstNumber() != null) {
				invoiceData.setStoreGstNumber(store.getMetadata().getGstNumber());
			}
			if (store.getMetadata().getPanNumber() != null) {
				invoiceData.setStorePanNumber(store.getMetadata().getPanNumber());
			}
		}
	}

	private void calculateRefundQtyTotals(List<FranchiseOrderEntity> refundOrders, InvoiceDataBean invoiceData) {
		Map<UUID, InvoiceRefundQtyAmountBean> refundQtyTotals = new HashMap<>();
		for (FranchiseOrderEntity refundOrder : refundOrders) {
			_LOGGER.debug(String.format("Calculation Refund Quantity total for refund Order : %s", refundOrder.getDisplayOrderId()));
			InvoiceRefundQtyAmountBean total = new InvoiceRefundQtyAmountBean();
			BigDecimal totalQty = new BigDecimal(0);
			for (FranchiseOrderItemEntity refundOrderItem : refundOrder.getOrderItems()) {
				totalQty = totalQty.add(BigDecimal.valueOf(refundOrderItem.getFinalQuantity()));
			}
			total.setTotalQty(totalQty);
			total.setTotalAmt(BigDecimal.valueOf(refundOrder.getFinalBillAmount()));
			refundQtyTotals.put(refundOrder.getId(), total);
		}
		invoiceData.setRefundQtyTotals(refundQtyTotals);
	}

	private void createAdjustmentsData(InvoiceDataBean invoiceData, List<OrderAdjustmentEntity> orderAdjustments) {
		_LOGGER.debug(String.format("Creating Adjustment Data for order: %s", invoiceData.getDisplayOrderId()));
		List<WalletStatementBean> statements = fetchStoreWalletStatement(invoiceData.getDisplayOrderId());
		String adjustmentList = ParamsUtils.getParam("INVOICE_MAP",
				"FO-DELIVERY-CHARGE:Delivery Charges,PACKING-CHARGES:Packing Charges,FO-OT-CHARGE:OT Charges,FO-LD-REFUND:Late delivery Refund,FO-UNLOADING-CHARGE:Unloading Charges,FO-SHORT-QUANTITY-REFUND:Short Quantity Refund,FO-RETURN-QUANTITY-REFUND:Return Quantity Refund,FO-EXTRA-MARGIN-DISCOUNT:Extra Margin Discount,FO-CRATE-ADJUSTMENT:Crate Adjustment,FO-OTHER-CHARGE:Other Charges,FO-OTHER-REFUND:Other Refunds");
		Map<String, String> invoiceKeys = Arrays.stream(adjustmentList.split(",")).map(pair -> pair.split(":"))
				.collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
		WalletStatementBean selectedStatement = statements.stream().filter(statement -> statement.getTxnType().equals("Franchise-Order")).findFirst()
				.orElse(null);
		Map<String, InvoiceAdjustmentsBean> adjustmentsMap = new HashMap<>();
		if (selectedStatement != null && selectedStatement.getTxnType().equals("Franchise-Order")) {
			InvoiceAdjustmentsBean invoiceAdjustment = new InvoiceAdjustmentsBean();
			invoiceAdjustment.setAdjustmentTxnType("OPENING-BALANCE");
			invoiceAdjustment.setAmount(BigDecimal.valueOf(selectedStatement.getBalance()).add(BigDecimal.valueOf(selectedStatement.getAmount())));
			adjustmentsMap.put("OPENING-BALANCE", invoiceAdjustment);
		}
		BigDecimal totalAdjustments = new BigDecimal(0);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			orderAdjustments = convertWalletStatementToAdjustment(statements);
		}
		if (invoiceData.getOrderDeliveryCharge() != null) {
			OrderAdjustmentEntity orderAdjustment = new OrderAdjustmentEntity();
			orderAdjustment.setTxnType("FO-DELIVERY-CHARGE");
			orderAdjustment.setDisplayOrderId(invoiceData.getDisplayOrderId());
			orderAdjustment.setAmount(invoiceData.getOrderDeliveryCharge().doubleValue());
			orderAdjustment.setTxnMode("DEBIT");
			orderAdjustments.add(orderAdjustment);
		}
		if (orderAdjustments != null) {
			for (OrderAdjustmentEntity adjustment : orderAdjustments) {
				BigDecimal amount = new BigDecimal(0);
				InvoiceAdjustmentsBean invoiceAdjustment = new InvoiceAdjustmentsBean();
				if (adjustment.getTxnType().equals("FO-ADJUSTMENT") || adjustment.getTxnType().equals("Franchise-Order")) {
					continue;
				}
				if (invoiceKeys.containsKey(adjustment.getTxnType())) {
					if (adjustmentsMap.containsKey(adjustment.getTxnType())) {
						BigDecimal adjustAmount = new BigDecimal(0);
						amount = adjustmentsMap.get(adjustment.getTxnType()).getAmount();
						if (adjustment.getTxnMode().equals("CREDIT")) {
							adjustAmount = adjustAmount.subtract(BigDecimal.valueOf(adjustment.getAmount()));
							amount = amount.subtract(BigDecimal.valueOf(adjustment.getAmount()));
						} else {
							adjustAmount = adjustAmount.add(BigDecimal.valueOf(adjustment.getAmount()));
							amount = amount.add(BigDecimal.valueOf(adjustment.getAmount()));
						}
						totalAdjustments = totalAdjustments.add(adjustAmount);
						adjustmentsMap.get(adjustment.getTxnType()).setAmount(amount);
					} else {
						if (adjustment.getTxnMode().equals("CREDIT")) {
							amount = amount.subtract(BigDecimal.valueOf(adjustment.getAmount()));
						} else {
							amount = amount.add(BigDecimal.valueOf(adjustment.getAmount()));
						}
						totalAdjustments = totalAdjustments.add(amount);
						invoiceAdjustment.setAdjustmentTxnType(adjustment.getTxnType());
						invoiceAdjustment.setAmount(amount);
						adjustmentsMap.put(adjustment.getTxnType(), invoiceAdjustment);
					}
				}
			}
		}
		invoiceData.setAdjustmentsMap(adjustmentsMap);
		invoiceData.setTotalAdjustments(totalAdjustments);
	}

	private List<OrderAdjustmentEntity> convertWalletStatementToAdjustment(List<WalletStatementBean> statements) {
		List<OrderAdjustmentEntity> orderAdjustmentList = new ArrayList<>();
		for (WalletStatementBean walletStatementBean : statements) {
			OrderAdjustmentEntity orderAdjustment = new OrderAdjustmentEntity();
			orderAdjustment.setTxnType(walletStatementBean.getTxnType());
			orderAdjustment.setDisplayOrderId(walletStatementBean.getTxnDetail());
			orderAdjustment.setAmount(walletStatementBean.getAmount());
			orderAdjustment.setTxnMode(walletStatementBean.getTxnMode());
			orderAdjustmentList.add(orderAdjustment);
		}
		return orderAdjustmentList;
	}

	private void calculateQtySubTotalAndPackingCharges(FranchiseOrderEntity franchiseOrder, InvoiceDataBean invoiceData) {
		_LOGGER.debug(String.format("Calculating Sub Quantity Totals and Packing Charges for order: %s", invoiceData.getDisplayOrderId()));
		String paramString = ParamsUtils.getParam("PACKING_SKU_CODES", "SC2016,SC2015,SC2014,SC2013,SC2008,SC2007,SC2006,SC2005,SC2004,SC2002,SC2001");
		HashSet<String> excludingSkus = new HashSet<String>(Arrays.asList(paramString.split(",")));
		invoiceData.setExcludingSkus(excludingSkus);
		BigDecimal subQtyTotal = new BigDecimal(0);
		List<FranchiseOrderItemEntity> mainOrderItems = new ArrayList<>();
		List<FranchiseOrderItemEntity> packingItems = new ArrayList<>();
		for (FranchiseOrderItemEntity franchiseOrderItem : franchiseOrder.getOrderItems()) {
			if (excludingSkus.contains(franchiseOrderItem.getSkuCode())) {
				packingItems.add(franchiseOrderItem);
				invoiceData.setPackingTotalCharges(invoiceData.getPackingTotalCharges().add(BigDecimal.valueOf(franchiseOrderItem.getFinalAmount())));
				invoiceData.setPackingTotalQty(invoiceData.getPackingTotalQty().add(BigDecimal.valueOf(franchiseOrderItem.getFinalQuantity())));
				invoiceData.setPackingTotalDiscount(invoiceData.getPackingTotalDiscount().add(BigDecimal.valueOf(franchiseOrderItem.getDiscountAmount())));
			} else {
				mainOrderItems.add(franchiseOrderItem);
				subQtyTotal = subQtyTotal.add(BigDecimal.valueOf(franchiseOrderItem.getFinalQuantity()));
			}
		}
		invoiceData.setOrderItems(mainOrderItems);
		invoiceData.setPackingSkusDetails(packingItems);
		invoiceData.setSubQtyTotal(subQtyTotal);
		invoiceData.setSubAmtTotal(BigDecimal.valueOf(franchiseOrder.getTotalSpGrossAmount()).subtract(invoiceData.getPackingTotalCharges()));
		invoiceData.setTotalMrpGrossAmount(BigDecimal.valueOf(franchiseOrder.getTotalMrpGrossAmount()).subtract(invoiceData.getPackingTotalCharges()));
		invoiceData.setDisplayFinalQty(subQtyTotal.subtract(invoiceData.getPackingTotalQty()));
		invoiceData.setDisplayFinalAmount(BigDecimal.valueOf(franchiseOrder.getFinalBillAmount()).subtract(invoiceData.getOrderDeliveryCharge())
				.subtract(invoiceData.getPackingTotalCharges()));
		invoiceData.setTotalDiscountAmount(invoiceData.getTotalDiscountAmount().subtract(invoiceData.getPackingTotalDiscount()));
		String offerType = (franchiseOrder.getOfferData() != null) ? franchiseOrder.getOfferData().getOfferType() : null;
		invoiceData.setOfferType(offerType);
		if (franchiseOrder.getOfferData() != null && franchiseOrder.getOfferData().getIsOfferApplied() != null
				&& franchiseOrder.getOfferData().getAmount() != null) {
			invoiceData.setOfferDiscountAmount(BigDecimal.valueOf(franchiseOrder.getOfferData().getAmount()));
		} else {
			invoiceData.setOfferDiscountAmount(BigDecimal.valueOf(0));
		}
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			InvoiceAdjustmentsBean invoiceAdjustment = new InvoiceAdjustmentsBean();
			invoiceAdjustment.setAdjustmentTxnType("PACKING-CHARGES");
			invoiceAdjustment.setAmount(invoiceData.getPackingTotalCharges());
			invoiceData.setTotalAdjustments(invoiceData.getTotalAdjustments().add(invoiceData.getPackingTotalCharges()));
			invoiceData.getAdjustmentsMap().put("PACKING-CHARGES", invoiceAdjustment);
		}
	}

	public Map<Integer, StoreResponse> getStoresDetails(Set<String> storeIds) {
		Map<Integer, StoreResponse> storesMap = new HashMap<>();
		int storesNum = storeIds.size();
		if (storesNum == 0) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Store selected", "stores"));
		}
		int batchSize = 500;
		int batches = ((storesNum - 1) / batchSize) + 1;
		List<String> storeIdsList = new ArrayList<>(storeIds);
		for (int i = 0; i < batches; i++) {
			int startPoint = i * batchSize;
			int endPoint = Math.min(startPoint + batchSize, storesNum);
			List<String> storeIdsSubList = storeIdsList.subList(startPoint, endPoint);
			Set<String> storeIdsSubSet = new HashSet<>(storeIdsSubList);
			List<StoreResponse> stores = clientService.fetchStoresDetails(storeIdsSubSet);
			if (CollectionUtils.isEmpty(stores)) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Franchise Store found", "stores"));
			}
			for (StoreResponse storeResponse : stores) {
				storesMap.put(storeResponse.getId(), storeResponse);
			}
		}
		return storesMap;
	}

	private void createInvoicePdf(InvoiceDataBean invoiceData) {
		pdfGenerator.generatePdfReport(invoiceData);
	}

	private List<FranchiseOrderEntity> getRefundOrders(UUID parentOrderId) {
		List<FranchiseOrderEntity> refundOrders = franchiseOrderService.findAllRefundOrders(parentOrderId);
		return refundOrders;
	}

	public FranchiseOrderEntity getFranchiseOrder(UUID orderId) {
		FranchiseOrderEntity franchiseOrder = franchiseOrderService.findRecordById(orderId);
		if (Objects.isNull(franchiseOrder)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Franchise Order found", "orderId"));
		}
		return franchiseOrder;
	}

	@Override
	public Class<InvoiceEntity> getEntity() {
		return InvoiceEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return invoiceRepository;
	}

	public void generatePaymentNotes(FranchiseOrderEntity order, List<OrderAdjustmentEntity> adjustments) {
		List<StoreResponse> stores = clientService.fetchStoresDetails(Set.of(order.getStoreId()));
		if (CollectionUtils.isEmpty(stores)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Franchise Store found", "stores"));
		}
		if (adjustments != null) {
			List<OrderAdjustmentEntity> debitAdjustments = getAdjustmentsByType(adjustments, "DEBIT");
			if (debitAdjustments != null && !debitAdjustments.isEmpty()) {
				InvoiceEntity invoice = processInvoice(order, stores.get(0), debitAdjustments, InvoiceType.FRANCHISE_DEBIT_NOTE, null);
				PaymentNoteData noteData = getLastNote(invoice);
				debitAdjustments.forEach((adjustment -> adjustment.getAdjustmentDetails().setPaymentNoteName(noteData.getName())));
				orderAdjustmentsService.saveOrderAdjustments(debitAdjustments);
			}
			List<OrderAdjustmentEntity> creditAdjustments = getAdjustmentsByType(adjustments, "CREDIT");
			if (creditAdjustments != null && !creditAdjustments.isEmpty()) {
				InvoiceEntity invoice = processInvoice(order, stores.get(0), creditAdjustments, InvoiceType.FRANCHISE_CREDIT_NOTE, null);
				PaymentNoteData noteData = getLastNote(invoice);
				creditAdjustments.forEach((adjustment -> adjustment.getAdjustmentDetails().setPaymentNoteName(noteData.getName())));
				orderAdjustmentsService.saveOrderAdjustments(creditAdjustments);
			}
		}
	}

	private PaymentNoteData getLastNote(InvoiceEntity invoice) {
		int totalNotes = invoice.getPaymentNotes().getNotesList().size();
		return invoice.getPaymentNotes().getNotesList().get(totalNotes - 1);
	}

	public InvoiceEntity generateRefundCreditNote(FranchiseOrderEntity parentOrder, FranchiseOrderEntity refundOrder) {
		List<StoreResponse> stores = clientService.fetchStoresDetails(Set.of(refundOrder.getStoreId()));
		if (CollectionUtils.isEmpty(stores)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No Franchise Store found", "stores"));
		}
		return processInvoice(parentOrder, stores.get(0), new ArrayList<>(), InvoiceType.FRANCHISE_CREDIT_NOTE, refundOrder);
	}
}