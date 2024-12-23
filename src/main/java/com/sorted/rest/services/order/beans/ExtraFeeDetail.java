package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

@Data
public class ExtraFeeDetail implements Serializable {

	private static final long serialVersionUID = -8555500226457969536L;

	private Double deliveryCharge = 0.0;

	private Double packingCharge = 0.0;

	private Double slotCharges = 0.0;

	private Double ozoneWashingCharge = 0.0;

}
