package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class InvoiceRefundQtyAmountBean implements Serializable {
	private BigDecimal totalQty;

	private BigDecimal totalAmt;
}
