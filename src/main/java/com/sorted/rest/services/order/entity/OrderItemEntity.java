package com.sorted.rest.services.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.AdditionalDiscount;
import com.sorted.rest.services.order.beans.OrderItemMetadata;
import com.sorted.rest.services.order.beans.StoreInventoryResponse;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.InventoryPriceBracket;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.ProductTags;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.StoreProductInventory;
import com.sorted.rest.services.order.beans.TaxDetails;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.OrderItemStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author mohit
 */
@Entity
@Table(name = OrderConstants.ORDER_ITEMS_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
public class OrderItemEntity extends BaseEntity {

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
	@JsonBackReference
	private OrderEntity order;

	@Column(nullable = false)
	private String skuCode;

	@Column(nullable = false)
	private String productName;

	@Column(nullable = false)
	private String uom;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private Integer categoryId;

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

	@Column(name = "mrp_gross_amount", nullable = false)
	private Double mrpGrossAmount;

	@Column(name = "sp_gross_amount", nullable = false)
	private Double spGrossAmount;

	@Column
	private Double discountAmount = 0.0;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private AdditionalDiscount additionalDiscount = new AdditionalDiscount();

	@Column
	private Double taxAmount = 0.0;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private TaxDetails taxDetails = new TaxDetails();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<ProductTags> productTags = new ArrayList<>();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<InventoryPriceBracket> priceBracket = new ArrayList<>();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OrderItemMetadata metadata = new OrderItemMetadata();

	@Column
	private Double refundAmount;

	@Column(nullable = false)
	private Double finalAmount;

	@Column()
	private Double finalItemBillCoins = 0.0d;

	@Column()
	private Double itemCoinsEarned = 0.0d;

	@Column()
	private Integer isCoinsRedeemedItem = 0;

	@Column(nullable = false)
	private Integer isRefundable = 1;

	@Column(nullable = false)
	private Integer isReturnable = 1;

	@Column(nullable = false)
	private OrderItemStatus status = OrderItemStatus.PENDING;

	@Column()
	private Double prorataAmount;

	@Column()
	private String hsn;

	@Column()
	private Boolean isRepeatItem;

	@Transient
	private ErrorBean error;

	public static OrderItemEntity newInstance() {
		OrderItemEntity entity = new OrderItemEntity();
		return entity;
	}

	public static OrderItemEntity newCartItem(OrderEntity storeCart, StoreProductInventory storeItem, Double quantity, Boolean isRepeatItem) {
		final OrderItemEntity entity = newInstance();
		entity.setOrder(storeCart);

		entity.setSkuCode(storeItem.getInventorySkuCode());
		entity.setProductName(storeItem.getProductDisplayName());
		entity.setHsn(storeItem.getProductHsn() != null ? storeItem.getProductHsn() : "");
		entity.setImageUrl(storeItem.getProductImageUrl());
		entity.setUom(storeItem.getProductUnitOfMeasurement());
		entity.setProductTags(storeItem.getProductTags());

		entity.setFinalQuantity(quantity);
		entity.setOrderedQty(quantity);

		entity.setPriceBracket(storeItem.getInventoryPriceBrackets());
		entity.setSalePrice(calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(), quantity, entity));
		entity.setMarkedPrice(
				storeItem.getInventorySalePrice().compareTo(storeItem.getInventoryMarketPrice()) <= 0
				? BigDecimal.valueOf(storeItem.getInventoryMarketPrice()) : BigDecimal.valueOf(storeItem.getInventorySalePrice()));

		assignGstInfo(entity, storeItem);

		entity.setIsRepeatItem(isRepeatItem);
		entity.setCategoryId(storeItem.getProductCategoryId());
		if (storeItem.getProductConsumerContents() != null && storeItem.getProductConsumerContents().getClasses() != null
				&& !storeItem.getProductConsumerContents().getClasses().isEmpty()) {
			entity.setCategoryName(storeItem.getProductConsumerContents().getClasses().get(0));
		} else {
			entity.setCategoryName(storeItem.getCategoryName());
		}

		entity.setIsCoinsRedeemedItem(storeItem.getProductIsCoinsRedeemable());

		entity.setIsRefundable(1);
		entity.setIsReturnable(1);

		entity.getMetadata().setSuffix(storeItem.getProductPerPcsSuffix());
		entity.getMetadata().setPerPiecesWeight(storeItem.getPerPiecesWeight());
		entity.getMetadata().setIsComplimentary(storeItem.getIsComplimentary() != null ? storeItem.getIsComplimentary() : Boolean.FALSE);
		if (storeItem.getIsPreBook() != null) {
			entity.getMetadata().setIsPrebooked(storeItem.getIsPreBook());
			entity.getMetadata().setPrebookDeliveryDate(storeItem.getPreBookDate());
		}
		return entity;
	}

	public static BigDecimal calcSalePriceBracket(Double inventorySalePrice, List<InventoryPriceBracket> priceBrackets, Double quantity,
			OrderItemEntity cartItem) {
		BigDecimal newSalePrice = BigDecimal.valueOf(inventorySalePrice);
		if (CollectionUtils.isNotEmpty(priceBrackets)) {
			Optional<InventoryPriceBracket> salePriceBracketOpt = priceBrackets.stream()
					.filter(p -> p.getMin().compareTo(quantity) <= 0 && p.getMax().compareTo(quantity) > 0).findFirst();
			if (salePriceBracketOpt.isPresent()) {
				InventoryPriceBracket salePriceBracket = salePriceBracketOpt.get();
				if (cartItem != null) {
					cartItem.getMetadata().setDiscountPercentage(salePriceBracket.getDiscountPercentage());
				}
				return BigDecimal.valueOf(inventorySalePrice).multiply(
						BigDecimal.valueOf(1d - ((salePriceBracket.getDiscountPercentage() != null ? salePriceBracket.getDiscountPercentage() : 0d) / 100d)));
			}
		}
		return newSalePrice;
	}

	public static OrderItemEntity createNewRefundOrderItem(OrderEntity refundOrder, OrderItemEntity parentOrderItem, Double quantity) {
		final OrderItemEntity entity = newInstance();
		entity.setOrder(refundOrder);
		entity.setSkuCode(parentOrderItem.getSkuCode());
		entity.setHsn(parentOrderItem.getHsn());
		entity.setProductName(parentOrderItem.getProductName());
		entity.setStatus(OrderItemStatus.PACKED);
		entity.setImageUrl(parentOrderItem.getImageUrl());
		entity.setUom(parentOrderItem.getUom());
		entity.setProductTags(parentOrderItem.getProductTags());
		entity.setFinalQuantity(quantity);
		entity.setOrderedQty(quantity);
		entity.setPriceBracket(parentOrderItem.getPriceBracket());
		if (parentOrderItem.getProrataAmount() != null) {
			entity.setSalePrice(BigDecimal.valueOf(parentOrderItem.getProrataAmount() / parentOrderItem.getFinalQuantity()).setScale(2, RoundingMode.HALF_UP));
		} else {
			entity.setSalePrice(parentOrderItem.getSalePrice());
		} // check for total refund
		entity.setMarkedPrice(parentOrderItem.getMarkedPrice());
		entity.getTaxDetails().setIgst(parentOrderItem.getTaxDetails().getIgst());
		entity.getTaxDetails().setCgst(parentOrderItem.getTaxDetails().getCgst());
		entity.getTaxDetails().setSgst(parentOrderItem.getTaxDetails().getSgst());
		entity.getTaxDetails().setCessgst(parentOrderItem.getTaxDetails().getCessgst());
		entity.setCategoryId(parentOrderItem.getCategoryId());
		entity.setCategoryName(parentOrderItem.getCategoryName());
		entity.setIsCoinsRedeemedItem(parentOrderItem.getIsCoinsRedeemedItem());
		entity.setIsRefundable(0);
		entity.setIsReturnable(0);
		return entity;
	}

	public static void assignGstInfo(OrderItemEntity cartItem, StoreProductInventory storeItem) {
		StoreInventoryResponse.GstInfo productGst = storeItem.getProductGst();
		cartItem.getTaxDetails().setCgst(productGst != null ? productGst.getCgst() : 0);
		cartItem.getTaxDetails().setIgst(productGst != null ? productGst.getIgst() : 0);
		cartItem.getTaxDetails().setCessgst(productGst != null ? productGst.getCess() : 0);
		cartItem.getTaxDetails().setSgst(productGst != null ? productGst.getSgst() : 0);
	}

}