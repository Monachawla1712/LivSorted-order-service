package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class StoreInventoryUpdateResponse implements Serializable {

	private static final long serialVersionUID = 5540219994460380044L;

	@JsonProperty("success")
	private Boolean success;

	@JsonProperty("errors")
	private List<Error> errors = null;

	@Data
	public static class Error {

		@JsonProperty("sku_code")
		private String skuCode;

		@JsonProperty("code")
		private String code;

		@JsonProperty("max_quantity")
		private Double maxQuantity;
	}

}
