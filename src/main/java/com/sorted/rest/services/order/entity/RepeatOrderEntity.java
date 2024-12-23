package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.RepeatOrderPreferences;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.REPEAT_ORDER_TABLE_NAME)
@Data
@DynamicUpdate
public class RepeatOrderEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = -6126674656137647862L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(nullable = false)
	private UUID customerId;

	@Column(nullable = false)
	private String skuCode;

	@Type(type = "jsonb")
	@Column(nullable = false, columnDefinition = "jsonb")
	private RepeatOrderPreferences preferences;

	@Column(nullable = false)
	private Date nextDeliveryDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderConstants.RepeatOrderStatus status;

	public static RepeatOrderEntity createRepeatOrder(UUID customerId, String skuCode, RepeatOrderPreferences preferences, Date nextDeliveryDate) {
		RepeatOrderEntity repeatOrderEntity = newInstance();
		repeatOrderEntity.setCustomerId(customerId);
		repeatOrderEntity.setSkuCode(skuCode);
		repeatOrderEntity.setPreferences(preferences);
		repeatOrderEntity.setNextDeliveryDate(nextDeliveryDate);
		repeatOrderEntity.setStatus(OrderConstants.RepeatOrderStatus.ACTIVE);
		return repeatOrderEntity;
	}

	public static RepeatOrderEntity newInstance() {
		return new RepeatOrderEntity();
	}
}