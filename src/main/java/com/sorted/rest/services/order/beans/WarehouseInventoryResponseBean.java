package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class WarehouseInventoryResponseBean implements Serializable {

	@JsonProperty("id")
	private Integer id;

	@JsonProperty("skuCode")
	private String skuCode;

	@JsonProperty("name")
	private String name;

	@JsonProperty("image")
	private String image;

	@JsonProperty("unit_of_measurement")
	private String unit_of_measurement;

	@JsonProperty("category")
	private String category;

	@JsonProperty("hsn")
	private String hsn;

	@JsonProperty("whId")
	private Integer whId;

	@JsonProperty("holdQty")
	private Double holdQty;

	@JsonProperty("crateQty")
	private Double crateQty;

	@JsonProperty("crateCount")
	private Integer crateCount;

	@JsonProperty("rejectedQty")
	private Double rejectedQty;

	@JsonProperty("wastageQty")
	private Double wastageQty;

	@JsonProperty("storeReturnQty")
	private Double storeReturnQty;

	@JsonProperty("day")
	private String day;
}
