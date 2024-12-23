package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.ExtraFeeDetail;
import com.sorted.rest.services.order.beans.FranchiseOrderMetadata;
import com.sorted.rest.services.order.beans.OfferData;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.SRType;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.FRANCHISE_ORDERS_TABLE_NAME)
@DynamicUpdate
@Data
public class FranchiseOrderEntity extends BaseEntity implements OrderEntityConstants {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;

	@Column
	private UUID customerId;

	@Column
	private String storeId;

	@Column(name = "display_order_id")
	private String displayOrderId;

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
	private Double finalBillAmount = 0.0d;

	@Column
	private Double amountReceived = 0.0d;

	@Column
	private Integer itemCount = 0;

	@Where(clause = "active = 1")
	@OrderBy(clause = "created_at ASC")
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<FranchiseOrderItemEntity> orderItems;

	@Column
	private FranchiseOrderConstants.FranchiseOrderStatus status = FranchiseOrderConstants.FranchiseOrderStatus.IN_CART;

	@Column
	private String notes;

	@Column
	private Integer isRefunded = 0;

	@Column
	private Date submittedAt;

	@Column
	private UUID parentOrderId;

	@Column
	private Date deliveryDate;

	@Column
	private Double estimatedBillAmount = 0.0d;

	@Transient
	private ErrorBean error;

	@Column
	private Integer isSrpStore;

	@Column
	private Integer isSrUploaded = 0;

	@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinColumn(name = "display_order_id", referencedColumnName = "display_order_id", insertable = false, updatable = false)
	private InvoiceEntity invoice;

	@Column
	private String challanUrl;

	@Column
	private Integer isRefundOrder = 0;

	@Column
	private String refundType;

	@Column
	private String slot;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OfferData offerData = new OfferData();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private FranchiseOrderMetadata metadata = new FranchiseOrderMetadata();

	@Column
	private String key;

	@Transient
	private Boolean isOfferRemoved = false;

	@Transient
	private Double effectiveSpGrossAmountForCashback;

	@Transient
	private Double spGrossAmountWithoutBulkSkus;

	@Transient
	private Boolean hasPendingRefundTicket;

	@Transient
	private Double walletAmount;

	@Column
	@Enumerated(EnumType.ORDINAL)
	private SRType type = SRType.NORMAL;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private ExtraFeeDetail extraFeeDetails = new ExtraFeeDetail();

	public void addOrderItem(FranchiseOrderItemEntity orderItem) {
		if (orderItems == null) {
			orderItems = new ArrayList<FranchiseOrderItemEntity>();
		}
		orderItems.add(orderItem);
	}

	public static FranchiseOrderEntity newInstance() {
		FranchiseOrderEntity entity = new FranchiseOrderEntity();
		return entity;
	}

	public static FranchiseOrderEntity createNewCart(UUID customerId, String storeId, String displayOrderId, Integer isSrpStore, Boolean isBackofficeCart) {
		final FranchiseOrderEntity entity = newInstance();
		entity.setDisplayOrderId("OF-" + displayOrderId);
		entity.setCustomerId(customerId);
		entity.setStoreId(storeId);
		entity.setItemCount(0);
		entity.setStatus(FranchiseOrderConstants.FranchiseOrderStatus.IN_CART);
		if (isBackofficeCart) {
			entity.setDeliveryDate(deliveryDateForBackoffice());
			entity.setSlot(slotForBackoffice());
		} else {
			entity.setDeliveryDate(deliveryDateForApp());
			entity.setSlot(slotForApp());
		}
		entity.setIsSrpStore(isSrpStore);
		return entity;
	}

	public static FranchiseOrderEntity createNewRefundOrder(FranchiseOrderEntity parentOrder, String displayOrderId, String returnIssue) {
		final FranchiseOrderEntity entity = newInstance();
		entity.setParentOrderId(parentOrder.getId());
		entity.setIsRefundOrder(1);
		entity.setDisplayOrderId("OF-" + displayOrderId);
		entity.setCustomerId(parentOrder.getCustomerId());
		entity.setStoreId(parentOrder.getStoreId());
		entity.setItemCount(0);
		entity.setIsSrpStore(parentOrder.getIsSrpStore());
		entity.setStatus(FranchiseOrderConstants.FranchiseOrderStatus.REFUND_REQUESTED);
		entity.setRefundType(returnIssue);
		entity.setDeliveryDate(parentOrder.getDeliveryDate());
		return entity;
	}

	static private Date deliveryDateForApp() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String start = ParamsUtils.getParam("NEXT_DAY_DELIVERY_SLOT_START_TIME", "14:00:00");
		if (localTime.isAfter(LocalTime.parse(start))) {
			return localDate.plusDays(1).toDate();
		} else {
			return localDate.toDate();
		}
	}

	static private String slotForApp() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String slot;
		String start = ParamsUtils.getParam("MORNING_7_AM_SLOT_START_TIME", "14:00:00");
		if (localTime.isAfter(LocalTime.parse(start))) {
			slot = ParamsUtils.getParam("MORNING_7_AM_SLOT", "MORNING_7_AM");
		} else {
			slot = ParamsUtils.getParam("NOON_12_PM_SLOT", "NOON_12_PM");
		}
		return slot;
	}

	static private Date deliveryDateForBackoffice() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String start = ParamsUtils.getParam("NEXT_DAY_DELIVERY_SLOT_START_TIME_BACKOFFICE", "15:00:00");
		if (localTime.isAfter(LocalTime.parse(start))) {
			return localDate.plusDays(1).toDate();
		} else {
			return localDate.toDate();
		}
	}

	static private String slotForBackoffice() {
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalTime localTime = localDate.toLocalTime();
		String slot;
		String start = ParamsUtils.getParam("MORNING_7_AM_SLOT_START_TIME_BACKOFFICE", "15:00:00");
		if (localTime.isBefore(LocalTime.parse("03:00:00")) || localTime.isAfter(LocalTime.parse(start))) {
			slot = ParamsUtils.getParam("MORNING_7_AM_SLOT", "MORNING_7_AM");
		} else {
			slot = ParamsUtils.getParam("NOON_12_PM_SLOT", "NOON_12_PM");
		}
		return slot;
	}
}
