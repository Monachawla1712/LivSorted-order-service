package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ClevertapEventResponse {

	@JsonProperty("status")
	private String status;

	@JsonProperty("processed")
	private Integer processed;

	@JsonProperty("unprocessed")
	private List<Object> unprocessed;

	static class ClevertapEventFailed {
		@JsonProperty("status")
		private String status;
		@JsonProperty("code")
		private String code;
		@JsonProperty("error")
		private String error;
		@JsonProperty("record")
		private Object record;
	}

}