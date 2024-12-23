package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Conditions implements Serializable {

	private static final long serialVersionUID = -4237934772330299550L;

	private List<Condition> all;
}
