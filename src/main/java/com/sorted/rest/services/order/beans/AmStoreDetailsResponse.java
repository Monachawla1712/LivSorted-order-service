package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AmStoreDetailsResponse {

	@JsonProperty("user")
	private UserServiceResponse user;

}