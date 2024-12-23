package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class TaxDetails implements Serializable {

	private static final long serialVersionUID = -8369031645403533698L;

	private Double cgst = 0d;

	private Double sgst = 0d;

	private Double igst = 0d;

	private Double cessgst = 0d;

	private BigDecimal cgstAmount = BigDecimal.ZERO;

	private BigDecimal sgstAmount = BigDecimal.ZERO;

	private BigDecimal igstAmount = BigDecimal.ZERO;

	private BigDecimal cessAmount = BigDecimal.ZERO;
}
