package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class FetchBulkSupportOrdersRequest {

	@NotEmpty
	private List<String> userIds;

}
