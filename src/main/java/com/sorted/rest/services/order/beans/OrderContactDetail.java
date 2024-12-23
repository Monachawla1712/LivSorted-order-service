package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

@Data
public class OrderContactDetail implements Serializable {

	private static final long serialVersionUID = -772465085619543410L;
	private String name;
	private String phone;
}
