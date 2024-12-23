package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.OrderWithRepeatItemBean;
import com.sorted.rest.services.order.beans.PnRequest;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.RepeatOrderEntity;
import com.sorted.rest.services.order.services.CartService;
import com.sorted.rest.services.order.services.OrderService;
import com.sorted.rest.services.order.services.RepeatOrderService;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import io.swagger.annotations.Api;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Order Cron Services", description = "Manage crons related services.")

public class CronController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(OrderController.class);

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	ClientService clientService;

	@Autowired
	OrderService orderService;

	@Autowired
	RepeatOrderService repeatOrderService;

	@Autowired
	CartService cartService;

	@PostMapping("/orders/repeat/add-to-cart")
	public void addRepeatOrderToCart() {
		_LOGGER.info("autoAddRepeatOrdersToCart:: triggered");
		try {
			Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
			boolean isTomorrowHoliday = DeliveryDateUtils.isHoliday(deliveryDate, null);
			int page = 0;
			int size = ParamsUtils.getIntegerParam("REPEAT_ORDER_CRON_PAGE_SIZE", 100);
			Pageable pageable = PageRequest.of(page, size);
			Page<RepeatOrderEntity> repeatOrdersPage;

			List<RepeatOrderEntity> allProcessedOrders = new ArrayList<>();

			do {
				repeatOrdersPage = repeatOrderService.getRepeatOrdersByDeliveryDate(deliveryDate, pageable);
				List<RepeatOrderEntity> repeatOrders = repeatOrdersPage.getContent();

				if (CollectionUtils.isEmpty(repeatOrders)) {
					_LOGGER.info("No repeat orders found for tomorrow");
					return;
				}
				_LOGGER.info(String.format("Found %d repeat orders for tomorrow", repeatOrders.size()));
				for (RepeatOrderEntity repeatOrder : repeatOrders) {
					if (!isTomorrowHoliday) {
						cartService.addRepeatOrderToCart(repeatOrder);
					}
					allProcessedOrders.add(repeatOrder);
				}
				pageable = repeatOrdersPage.nextPageable();
			} while (repeatOrdersPage.hasNext());

			// set next delivery date for all processed orders
			repeatOrderService.bulkUpdateNextDeliveryDate(allProcessedOrders);
			_LOGGER.info("autoAddRepeatOrdersToCart:: completed");
		} catch (Exception e) {
			_LOGGER.error("Error occurred while adding repeat orders to cart", e);
		}
	}

	@PostMapping("/orders/repeat/notify")
	public void notifyCustomerToRechargeWallet() {
		_LOGGER.info("notifyLowWalletBalanceCarts:: started");
		try {
			int page = 0;
			int size = ParamsUtils.getIntegerParam("REPEAT_ORDER_CRON_PAGE_SIZE", 100);
			Pageable pageable = PageRequest.of(page, size);
			Page<OrderWithRepeatItemBean> cartsPage;
			Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
			do {
				cartsPage = orderService.getRepeatOrdersByDeliveryDate(deliveryDate, pageable);
				List<OrderWithRepeatItemBean> carts = cartsPage.getContent();
				if (CollectionUtils.isNotEmpty(carts)) {
					List<OrderWithRepeatItemBean> cartsWithLowWalletBalance = orderService.getLowWalletBalanceCarts(carts);
					if (CollectionUtils.isNotEmpty(cartsWithLowWalletBalance)) {
						_LOGGER.info(String.format("Found %d carts with low balance", cartsWithLowWalletBalance.size()));
						List<UUID> customerIds = cartsWithLowWalletBalance.stream().map(OrderWithRepeatItemBean::getCustomerId).collect(Collectors.toList());
						sendDoRechargePNToCustomers(customerIds);
					}
				}
				pageable = cartsPage.nextPageable();
			} while (cartsPage.hasNext());

			_LOGGER.info("notifyLowWalletBalanceCarts:: completed");
		} catch (Exception e) {
			_LOGGER.error("Error occurred while notifying customers to recharge wallet", e);
		}
	}

	private void sendDoRechargePNToCustomers(List<UUID> customerIds) {
		Map<String, String> fillers = new HashMap<>();
		List<PnRequest> requests = new ArrayList<>();
		LocalDateTime time = DateUtils.getLocalDateTimeIST();
		String templateName;

		if(time.toLocalTime().isBefore(LocalTime.of(12, 0))) {
			templateName = OrderConstants.RECHARGE_WALLET_BEFORE_12_PM_TEMPLATE_NAME;
		}
		else if (time.toLocalTime().isBefore(LocalTime.of(18, 0))) {
			templateName = OrderConstants.RECHARGE_WALLET_BEFORE_6_PM_TEMPLATE_NAME;
		} else {
			templateName = OrderConstants.RECHARGE_WALLET_AFTER_6_PM_TEMPLATE_NAME;
		}

		for (UUID customerId : customerIds) {
			PnRequest pnRequest = PnRequest.builder().userId(customerId.toString()).templateName(templateName).fillers(fillers)
					.build();
			requests.add(pnRequest);
		}
		CollectionUtils.batches(requests, 100).forEach(batch -> {
			clientService.sendPushNotifications(batch);
		});
	}

	@PostMapping("/orders/repeat/fail")
	public void failLowWalletBalanceCarts() {
		_LOGGER.info("failLowWalletBalanceCarts:: started");
		try {
			int page = 0;
			int size = ParamsUtils.getIntegerParam("REPEAT_ORDER_CRON_PAGE_SIZE", 100);
			Pageable pageable = PageRequest.of(page, size);
			Page<OrderWithRepeatItemBean> cartsPage;
			Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
			List<UUID> failOrderIds = new ArrayList<>();
			do {
				cartsPage = orderService.getRepeatOrdersByDeliveryDate(deliveryDate, pageable);
				List<OrderWithRepeatItemBean> carts = cartsPage.getContent();

				if (CollectionUtils.isNotEmpty(carts)) {
					List<OrderWithRepeatItemBean> cartsWithLowWalletBalance = orderService.getLowWalletBalanceCarts(carts);
					if (CollectionUtils.isNotEmpty(cartsWithLowWalletBalance)) {
						_LOGGER.info(String.format("Found %d carts with low balance", cartsWithLowWalletBalance.size()));
						failOrderIds.addAll(cartsWithLowWalletBalance.stream().map(OrderWithRepeatItemBean::getId).collect(Collectors.toList()));
					}
				}
				pageable = cartsPage.nextPageable();
			} while (cartsPage.hasNext());

			// fail carts with low wallet balance
			orderService.failRepeatOrdersWithLowBalance(failOrderIds);
			_LOGGER.info("failLowWalletBalanceCarts:: completed");
		} catch (Exception e) {
			_LOGGER.error("Error occurred while failing low wallet balance carts", e);
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
