package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreInventoryUpdateRequest implements Serializable {

	private static final long serialVersionUID = 8949302740350182245L;

	@JsonProperty("user_id")
	private UUID userId;

	private List<StoreInventoryUpdateData> data;

	@Data
	@AllArgsConstructor
	public static class StoreInventoryUpdateData {

		@JsonProperty("quantity")
		private Double quantity;

		@JsonProperty("sku_code")
		private String skuCode;
	}

}
