package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Ticket Metadata Bean")
@Data
public class TicketMetadataBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private ConsumerOrderDetailsBean consumerOrderDetails;

	@Data
	public static class ConsumerOrderDetailsBean implements Serializable {

		private static final long serialVersionUID = 2102504245219017738L;

		private Double totalRefundableAmount;

		private Double totalRefundAmount;

	}

	public static TicketMetadataBean newInstance() {
		return new TicketMetadataBean();
	}
}
