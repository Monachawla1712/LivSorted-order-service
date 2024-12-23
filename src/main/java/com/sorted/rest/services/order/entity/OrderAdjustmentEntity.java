package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.OrderAdjustmentDetails;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.ORDER_ADJUSTMENTS_TABLE_NAME)
@DynamicUpdate
@Data
@Where(clause = "active = 1")
public class OrderAdjustmentEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(nullable = false)
	private String displayOrderId;

	@Column(nullable = false)
	private Double amount;

	@Column(nullable = false)
	private String txnMode;

	@Column(nullable = false)
	private String txnType;

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = false)
	private FranchiseOrderConstants.OrderAdjustmentStatus status;

	@Column(nullable = true)
	private UUID requestedBy;

	@Column(nullable = true)
	private UUID approvedBy;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OrderAdjustmentDetails adjustmentDetails = new OrderAdjustmentDetails();

	public static OrderAdjustmentEntity newInstance() {
		return new OrderAdjustmentEntity();
	}

	public static OrderAdjustmentEntity createOrderAdjustmentEntity(UUID requesterId, String displayOrderId, Double amount, String txnType, String remarks,
			OrderAdjustmentDetails adjustmentDetails) {
		OrderAdjustmentEntity orderAdjustment = newInstance();
		orderAdjustment.setRequestedBy(requesterId);
		orderAdjustment.setDisplayOrderId(displayOrderId);
		orderAdjustment.setStatus(FranchiseOrderConstants.OrderAdjustmentStatus.PENDING);
		orderAdjustment.setAmount(Math.abs(amount));
		orderAdjustment.setTxnType(txnType);
		if (amount < 0) {
			orderAdjustment.setTxnMode(FranchiseOrderConstants.OrderAdjustmentTransactionMode.DEBIT.toString());
		} else {
			orderAdjustment.setTxnMode(FranchiseOrderConstants.OrderAdjustmentTransactionMode.CREDIT.toString());
		}
		orderAdjustment.setRemarks(remarks);
		orderAdjustment.setAdjustmentDetails(adjustmentDetails);
		return orderAdjustment;
	}

}
