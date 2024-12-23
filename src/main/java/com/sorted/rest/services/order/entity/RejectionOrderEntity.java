package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = RejectionOrderConstants.REJECTION_ORDER_TABLE_NAME)
@DynamicUpdate
@Data
public class RejectionOrderEntity extends BaseEntity {

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

	@Column
	private Integer whId;

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
	private Double finalBillAmount = 0.0d;

	@Column
	private Double amountReceived = 0.0d;

	@Column
	private Integer itemCount = 0;

	@Where(clause = "active = 1")
	@OrderBy(clause = "created_at ASC")
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<RejectionOrderItemEntity> orderItems;

	@Column
	private RejectionOrderConstants.RejectionOrderStatus status = RejectionOrderConstants.RejectionOrderStatus.NEW_ORDER;

	@Column
	private Date submittedAt;

	@Column
	private Date deliveryDate;

	@Column
	private Double estimatedBillAmount = 0.0d;

	@Column
	private RejectionOrderConstants.RejectionOrderType orderType;

	@Transient
	private ErrorBean error;

	public static RejectionOrderEntity newInstance() {
		RejectionOrderEntity entity = new RejectionOrderEntity();
		return entity;
	}

	public static RejectionOrderEntity createNewOrder(UUID customerId, String storeId, String displayOrderId, Integer whId,
			RejectionOrderConstants.RejectionOrderType orderType) {
		final RejectionOrderEntity entity = newInstance();
		entity.setDisplayOrderId("OR-" + displayOrderId);
		entity.setCustomerId(customerId);
		entity.setStoreId(storeId);
		entity.setItemCount(0);
		entity.setWhId(whId);
		entity.setStatus(RejectionOrderConstants.RejectionOrderStatus.NEW_ORDER);
		entity.setOrderType(orderType);
		return entity;
	}

	public void addOrderItem(RejectionOrderItemEntity orderItem) {
		if (orderItems == null) {
			orderItems = new ArrayList<RejectionOrderItemEntity>();
		}
		orderItems.add(orderItem);
	}

}
