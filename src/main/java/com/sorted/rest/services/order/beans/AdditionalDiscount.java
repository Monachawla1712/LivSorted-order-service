package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdditionalDiscount implements Serializable {

	private static final long serialVersionUID = 6591657119039117441L;

	private Double offerDiscount = 0d;

	private Double deliveryDiscount = 0d;

	private Double itemLevelDiscount = 0d;

}
