package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.FranchiseOrderItemMetadata;
import com.sorted.rest.services.order.beans.FranchiseOrderMetadata;
import com.sorted.rest.services.order.beans.FranchiseRefundDetails;
import com.sorted.rest.services.order.beans.FranchiseStoreInventoryResponse;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.FRANCHISE_ORDER_ITEMS_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FranchiseOrderItemEntity extends BaseEntity {

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
	private FranchiseOrderEntity order;

	@Column(nullable = false)
	private String skuCode;

	@Column(nullable = false)
	private Double moq;

	@Column(nullable = false)
	private Integer whId;

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
	private Double refundAmount = 0.0;

	@Column
	private Integer orderedCrateQty = 0;

	@Column
	private Integer finalCrateQty = 0;

	@Column(nullable = false)
	private Integer isRefundable = 1;

	@Column(nullable = false)
	private Integer isReturnable = 1;

	@Column(nullable = false)
	private FranchiseOrderConstants.FranchiseOrderItemStatus status = FranchiseOrderConstants.FranchiseOrderItemStatus.PENDING;

	@Column
	private Integer cratesPicked;

	@Column
	private Double weightPicked;

	@Transient
	private ErrorBean error;

	@Column
	private String uom;

	@Column
	private String hsn;

	@Column
	private Double marginDiscountPercent;

	@Column
	private Double prorataAmount;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private FranchiseRefundDetails refundDetails = new FranchiseRefundDetails();

	@Column
	private BigDecimal offerDiscountAmount;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private FranchiseOrderItemMetadata metadata = new FranchiseOrderItemMetadata();

	public static FranchiseOrderItemEntity newInstance() {
		FranchiseOrderItemEntity entity = new FranchiseOrderItemEntity();
		return entity;
	}

	public static FranchiseOrderItemEntity newCartItem(FranchiseOrderEntity storeCart, FranchiseStoreInventoryResponse storeItem, Integer quantity,
			Integer isSrpStore) {
		final FranchiseOrderItemEntity entity = newInstance();
		entity.setWhId(storeItem.getWhId());
		entity.setHsn(storeItem.getHsn());
		entity.setOrder(storeCart);
		entity.setMoq(storeItem.getMoq());
		entity.setSkuCode(storeItem.getSkuCode());
		entity.setProductName(storeItem.getName());
		entity.setImageUrl(storeItem.getImage());
		entity.setUom(storeItem.getUnit_of_measurement());
		entity.setFinalQuantity(storeItem.getMoq() * quantity);
		entity.setOrderedQty(storeItem.getMoq() * quantity);
		entity.setMarkedPrice(calcMarkedPrice(storeItem, isSrpStore));
		if (isSrpStore == 1) {
			entity.setSalePrice(BigDecimal.valueOf(storeItem.getSortedRetailPrice()));
			entity.setMarginDiscountPercent(storeItem.getMarginDiscount());
		} else {
			entity.setSalePrice(BigDecimal.valueOf(storeItem.getSalePrice()));
		}
		entity.setCategoryName(storeItem.getCategory());
		entity.setIsRefundable(1);
		entity.setIsReturnable(1);
		entity.setOrderedCrateQty(quantity);
		entity.setFinalCrateQty(quantity);
		return entity;
	}

	public static BigDecimal calcMarkedPrice(FranchiseStoreInventoryResponse storeItem, Integer isSrpStore) {
		BigDecimal markedPrice = null;
		if (storeItem.getMarkedPrice() != null) {
			markedPrice = BigDecimal.valueOf(storeItem.getMarkedPrice());
		} else if (isSrpStore == 1) {
			markedPrice = BigDecimal.valueOf(storeItem.getSortedRetailPrice());
		} else {
			markedPrice = BigDecimal.valueOf(storeItem.getSalePrice());
		}
		return markedPrice;
	}

	public static FranchiseOrderItemEntity createNewRefundOrderItem(FranchiseOrderEntity refundOrder, Double quantity, FranchiseOrderItemEntity parentOrderItem,
			FranchiseRefundDetails refundDetails) {
		final FranchiseOrderItemEntity entity = newInstance();
		entity.setOrder(refundOrder);
		entity.setOrderId(refundOrder.getId());

		entity.setSkuCode(parentOrderItem.getSkuCode());
		entity.setProductName(parentOrderItem.getProductName());
		entity.setImageUrl(parentOrderItem.getImageUrl());
		entity.setUom(parentOrderItem.getUom());
		entity.setMarginDiscountPercent(parentOrderItem.getMarginDiscountPercent());

		entity.setFinalQuantity(quantity);
		entity.setOrderedQty(quantity);
		entity.setOrderedCrateQty(0);
		entity.setFinalCrateQty(0);

		entity.setSalePrice(BigDecimal.valueOf(parentOrderItem.getProrataAmount() / parentOrderItem.getFinalQuantity()).setScale(2, RoundingMode.HALF_UP));
		entity.setMarkedPrice(parentOrderItem.getMarkedPrice());
		entity.setHsn(parentOrderItem.getHsn());

		entity.setCategoryName(parentOrderItem.getCategoryName());

		entity.setIsRefundable(1);
		entity.setIsReturnable(1);

		entity.setRefundDetails(refundDetails);

		return entity;
	}

}
