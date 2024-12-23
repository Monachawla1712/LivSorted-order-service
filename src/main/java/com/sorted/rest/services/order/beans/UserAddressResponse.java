package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddressResponse implements Serializable {

	private static final long serialVersionUID = 2120168687740657002L;

	private Long id;

	@JsonProperty("society_id")
	private Integer societyId;

}
