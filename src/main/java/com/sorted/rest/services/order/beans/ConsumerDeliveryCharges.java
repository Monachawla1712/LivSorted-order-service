package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ConsumerDeliveryCharges implements Serializable {

	private static final long serialVersionUID = 6591657119039117441L;

	private Double chargeLowerLimit;

	private Double chargeUpperLimit;

	private Double chargeLimit;

	private Double discountLowerLimit;

	private Double discountUpperLimit;

	private Double discountLimit;

}
