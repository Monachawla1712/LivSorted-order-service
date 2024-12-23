package com.sorted.rest.services.order.beans;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ConsumerAddressResponse {

	@JsonProperty("id")
	private Integer id;

	@JsonProperty("user_id")
	private UUID userId;

	@JsonProperty("name")
	private String name;

	@JsonProperty("type")
	private String type;

	@JsonProperty("lat")
	private Double latitude;

	@JsonProperty("long")
	private Double longitude;

	@JsonProperty("is_default")
	private Object isDefault;

	@JsonProperty("address_line_1")
	private String addressLine1;

	@JsonProperty("address_line_2")
	private String addressLine2;

	@JsonProperty("landmark")
	private String landmark;

	@JsonProperty("city")
	private String city;

	@JsonProperty("state")
	private String state;

	@JsonProperty("street")
	private String street;

	@JsonProperty("pincode")
	private Integer pincode;

	@JsonProperty("is_active")
	private Boolean isActive;

	@JsonProperty("contact_number")
	private String contactNumber;

	@JsonProperty("society_id")
	private Integer societyId;

	@JsonProperty("floor")
	private String floor;

	@JsonProperty("house")
	private String house;

	@JsonProperty("society")
	private String society;

	@JsonProperty("sector")
	private String sector;

	@JsonProperty("tower")
	private String tower;

}
