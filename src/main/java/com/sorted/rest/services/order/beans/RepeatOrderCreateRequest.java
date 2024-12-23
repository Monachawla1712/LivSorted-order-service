package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class RepeatOrderCreateRequest implements Serializable {

	private static final long serialVersionUID = 6439357294784511065L;

	@NotNull
	private String skuCode;

	@NotNull
	private RepeatOrderPreferences preferences;

}
