package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.PREBOOKED_ORDERS)
@DynamicUpdate
@Data
public class PreBookOrderEntity extends BaseEntity {

	private static final long serialVersionUID = -7473715611338661006L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;

	@Column(nullable = false)
	private String customerId;

	@Column(nullable = false)
	private String orderId;

	@Column(nullable = false)
	private String orderItemId;

	@Column(nullable = false)
	private String skuCode;

	@Column(nullable = false)
	private Date prebookDeliveryDate;

	public static PreBookOrderEntity newInstance(){
		return new PreBookOrderEntity();
	}
}
