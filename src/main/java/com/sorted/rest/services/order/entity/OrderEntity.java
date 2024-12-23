package com.sorted.rest.services.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;
import lombok.Data;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author mohit
 */
@Entity
@Table(name = OrderConstants.ORDERS_TABLE_NAME)
@DynamicUpdate
@Data
public class OrderEntity extends BaseEntity implements OrderEntityConstants{

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;

	@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinColumn(name = "display_order_id", referencedColumnName = "display_order_id", insertable = false, updatable = false)
	private InvoiceEntity invoice;

	@Column
	private UUID customerId;

	@Column(name = "display_order_id")
	private String displayOrderId;

	@Column
	private String storeId;

	@Column
	private String storeDeviceId;

	@Column
	private Double totalMrpGrossAmount = 0.0d;

	@Column
	private Double totalSpGrossAmount = 0.0d;

	@Column
	private Double totalDiscountAmount = 0.0d;

	@Column
	private Double totalTaxAmount = 0.0d;

	@Column
	private Double totalExtraFeeAmount = 0.0d;

	@Column
	private Double refundAmount = 0.0d;

	@Column
	private Double estimatedBillAmount = 0.0d;

	@Column
	private Double finalBillAmount = 0.0d;

	@Column
	private Double amountReceived = 0.0d;

	@Column
	private Double finalBillCoins = 0.0d;

	@Column
	private Double cartCoinsEarned = 0.0d;

	@Column
	private Double coinsReceived = 0.0d;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private TaxDetails taxDetails = new TaxDetails();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private AdditionalDiscount totalAdditionalDiscount = new AdditionalDiscount();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private ExtraFeeDetail extraFeeDetails = new ExtraFeeDetail();

	@Column
	private Integer itemCount;

	@Column
	private String channel;

	@Column
	private ShippingMethod shippingMethod;

	@Column
	private PaymentMethod paymentMethod;

	@Where(clause = "active = 1")
	@OrderBy(clause = "created_at ASC")
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<OrderItemEntity> orderItems;

	@Column
	private OrderStatus status = OrderStatus.IN_CART;

	@Column
	private Long deliveryAddress;

	@Column
	private String notes;

	@Column
	private Integer isRefunded = 0;

	@Column
	private Date submittedAt;

	@Column
	private Date appRequestTime;

	@Column
	private Long lithosOrderId;

	@Column
	private Integer lithosSynced = 0;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OrderMetadata metadata = new OrderMetadata();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OfferData offerData = new OfferData();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private PaymentDetail paymentDetail = new PaymentDetail();

	@Column
	private Date deliveryDate;

	@Column
	private UUID parentOrderId;

	@Column
	private Integer isRefundOrder = 0;

	@Column
	private Integer slotId;

	@Column
	private String key;

	@Column
	private Integer hasPpdOrder = 0;

	@Transient
	private Double walletAmount;

	@Transient
	private ErrorBean error;

	public void addOrderItem(OrderItemEntity orderItem) {
		if (CollectionUtils.isEmpty(orderItems)) {
			orderItems = new ArrayList<>();
		}
		orderItems.add(orderItem);
	}

	public static OrderEntity newInstance() {
		OrderEntity entity = new OrderEntity();
		return entity;
	}

	public static OrderEntity createNewCart(UUID customerId, Long addressId, String storeId, String channel, OrderContactDetail contactDetail,
			Location location, long time, String displayOrderId, Double storeDistance, List<Double> coordinates, String zoneId, Boolean isPosRequest,
			Date deliveryDate, Integer slotId) {
		final OrderEntity entity = newInstance();
		entity.setDisplayOrderId("OD-" + displayOrderId);
		entity.setCustomerId(customerId);
		entity.setDeliveryAddress(addressId);
		entity.setStoreId(storeId);
		entity.setItemCount(0);
		entity.setChannel(channel);
		entity.setStatus(OrderStatus.IN_CART);
		entity.setSlotId(slotId);

		entity.getTaxDetails().setIgst(0d);
		entity.getTaxDetails().setCgst(0d);
		entity.getTaxDetails().setSgst(0d);
		entity.getTaxDetails().setCessgst(0d);

		entity.getMetadata().setContactDetail(contactDetail);
		entity.getMetadata().setLocation(location);
		entity.getMetadata().setStoreDistance(storeDistance);
		entity.getMetadata().setZoneId(zoneId);

		setStoreLocation(entity, coordinates);

		entity.setAppRequestTime(new Date(time));
		entity.setDeliveryDate(deliveryDate);

		return entity;
	}

	public static void setStoreLocation(OrderEntity order, List<Double> coordinates) {
		if (coordinates != null) {
			Location storeLocation = new Location();
			storeLocation.setLongitude(String.valueOf(coordinates.get(0)));
			storeLocation.setLatitude(String.valueOf(coordinates.get(1)));
			order.getMetadata().setStoreLocation(storeLocation);
		}
	}

	public static OrderEntity createNewRefundOrder(OrderEntity parentOrder, String displayOrderId, String key) {
		final OrderEntity entity = newInstance();
		entity.setParentOrderId(parentOrder.getId());
		entity.setIsRefundOrder(1);

		entity.setDisplayOrderId("OD-" + displayOrderId);
		entity.setCustomerId(parentOrder.getCustomerId());
		entity.setDeliveryAddress(parentOrder.getDeliveryAddress());
		entity.setStoreId(parentOrder.getStoreId());
		entity.setItemCount(0);
		entity.setChannel(parentOrder.getChannel());
		entity.setStatus(OrderStatus.ORDER_REFUNDED);
		entity.setDeliveryDate(parentOrder.getDeliveryDate());
		entity.setSlotId(parentOrder.getSlotId());

		entity.getTaxDetails().setIgst(0d);
		entity.getTaxDetails().setCgst(0d);
		entity.getTaxDetails().setSgst(0d);
		entity.getTaxDetails().setCessgst(0d);

		entity.getMetadata().setContactDetail(parentOrder.getMetadata().getContactDetail());
		entity.getMetadata().setLocation(parentOrder.getMetadata().getLocation());
		entity.getMetadata().setOrderSlot(parentOrder.getMetadata().getOrderSlot());
		entity.setKey(key);
		return entity;
	}

}