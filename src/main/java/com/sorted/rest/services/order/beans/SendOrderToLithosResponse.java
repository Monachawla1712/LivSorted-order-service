package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class SendOrderToLithosResponse implements Serializable {

	private static final long serialVersionUID = 5540219994460380044L;

	@JsonProperty("success")
	private Boolean success;

}
