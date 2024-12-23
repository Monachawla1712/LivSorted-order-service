package com.sorted.rest.services.order.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.OrderSlotMetadata;
import com.sorted.rest.services.order.constants.OrderConstants;

import lombok.Data;

@Entity
@Table(name = OrderConstants.ORDER_SLOT_TABLE_NAME)
@DynamicUpdate
@Data
public class OrderSlotEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column
	private String slot;

	@Column
	private Integer availableCount = 0;

	@Column
	private Integer remainingCount = 0;

	@Column
	private Double fees;

	@Column
	private Integer priority;

	@Column
	private String eta;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OrderSlotMetadata metadata = new OrderSlotMetadata();
}
