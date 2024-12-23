package com.sorted.rest.services.order.services;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FranchisePricingService {

	AppLogger _LOGGER = LoggingManager.getLogger(FranchisePricingService.class);

	@Autowired
	private ClientService clientService;

	@Autowired
	private FranchiseOrderService franchiseOrderService;

	@Transactional(propagation = Propagation.REQUIRED)
	public void setAmountAndTaxesInOrderAndItem(FranchiseOrderEntity order) {
		setAmountAndTaxesInOrderAndItems(order);
		if (order.getStatus().equals(FranchiseOrderConstants.FranchiseOrderStatus.IN_CART)) {
			checkAndSetOffer(order);
			setAmountAndTaxesInOrderAndItems(order);
		}
	}

	private void checkAndSetOffer(FranchiseOrderEntity order) {
		OfferClientApplyFranchiseOfferRequest offerRequest = buildFranchiseOfferRequestData(order);
		if (order.getIsOfferRemoved() != null && !order.getIsOfferRemoved() && order.getOfferData().getVoucherCode() == null && (order.getOfferData()
				.getHasAutoApplicableVoucher() == null || order.getOfferData().getHasAutoApplicableVoucher())) {
			FranchiseAutoApplyOfferResponse autoApplyCoupon = clientService.getAutoApplyCouponCode(offerRequest);
			if (autoApplyCoupon != null && autoApplyCoupon.getVoucherCode() != null) {
				order.getOfferData().setVoucherCode(autoApplyCoupon.getVoucherCode());
				offerRequest.setCode(autoApplyCoupon.getVoucherCode());
				order.getOfferData().setHasAutoApplicableVoucher(true);
			} else {
				order.getOfferData().setHasAutoApplicableVoucher(false);
			}
		}
		if (order.getOfferData().getVoucherCode() != null) {
			FranchiseOfferResponse offer = clientService.getFranchiseOfferResponse(offerRequest);
			if (offer != null) {
				setOrderOffers(order, offer);
			} else {
				removeOrderOffers(order);
			}
		}
	}

	private OfferClientApplyFranchiseOfferRequest buildFranchiseOfferRequestData(FranchiseOrderEntity order) {
		OfferClientApplyFranchiseOfferRequest offerRequest = new OfferClientApplyFranchiseOfferRequest();
		offerRequest.setCode(order.getOfferData().getVoucherCode());
		offerRequest.setStoreId(order.getStoreId());
		offerRequest.setOrderId(order.getId().toString());
		offerRequest.setOrder(new OfferClientFranchiseOrderBean(order));
		Long orderCount = franchiseOrderService.getDeliveredOrderCount(order.getStoreId());
		offerRequest.getOrder().setOrderCount(orderCount);
		FranchiseOrderEntity firstOrder = franchiseOrderService.getFirstOrder(order.getStoreId());
		if (firstOrder != null) {
			offerRequest.getOrder().setFirstOrderDate(firstOrder.getDeliveryDate());
		}
		franchiseOrderService.addSpGrossAmountWithoutBulkSkus(order);
		offerRequest.getOrder().setSpGrossAmountWithoutBulkSkus(order.getEffectiveSpGrossAmountForCashback());
		return offerRequest;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void setAmountAndTaxesInOrderAndItems(FranchiseOrderEntity order) {
		Map<String, Double> skuDiscountMap = getSkuDiscountMap(order.getOfferData());

		BigDecimal mrpGrossAmount;
		BigDecimal spGrossAmount;
		BigDecimal totalDiscount = BigDecimal.ZERO;
//		Double igstAmount = 0d;
//		Double cgstAmount = 0d;
//		Double sgstAmount = 0d;
//		Double cessAmount = 0d;
		BigDecimal totalTaxAmount = BigDecimal.ZERO;
		BigDecimal totalmrpGrossAmount = BigDecimal.ZERO;
		BigDecimal totalspGrossAmount = BigDecimal.ZERO;
		Integer itemCount = 0;
		BigDecimal totalMarginalDiscountAmount = BigDecimal.ZERO;
		BigDecimal totalOfferDiscountAmount = BigDecimal.ZERO;
		BigDecimal totalOrderItemOfferAmount = BigDecimal.ZERO;
		BigDecimal totalExtraFeeAmount = BigDecimal.ZERO;
		BigDecimal finalBillAmount = BigDecimal.ZERO;

		for (FranchiseOrderItemEntity cartItem : order.getOrderItems()) {
			itemCount = itemCount + 1;
			mrpGrossAmount = BigDecimal.valueOf(cartItem.getFinalQuantity()).multiply(cartItem.getMarkedPrice());
			spGrossAmount = BigDecimal.valueOf(cartItem.getFinalQuantity()).multiply(cartItem.getSalePrice());
			BigDecimal itemOfferDiscountAmount = BigDecimal.ZERO;
			// discount calculation
			BigDecimal itemTotalDiscount = BigDecimal.ZERO;
			BigDecimal itemMarginDiscount = BigDecimal.ZERO;
			if (order.getIsSrpStore() == 1) {
				itemMarginDiscount = BigDecimal.valueOf(cartItem.getMarginDiscountPercent()).multiply(spGrossAmount).divide(BigDecimal.valueOf(100));
				itemTotalDiscount = itemTotalDiscount.add(itemMarginDiscount);
				totalMarginalDiscountAmount = totalMarginalDiscountAmount.add(itemMarginDiscount);
			}
			if (mrpGrossAmount.compareTo(spGrossAmount) > 0) {
				itemTotalDiscount = itemTotalDiscount.add(mrpGrossAmount.subtract(spGrossAmount));
			}
			if (skuDiscountMap != null && skuDiscountMap.containsKey(cartItem.getSkuCode())) {
				Double finalItemOfferDiscount = Math.min(skuDiscountMap.get(cartItem.getSkuCode()), spGrossAmount.subtract(itemMarginDiscount).doubleValue());
				totalOrderItemOfferAmount = totalOrderItemOfferAmount.add(BigDecimal.valueOf(finalItemOfferDiscount));
				itemOfferDiscountAmount = BigDecimal.valueOf(finalItemOfferDiscount);
				itemTotalDiscount = itemTotalDiscount.add(itemOfferDiscountAmount);
			}
			cartItem.setDiscountAmount(itemTotalDiscount.doubleValue());
			totalDiscount = totalDiscount.add(itemTotalDiscount);
			totalOfferDiscountAmount = totalOfferDiscountAmount.add(itemOfferDiscountAmount);
			cartItem.setOfferDiscountAmount(itemOfferDiscountAmount);
			cartItem.setSpGrossAmount(spGrossAmount.doubleValue());
			cartItem.setMrpGrossAmount(mrpGrossAmount.doubleValue());
			cartItem.setFinalAmount(spGrossAmount.subtract(itemMarginDiscount).subtract(itemOfferDiscountAmount).doubleValue());
			cartItem.setProrataAmount(spGrossAmount.subtract(itemOfferDiscountAmount).doubleValue());
			totalspGrossAmount = spGrossAmount.add(totalspGrossAmount);
			totalmrpGrossAmount = mrpGrossAmount.add(totalmrpGrossAmount);
		}
		order.setTotalMrpGrossAmount(totalmrpGrossAmount.doubleValue());
		order.setTotalSpGrossAmount(totalspGrossAmount.doubleValue());
		finalBillAmount = totalspGrossAmount;
		if (order.getIsSrpStore() == 1) {
			finalBillAmount = totalspGrossAmount.subtract(totalMarginalDiscountAmount);
		}

		if (checkOfferApplicationForOrderLevel(order)) {
			Double finalOrderLevelDiscount = Math.min(order.getOfferData().getAmount(), finalBillAmount.doubleValue());
			order.getOfferData().setAmount(finalOrderLevelDiscount);
			totalOfferDiscountAmount = BigDecimal.valueOf(finalOrderLevelDiscount);
			totalDiscount = totalDiscount.add(totalOfferDiscountAmount);
			setProrataAmount(order, totalOfferDiscountAmount);
		} else if (checkOfferApplicationForOrderItemLevel(order, skuDiscountMap)) {
			order.getOfferData().setAmount(totalOrderItemOfferAmount.doubleValue());
		}
		order.setTotalTaxAmount(totalTaxAmount.doubleValue());

		order.setTotalDiscountAmount(totalDiscount.doubleValue());
		finalBillAmount = finalBillAmount.subtract(totalOfferDiscountAmount).add(totalTaxAmount);

		checkAndSetDeliveryCharges(order, finalBillAmount);
		totalExtraFeeAmount = totalExtraFeeAmount.add(BigDecimal.valueOf(order.getExtraFeeDetails().getDeliveryCharge()));
		order.setTotalExtraFeeAmount(totalExtraFeeAmount.doubleValue());

		finalBillAmount = finalBillAmount.add(totalExtraFeeAmount);
		order.setFinalBillAmount(finalBillAmount.setScale(0, RoundingMode.FLOOR).doubleValue());
		if (order.getStatus().equals(FranchiseOrderConstants.FranchiseOrderStatus.IN_CART)) {
			order.setEstimatedBillAmount(finalBillAmount.doubleValue());
		}
		order.setItemCount(itemCount);
		franchiseOrderService.saveFranchiseEntity(order);
	}

	private Map<String, Double> getSkuDiscountMap(OfferData offerData) {
		Map<String, Double> skuDiscountMap = null;
		if (offerData != null && Objects.equals(offerData.getOfferType(), "ORDERITEM")) {
			skuDiscountMap = new HashMap<>();
			if (offerData.getItemOfferAmounts() != null) {
				for (FranchiseOfferResponse.FranchiseSkuLevelOffer skuLevelOffer : offerData.getItemOfferAmounts()) {
					skuDiscountMap.put(skuLevelOffer.getSkuCode(), skuLevelOffer.getDiscountValue());
				}
			}
		}
		return skuDiscountMap;
	}

	private boolean checkOfferApplicationForOrderLevel(FranchiseOrderEntity order) {
		return order.getOfferData() != null && order.getOfferData().getAmount() != null && order.getOfferData().getIsOfferApplied() && Objects.equals(
				order.getOfferData().getOfferType(), "ORDER");
	}

	private boolean checkOfferApplicationForOrderItemLevel(FranchiseOrderEntity order, Map<String, Double> skuDiscountMap) {
		return order.getOfferData() != null && order.getOfferData().getIsOfferApplied() && Objects.equals(order.getOfferData().getOfferType(),
				"ORDERITEM") && order.getOfferData().getItemOfferAmounts() != null && skuDiscountMap != null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	void setProrataAmount(FranchiseOrderEntity order, BigDecimal totalOfferDiscountAmount) {
		if (order != null && totalOfferDiscountAmount != null) {
			List<FranchiseOrderItemEntity> items = order.getOrderItems();
			double discountValue = totalOfferDiscountAmount.doubleValue();
			double beforeDiscount = order.getTotalSpGrossAmount();
			double fraction = discountValue / beforeDiscount;
			if (CollectionUtils.isNotEmpty(order.getOrderItems())) {
				for (FranchiseOrderItemEntity item : items) {
					item.setProrataAmount(item.getFinalAmount() - item.getFinalAmount() * fraction);
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void overrideOrderFinalAmount(FranchiseOrderEntity order, Map<String, Double> skuCodeToFinalAmountMap) {
		BigDecimal finalAmount = BigDecimal.ZERO;
		for (FranchiseOrderItemEntity cartItem : order.getOrderItems()) {
			cartItem.setFinalAmount(skuCodeToFinalAmountMap.get(cartItem.getSkuCode()));
			finalAmount = finalAmount.add(BigDecimal.valueOf(cartItem.getFinalAmount()));
		}
		BigDecimal finalBillAmount = finalAmount;
		order.setFinalBillAmount(finalBillAmount.doubleValue());
		franchiseOrderService.saveFranchiseEntity(order);
	}

	private void setOrderOffers(FranchiseOrderEntity order, FranchiseOfferResponse offer) {
		order.getOfferData().setIsOfferApplied(true);
		order.getOfferData().setOfferType(offer.getOfferType());
		order.getOfferData().setOfferTitle(offer.getOfferTitle());
		order.getOfferData().setOfferId(offer.getOfferId());
		order.getOfferData().setAppliedBy(SessionUtils.getAuthUserId());
		order.getOfferData().setAppliedAt(franchiseOrderService.getIstTimeString());
		Double offerAmount = 0d;
		if (offer.getOrderLevel() != null && offer.getOrderLevel().getDiscountValue() != null) {
			offerAmount += offer.getOrderLevel().getDiscountValue();
			order.getOfferData().setAmount(offerAmount);
		} else if (offer.getSkuLevel() != null) {
			order.getOfferData().setItemOfferAmounts(offer.getSkuLevel());
			for (FranchiseOfferResponse.FranchiseSkuLevelOffer skuLevelOffer : offer.getSkuLevel()) {
				offerAmount += skuLevelOffer.getDiscountValue();
			}
			order.getOfferData().setAmount(offerAmount);
		} else if (offer.getCashbackDetails() != null) {
			order.getOfferData().setCashbackDetails(offer.getCashbackDetails());
		}
		_LOGGER.debug(String.format("OFFER APPLIED -> storeId : %s, offerAmount : %s, orderId  : %s", order.getStoreId(), offerAmount, order.getId()));
	}

	private void removeOrderOffers(FranchiseOrderEntity order) {
		_LOGGER.info(String.format("Voucher %s conditions failed, offer removed, storeId : %s", order.getOfferData().getVoucherCode(), order.getStoreId()));
		order.getOfferData().setIsOfferApplied(false);
		order.getOfferData().setOfferType(null);
		order.getOfferData().setAmount(null);
		order.getOfferData().setItemOfferAmounts(null);
		order.getOfferData().setCashbackDetails(null);
	}

	private void checkAndSetDeliveryCharges(FranchiseOrderEntity order, BigDecimal finalBillAmount) {
		if (!order.getStatus().equals(FranchiseOrderConstants.FranchiseOrderStatus.IN_CART)) {
			_LOGGER.info(String.format("Delivery Charges are not applicable for store id %s, status is %s", order.getStoreId(), order.getStatus()));
			return;
		}
		if (order.getWalletAmount() == null) {
			WalletBean walletBean = clientService.getStoreWallet(order.getStoreId());
			order.setWalletAmount(walletBean.getAmount());
		}
		if (order.getWalletAmount() != null && Double.compare(finalBillAmount.doubleValue(), order.getWalletAmount()) > 0 && !getSkipDeliveryChargesStoreList().contains(order.getStoreId())) {
			_LOGGER.info(String.format("Delivery Charges are getting applied for store id %s, status is %s", order.getStoreId(), order.getStatus()));
			order.getExtraFeeDetails().setDeliveryCharge(Double.valueOf(ParamsUtils.getIntegerParam("FRANCHISE_ORDER_DELIVERY_CHARGE", 200)));
		} else {
			_LOGGER.info(String.format("Delivery Charges are skipped/removed for store id %s, status is %s", order.getStoreId(), order.getStatus()));
			order.getExtraFeeDetails().setDeliveryCharge(0d);
		}
	}

	private Set<String> getSkipDeliveryChargesStoreList() {
		String skipDeliveryChargesStores = ParamsUtils.getParam("SKIP_DELIVERY_CHARGES_STORE_LIST", "5002");
		return Arrays.stream(skipDeliveryChargesStores.split(",")).collect(Collectors.toCollection(HashSet::new));
	}
}
