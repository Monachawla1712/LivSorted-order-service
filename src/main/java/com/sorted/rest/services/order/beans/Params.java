package com.sorted.rest.services.order.beans;

import ch.qos.logback.core.joran.conditional.Condition;
import com.sorted.rest.services.order.constants.OrderConstants.DiscountType;
import lombok.Data;

import java.io.Serializable;

@Data
public class Params implements Serializable {

	private static final long serialVersionUID = 3170450918802833248L;

	private DiscountType discountType;

	private Double discountValue;

}
