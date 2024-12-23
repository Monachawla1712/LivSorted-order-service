package com.sorted.rest.services.order.services;

import com.sorted.rest.services.order.entity.RejectionOrderEntity;
import com.sorted.rest.services.order.entity.RejectionOrderItemEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RejectionOrderPricingService {

	@Autowired
	private RejectionOrderService rejectionOrderService;

	public void setAmountAndTaxesInOrderAndItem(RejectionOrderEntity order) {
		BigDecimal mrpGrossAmount;
		BigDecimal spGrossAmount;
		BigDecimal totalDiscount = BigDecimal.ZERO;

		Double igstAmount = 0d;
		Double cgstAmount = 0d;
		Double sgstAmount = 0d;
		Double cessAmount = 0d;
		Double totalTaxAmount = 0d;
		BigDecimal totalMrpGrossAmount = BigDecimal.ZERO;
		BigDecimal totalSpGrossAmount = BigDecimal.ZERO;
		Integer itemCount = 0;

		for (RejectionOrderItemEntity cartItem : order.getOrderItems()) {
			itemCount = itemCount + 1;
			totalTaxAmount = totalTaxAmount + igstAmount + cessAmount + sgstAmount + cgstAmount;
			mrpGrossAmount = BigDecimal.valueOf(cartItem.getFinalQuantity()).multiply(cartItem.getMarkedPrice());
			spGrossAmount = BigDecimal.valueOf(cartItem.getFinalQuantity()).multiply(cartItem.getSalePrice());
			cartItem.setSpGrossAmount(spGrossAmount.doubleValue());
			cartItem.setMrpGrossAmount(mrpGrossAmount.doubleValue());
			cartItem.setFinalAmount(spGrossAmount.doubleValue() - cartItem.getDiscountAmount());
			totalSpGrossAmount = spGrossAmount.add(totalSpGrossAmount);
			totalMrpGrossAmount = mrpGrossAmount.add(totalMrpGrossAmount);
		}

		order.setTotalMrpGrossAmount(totalMrpGrossAmount.doubleValue());
		order.setTotalSpGrossAmount(totalSpGrossAmount.doubleValue());
		BigDecimal finalBillAmount = totalSpGrossAmount.subtract(totalDiscount);

		order.setFinalBillAmount(finalBillAmount.doubleValue());
		order.setTotalTaxAmount(totalTaxAmount);
		order.setItemCount(itemCount);
		rejectionOrderService.saveRejectionOrder(order);
	}

}
