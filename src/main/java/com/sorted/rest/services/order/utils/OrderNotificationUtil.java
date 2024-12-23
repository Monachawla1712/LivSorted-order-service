package com.sorted.rest.services.order.utils;

import org.springframework.stereotype.Component;

/**
 * <p>
 * OrderNotificationUtil class.
 * </p>
 *
 * @author mohit
 * @version $Id: $Id
 */
@Component
public class OrderNotificationUtil {
//
//	@Autowired
//	private PushService pushService;
//
//	public void sendOrderUpdateCommunication(OrderEntity order) {
//		List<OrderEntity> orders = new ArrayList<>();
//		orders.add(order);
//		sendOrderUpdateCommunication(orders);
//	}
//
//	public void sendOrderUpdateCommunication(List<OrderEntity> orders) {
//		List<PushRequestBean> pushBeans = orders.parallelStream().map(o -> {
//			return preparePushRequest(o);
//		}).collect(Collectors.toList());
//		sendNotifyPush(pushBeans);
//	}
//
//	public PushRequestBean preparePushRequest(OrderEntity order) {
//		final Map<String, Object> messageData = new HashMap<>();
//		messageData.put("ORDER_ID", order.getId());
//		messageData.put("CUSTOMER_NAME", order.getCustomerName());
//		messageData.put("STORE_NAME", order.getStoreName());
//
//		OrderConstants.ShippingMethod shipping = OrderConstants.ShippingMethod.shippingMethod(order.getShippingMethod());
//		String shippingMethod = "";
//		switch (shipping) {
//		case STORE_PICKUP:
//			shippingMethod = "STORE-PICKUP";
//			break;
//		case HOME_DELIVERY:
//			shippingMethod = "HOME-DELIVERY";
//			break;
//		default:
//			break;
//		}
//		messageData.put("SHIPPING_METHOD", shippingMethod);
//
//		final OrderConstants.OrderStatus status = OrderConstants.OrderStatus.orderStatus(order.getStatus());
//		String templateName = "";
//		Long userId = 0L;
//		switch (status) {
//		case NEW_ORDER:
//			templateName = "NEW-ORDER";
//			userId = order.getStoreId();
//			break;
//		case ORDER_CANCELLED_BY_STORE:
//			templateName = "ORDER-CANCELLED-BY-STORE";
//			userId = order.getCustomerId();
//			break;
//		case ORDER_CANCELLED_BY_CUSTOMER:
//			templateName = "ORDER-CANCELLED-BY-CUSTOMER";
//			userId = order.getStoreId();
//			break;
//		case ORDER_PACKED:
//			templateName = "ORDER-PACKED";
//			userId = order.getCustomerId();
//			break;
//		case READY_FOR_PICKUP:
//			templateName = "READY-FOR-PICKUP";
//			userId = order.getCustomerId();
//			break;
//		case ORDER_OUT_FOR_DELIVERY:
//			templateName = "ORDER-OUT-FOR-DELIVERY";
//			userId = order.getCustomerId();
//			break;
//		case ORDER_DELIVERED:
//			templateName = "ORDER-DELIVERED";
//			userId = order.getCustomerId();
//			break;
//		default:
//			break;
//		}
//		final PushRequestBean pushBean = new PushRequestBean();
//		pushBean.setTemplateName(templateName);
//		pushBean.setMessageData(messageData);
//		pushBean.setUserId(userId);
//		return pushBean;
//	}
//
//	private void sendNotifyPush(final List<PushRequestBean> pushBeans) {
//		CompletableFuture.runAsync(() -> {
//			pushService.send(pushBeans);
//		});
//	}

}
