package com.sorted.rest.services.order.services;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.DiscountType;
import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.entity.OrderItemEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created by mohit on 06.07.20.
 */
@Service
public class PricingService {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private ClientService clientService;

	AppLogger _LOGGER = LoggingManager.getLogger(PricingService.class);

	@Transactional(propagation = Propagation.REQUIRED)
	public void setAmountAndTaxesInOrderAndItem(OrderEntity order, OrderItemEntity item, CoinsParamsObject coinsParamsObject) {
		String voucherCode = order.getOfferData().getVoucherCode();
		setAmountAndTaxesInOrderAndItem(order, item, voucherCode, coinsParamsObject);
	}

	private void setOrderOffers(OrderEntity order, OfferResponse offer) {
		order.getOfferData().setIsOfferApplied(true);
		order.getOfferData().setOfferId(offer.getOfferDetails().getOffer().getId().toString());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void setAmountAndTaxesInOrderAndItem(OrderEntity order, OrderItemEntity item, String voucherCode, CoinsParamsObject coinsParamsObject) {
		setAmountAndTaxesInItem(item, null, coinsParamsObject, order);
		setAmountAndTaxesInOrder(order, null, coinsParamsObject);
	}

	private OfferClientApplyOfferRequest createOfferRequest(OrderEntity order) {
		OfferClientOrderBean offerClientOrderBean = new OfferClientOrderBean(order);
		offerClientOrderBean.setOrderCount(order.getMetadata().getOrderCount().longValue());
		String voucherCode = order.getOfferData().getVoucherCode();
		OfferClientApplyOfferRequest req = new OfferClientApplyOfferRequest();
		req.setOrder(offerClientOrderBean);
		req.setCode(voucherCode);
		return req;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void setAmountAndTaxesInOrderAndItems(OrderEntity order, CoinsParamsObject coinsParamsObject) {
		if (order != null && CollectionUtils.isNotEmpty(order.getOrderItems())) {
			List<OrderItemEntity> items = order.getOrderItems();
			OrderEntity finalOrder = order;
			items.stream().forEach(i -> setAmountAndTaxesInItem(i, null, coinsParamsObject, finalOrder));
		}
		setAmountAndTaxesInOrder(order, null, coinsParamsObject);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity setAmountAndTaxesInOrder(OrderEntity order, OfferResponse offer, CoinsParamsObject coinsParamsObject) {
		if (order != null) {
			List<OrderItemEntity> items = order.getOrderItems();

			BigDecimal totalMrpGrossAmount = BigDecimal.ZERO;
			BigDecimal totalSpGrossAmount = BigDecimal.ZERO;

			BigDecimal finalWalletBillAmount = BigDecimal.ZERO;
			BigDecimal finalCoinsAmount = BigDecimal.ZERO;
			BigDecimal coinsGiven = BigDecimal.ZERO;

			BigDecimal totalTaxAmount = BigDecimal.ZERO;
			BigDecimal totalExtraFeeAmount = BigDecimal.ZERO;

			BigDecimal igstAmount = BigDecimal.ZERO;
			BigDecimal cgstAmount = BigDecimal.ZERO;
			BigDecimal sgstAmount = BigDecimal.ZERO;
			BigDecimal cessAmount = BigDecimal.ZERO;

			BigDecimal orderLeveldeduction = BigDecimal.ZERO;

			BigDecimal itemLeveldeduction = BigDecimal.ZERO;

			if (CollectionUtils.isNotEmpty(order.getOrderItems())) {
				for (OrderItemEntity item : items) {
					totalMrpGrossAmount = totalMrpGrossAmount.add(BigDecimal.valueOf(item.getMrpGrossAmount()));
					totalSpGrossAmount = totalSpGrossAmount.add(BigDecimal.valueOf(item.getSpGrossAmount()));
					igstAmount = igstAmount.add(item.getTaxDetails().getIgstAmount());
					sgstAmount = sgstAmount.add(item.getTaxDetails().getSgstAmount());
					cgstAmount = cgstAmount.add(item.getTaxDetails().getCgstAmount());
					cessAmount = cessAmount.add(item.getTaxDetails().getCessAmount());

					if (coinsParamsObject != null && coinsParamsObject.getIsOtpEnabled() == 1) {
						if (item.getIsCoinsRedeemedItem() == 0) {
							finalWalletBillAmount = finalWalletBillAmount.add(BigDecimal.valueOf(item.getFinalAmount()));
							coinsGiven = coinsGiven.add(BigDecimal.valueOf(item.getItemCoinsEarned()));
						} else {
							finalCoinsAmount = finalCoinsAmount.add(BigDecimal.valueOf(item.getFinalItemBillCoins()));
						}
					} else {
						finalWalletBillAmount = finalWalletBillAmount.add(BigDecimal.valueOf(item.getFinalAmount()));
					}
					itemLeveldeduction = itemLeveldeduction.add(BigDecimal.valueOf(item.getDiscountAmount()));
				}
				totalTaxAmount = igstAmount.add(sgstAmount).add(cgstAmount).add(cessAmount);
			}
			order.getTotalAdditionalDiscount().setItemLevelDiscount(itemLeveldeduction.doubleValue());
			checkAndSetExtraChargesAndDiscounts(order, finalWalletBillAmount);
			totalExtraFeeAmount = totalExtraFeeAmount.add(BigDecimal.valueOf(order.getExtraFeeDetails().getDeliveryCharge()));
			totalExtraFeeAmount = totalExtraFeeAmount.add(BigDecimal.valueOf(order.getExtraFeeDetails().getSlotCharges()));
			totalExtraFeeAmount = totalExtraFeeAmount.add(BigDecimal.valueOf(order.getExtraFeeDetails().getOzoneWashingCharge()));
			order.setTotalExtraFeeAmount(totalExtraFeeAmount.doubleValue());

			order.setTotalMrpGrossAmount(totalMrpGrossAmount.doubleValue());
			order.setTotalSpGrossAmount(totalSpGrossAmount.doubleValue());

			//orderLeveldeduction = calculateOrderLevelDeduction(offer, totalSpGrossAmount).max(BigDecimal.ZERO);
			BigDecimal offerDiscount = BigDecimal.ZERO;
			offerDiscount = calculateOfferDiscount(order, orderLeveldeduction, totalSpGrossAmount);
			BigDecimal totalDiscountAmount = setScale(offerDiscount.add(BigDecimal.valueOf(order.getTotalAdditionalDiscount().getDeliveryDiscount())));
			//			Double finalBillAmount = totalSpGrossAmount.doubleValue() + totalExtraFeeAmount - totalDiscountAmount;
			Double finalBillAmount = setScale(finalWalletBillAmount.add(totalExtraFeeAmount)).doubleValue();
			if (BigDecimal.valueOf(finalBillAmount).compareTo(totalDiscountAmount) <= 0) {
				if (itemLeveldeduction.compareTo(BigDecimal.valueOf(finalBillAmount)) >= 0) {
					totalDiscountAmount = itemLeveldeduction;
					order.setTotalDiscountAmount(setScale(totalDiscountAmount).doubleValue());
					order.getTotalAdditionalDiscount().setOfferDiscount(0d);
					order.getTotalAdditionalDiscount().setDeliveryDiscount(0d);
				} else {
					offerDiscount = BigDecimal.valueOf(Math.min(finalBillAmount, offerDiscount.doubleValue()))
							.subtract(BigDecimal.valueOf(order.getTotalAdditionalDiscount().getDeliveryDiscount())).max(BigDecimal.ZERO);
					order.getTotalAdditionalDiscount().setOfferDiscount(setScale(offerDiscount).doubleValue());
					totalDiscountAmount = setScale(offerDiscount.add(BigDecimal.valueOf(order.getTotalAdditionalDiscount().getDeliveryDiscount())));
					order.setTotalDiscountAmount(setScale(totalDiscountAmount.add(itemLeveldeduction)).doubleValue());
				}
				finalBillAmount = 0d;
			} else {
				finalBillAmount = setScale(finalWalletBillAmount.add(totalExtraFeeAmount).subtract(totalDiscountAmount)).setScale(0, RoundingMode.HALF_UP)
						.doubleValue();
				order.setTotalDiscountAmount(setScale(totalDiscountAmount.add(itemLeveldeduction)).doubleValue());
				order.getTotalAdditionalDiscount().setOfferDiscount(setScale(offerDiscount).doubleValue());
			}
			setProrataAmount(order, totalDiscountAmount);
			order.setFinalBillAmount(finalBillAmount);
			setOrderLevelCashback(order, totalSpGrossAmount);
			order.setFinalBillCoins(finalCoinsAmount.doubleValue());
			order.setCartCoinsEarned(coinsGiven.doubleValue());

			if (order.getStatus().equals(OrderStatus.IN_CART) || order.getStatus().equals(OrderStatus.NEW_ORDER)) {
				order.setEstimatedBillAmount(finalBillAmount);
			}
			Double roundedTaxAmount = setScale(totalTaxAmount).doubleValue();
			order.getTaxDetails().setCgstAmount(cgstAmount);
			order.getTaxDetails().setIgstAmount(igstAmount);
			order.getTaxDetails().setSgstAmount(sgstAmount);
			order.getTaxDetails().setCessAmount(cessAmount);
			order.setTotalTaxAmount(roundedTaxAmount);

			order.setItemCount(items.size());
			order = orderService.save(order);
		}
		return order;
	}

	private BigDecimal setScale(BigDecimal value) {
		return value.setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal calculateOrderLevelDeduction(OfferResponse offer, BigDecimal totalSpGrossAmount) {
		BigDecimal orderLeveldeduction = BigDecimal.ZERO;
		if (offer != null && offer.getOrderLevel() != null) {
			if (offer.getOrderLevel().getMaxDiscountAmount() != null) {
				orderLeveldeduction = BigDecimal.valueOf(offer.getOrderLevel().getMaxDiscountAmount());
			}
			if (DiscountType.FLAT.equals(offer.getOrderLevel().getDiscountType())) {
				orderLeveldeduction = BigDecimal.valueOf(offer.getOrderLevel().getDiscountValue());
			} else {
				orderLeveldeduction = orderLeveldeduction.min(
						totalSpGrossAmount.multiply(BigDecimal.valueOf(offer.getOrderLevel().getDiscountValue()))
								.divide(new BigDecimal(100), RoundingMode.HALF_UP)
				);
			}
			orderLeveldeduction = orderLeveldeduction.max(BigDecimal.ZERO);
		}
		return orderLeveldeduction;
	}

	private BigDecimal calculateOfferDiscount(OrderEntity order, BigDecimal orderLevelDeduction, BigDecimal totalSpGrossAmount) {
		BigDecimal totalOfferDiscount = BigDecimal.ZERO;
		if (order.getOfferData() != null && order.getOfferData().getOfferType() != null && !OrderConstants.OfferType.ORDER_DISCOUNT_OFFER.equals(
				OrderConstants.OfferType.valueOf(order.getOfferData().getOfferType()))) {
			return totalOfferDiscount;
		}

		if (order.getStatus().equals(OrderStatus.IN_CART)) {
			totalOfferDiscount = calculateInCartDiscount(order, orderLevelDeduction, totalSpGrossAmount);
		} else {
			totalOfferDiscount = calculateOtherDiscount(order, totalSpGrossAmount);
		}
		return setScale(totalOfferDiscount);
	}

	private BigDecimal calculateInCartDiscount(OrderEntity order, BigDecimal orderLevelDeduction, BigDecimal totalSpGrossAmount) {
		BigDecimal totalDiscountAmount = orderLevelDeduction.doubleValue() > 0 ? orderLevelDeduction : BigDecimal.ZERO;
		if (totalDiscountAmount.equals(BigDecimal.ZERO) && order.getOfferData() != null && order.getOfferData().getOrderDiscount() != null
				&& order.getOfferData().getOrderDiscount() > 0) {
			totalDiscountAmount = calculateOfferDiscount(order, totalSpGrossAmount);
		}
		return totalDiscountAmount;
	}

	private BigDecimal calculateOtherDiscount(OrderEntity order, BigDecimal totalSpGrossAmount) {
		BigDecimal offerDiscount = BigDecimal.ZERO;
		if (order.getOfferData() != null && order.getOfferData().getOrderDiscount() != null && order.getOfferData().getOrderDiscount() > 0) {
			offerDiscount = calculateOfferDiscount(order, totalSpGrossAmount);
		}
		return offerDiscount;
	}

	private BigDecimal calculateOfferDiscount(OrderEntity order, BigDecimal totalSpGrossAmount) {
		BigDecimal offerDiscount = BigDecimal.ZERO;
		OfferData offerData = order.getOfferData();

		if (offerData.getConstraint() != null && offerData.getConstraint().getMaxDiscount() != null) {
			if (order.getStatus().equals(OrderStatus.IN_CART) && offerData.getConstraint().getMinCartValue().compareTo(totalSpGrossAmount.doubleValue()) > 0) {
				return offerDiscount;
			}
			offerDiscount = BigDecimal.valueOf(offerData.getConstraint().getMaxDiscount());
		}
		if (DiscountType.FLAT.equals(offerData.getDiscountType())) {
			offerDiscount = BigDecimal.valueOf(offerData.getOrderDiscount());
		} else if (DiscountType.PERCENT.equals(offerData.getDiscountType())) {
			offerDiscount = offerDiscount.min(
					totalSpGrossAmount.multiply(BigDecimal.valueOf(offerData.getOrderDiscount())).divide(new BigDecimal(100), RoundingMode.HALF_UP));
		}
		return offerDiscount;
	}

	private void setProrataAmount(OrderEntity order, BigDecimal totalOfferDiscountAmount) {
		if (order != null && totalOfferDiscountAmount != null) {
			List<OrderItemEntity> items = order.getOrderItems();
			Double discountValue = totalOfferDiscountAmount.doubleValue();
			Double beforeDiscount = order.getTotalSpGrossAmount();
			Double fraction = 0d;
			if (beforeDiscount.compareTo(0d) > 0) {
				fraction = discountValue / beforeDiscount;
			}
			if (CollectionUtils.isNotEmpty(order.getOrderItems())) {
				for (OrderItemEntity item : items) {
					item.setProrataAmount(item.getFinalAmount() - item.getFinalAmount() * fraction);
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderItemEntity setAmountAndTaxesInItem(OrderItemEntity item, OfferResponse offer, CoinsParamsObject coinsParamsObject, OrderEntity order) {
		item.setMarkedPrice(item.getMarkedPrice().max(item.getSalePrice()));
		final BigDecimal mrpGrossAmount = item.getMarkedPrice().multiply(BigDecimal.valueOf(item.getFinalQuantity())).setScale(0, RoundingMode.HALF_UP);
		final BigDecimal spGrossAmount = item.getSalePrice().multiply(BigDecimal.valueOf(item.getFinalQuantity())).setScale(0, RoundingMode.HALF_UP);

		BigDecimal finalCoins = BigDecimal.ZERO;
		BigDecimal coinsGiven = BigDecimal.ZERO;

		if (coinsParamsObject != null && coinsParamsObject.getIsOtpEnabled() == 1) {
			if (item.getIsCoinsRedeemedItem() == 0) {
				coinsGiven = spGrossAmount.multiply(BigDecimal.valueOf(coinsParamsObject.getCoinsGivenRatio()));
			} else {
				finalCoins = spGrossAmount.multiply(BigDecimal.valueOf(coinsParamsObject.getSpToCoinsRatio()));
			}
		}

		// Calculate Tax
		final BigDecimal taxRate = BigDecimal.valueOf(item.getTaxDetails().getCgst() + item.getTaxDetails().getSgst() + item.getTaxDetails().getCessgst());
		final BigDecimal igstAmount = spGrossAmount.multiply(BigDecimal.valueOf(item.getTaxDetails().getIgst())).divide(BigDecimal.valueOf(100).add(taxRate), 2,
				RoundingMode.HALF_UP);
		final BigDecimal cgstAmount = spGrossAmount.multiply(BigDecimal.valueOf(item.getTaxDetails().getCgst())).divide(BigDecimal.valueOf(100).add(taxRate), 2,
				RoundingMode.HALF_UP);
		final BigDecimal sgstAmount = spGrossAmount.multiply(BigDecimal.valueOf(item.getTaxDetails().getSgst())).divide(BigDecimal.valueOf(100).add(taxRate), 2,
				RoundingMode.HALF_UP);
		final BigDecimal cessAmount = spGrossAmount.multiply(BigDecimal.valueOf(item.getTaxDetails().getCessgst())).divide(BigDecimal.valueOf(100).add(taxRate),
				2, RoundingMode.HALF_UP);
		final BigDecimal taxAmount = spGrossAmount.subtract(igstAmount.add(sgstAmount).add(cgstAmount).add(cessAmount));

		BigDecimal deduction = BigDecimal.ZERO;
		if (offer != null) {
			if (offer.getSkuLevel() != null && !offer.getSkuLevel().isEmpty()) {
				for (SkuLevelOffer offerItem : offer.getSkuLevel()) {
					if (Objects.equals(offerItem.getSkuCode(), item.getSkuCode())) {
						if (offerItem.discountType == DiscountType.FLAT) {
							deduction = BigDecimal.valueOf(offerItem.getDiscountValue());
						} else {
							deduction = spGrossAmount.multiply(BigDecimal.valueOf(offerItem.getDiscountValue())).divide(new BigDecimal(100));
						}
						break;
					}
				}
			}
			if (offer.getOrderLevel() != null) {
				item.setDiscountAmount(0d);
			}
		}
		if (order != null && order.getOfferData() != null && order.getOfferData().getOrderDiscount() != null && order.getOfferData().getOrderDiscount() > 0) {
			item.setDiscountAmount(0d);
		}
		BigDecimal offerDiscountAmount = BigDecimal.ZERO;
		if (deduction.doubleValue() > 0 && item.getOrder().getStatus().equals(OrderStatus.IN_CART)) {
			offerDiscountAmount = deduction;
			item.getAdditionalDiscount().setOfferDiscount(offerDiscountAmount.doubleValue());
		} else {
			offerDiscountAmount = BigDecimal.valueOf(item.getAdditionalDiscount().getOfferDiscount());
		}
		BigDecimal finalAmount = BigDecimal.ZERO;
		if (coinsParamsObject == null || coinsParamsObject.getIsOtpEnabled() == 0 || item.getIsCoinsRedeemedItem() == 0) {
			finalAmount = spGrossAmount.subtract(offerDiscountAmount).max(BigDecimal.ZERO);
		}
		BigDecimal discount = mrpGrossAmount.subtract(finalAmount);
		item.setDiscountAmount(discount.doubleValue());
		double roundedFinalAmount = finalAmount.setScale(0, RoundingMode.HALF_UP).doubleValue();
		item.setFinalAmount(roundedFinalAmount);
		if (item.getOrder().getStatus().equals(OrderStatus.IN_CART)) {
			if(item.getMetadata().getIsOrderOfferItem()) {
				item.getMetadata().setOrderedFinalAmount(roundedFinalAmount);
			} else {
				item.getMetadata().setOrderedFinalAmount(mrpGrossAmount.setScale(0, RoundingMode.HALF_UP).doubleValue());
			}
		}
		item.setProrataAmount(roundedFinalAmount);

		item.setMrpGrossAmount(mrpGrossAmount.doubleValue());
		item.setSpGrossAmount(spGrossAmount.doubleValue());

		item.setFinalItemBillCoins(finalCoins.doubleValue());
		item.setItemCoinsEarned(coinsGiven.doubleValue());

		item.getTaxDetails().setCgstAmount(cgstAmount);
		item.getTaxDetails().setIgstAmount(igstAmount);
		item.getTaxDetails().setSgstAmount(sgstAmount);
		item.getTaxDetails().setCessAmount(cessAmount);
		double roundedTaxAmount = taxAmount.setScale(0, RoundingMode.HALF_UP).doubleValue();
		item.setTaxAmount(roundedTaxAmount);

		item = orderItemService.save(item);
		return item;
	}

	private void checkAndSetExtraChargesAndDiscounts(OrderEntity order, BigDecimal finalBillAmount) {
		if (!order.getStatus().equals(OrderStatus.IN_CART)) {
			_LOGGER.debug(String.format("Delivery Charges are not applicable for store id %s, status is %s", order.getStoreId(), order.getStatus()));
			return;
		}
		ConsumerDeliveryCharges deliveryCharges = orderService.getConsumerDeliveryCharges();
		Double deliveryCharge = 0d;
		if (finalBillAmount.doubleValue() >= deliveryCharges.getChargeLowerLimit() && finalBillAmount.doubleValue() < deliveryCharges.getChargeUpperLimit()) {
//			deliveryCharge = interpolateLinear(finalBillAmount.doubleValue(), deliveryCharges.getChargeLowerLimit(), deliveryCharges.getChargeUpperLimit(), 0d,
//					deliveryCharges.getChargeLimit());
			deliveryCharge = deliveryCharges.getChargeLimit();
		}
		double deliveryDiscount = 0d;
		if (finalBillAmount.doubleValue() >= deliveryCharges.getDiscountLowerLimit()) {
			deliveryDiscount = Math.min(interpolateLinear(finalBillAmount.doubleValue(), deliveryCharges.getDiscountLowerLimit(),
					deliveryCharges.getDiscountUpperLimit(), 0d, deliveryCharges.getDiscountLimit()), deliveryCharges.getDiscountLimit());
		}
		setOzoneWashingCharges(order);
		order.getExtraFeeDetails().setDeliveryCharge(deliveryCharge);
		order.getTotalAdditionalDiscount().setDeliveryDiscount(deliveryDiscount);
	}

	private void setOzoneWashingCharges(OrderEntity order) {
		double totalOzoneWashingCharges = 0d;
		if (CollectionUtils.isNotEmpty(order.getOrderItems())) {
			for (OrderItemEntity oi : order.getOrderItems()) {
				if (oi.getMetadata().getIsOzoneWashedItem() != null && oi.getMetadata().getIsOzoneWashedItem()) {
					totalOzoneWashingCharges += oi.getMetadata().getOzoneWashingCharge();
				}
			}
		}
		order.getExtraFeeDetails().setOzoneWashingCharge(totalOzoneWashingCharges);
	}

	private Double interpolateLinear(Double x, Double x1, Double x2, Double y1, Double y2) {
		return y1 + (y2 - y1) * ((x - x1) / (x2 - x1));
	}

	public void setOrderLevelCashback(OrderEntity cart, BigDecimal totalSpGrossAmount) {
		OfferData offerData = cart.getOfferData();
		if (offerData != null && offerData.getOfferType() != null
				&& OrderConstants.OfferType.CASHBACK_OFFER.equals(OrderConstants.OfferType.valueOf(offerData.getOfferType()))
				&& offerData.getIsOfferApplied()) {
			BigDecimal cashBackAmount = calculateOfferDiscount(cart, totalSpGrossAmount);
			BigDecimal finalBillAmount = BigDecimal.valueOf(cart.getFinalBillAmount());
			if (cashBackAmount.compareTo(finalBillAmount) > 0) {
				cashBackAmount = finalBillAmount;
			}
			cart.getMetadata().setIsCashbackApplicable(true);
			cart.getMetadata().setCashbackAmount(setScale(cashBackAmount).doubleValue());
		}
	}
}