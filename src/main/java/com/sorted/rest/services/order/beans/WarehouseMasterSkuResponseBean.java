package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class WarehouseMasterSkuResponseBean implements Serializable {

	private Integer id;

	private String skuCode;

	private String name;

	private String image;

	private String unit_of_measurement;

	private String category;

	private String hsn;
}
