package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RejectionInventoryUpdateResponse implements Serializable {

	private static final long serialVersionUID = 5540219994460380044L;

	@JsonProperty("success")
	private Boolean success;

	@JsonProperty("errors")
	private List<Error> errors = null;

	@Data
	public static class Error {

		@JsonProperty("skuCode")
		private String skuCode;

		@JsonProperty("code")
		private String code;

		@JsonProperty("maxQuantity")
		private Double maxQuantity;
	}

}