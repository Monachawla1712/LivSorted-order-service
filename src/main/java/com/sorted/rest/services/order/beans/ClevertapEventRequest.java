package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClevertapEventRequest {

	private List<ClevertapEventData> d;

	@Data
	public static class ClevertapEventData {

		Map<String, Object> evtData;

		private String identity;

		private Long ts;

		private String type;

		private String evtName;

		Map<String, Object> profileData;

	}

}
