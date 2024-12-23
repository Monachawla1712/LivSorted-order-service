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
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.OrderAdjustmentEntity;
import com.sorted.rest.services.order.repository.OrderAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderAdjustmentsService implements BaseService<OrderAdjustmentEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(OrderAdjustmentsService.class);

	@Autowired
	ClientService clientService;

	@Autowired
	OrderAdjustmentRepository orderAdjustmentRepository;

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Override
	public Class<OrderAdjustmentEntity> getEntity() {
		return OrderAdjustmentEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return orderAdjustmentRepository;
	}

	public UUID getUserId() {
		UUID customerId = SessionUtils.getAuthUserId();
		Assert.notNull(customerId, "CustomerId could not be empty");
		return customerId;
	}

	public void saveOrderAdjustments(List<OrderAdjustmentEntity> orderAdjustments) {
		orderAdjustmentRepository.saveAll(orderAdjustments);
	}

	public List<BackofficeOrderAdjustmentRequest> preProcessOrderAdjustmentsUpload(List<BackofficeOrderAdjustmentRequest> rawBeans) {
		List<BackofficeOrderAdjustmentRequest> beans = sanitizeOrderAdjustmentsUpload(rawBeans);
		return beans;
	}

	private List<BackofficeOrderAdjustmentRequest> sanitizeOrderAdjustmentsUpload(List<BackofficeOrderAdjustmentRequest> rawBeans) {
		Set<String> txnTypesSet = getOrderTransactionTypes();
		List<String> displayOrderIdList = rawBeans.stream().map(BackofficeOrderAdjustmentRequest::getDisplayOrderId).distinct().collect(Collectors.toList());
		List<FranchiseOrderEntity> orders = franchiseOrderService.findOrdersByDisplayOrderId(displayOrderIdList);
		Set<String> displayOrderIdSet = orders.stream().map(FranchiseOrderEntity::getDisplayOrderId).collect(Collectors.toSet());
		for (BackofficeOrderAdjustmentRequest adjustment : rawBeans) {
			if (!displayOrderIdSet.contains(adjustment.getDisplayOrderId())) {
				adjustment.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE,
						String.format("Order for Display Order Id : %s not found", adjustment.getDisplayOrderId()), "displayOrderId"));
			}
			if (!txnTypesSet.contains(adjustment.getTxnType())) {
				adjustment.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE,
						String.format("input transaction type not found for Display Order Id : %s ", adjustment.getDisplayOrderId()), "txnType"));
			}
		}
		return rawBeans;
	}

	private Set<String> getOrderTransactionTypes() {
		String paramString = ParamsUtils.getParam("ORDER_TRANSACTION_TYPES",
				"FO-DELIVERY-CHARGE,FO-CRATE-ADJUSTMENT,FO-OT-CHARGE,FO-LD-REFUND,FO-UNLOADING-CHARGE,FO-EXTRA-MARGIN-DISCOUNT,FO-OTHER-REFUND,FO-OTHER-CHARGE");
		Set<String> paramSet = new HashSet<>(Arrays.asList(paramString.split(",")));
		return paramSet;
	}

	public void validateOrderAdjustmentsOnUpload(BackofficeOrderAdjustmentRequest bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (Objects.isNull(bean.getDisplayOrderId())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Order Id not found", "displayOrderId"));
			}
			if (Objects.isNull(bean.getTxnType())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Txn Type not found", "quantity"));
			}
			if (Objects.isNull(bean.getAmount())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "amount not found", "quantity"));
			}
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	public PageAndSortResult<OrderAdjustmentEntity> findRejectionOrdersByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<OrderAdjustmentEntity> rejectionOrders;
		try {
			rejectionOrders = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error("Some error occurred while getting data from findRejectionOrdersByPage", e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return rejectionOrders;
	}

	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	public void changeAdjustmentStatus(OrderAdjustmentEntity adjustment, FranchiseOrderConstants.OrderAdjustmentStatus status,
			UserServiceResponse userDetails) {
		if (adjustment.getAdjustmentDetails() == null) {
			adjustment.setAdjustmentDetails(new OrderAdjustmentDetails());
		}
		adjustment.getAdjustmentDetails().setApprovalDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
		setOrderAdjustmentsData(adjustment.getAdjustmentDetails(), userDetails, "APPROVER");
		adjustment.setApprovedBy(getUserId());
		adjustment.setStatus(status);
		orderAdjustmentRepository.save(adjustment);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void createOrderAdjustments(List<BackofficeOrderAdjustmentRequest> adjustments) {
		UUID userId = getUserId();
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(userId);
		for (BackofficeOrderAdjustmentRequest orderAdjustmentRequest : adjustments) {
			orderAdjustmentRepository.deactivateOldAdjustment(orderAdjustmentRequest.getDisplayOrderId(), orderAdjustmentRequest.getTxnType());
			OrderAdjustmentDetails adjustmentDetails = new OrderAdjustmentDetails();
			adjustmentDetails.setRequestDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
			setOrderAdjustmentsData(adjustmentDetails, userDetails, "REQUESTER");
			OrderAdjustmentEntity orderAdjustment = OrderAdjustmentEntity.createOrderAdjustmentEntity(userId, orderAdjustmentRequest.getDisplayOrderId(),
					orderAdjustmentRequest.getAmount(), orderAdjustmentRequest.getTxnType(), orderAdjustmentRequest.getRemarks(), adjustmentDetails);
			orderAdjustmentRepository.save(orderAdjustment);
		}
	}

	private void setOrderAdjustmentsData(OrderAdjustmentDetails adjustmentDetails, UserServiceResponse userDetails, String userType) {
		OrderAdjustmentUserDetails orderAdjustmentUserDetails = new OrderAdjustmentUserDetails();
		if (userDetails != null) {
			orderAdjustmentUserDetails.setId(userDetails.getId());
			orderAdjustmentUserDetails.setName(userDetails.getName());
			orderAdjustmentUserDetails.setEmailAddress(userDetails.getEmail());
		}
		if (Objects.equals(userType, "REQUESTER")) {
			adjustmentDetails.setRequesterData(orderAdjustmentUserDetails);
		} else {
			adjustmentDetails.setApproverData(orderAdjustmentUserDetails);
		}
	}
}