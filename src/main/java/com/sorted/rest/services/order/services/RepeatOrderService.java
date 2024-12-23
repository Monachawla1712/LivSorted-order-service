package com.sorted.rest.services.order.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.controller.OrderController;
import com.sorted.rest.services.order.entity.RepeatOrderEntity;
import com.sorted.rest.services.order.repository.RepeatOrderRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepeatOrderService implements BaseService<RepeatOrderEntity> {

	@Autowired
	private RepeatOrderRepository repeatOrderRepository;

	@Autowired
	private ClientService clientService;

	AppLogger _LOGGER = LoggingManager.getLogger(OrderController.class);

	@Override
	public Class<RepeatOrderEntity> getEntity() {
		return RepeatOrderEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return repeatOrderRepository;
	}

	public RepeatOrderEntity createRepeatOrder(RepeatOrderCreateRequest repeatOrderBean, UUID customerId) {
		Date nextDeliveryDate = DateUtils.getDate(String.valueOf(findNextDeliveryDate(repeatOrderBean.getPreferences().getDays())));
		RepeatOrderEntity repeatOrderEntity = RepeatOrderEntity.createRepeatOrder(customerId, repeatOrderBean.getSkuCode(), repeatOrderBean.getPreferences(),
				nextDeliveryDate);
		repeatOrderRepository.save(repeatOrderEntity);
		return repeatOrderEntity;
	}

	public static LocalDate findNextDeliveryDate(ArrayList<DayOfWeek> days) {
		LocalDate today = DateUtils.getLocalDateIST();
		LocalDate tomorrow = today.plusDays(1);
		List<LocalDate> deliveryDates = days.stream().map(day -> today.with(java.time.temporal.TemporalAdjusters.next(day))).collect(Collectors.toList());
		// if tomorrow is the only day we have then take next week's date
		if (deliveryDates.size() == 1 && deliveryDates.contains(tomorrow)) {
			return tomorrow.plusWeeks(1);
		}
		return deliveryDates.stream().filter(date -> !date.equals(tomorrow)).min(Comparator.naturalOrder())
				.orElseThrow(() -> new IllegalArgumentException("No valid delivery days provided"));
	}

	public RepeatOrderEntity updateRepeatOrder(RepeatOrderEntity repeatOrderEntity, RepeatOrderUpdateRequest request) {
		//case when we have to inactivate the repeat order
		if (request.getPreferences() == null
				|| (request.getPreferences().getQuantity() == null || request.getPreferences().getQuantity() == 0d) && (CollectionUtils.isEmpty(
				request.getPreferences().getDays()))) {
			repeatOrderEntity.getPreferences().setQuantity(0d);
			repeatOrderEntity.getPreferences().setDays(new ArrayList<>());
			repeatOrderEntity.setActive(0);
		} else {
			if ((request.getPreferences().getQuantity() == null || request.getPreferences().getQuantity() == 0d) && (request.getPreferences().getDays() != null
					&& !request.getPreferences().getDays().isEmpty())) {
				_LOGGER.error("Quantity is 0 or null but days are present");
				throw new IllegalArgumentException("Quantity is 0 or null but days are present");
			}
			if ((request.getPreferences().getQuantity() != null && request.getPreferences().getQuantity() != 0d) && (request.getPreferences().getDays() == null
					|| request.getPreferences().getDays().isEmpty())) {
				_LOGGER.error("Days are null or empty but quantity is present");
				throw new IllegalArgumentException("Days are null or empty but quantity is present");
			}
			BeanUtils.copyProperties(request, repeatOrderEntity);
			if (!repeatOrderEntity.getStatus().equals(OrderConstants.RepeatOrderStatus.PAUSED)) {
				// if repeat order is not paused, then we update the next delivery date
				Date nextDeliveryDate = DateUtils.getDate(String.valueOf(findNextDeliveryDate(repeatOrderEntity.getPreferences().getDays())));
				repeatOrderEntity.setNextDeliveryDate(nextDeliveryDate);
			}
		}
		repeatOrderRepository.save(repeatOrderEntity);
		return repeatOrderEntity;
	}

	public RepeatOrderEntity findByCustomerIdAndSkuCode(UUID customerId, String skuCode) {
		return repeatOrderRepository.findByCustomerIdAndSkuCode(customerId, skuCode);
	}

	public List<RepeatOrderEntity> getCustomerActiveRepeatOrders(UUID customerId) {
		return repeatOrderRepository.findByCustomerId(customerId);
	}

	public Page<RepeatOrderEntity> getRepeatOrdersByDeliveryDate(Date deliveryDate, Pageable pageable) {
		return repeatOrderRepository.findByNextDeliveryDateAndStatus(new java.sql.Date(deliveryDate.getTime()), OrderConstants.RepeatOrderStatus.ACTIVE,
				pageable);
	}

	public void updateNextDeliveryDate(RepeatOrderEntity repeatOrder) {
		Date nextDeliveryDate = DateUtils.getDate(String.valueOf(findNextDeliveryDate(repeatOrder.getPreferences().getDays())));
		repeatOrder.setNextDeliveryDate(nextDeliveryDate);
	}

	public void bulkUpdateNextDeliveryDate(List<RepeatOrderEntity> repeatOrders) {
		if (CollectionUtils.isEmpty(repeatOrders)) {
			return;
		}
		repeatOrders.forEach(this::updateNextDeliveryDate);
		saveAll(repeatOrders);
	}

	public void saveAll(List<RepeatOrderEntity> repeatOrders) {
		repeatOrderRepository.saveAll(repeatOrders);
	}

	public String getUserStore(UUID customerId) {
		List<UserAddressResponse> userAddresses;
		try {
			userAddresses = clientService.getUserAddressByUserId(customerId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching user addresses", e);
			return null;
		}
		if (CollectionUtils.isEmpty(userAddresses)) {
			return null;
		}
		UserAddressResponse userAddress = userAddresses.get(0);
		SocietyListItemBean society;
		try {
			society = clientService.getSocietyById(userAddress.getSocietyId());
		} catch (Exception e) {
			_LOGGER.error("Error while fetching society by ID", e);
			return null;
		}
		if (society == null) {
			return null;
		}
		return society.getStoreId();
	}

}
