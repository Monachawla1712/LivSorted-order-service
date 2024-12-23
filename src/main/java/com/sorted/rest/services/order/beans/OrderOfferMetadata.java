package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class OrderOfferMetadata implements Serializable {

	private static final long serialVersionUID = -8692382018611300810L;

	@NotNull
	@NotEmpty
	private String quantityPerUnit;

	@NotNull
	@NotEmpty
	private List<String> terms;

	private List<Integer> audienceIds;

	private List<Integer> hideForAudienceIds;

	@NotNull
	@NotEmpty
	private String displayName;

	private Integer priority;
}
