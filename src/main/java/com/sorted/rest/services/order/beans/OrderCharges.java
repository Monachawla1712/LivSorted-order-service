package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

@Data
public class OrderCharges implements Serializable {

	private static final long serialVersionUID = 6071357798367158591L;

	private String name;

	private Double amount;
}
