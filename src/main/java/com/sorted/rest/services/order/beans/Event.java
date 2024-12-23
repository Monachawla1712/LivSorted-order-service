package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class Event implements Serializable {

	private static final long serialVersionUID = 5318638832747520L;

	private String type;

	private Params params;
}