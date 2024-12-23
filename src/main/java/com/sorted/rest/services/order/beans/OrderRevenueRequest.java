package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;


@Data
public class OrderRevenueRequest {

	@NotNull
	private List<String> storeIds;

	@NotEmpty
	private String startDate;

}
