package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class PartnerContent implements Serializable {

	private static final long serialVersionUID = 8400911358572467824L;

	private String name;

	private String prop;

	private String value;

}
