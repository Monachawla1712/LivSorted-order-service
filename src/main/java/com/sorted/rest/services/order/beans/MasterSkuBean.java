package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class MasterSkuBean implements Serializable {

	private String skuCode;

	private String name;
}
