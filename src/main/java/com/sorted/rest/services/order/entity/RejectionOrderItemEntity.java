package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.WarehouseInventoryResponseBean;
import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = RejectionOrderConstants.REJECTION_ORDER_ITEMS_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RejectionOrderItemEntity extends BaseEntity {

	private static final long serialVersionUID = 8485511358572467824L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;

	@Column(name = "order_id", updatable = false, insertable = false)
	private UUID orderId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private RejectionOrderEntity order;

	@Column(nullable = false)
	private String skuCode;

	@Column(nullable = false)
	private String productName;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private String categoryName;

	@Column(nullable = false)
	private Double orderedQty;

	@Column(nullable = false)
	private Double finalQuantity;

	@Column(nullable = false)
	private BigDecimal salePrice;

	@Column(nullable = false)
	private BigDecimal markedPrice;

	@Column(nullable = false)
	private Double mrpGrossAmount = 0.0;

	@Column(nullable = false)
	private Double spGrossAmount = 0.0;

	@Column(nullable = false)
	private Double discountAmount = 0.0;

	@Column(nullable = false)
	private Double taxAmount = 0.0;

	@Column(nullable = false)
	private Double finalAmount = 0.0;

	@Column(nullable = false)
	private RejectionOrderConstants.RejectionOrderItemStatus status = RejectionOrderConstants.RejectionOrderItemStatus.PENDING;

	@Transient
	private ErrorBean error;

	@Column
	private String uom;

	@Column
	private String hsn;

	public static RejectionOrderItemEntity newInstance() {
		RejectionOrderItemEntity entity = new RejectionOrderItemEntity();
		return entity;
	}

	public static RejectionOrderItemEntity newRejectionOrderItem(RejectionOrderEntity rejectionOrder, WarehouseInventoryResponseBean whItem) {
		final RejectionOrderItemEntity entity = RejectionOrderItemEntity.newInstance();
		entity.setHsn(whItem.getHsn());
		entity.setOrder(rejectionOrder);
		entity.setSkuCode(whItem.getSkuCode());
		entity.setProductName(whItem.getName());
		entity.setImageUrl(whItem.getImage());
		entity.setUom(whItem.getUnit_of_measurement());
		entity.setCategoryName(whItem.getCategory());
		entity.setMarkedPrice(BigDecimal.valueOf(0));
		entity.setSalePrice(BigDecimal.valueOf(0));
		rejectionOrder.addOrderItem(entity);
		return entity;
	}
}
