package com.sorted.rest.services.order.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.OrderConstants;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Abhishek
 *
 */
@Entity
@Table(name = OrderConstants.ORDER_EVENT_LOGS_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
public class OrderEventLogEntity extends BaseEntity {

	private static final long serialVersionUID = 8485511358572467824L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private UUID customerId;

	@Column(nullable = false)
	private String storeId;

	@Column(nullable = false)
	private OrderConstants.OrderStatus status = OrderConstants.OrderStatus.IN_CART;

	@Column
	private Long lithosOrderId;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private Object dataRequest;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private Object dataResponse;

	public static OrderEventLogEntity newInstance() {
		return new OrderEventLogEntity();
	}

	public static OrderEventLogEntity createLog(OrderEntity order, Object request, Object response) {
		final OrderEventLogEntity orderLog = newInstance();

		orderLog.setOrderId(order.getId());
		orderLog.setCustomerId(order.getCustomerId());
		orderLog.setLithosOrderId(order.getLithosOrderId());
		orderLog.setStatus(order.getStatus());
		orderLog.setStoreId(order.getStoreId());

		orderLog.setDataRequest(request);
		orderLog.setDataResponse(response);

		return orderLog;
	}
}