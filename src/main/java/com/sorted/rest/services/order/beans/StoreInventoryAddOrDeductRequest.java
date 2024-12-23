package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class StoreInventoryAddOrDeductRequest implements Serializable {

	private static final long serialVersionUID = 8949302740350182245L;

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
