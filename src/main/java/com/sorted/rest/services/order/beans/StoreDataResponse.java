package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
public class StoreDataResponse {

	@JsonProperty("name")
	private String name;

	@JsonProperty("open_time")
	private String openTime;

	@JsonProperty("store_id")
	private String storeId;

	@JsonProperty("is_open")
	private Boolean isOpen;

	@JsonProperty("location")
	private Location location;

	@JsonProperty("distance")
	private Double distance;

	@JsonProperty("store_found")
	private Boolean storeFound;

	@JsonProperty("zoneId")
	private String zoneId;

	@JsonProperty("city")
	private String city;

	@JsonProperty("ownerId")
	private UUID ownerId;

	@JsonProperty("addressLine_1")
	private String addressLine1;

	@JsonProperty("addressLine_2")
	private String addressLine2;

	@Data
	@NoArgsConstructor
	public static class Location {

		@JsonProperty("coordinates")
		private List<Double> coordinates = null;
	}

}
