package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class InvoiceAdjustmentsBean implements Serializable {
	private String adjustmentTxnType;

	private BigDecimal amount;
}
